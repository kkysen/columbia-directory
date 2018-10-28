package sen.khyber.columbia.directory

import okhttp3.OkHttpClient
import org.litote.kmongo.async.KMongo
import org.openqa.selenium.WebDriver
import java.io.Closeable
import java.util.logging.Logger

class ColumbiaDirectory(
        fetchContext: FetchContext,
        dbConnectionUrl: String = "mongodb://localhost:27017/CUDirectory"
) : Closeable {
    
    data class FetchContext(val logger: Logger, val httpClient: OkHttpClient, val webDriver: WebDriver)
    
    private val dbClient = KMongo.createClient(dbConnectionUrl)
    private val db = dbClient.getDatabase("ColumbiaDirectory")
    
    val unis: Data<UNI>
    val persons: Data<Person>
    
    init {
        // TODO include logger in fetchContext
        
        // expose as Data, but call DependentData.map() internally
        unis = data(db, fetchContext, ::fetchUnis)
        persons = unis.map(::fetchPersons) { it.uni }
    }
    
    fun update() {
        unis.update()
        persons.update()
    }
    
    override fun close() = dbClient.close()
    
}

fun main() {
//    ColumbiaDirectory(ok).use {
//        it.unis.flow().subscribe { println(it) }
//        it.persons.flow().subscribe { println(it) }
//    }



}