package io.legado.app.lib.mobi.decompress

import io.legado.app.lib.mobi.MobiBook
import io.legado.app.lib.mobi.entities.MobiHeader
import io.legado.app.lib.mobi.utils.readIntArray
import io.legado.app.lib.mobi.utils.readString
import io.legado.app.lib.mobi.utils.readUInt16
import io.legado.app.lib.mobi.utils.readUInt32
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.min

@Suppress("SpellCheckingInspection")
class HuffcdicDecompressor(
    mobiBook: MobiBook,
    mobiHeader: MobiHeader,
) : Decompressor {

    private val magic: String
    private val offset1: Int
    private val offset2: Int
    private val table1: IntArray
    private val mincodeTable = LongArray(33)
    private val maxcodeTable = LongArray(33)
    private val dictionary = arrayListOf<CDICEntry>()

    init {
        val huff = mobiBook.getRecord(mobiHeader.huffcdic)
        magic = huff.readString(0, 4)
        if (magic != "HUFF") error("Invalid HUFF record")
        offset1 = huff.readUInt32(8)
        offset2 = huff.readUInt32(12)

        table1 = huff.readIntArray(offset1, 256)

        huff.position(offset2)

        for (i in 1..32) {
            val mincode = huff.readUInt32().toLong()
            val maxcode = huff.readUInt32().toLong()
            mincodeTable[i] = mincode shl (32 - i)
            maxcodeTable[i] = ((maxcode + 1) shl (32 - i)) - 1
        }

        for (i in 1..<mobiHeader.numHuffcdic) {
            val record = mobiBook.getRecord(mobiHeader.huffcdic + i)
            val magic = record.readString(0, 4)
            if (magic != "CDIC") error("Invalid CDIC record")
            val length = record.readUInt32(4)
            val numEntries = record.readUInt32(8)
            val codeLength = record.readUInt32(12)

            val n = min(1 shl codeLength, numEntries - dictionary.size)

            record.position(length)
            val buffer = record.slice()
            for (j in 0..<n) {
                val offset = buffer.readUInt16(j * 2)
                val x = buffer.readUInt16(offset)
                val len = x and 0x7fff
                val decompressed = x and 0x8000 != 0
                val data = ByteArray(len)
                buffer.position(offset + 2)
                buffer.get(data)
                dictionary.add(CDICEntry(data, decompressed))
            }

        }

    }

    override fun decompress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()

        val buffer = ByteBuffer.wrap(data)

        var bitsleft = data.size * 8
        var pos = 0
        var x = buffer.readUIntX(pos, 8)
        var bitcount = 32

        while (true) {
            if (bitcount <= 0) {
                pos += 4
                x = buffer.readUIntX(pos, 8)
                bitcount += 32
            }

            val code = (x shr bitcount) and ((1L shl 32) - 1)
            val t1 = table1[(code shr 24).toInt()]
            var codelen = t1 and 0x1f
            var maxcode = (((t1.toLong() shr 8) + 1) shl ((32L - codelen).toInt())) - 1

            if (t1 and 0x80 == 0) {
                while (code < mincodeTable[codelen]) {
                    codelen++
                }
                maxcode = maxcodeTable[codelen]
            }

            bitcount -= codelen
            bitsleft -= codelen

            if (bitsleft < 0) {
                break
            }

            val index = (maxcode - code) shr ((32 - codelen))
            val entry = dictionary[index.toInt()]
            if (!entry.decompressed) {
                entry.data = decompress(entry.data)
                entry.decompressed = true
            }

            bos.write(entry.data)

        }

        return bos.toByteArray()
    }

    private fun ByteBuffer.readUIntX(offset: Int, maxlen: Int): Long {
        position(offset)
        var value = 0L
        var i = maxlen
        var bytesLeft = limit() - position()
        while (i-- > 0 && bytesLeft-- > 0) {
            value = value or ((get().toLong() and 0xFF) shl (i * 8))
        }
        return value
    }

}
