package sen.khyber.rx

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.rxkotlin.toFlowable
import java.util.*

fun <T, R : Any> Flowable<T>.mapNotNull(mapper: (T) -> R?): Flowable<R> =
        map { Optional.ofNullable(mapper(it)) }
                .filter { it.isPresent }
                .map { it.get() }

fun <T> Flowable<T>.afterFlatMapCompletable(mapper: (T) -> Completable): Flowable<T> =
        flatMapSingle { it -> mapper(it).toSingleDefault(it) }


fun naturals(): Flowable<Long> = generateSequence(1L) { it + 1 }.toFlowable()