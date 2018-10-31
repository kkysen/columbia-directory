package sen.khyber.async

import io.reactivex.Single
import java.util.concurrent.CompletableFuture

typealias Promise<T> = CompletableFuture<T>

fun <T> Promise<T>.toSingle(): Single<T> = Single.create {
    whenCompleteAsync { t, e ->
        if (e != null) {
            it.onError(e)
        } else {
            it.onSuccess(t)
        }
    }
}

fun <T> Sequence<Promise<T>>.awaitAll(): Promise<Sequence<T>> =
        Promise.allOf(*this
                .filterNot { it.isCancelled }
                .toList()
                .toTypedArray()
        ).thenApplyAsync { map { it.get() } }

fun <T, R> Sequence<Promise<T>>.asyncMap(transform: (T) -> R): Sequence<Promise<R>> =
        map { it.thenApplyAsync(transform) }

fun <T> Sequence<Promise<T>>.asyncFilter(predicate: (T) -> Boolean): Sequence<Promise<T>> =
        map { promise ->
            promise.thenApplyAsync {
                if (predicate(it)) {
                    return@thenApplyAsync it
                } else {
                    promise.cancel(true)
                    return@thenApplyAsync it
                }
            }
        }

fun <T> Sequence<Promise<T>>.asyncFilterNot(predicate: (T) -> Boolean): Sequence<Promise<T>> =
        asyncFilter { !predicate(it) }

@Suppress("UNCHECKED_CAST")
fun <T : Any> Sequence<Promise<T?>>.asyncFilterNotNull(): Sequence<Promise<T>> =
        asyncFilterNot { it == null } as Sequence<Promise<T>>

fun <T, R : Any> Sequence<Promise<T>>.asyncMapNotNull(transform: (T) -> R?): Sequence<Promise<R>> =
        asyncMap(transform).asyncFilterNotNull()