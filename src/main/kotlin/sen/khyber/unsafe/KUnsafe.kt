package sen.khyber.unsafe

import sun.misc.Unsafe

inline class NativeAddress<T>(val value: Long) {
    
    companion object {
        
        fun <T> at(obj: T): NativeAddress<T> {
            return NativeAddress(NativeUnsafe.getAddress(obj))
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <T> indexedAt(obj: Any): NativeAddress<T> {
            return at(obj) as NativeAddress<T>
        }
        
    }
    
    @Suppress("UNCHECKED_CAST")
    fun deRef(): T {
        return NativeUnsafe.getObject(value) as T
    }
    
    operator fun plus(address: Long) = NativeAddress<T>(value + address)
    
    operator fun plus(address: NativeAddress<T>) = this + address.value
    
    operator fun minus(address: Long) = NativeAddress<T>(value - address)
    
    operator fun minus(address: NativeAddress<T>) = this - address.value
    
    // like C indexing
    operator fun get(address: Long): T = (this + address).deRef()
    
    operator fun plus(address: Int) = NativeAddress<T>(value + address)
    
    operator fun minus(address: Int) = NativeAddress<T>(value - address)
    
    // like C indexing
    operator fun get(address: Int): T = (this + address).deRef()
    
}

object KUnsafe {
    
    @Suppress("ClassName")
    object oops {
        
        val size = Unsafe.ARRAY_OBJECT_INDEX_SCALE
        val compressed = when (size) {
            Int.SIZE_BYTES -> true
            Long.SIZE_BYTES -> false
            else -> throw Error("impossible: oop size = $size")
        }
        
        val objectOffset = if (compressed) 16 else 24
        val arrayOffset = objectOffset
        
    }
    
    private fun javaReflectUnsafe(): Unsafe {
        val theUnsafe = Unsafe::class.java.getDeclaredField("theUnsafe")
        theUnsafe.isAccessible = true
        return theUnsafe.get(null) as Unsafe
    }
    
    private fun nativeReflectUnsafe(): Unsafe {
        /*
         * use JNI to bypass security if JNI is still allowed
         *
         * long theUnsafeStaticOffset = COMPRESSED_OOPS ? 104 : 160;
         * I would use that, but that uses COMPRESSED_OOPS,
         * which uses Unsafe.ARRAY_OBJECT_INDEX_SCALE,
         * which will be blocked.
         *
         * Therefore, either I used native code,
         * or I try both offsets. TODO FIXME
         */
        val theUnsafeStaticOffset = if (oops.compressed) 104 else 160
        return NativeAddress.indexedAt<Unsafe>(Unsafe::class.java)[theUnsafeStaticOffset]
    }
    
    private fun reflectUnsafe() =
            if (System.getSecurityManager() != null) nativeReflectUnsafe() else javaReflectUnsafe()
    
    val unsafe = reflectUnsafe()
    
}

// TODO finish porting