package sen.khyber.columbia.directory

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
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

fun Response.toDocument(): Document {
    val body = body()!!
    val charset = (body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8);
    return Jsoup.parse(
            body.byteStream(),
            charset.name(),
            request().url().toString()
    )
}

val ok = OkHttpClient()