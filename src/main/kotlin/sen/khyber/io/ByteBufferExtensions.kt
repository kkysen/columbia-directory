package sen.khyber.io

import java.nio.ByteBuffer
import java.nio.ByteOrder


object Endianness {
    
    val little = ByteOrder.LITTLE_ENDIAN
    val big = ByteOrder.BIG_ENDIAN
    val native = ByteOrder.nativeOrder()
    val nonNative = if (native == little) big else little
    val isBig = native == big
    val isLittle = native == little
    
}

fun ByteBuffer.nativeOrder(): ByteBuffer = order(Endianness.native)

fun ByteBuffer.nonNativeOrder(): ByteBuffer = order(Endianness.nonNative)

// TODO finish porting