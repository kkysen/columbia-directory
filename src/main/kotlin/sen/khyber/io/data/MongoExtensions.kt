package sen.khyber.io.data

import com.mongodb.async.client.MongoCollection

fun <T, C : MutableCollection<T>> MongoCollection<T>.into(c: C): C {
    find().into(c) { _, _ -> }
    return c
}