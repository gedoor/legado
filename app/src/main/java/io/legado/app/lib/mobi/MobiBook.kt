package io.legado.app.lib.mobi

import android.util.SparseArray
import io.legado.app.lib.mobi.decompress.Decompressor
import io.legado.app.lib.mobi.decompress.HuffcdicDecompressor
import io.legado.app.lib.mobi.decompress.Lz77Decompressor
import io.legado.app.lib.mobi.decompress.PlainDecompressor
import io.legado.app.lib.mobi.entities.IndexData
import io.legado.app.lib.mobi.entities.IndexEntry
import io.legado.app.lib.mobi.entities.IndexTag
import io.legado.app.lib.mobi.entities.IndxHeader
import io.legado.app.lib.mobi.entities.MobiEntryHeaders
import io.legado.app.lib.mobi.entities.MobiMetadata
import io.legado.app.lib.mobi.entities.NCX
import io.legado.app.lib.mobi.entities.Ptagx
import io.legado.app.lib.mobi.entities.TOC
import io.legado.app.lib.mobi.entities.TagxHeader
import io.legado.app.lib.mobi.entities.TagxTag
import io.legado.app.lib.mobi.utils.readString
import io.legado.app.lib.mobi.utils.readUInt16Array
import io.legado.app.lib.mobi.utils.readUInt32
import io.legado.app.lib.mobi.utils.readUInt8
import okhttp3.internal.and
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Suppress("SpellCheckingInspection")
abstract class MobiBook(
    private val pdbFile: PDBFile,
    val headers: MobiEntryHeaders,
    private val kf8BoundaryOffset: Int,
    private val resourceStart: Int,
) {
    val mobi = headers.mobi
    val palmdoc = headers.palmdoc
    private val exth = headers.exth
    private val trailingFlags = mobi.trailingFlags

    val charset: Charset = when (val charset = mobi.encoding) {
        65001 -> Charsets.UTF_8
        1252 -> Charset.forName("windows-1252")
        else -> error("unknown charset $charset")
    }

    private val decompressor: Decompressor = when (val compression = palmdoc.compression) {
        1 -> PlainDecompressor()
        2 -> Lz77Decompressor(max(4096, palmdoc.recordSize))
        17480 -> HuffcdicDecompressor(this, mobi)
        else -> error("unknown compression $charset")
    }

    @Suppress("UNCHECKED_CAST")
    val metadata: MobiMetadata by lazy {
        MobiMetadata(
            mobi.uid.toString(),
            exth["title"] as? String ?: mobi.title,
            exth["creator"] as? List<String> ?: emptyList(),
            exth["publisher"] as? String ?: "",
            exth["language"] as? String ?: mobi.languege,
            exth["date"] as? String ?: "",
            exth["description"] as? String ?: "",
            exth["subject"] as? List<String> ?: emptyList(),
            exth["rights"] as? String ?: ""
        )
    }

    var toc: List<TOC>? = null

    private var textRecordOffsets = arrayListOf<Int>()

    init {
        buildTextRecordOffsets()
    }

    private fun buildTextRecordOffsets() {
        var offset = 0
        for (i in 0..<palmdoc.numTextRecords) {
            offset += getTextRecord(i).size
            textRecordOffsets.add(offset)
        }
    }

    fun getRecord(index: Int): ByteBuffer {
        return pdbFile.getRecordData(kf8BoundaryOffset + index)
    }

    fun getTextRecord(index: Int): ByteArray {
        if (index < 0 && index >= palmdoc.numTextRecords) {
            throw IndexOutOfBoundsException("Text record index out of bounds")
        }
        var content = getRecord(index + 1).array()
        content = removeTrailingEntries(content)
        return decompressor.decompress(content)
    }

    fun getTextRecordInputStream(): InputStream {
        return object : InputStream() {

            private var index = -1
            private var bis: ByteArrayInputStream = emptyByteArrayInputStream
            private var available = textRecordOffsets.last()
            private var pos = 0

            override fun read(): Int {
                if (index >= palmdoc.numTextRecords) {
                    return -1
                }

                if (bis.available() == 0) {
                    if (++index >= palmdoc.numTextRecords) {
                        return -1
                    }
                    bis = getTextRecord(index).inputStream()
                }

                val b = bis.read()

                available--
                pos++

                return b
            }

            override fun skip(n: Long): Long {
                if (n == 0L) return 0L

                val n1 = min(available, n.toInt())

                if (n1 < bis.available()) {
                    bis.skip(n1.toLong())

                    available -= n1
                    pos += n1

                    return n1.toLong()
                }

                val bIndex = textRecordOffsets.binarySearch(pos + n1)
                index = abs(bIndex + 1)

                bis = getTextRecord(index).inputStream()

                val offset = textRecordOffsets.getOrNull(index - 1) ?: 0
                bis.skip((pos + n1 - offset).toLong())

                available -= n1
                pos += n1

                return n1.toLong()
            }

            override fun available(): Int {
                return available
            }

        }
    }

    private fun removeTrailingEntries(byteArray: ByteArray): ByteArray {
        if (trailingFlags == 0) return byteArray
        val multibyte = trailingFlags and 1 != 0
        val numTrailingEntries = (trailingFlags shr 1).countOneBits()
        val lastIndex = byteArray.lastIndex
        var extraSize = 0
        for (i in 0..<numTrailingEntries) {
            var value = 0
            for (j in max(0, lastIndex - 4 - extraSize)..max(0, lastIndex - extraSize)) {
                val byte = byteArray[j]
                if (byte and 0b1000_0000 != 0) {
                    value = 0
                }
                value = (value shl 7) or (byte and 0b111_1111)
            }
            extraSize += value
        }
        if (multibyte) {
            val byte = byteArray[byteArray.lastIndex - extraSize]
            extraSize += (byte and 0b11) + 1
        }
        return byteArray.copyOfRange(0, byteArray.size - extraSize)
    }

    fun getResource(index: Int): ByteBuffer {
        return pdbFile.getRecordData(resourceStart + index)
    }

    fun getNCX(): List<NCX>? {
        val indxIndex = mobi.indx
        if (indxIndex == -1) {
            return null
        }
        val indexData = getIndexData(indxIndex)
        val items = indexData.table.mapIndexed { index, indexEntry ->
            val tagMap = indexEntry.tagMap
            NCX(
                index,
                tagMap[1]?.tagValues?.getOrNull(0),
                tagMap[2]?.tagValues?.getOrNull(0),
                indexData.cncx[tagMap[3].tagValues[0]],
                tagMap[4]?.tagValues?.getOrNull(0),
                tagMap[6]?.tagValues,
                tagMap[21]?.tagValues?.getOrNull(0),
                tagMap[22]?.tagValues?.getOrNull(0),
                tagMap[23]?.tagValues?.getOrNull(0),
            )
        }

        val parentItemMap = hashMapOf<Int, ArrayList<NCX>>()

        items.forEach {
            val parent = it.parent ?: return@forEach
            val array = parentItemMap.getOrPut(parent) { arrayListOf() }
            array.add(it)
        }

        fun getChildren(item: NCX): NCX {
            if (item.firstChild == null) return item
            item.children = parentItemMap[item.index]?.map(::getChildren)
            return item
        }

        return items.filter { it.headingLevel == 0 }.map(::getChildren)
    }

    fun getCover(): ByteArray? {
        val coverOffset = exth["coverOffset"] as? Int
        val thumbnailOffset = exth["thumbnailOffset"] as? Int

        if (coverOffset != null && coverOffset != -1) {
            return getResource(coverOffset).array()
        }

        if (thumbnailOffset != null && thumbnailOffset != -1) {
            return getResource(thumbnailOffset).array()
        }

        return null
    }

    fun getIndexData(indxIndex: Int): IndexData {
        val indxRecord = getRecord(indxIndex)
        val indx = readIndxHeader(indxRecord)
        indxRecord.position(indx.length)
        val tagxBuffer = indxRecord.slice()
        val tagx = readTagxHeader(tagxBuffer)
        val tagTable = readTagxTags(tagx, tagxBuffer)
        val cncx = readCncx(indxIndex, indx)

        val table = arrayListOf<IndexEntry>()

        for (i in 0..<indx.numRecords) {
            val indxBuffer = getRecord(indxIndex + 1 + i)
            val indxHeader = readIndxHeader(indxBuffer)
            val idxt = readIdxt(indxBuffer, indxHeader)
            for (j in 0..<indxHeader.numRecords) {
                val idxtOffset = idxt[j]
                val entry = readIndexEntry(indxBuffer, tagx, tagTable, idxtOffset)
                table.add(entry)
            }
        }

        return IndexData(table, cncx)
    }

    private fun readIndexEntry(
        indxBuffer: ByteBuffer,
        tagx: TagxHeader,
        tagTable: List<TagxTag>,
        idxtOffset: Int
    ): IndexEntry {
        val array = indxBuffer.array()

        val len = indxBuffer.readUInt8(idxtOffset)
        val label = indxBuffer.readString(idxtOffset + 1, len)

        val ptagxs = arrayListOf<Ptagx>()
        val startPos = idxtOffset + 1 + len
        var controlByteIndex = 0
        var pos = startPos + tagx.numControlBytes

        for (tag in tagTable) {
            if (tag.controlByte == 1) {
                controlByteIndex++
                continue
            }
            val offset = startPos + controlByteIndex
            var value = indxBuffer.readUInt8(offset) and tag.bitmask
            if (value == tag.bitmask) {
                if (tag.bitmask.countOneBits() > 1) {
                    var v = 0
                    for (a in pos..<min(pos + 4, array.size)) {
                        val byte = array[a]
                        v = (v shl 7) or (byte and 0b111_1111)
                        pos++
                        if (byte and 0b1000_0000 != 0) break
                    }
                    ptagxs.add(Ptagx(tag.tag, tag.numValues, null, v))
                } else {
                    ptagxs.add(Ptagx(tag.tag, tag.numValues, 1, null))
                }
            } else {
                var mask = tag.bitmask
                while ((mask and 1) == 0) {
                    mask = mask shr 1
                    value = value shr 1
                }
                ptagxs.add(Ptagx(tag.tag, tag.numValues, value, null))
            }
        }

        val tags = arrayListOf<IndexTag>()
        val tagMap = SparseArray<IndexTag>()

        for (ptagx in ptagxs) {
            val values = arrayListOf<Int>()
            if (ptagx.valueCount != null) {
                repeat(ptagx.valueCount * ptagx.tagValueCount) {
                    var v = 0
                    for (a in pos..<min(pos + 4, array.size)) {
                        val byte = array[a]
                        v = (v shl 7) or (byte and 0b111_1111)
                        pos++
                        if (byte and 0b1000_0000 != 0) break
                    }
                    values.add(v)
                }
            } else {
                var count = 0
                while (count < ptagx.valueBytes!!) {
                    var v = 0
                    for (a in pos..<min(pos + 4, array.size)) {
                        val byte = array[a]
                        v = (v shl 7) or (byte and 0b111_1111)
                        pos++
                        count++
                        if (byte and 0b1000_0000 != 0) break
                    }
                    values.add(v)
                }
            }
            val tag = IndexTag(ptagx.tag, values)
            tags.add(tag)
            tagMap[tag.tagId] = tag
        }

        return IndexEntry(label, tags, tagMap)
    }

    private fun readIdxt(buffer: ByteBuffer, indxHeader: IndxHeader): IntArray {
        return buffer.readUInt16Array(indxHeader.idxt + 4, indxHeader.numRecords)
    }

    private fun readTagxTags(tagx: TagxHeader, tagxBuffer: ByteBuffer): List<TagxTag> {
        val numTags = (tagx.length - 12) / 4
        val tags = arrayListOf<TagxTag>()
        tagxBuffer.position(12)
        for (i in 0..<numTags) {
            val tag = tagxBuffer.readUInt8()
            val numValues = tagxBuffer.readUInt8()
            val bitmask = tagxBuffer.readUInt8()
            val controlByte = tagxBuffer.readUInt8()
            tags.add(TagxTag(tag, numValues, bitmask, controlByte))
        }
        return tags
    }

    private fun readCncx(indxIndex: Int, indx: IndxHeader): SparseArray<String> {
        val cncx = SparseArray<String>()
        var cncxRecordOffset = 0
        for (i in 0..<indx.numCncx) {
            val record = getRecord(indxIndex + indx.numRecords + i + 1)
            val array = record.array()
            var pos = 0
            while (pos < array.size) {
                val index = pos
                var value = 0
                var length = 0
                for (a in pos..<min(pos + 4, array.size)) {
                    val byte = array[a]
                    value = (value shl 7) or (byte and 0b111_1111)
                    length++
                    if (byte and 0b1000_0000 != 0) break
                }
                pos += length
                val result = record.readString(pos, value, charset)
                pos += value
                cncx[cncxRecordOffset + index] = result
            }
            cncxRecordOffset += 0x10000
        }

        return cncx
    }

    private fun readIndxHeader(indx: ByteBuffer): IndxHeader {
        val magic = indx.readString(0, 4)
        if (magic != "INDX") {
            error("Invalid INDX record")
        }
        val length = indx.readUInt32(4)
        val type = indx.readUInt32(8)
        val idxt = indx.readUInt32(20)
        val numRecords = indx.readUInt32(24)
        val encoding = indx.readUInt32(28)
        val language = indx.readUInt32(32)
        val total = indx.readUInt32(36)
        val ordt = indx.readUInt32(40)
        val ligt = indx.readUInt32(44)
        val numLigt = indx.readUInt32(48)
        val numCncx = indx.readUInt32(52)

        return IndxHeader(
            magic, length, type, idxt, numRecords, encoding, language, total, ordt,
            ligt, numLigt, numCncx
        )
    }

    private fun readTagxHeader(buffer: ByteBuffer): TagxHeader {
        val magic = buffer.readString(0, 4)
        if (magic != "TAGX") {
            error("Invalid INDX record")
        }
        val length = buffer.readUInt32(4)
        val numControlBytes = buffer.readUInt32(8)

        return TagxHeader(magic, length, numControlBytes)
    }

    fun close() {
        pdbFile.close()
    }

    protected fun finalize() {
        close()
    }

    companion object {
        private val emptyByteArrayInputStream = ByteArrayInputStream(ByteArray(0))
    }

}
