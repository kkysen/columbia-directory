package sen.khyber.columbia.directory

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.toFlowable
import okhttp3.OkHttpClient
import okhttp3.Request
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import sen.khyber.columbia.directory.ColumbiaDirectory.FetchContext
import java.util.logging.Logger

private fun fetchSamplePagesWithUnis(httpClient: OkHttpClient): Flowable<String> {
    return Flowable.just("http://0.0.0.0:8000/Downloads/unis.html")
            .map { Request.Builder().url(it) }
            .map { it.build() }
            .map { httpClient.callAsync(it) }
            .flatMapSingle { it.toSingle() }
            .map { it.body() }
            .map { it.string() }
}

private fun readWebPage(driver: WebDriver, url: String): Single<String> {
    driver.get(url)
    // TODO make this async
    return Single.just(driver.pageSource)
}

private const val baseUrl = "https://directory.columbia.edu/people/browse/"

private fun fetchPagesWithUnisAfterLoggedIn(driver: WebDriver): Flowable<String> {
    val numTabs = 5;
    return arrayOf("students", "facultyandstaff")
            .toFlowable()
            .map { "$baseUrl$it?filter.lnameFname=2&filter.initialLetter=" }
            .flatMapIterable {url -> ('A'..'Z').asIterable().map { "$url$it" } }
//            .flatMapSingle({ readWebPage(driver, it) }, false, numTabs)
            .concatMapSingle { readWebPage(driver, it) }
}

private fun logIn(driver: WebDriver): Completable {
    driver.get("${baseUrl}students")
    TODO()
}

private fun fetchPagesWithUnis(driver: WebDriver): Flowable<String> =
        Flowable.concat(
                logIn(driver).toFlowable(),
                fetchPagesWithUnisAfterLoggedIn(driver)
        )


private const val uniUrlRegex = "/people/uni\\?code=([a-z]+[0-9]+)"

private fun findUnisInPage(html: String): Iterable<String> {
    return uniUrlRegex
            .toRegex()
            .findAll(html)
            .map { it.groupValues[1] }
            .asIterable()
}

fun fetchUnis(context: FetchContext): Flowable<UNI> = context.webDriver
        .let { fetchPagesWithUnis(it) }
        .map { findUnisInPage(it) }
        .flatMap { it.toFlowable() }
        .map { UNI(it) }

fun main() {
    System.setProperty("webdriver.chrome.exe", "")
    val driver = ChromeDriver()
    val context = FetchContext(Logger.getLogger("fetchUnis"), ok, driver)
    fetchUnis(context).subscribe(::println)
}