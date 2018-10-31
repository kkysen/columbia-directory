package sen.khyber.io

import sen.khyber.util.Sanitizer
import java.nio.file.LinkOption
import java.nio.file.Path
import java.util.*
import java.util.Spliterator.IMMUTABLE
import java.util.Spliterator.NONNULL
import java.util.Spliterator.ORDERED
import java.util.Spliterator.SIZED

operator fun Path.div(other: Path): Path = resolve(other)

operator fun Path.div(other: String): Path = resolve(other)

operator fun Path.get(index: Int): Path = getName(index)

fun Path.subPath(beginIndex: Int, endIndex: Int): Path = subpath(beginIndex, endIndex)

fun Path.subPath(indices: IntRange): Path = subPath(indices.first, indices.endInclusive)

operator fun Path.get(indices: IntRange): Path = subPath(indices)


fun String.toPath(): Path = Path.of(this)


val fileNameSanitizer = Sanitizer.forChars(":/\"?*|<>\\", '_')

fun Path.sanitized(defaultReplacementChar: Char = fileNameSanitizer.defaultReplacementChar): SanitizedPath =
        SanitizedPath(this, defaultReplacementChar)

fun String.toSanitizedPath(
        defaultReplacementChar: Char = fileNameSanitizer.defaultReplacementChar): SanitizedPath =
        toPath().sanitized()

class SanitizedPath
private constructor(private val sanitizer: Sanitizer, val path: Path) : Path by path {
    
    internal constructor(path: Path, defaultReplacementChar: Char = fileNameSanitizer.defaultReplacementChar) :
            this(fileNameSanitizer.withDefaultReplacementChar(defaultReplacementChar), path)
    
    private fun sanitize(pathPart: String?) = pathPart?.let { sanitizer.sanitize(it) }
    
    private fun withPath(path: Path): SanitizedPath =
            if (path is SanitizedPath) path else SanitizedPath(sanitizer, path)
    
    // override delegations to wrap w/ SanitizedPath
    // override default methods too, b/c could become non-default later
    
    override fun getRoot() = withPath(path.root)
    
    override fun getFileName() = withPath(path.fileName)
    
    override fun getParent() = withPath(path.parent)
    
    override fun getName(index: Int) = withPath(path.getName(index))
    
    override fun subpath(beginIndex: Int, endIndex: Int) = withPath(path.subpath(beginIndex, endIndex))
    
    override fun normalize() = withPath(path.normalize())
    
    override fun resolve(other: Path?) = withPath(path.resolve(other))
    
    override fun resolve(other: String?) = withPath(path.resolve(sanitize(other)))
    
    override fun resolveSibling(other: Path?) = withPath(path.resolveSibling(other))
    
    override fun resolveSibling(other: String?) = withPath(path.resolveSibling(sanitize(other)))
    
    override fun relativize(other: Path?) = withPath(path.relativize(other))
    
    override fun toAbsolutePath() = withPath(path.toAbsolutePath())
    
    override fun toRealPath(vararg options: LinkOption?) = withPath(path.toRealPath(*options))
    
    override fun iterator(): MutableIterator<SanitizedPath> {
        val iterator = path.iterator()
        @Suppress("CAST_NEVER_SUCCEEDS")
        return object : MutableIterator<SanitizedPath> {
            
            override fun hasNext() = iterator.hasNext()
            
            override fun next() = withPath(iterator.next())
            
            override fun remove() = iterator.remove()
            
        }
    }
    
    override fun toString() = path.toString()
    
    override fun compareTo(other: Path?): Int {
        return path.compareTo(if (other is SanitizedPath) other.path else other)
    }
    
    // SanitizedPath == native Path, but native Path != SanitizedPath
    
    override fun equals(other: Any?) = other != null && other is Path && compareTo(other) == 0
    
    override fun hashCode() = path.hashCode()
    
    fun forEachSanitized(action: ((SanitizedPath) -> Unit)?) {
        action?.let { path.forEach { action(withPath(it)) } }
    }
    
    // Iterable.spliterator() doesn't have covariance, i.e. Spliterator<? extends T>
    fun spliteratorSanitized(): Spliterator<SanitizedPath> = Spliterators.spliterator(iterator(), nameCount.toLong(),
            ORDERED or SIZED or NONNULL or IMMUTABLE)
    
    operator fun div(other: Path) = resolve(other)
    
    operator fun div(other: String) = resolve(other)
    
    operator fun get(index: Int) = getName(index)
    
    fun subPath(beginIndex: Int, endIndex: Int) = subpath(beginIndex, endIndex)
    
    fun subPath(indices: IntRange) = subPath(indices.first, indices.endInclusive)
    
    operator fun get(indices: IntRange) = subPath(indices)
    
}
