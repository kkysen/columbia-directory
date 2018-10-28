package sen.khyber.columbia.directory

import io.reactivex.Completable
import io.reactivex.Flowable
import java.util.*

fun <T, R : Any> Flowable<T>.mapNotNull(mapper: (T) -> R?): Flowable<R> =
        map { Optional.ofNullable(mapper(it)) }
                .filter { it.isPresent }
                .map { it.get() }

fun <T> Flowable<T>.afterFlatMapCompletable(mapper: (T) -> Completable): Flowable<T> =
        flatMapSingle { it -> mapper(it).toSingleDefault(it) }

//fun <T> Completable.mapToSingle(mapper: () -> Single<T>): Single<T> {
//    return andThen(mapper())
//    return toSingleDefault(Unit).flatMap { mapper() }
//}