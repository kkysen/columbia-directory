package sen.khyber.util

import sen.khyber.functional.not
import sen.khyber.util.Sanitizer.SanitizerPredicate.BINARY_SEARCH
import sen.khyber.util.Sanitizer.SanitizerPredicate.BIT_SET
import sen.khyber.util.Sanitizer.SanitizerPredicate.BIT_SET_64
import sen.khyber.util.Sanitizer.SanitizerPredicate.HASH_SET
import sen.khyber.util.Sanitizer.SanitizerPredicate.LINEAR_SEARCH

interface Sanitizer {
    
    val illegalChars: List<Char>
    
    val defaultReplacementChar: Char
    
    val isLegal: (Char) -> Boolean
    
    fun sanitize(chars: CharArray, replacementChar: Char = defaultReplacementChar,
                 out: CharArray, index: Int)
    
    fun sanitize(chars: CharArray, replacementChar: Char = defaultReplacementChar): CharArray
    
    fun sanitize(string: String, replacementChar: Char = defaultReplacementChar): String
    
    enum class SanitizerPredicate(val create: (List<Char>, Char, Int) -> (Char) -> Boolean) {
        BIT_SET_64({ chars, min, _ -> create64BitPredicate(chars, min) }),
        BIT_SET(::createBitSetPredicate),
        HASH_SET({ chars, _, _ -> createHashSetPredicate(chars) }),
        BINARY_SEARCH({ chars, _, _ -> createBinarySearchPredicate(chars) }),
        LINEAR_SEARCH({ chars, _, _ -> createLinearSearchPredicate(chars) })
    }
    
    companion object {
        
        fun forChars(illegalChars: CharArray, defaultReplacementChar: Char,
                     predicate: SanitizerPredicate? = null): Sanitizer {
            val size = illegalChars.size
            if (size == 0) {
                return nullSanitizer
            }
            // non-null if non-empty
            val min = illegalChars.min()!!
            val max = illegalChars.max()!!
            val numBitsSpan = max - min
            val span = (numBitsSpan ushr 6) + if (numBitsSpan and (Long.SIZE_BITS - 1) == 0) 0 else 1
            val chars = illegalChars.asList()
            // TODO check, maybe tune optimization parameters here
            val realPredicate = predicate ?: when {
                span <= 1 -> BIT_SET_64 // low memory, extremely fast
                span <= 64 / Byte.SIZE_BYTES -> BIT_SET // low memory bit set
                size <= 8 -> LINEAR_SEARCH // linear search faster for very short arrays
                span <= 1024 / Byte.SIZE_BYTES -> BIT_SET // a little more memory bit set
                size <= 1024 -> BINARY_SEARCH
                else -> HASH_SET // log(1024) = 10, boxing hash should be faster than > 10 comparisons
            }
            return SanitizerImpl(!realPredicate.create(chars, min, span), chars, defaultReplacementChar)
        }
        
        fun forChars(illegalChars: String, defaultReplacementChar: Char,
                     predicate: SanitizerPredicate? = null): Sanitizer =
                forChars(illegalChars.toCharArray(), defaultReplacementChar,
                        predicate)
        
    }
    
    fun withDefaultReplacementChar(defaultReplacementChar: Char): Sanitizer
    
}

val nullSanitizer = object : Sanitizer {
    
    override val illegalChars: List<Char> get() = emptyList()
    
    override val defaultReplacementChar: Char get() = 0.toChar()
    
    override val isLegal: (Char) -> Boolean get() = { true }
    
    override fun sanitize(chars: CharArray, replacementChar: Char, out: CharArray, index: Int) {
        chars.copyInto(out, index)
    }
    
    override fun sanitize(chars: CharArray, replacementChar: Char) = chars
    
    override fun sanitize(string: String, replacementChar: Char) = string
    
    override fun withDefaultReplacementChar(defaultReplacementChar: Char) = this
    
}

class SanitizerImpl(
        override val isLegal: (Char) -> Boolean,
        override val illegalChars: List<Char>,
        override val defaultReplacementChar: Char
) : Sanitizer {
    
    override fun sanitize(chars: CharArray, replacementChar: Char,
                          out: CharArray, index: Int) {
        if (index < 0 || index >= out.size) {
            throw IndexOutOfBoundsException(Math.min(index, out.size))
        }
        for (i in chars.indices) {
            val c = chars[i]
            out[i + index] = if (isLegal(c)) c else replacementChar
        }
    }
    
    override fun sanitize(chars: CharArray, replacementChar: Char): CharArray {
        val sanitized = CharArray(chars.size)
        sanitize(chars, replacementChar, sanitized, 0)
        return sanitized
    }
    
    override fun sanitize(string: String, replacementChar: Char): String {
        return String(sanitize(string.toCharArray(), replacementChar))
    }
    
    override fun withDefaultReplacementChar(defaultReplacementChar: Char) =
            if (defaultReplacementChar == this.defaultReplacementChar)
                this
            else SanitizerImpl(isLegal, illegalChars, defaultReplacementChar)
    
}

private fun create64BitPredicate(chars: List<Char>, min: Char): (Char) -> Boolean {
    val bits = run {
        var bits = 0L
        for (c in chars) {
            bits = bits or (1L shl (c - min))
        }
        return@run bits
    }
    return { c -> ((bits ushr (c - min)) and 1L) == 1L }
}

private fun createBitSetPredicate(chars: List<Char>, min: Char, span: Int): (Char) -> Boolean {
    val bits = LongArray(span)
    for (c in chars) {
        val d = c - min
        val i = d ushr 6
        val j = d and (Long.SIZE_BITS - 1)
        bits[i] = bits[i] or (1L shl j)
    }
    return { c ->
        val d = c - min
        val i = d ushr 6
        val j = d and (Long.SIZE_BITS - 1)
        i < bits.size && (((bits[i] ushr j) and 1L) == 1L)
    }
}

private fun createHashSetPredicate(chars: List<Char>): (Char) -> Boolean {
    val charSet = chars.toSet()
    return charSet::contains
}

private fun createBinarySearchPredicate(charList: List<Char>): (Char) -> Boolean {
    val chars = charList.toCharArray()
    chars.sort()
    return { c -> chars.binarySearch(c) >= 0 }
}

private fun createLinearSearchPredicate(charList: List<Char>): (Char) -> Boolean {
    val chars = charList.toCharArray()
    return chars::contains
}