package sen.khyber.columbia.directory

import io.reactivex.Flowable
import sen.khyber.async.toSingle
import sen.khyber.columbia.directory.ColumbiaDirectory.FetchContext
import sen.khyber.io.web.WebResponse
import sen.khyber.io.web.getAsync
import sen.khyber.rx.mapNotNull

private const val baseUrl = "https://directory.columbia.edu/people/uni?code="

fun fetchPersons(context: FetchContext, unis: Flowable<UNI>): Flowable<Person> {
    val logger = context.logger
    return unis
            .map { uni -> "$baseUrl${uni.value}" }
            .map { context.httpClient.getAsync(it) }
            .flatMapSingle { it.toSingle() }
            .map { WebResponse(it) }
            .map { it.document }
            .mapNotNull { logIfNull({ logger.warning { it.toString() } }, it, Person.parse) }
            .doOnNext { logger.info { it.toString() } }
            .filter { false }
}