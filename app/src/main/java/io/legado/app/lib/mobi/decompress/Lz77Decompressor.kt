package io.legado.app.lib.mobi.decompress

import androidx.core.util.Pools.SynchronizedPool

class Lz77Decompressor(private val textRecordSize: Int) : Decompressor {

    val pool = SynchronizedPool<ByteArray>(2)

    override fun decompress(data: ByteArray): ByteArray {
        val out = pool.acquire() ?: ByteArray(textRecordSize)

        var i = 0
        var o = 0
        while (i < data.size) {
            var c = data[i++].toInt() and 0x00FF
            if (c in 0x01..0x08) {
                var j = 0
                while (j < c && i + j < data.size) {
                    out[o++] = data[i + j]
                    j++
                }
                i += c
            } else if (c <= 0x7f) {
                out[o++] = c.toByte()
            } else if (c >= 0xC0) {
                out[o++] = ' '.code.toByte()
                out[o++] = (c xor 0x80).toByte()
            } else {
                if (i < data.size) {
                    c = c shl 8 or (data[i++].toInt() and 0xFF)
                    val length = (c and 0x0007) + 3
                    val location = (c shr 3) and 0x7FF

                    if (location in 1..o) {
                        for (j in 0 until length) {
                            val idx = o - location
                            out[o++] = out[idx]
                        }
                    }
                }
            }
        }

        val result = ByteArray(o)
        System.arraycopy(out, 0, result, 0, o)
        return result.also { pool.release(out) }
    }

}
