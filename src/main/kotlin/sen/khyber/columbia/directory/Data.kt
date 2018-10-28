package sen.khyber.columbia.directory

import com.mongodb.async.client.MongoCollection
import com.mongodb.async.client.MongoDatabase
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.rxkotlin.toFlowable
import org.litote.kmongo.rxjava2.createView
import org.litote.kmongo.rxjava2.insertOne
import org.litote.kmongo.util.KMongoUtil
import kotlin.reflect.KClass

interface Data<T> {
    val view: Single<MongoCollection<T>>
    fun update()
    fun flow(): Flowable<T>
}

@PublishedApi internal val noData = object : Data<Unit> {
    
    // not used and can safely do the cast b/c generics are erased
    @Suppress("UNCHECKED_CAST")
    override val view = Single.just(Unit) as Single<MongoCollection<Unit>>
    
    override fun flow(): Flowable<Unit> = Flowable.just(Unit)
    
    override fun update() = Unit
    
}

class DependentData<T : Any, R : Any, FetchContext>
@PublishedApi internal constructor(
        @PublishedApi internal val db: MongoDatabase,
        @PublishedApi internal val fetchContext: FetchContext,
        klass: KClass<R>,
        private val source: Data<T>,
        private val fetch: (FetchContext, Flowable<T>) -> Flowable<R>,
        private val unMap: ((R) -> T)?
) : Data<R> {
    
    private val collection: MongoCollection<R>
    override val view: Single<MongoCollection<R>>
    init {
        val collectionName = KMongoUtil.defaultCollectionName(klass)
        collection = db.getCollection(collectionName, klass.java)
        val viewName = "$collectionName$.view"
        view = db.createView(viewName, collectionName, emptyList())
                .toSingleDefault(db.getCollection(viewName, klass.java))
    }
    
    private val all = collection.into(mutableListOf())
    private var flow: Flowable<R>? = null
    
    override fun update() {
        val existingSource = unMap?.let {
            all.asSequence().map(unMap).toHashSet()
        }
        val filterOutExisting: (Flowable<T>) -> Flowable<T> =
                if (existingSource != null) { it -> it.filter { !existingSource.contains(it) } }
                else { it -> it }
        // TODO update if already exists instead parse skipping?
        // need a key to tell if already exists and then deep equals to check if should update
        // when updating, what should keep track parse when update is done, b/c not kept in pipeline
        val flow = source
                .flow()
                .let { filterOutExisting(it) }
                .let { fetch(fetchContext, it) }
                .afterFlatMapCompletable { collection.insertOne(it) }
        this.flow = flow
        flow
                .toList()
                .subscribe { it ->
                    all.clear()
                    all.addAll(it)
                    this.flow = null
                }
    }
    
    override fun flow(): Flowable<R> = this.flow ?: all.toFlowable()
    
    inline fun <reified U : Any> map(
            noinline fetch: (FetchContext, Flowable<R>) -> Flowable<U>,
            noinline unMap: ((U) -> R)?
    ): DependentData<R, U, FetchContext> =
            DependentData(db, fetchContext, U::class, this, fetch, unMap)
    
}

inline fun <reified T : Any, FetchContext> data(
        db: MongoDatabase,
        fetchContext: FetchContext,
        crossinline fetch: (FetchContext) -> Flowable<T>
): DependentData<Unit, T, FetchContext> =
        DependentData(db, fetchContext, T::class, noData, { context, _ -> fetch(context) }, null)