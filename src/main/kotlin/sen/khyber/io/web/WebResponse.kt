package sen.khyber.io.web

import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.ByteArrayInputStream
import java.io.CharArrayReader
import java.io.Closeable
import java.io.InputStream
import java.io.Reader
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import kotlin.LazyThreadSafetyMode.NONE

class WebResponse(private val response: Response) : Closeable by response {
    
    val request: Request get() = response.request()
    
    val url: HttpUrl get() = request.url()
    
    val urlString: String get() = url.toString()
    
    val body: ResponseBody get() = response.body()!!
    
    val contentType: MediaType by lazy(NONE) { body.contentType()!! }
    
    val charset: Charset? by lazy(NONE) { contentType.charset() }
    
    fun charset(default: Charset = Charsets.UTF_8): Charset = charset ?: default
    
    private val lazyByteBuffer = lazy(NONE) { ByteBuffer.wrap(body.bytes()) }
    
    val byteBuffer: ByteBuffer by lazyByteBuffer
    
    val charBuffer: CharBuffer by lazy(NONE) { charset().decode(byteBuffer) }
    
    val length: Long
        get() =
            if (lazyByteBuffer.isInitialized()) byteBuffer.array().size.toLong() else body.contentLength()
    
    val bytes: ByteArray get() = byteBuffer.array()
    
    val chars: CharArray get() = charBuffer.array()
    
    val string: String by lazy(NONE) { charBuffer.toString() }
    
    fun append(sb: StringBuilder): StringBuilder = sb.append(chars)
    
    val stringBuilder: StringBuilder get() = append(StringBuilder(chars.size))
    
    val inputStream: InputStream get() = ByteArrayInputStream(bytes)
    
    val reader: Reader get() = CharArrayReader(chars)
    
    fun put(out: ByteBuffer): ByteBuffer = out.put(byteBuffer.duplicate())
    
    val document: Document by lazy(NONE) { Jsoup.parse(inputStream, charset().name(), urlString) }
    
}