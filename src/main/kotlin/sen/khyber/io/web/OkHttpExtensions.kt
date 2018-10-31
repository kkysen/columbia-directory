package sen.khyber.io.web

import io.reactivex.Flowable
import io.reactivex.rxkotlin.toFlowable
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import sen.khyber.async.Promise
import sen.khyber.async.toSingle
import sen.khyber.rx.naturals
import java.io.IOException
import java.util.concurrent.CompletableFuture

fun OkHttpClient.callAsync(request: Request): Promise<Response> {
    val promise = CompletableFuture<Response>()
    newCall(request).enqueue(object : Callback {
        
        override fun onFailure(call: Call, e: IOException) {
            promise.completeExceptionally(e)
        }
        
        override fun onResponse(call: Call, response: Response) {
            promise.complete(response)
        }
        
    })
    return promise
}

fun OkHttpClient.getAsync(url: String): Promise<Response> = callAsync(Request.Builder().url(url).build())

fun OkHttpClient.getAsync(url: HttpUrl): Promise<Response> = callAsync(Request.Builder().url(url).build())

private fun OkHttpClient.readPaginated(
        request: Request, numPages: Int,
        paginateRequest: (Request, pageNum: Int) -> Request, firstPage: Int): Flowable<WebResponse> =
        (firstPage until numPages + firstPage)
                .asSequence()
                .map { paginateRequest(request, it) }
                .map { callAsync(it) }
                .toFlowable()
                .flatMapSingle { it.toSingle() }
                .map { WebResponse(it) }

fun defaultPaginateRequest(request: Request, pageNum: Int): Request = request
        .newBuilder()
        .url(request
                .url()
                .newBuilder()
                .addQueryParameter("page", pageNum.toString())
                .build()
        ).build()

fun OkHttpClient.readPaginated(request: Request, findNumPages: (WebResponse) -> Int?,
                               paginateRequest: (Request, pageNum: Int) -> Request = ::defaultPaginateRequest,
                               firstPage: Int = 1): Flowable<WebResponse> =
        callAsync(request)
                .toSingle()
                .map { WebResponse(it) }
                .map { findNumPages(it) ?: 1 }
                .toFlowable()
                .flatMap { readPaginated(request, it, paginateRequest, firstPage) }

data class PageTotals(val numPages: Int, val numItems: Int)

fun <T : Any> OkHttpClient.readPaginatedRetrying(
        request: Request, findPageTotals: (WebResponse) -> PageTotals?,
        toItems: (Flowable<WebResponse>) -> Flowable<T>,
        paginateRequest: (Request, pageNum: Int) -> Request = ::defaultPaginateRequest,
        maxRetries: Long = -1L, firstPage: Int = 1
): Flowable<T> =
        callAsync(request)
                .toSingle()
                .map { WebResponse(it) }
                .map { findPageTotals(it) ?: PageTotals(1, 0) }
                .flatMap { totals ->
                    naturals()
                            .let { if (maxRetries < 0) it else it.take(maxRetries) }
                            .map { readPaginated(request, totals.numPages, paginateRequest, firstPage) }
                            .map { toItems(it) }
                            .flatMapSingle({ it.toList() }, false, 1) // sequential scan
                            .map { it.toSet() }
                            .scan { a, b -> a + b }
                            .skipWhile { it.size < totals.numItems }
                            .firstOrError()
                }
                .toFlowable()
                .flatMap { it.toFlowable() }

val ok = OkHttpClient()