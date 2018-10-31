package sen.khyber.columbia.directory

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.toFlowable
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.intellij.lang.annotations.Language
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.WebDriverWait
import sen.khyber.async.toSingle
import sen.khyber.columbia.directory.ColumbiaDirectory.FetchContext
import sen.khyber.io.FS
import sen.khyber.io.sanitized
import sen.khyber.io.web.PageTotals
import sen.khyber.io.web.WebResponse
import sen.khyber.io.web.callAsync
import sen.khyber.io.web.ok
import sen.khyber.io.web.readPaginatedRetrying
import sen.khyber.io.web.untilTrue
import java.nio.file.Files
import java.nio.file.StandardOpenOption.CREATE
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger

private fun fetchSamplePagesWithUnis(httpClient: OkHttpClient): Flowable<String> {
    return Flowable.just("http://0.0.0.0:8000/Downloads/unis.html")
            .map { Request.Builder().url(it) }
            .map { it.build() }
            .map { httpClient.callAsync(it) }
            .flatMapSingle { it.toSingle() }
            .map { WebResponse(it) }
            .map { it.string }
}

private data class Cookies(val cookies: Set<Cookie>) {
    
    val header: String get() = cookies.asSequence().map { "${it.name}=${it.value}" }.joinToString(";")
    
}

private const val baseUrl = "https://directory.columbia.edu/people/browse/"

fun saveResponse(response: WebResponse) {
    val dir = FS.Downloads.sanitized() / "unis"
    val url = response.urlString
    val path = dir / ("$url.html")
    val unis = findUnisInPage(response.string).toList()
    println("$url: ${unis.size}: $unis")
    val r = repeats(unis)
    if (r.isNotEmpty()) println(r)
    Files.write(path.path, arrayOf(
            url,
            response.string,
            "UNIs (${unis.size}): \n${unis.joinToString("\n")}"
    ).joinToString("\n\n").toByteArray(), CREATE)
}

private fun fetchPagesWithUnisAfterLoggedIn(context: FetchContext, sessionCookie: Cookies): Flowable<UNI> {
//    val letters = 'A'..'Z'
    val letters = 'Q'..'Q'
    val baseUrl = HttpUrl.get(baseUrl)
    return arrayOf("students", "facultyandstaff").sliceArray(0..1)
            .asSequence()
            .map { baseUrl.newBuilder().addPathSegment(it) }
            .map { it.addQueryParameter("lnameFname", "2") }
            .map { it.build() }
            .flatMap { url ->
                letters.asSequence().map {
                    url.newBuilder().addQueryParameter("filter.initialLetter", it.toString())
                }
            }
            .map { it.build() }
            .onEach { context.logger.info { it.toString() } }
            .map { Request.Builder().url(it) }
            .map { it.addHeader("Cookie", sessionCookie.header) }
            .map { it.build() }
            .toFlowable()
            .flatMap {
                context.httpClient.readPaginatedRetrying(
                        request = it,
                        findPageTotals = { findNumUnisAndPages(it.string) },
                        toItems = {
                            it
                                    .map { findUnisInPage(it.string) }
                                    .flatMap { it.toFlowable() }
                                    .map { UNI(it) }
                        },
                        firstPage = 0,
                        maxRetries = 100
                )
            }
}

@Language("RegExp")
private fun findNumUnisAndPages(html: String): PageTotals? = """([0-9]+) \| Page [0-9]+ of ([0-9]+)"""
        .replace(" ", "\\s+")
        .toRegex()
        .find(html)
        ?.let { it.groupValues.slice(1..2).map { Integer.parseInt(it) } }
        ?.let { PageTotals(it[1], it[0]) }

var numUnis = AtomicInteger(0)
val unis = mutableListOf<List<String>>()

private fun findUnisInPage(html: String): Iterable<String> = """/people/uni\?code=([a-z]+[0-9]+)"""
        .toRegex()
        .findAll(html)
        .map { it.groupValues[1] }
        .asIterable()
        .let {
            //            it.toList().size.let { println(it) }
            unis.add(it.toList())
            numUnis.addAndGet(it.toList().size)
            it
        }

private var cookies: Cookies? = null

private fun logIn(driver: WebDriver): Single<Cookies> = Single.fromCallable {
    if (cookies != null) {
        return@fromCallable cookies
    }
    val loginUrl = "${baseUrl}students"
    driver.get(loginUrl)
    driver.findElement(By.id("username")).sendKeys("ks3343")
    driver.findElement(By.id("password")).sendKeys("2779@columbia")
    WebDriverWait(driver, 1000)
            .untilTrue { it.currentUrl.startsWith(loginUrl) }
    cookies = Cookies(driver.manage().cookies)
    return@fromCallable cookies
    // TODO close WebDriver
}

fun fetchUnis(context: FetchContext): Flowable<UNI> = context.webDriver
        .let { logIn(it) }
        .toFlowable()
        .flatMap { fetchPagesWithUnisAfterLoggedIn(context, it) }

fun main() {
    saveResponse(WebResponse(ok.newCall(Request.Builder().url("http://google.com").build()).execute()))
    System.setProperty("webdriver.chrome.driver",
            "C:/Program Files/Wolfram Research/Mathematica/11.3/SystemFiles/Components/WebUnit/Resources/DriverBinaries/ChromeDriver/Windows/chromedriver.exe")
    val driver = ChromeDriver()
    val logger = Logger.getLogger("fetchUnis")
    logger.level = Level.WARNING
    val context = FetchContext(logger, ok, driver)
    
//    val trials = 100
//    val repeats = 2
//    val target = 342 //117
//
//    (1..trials)
//            .toFlowable()
//            .flatMapSingle {
//                (1..repeats)
//                        .toFlowable()
//                        .flatMap { fetchUnis(context) }
//                        .distinct()
//                        .count()
//                        .map { it.toInt() }
//            }
//            .filter { it != target }
//            .subscribe { println(it) }


    (1..1).toFlowable()
            .flatMap { fetchUnis(context) }
            .distinct()
            .map { it.value }
            .toList()
            .subscribe({
                println("${it.size}")
                println("numUnis = $numUnis")
                println("${it.size}: ${it.sorted()}")
                val u = unis.flatten()
                println("${u.size}: ${u.sorted()}")
                println("${u.toSet().size}: ${u.toSet().toList().sorted()}")
                println(it.toSet() == u.toSet())
                val repeats = repeats(u)
                println("${repeats.size}: ${repeats}")
            }, { it.printStackTrace() })

//    fetchSamplePagesWithUnis(ok).subscribe { println(findNumPages(it)) }
//    logIn(driver).blockingGet()
    
    println("End of Main")
}

fun <T> repeats(iterable: Iterable<T>): Map<T, Int> =
        iterable.groupingBy { it }.eachCount().filterValues { it > 1 }