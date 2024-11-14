package io.legado.app.lib.mobi.utils

import okhttp3.internal.and
import java.nio.ByteBuffer
import java.nio.charset.Charset

fun ByteBuffer.readByteArray(offset: Int, len: Int): ByteArray {
    position(offset)
    val b = ByteArray(len)
    get(b)
    return b
}

fun ByteBuffer.readByteArray(len: Int): ByteArray {
    val b = ByteArray(len)
    get(b)
    return b
}

fun ByteBuffer.readIntArray(offset: Int, len: Int): IntArray {
    position(offset)
    return IntArray(len) {
        getInt()
    }
}

fun ByteBuffer.readUInt16Array(offset: Int, len: Int): IntArray {
    position(offset)
    return IntArray(len) {
        getShort() and 0xFFFF
    }
}

fun ByteBuffer.readString(len: Int): String {
    return String(readByteArray(len))
}

fun ByteBuffer.readString(offset: Int, len: Int): String {
    return String(readByteArray(offset, len))
}

fun ByteBuffer.readString(offset: Int, len: Int, charset: Charset): String {
    return String(readByteArray(offset, len), charset)
}

fun ByteBuffer.readUInt8(offset: Int): Int {
    position(offset)
    return get() and 0xFF
}

fun ByteBuffer.readUInt8(): Int {
    return get() and 0xFF
}

fun ByteBuffer.readUInt16(offset: Int): Int {
    position(offset)
    return getShort() and 0xFFFF
}

fun ByteBuffer.readUInt32(offset: Int): Int {
    position(offset)
    return getInt()
}

fun ByteBuffer.readUInt32(): Int {
    return getInt()
}

fun ByteBuffer.readUInt64(offset: Int): Long {
    position(offset)
    return getLong()
}

