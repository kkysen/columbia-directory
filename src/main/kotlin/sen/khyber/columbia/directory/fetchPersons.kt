package sen.khyber.columbia.directory

import io.reactivex.Flowable
import okhttp3.Request
import sen.khyber.columbia.directory.ColumbiaDirectory.FetchContext

private const val baseUrl = "https://directory.columbia.edu/people/uni?code="

fun fetchPersons(context: FetchContext, unis: Flowable<UNI>): Flowable<Person> {
    return unis
            .map { uni -> "$baseUrl${uni.value}" }
            .map { Request.Builder().url(it) }
            .map { it.build() }
            .map { context.httpClient.callAsync(it) }
            .flatMapSingle { it.toSingle() }
            .map { it.toDocument() }
            .mapNotNull { logIfNull(System.err::println, it, Person.parse) }
            .doOnNext { println(it) }
            .filter { false }
}