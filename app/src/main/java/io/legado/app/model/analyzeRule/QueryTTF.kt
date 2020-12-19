package io.legado.app.model.analyzeRule

import org.apache.commons.lang3.tuple.Pair
import org.apache.commons.lang3.tuple.Triple
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.experimental.and

class QueryTTF(buffer: ByteArray) {
    private class Header(
            var majorVersion: Int = 0,
            var minorVersion: Int = 0,
            var numOfTables: Int = 0,
            var searchRange: Int = 0,
            var entrySelector: Int = 0,
            var rangeShift: Int = 0,
    )

    private class Directory {
        lateinit var tag: String  // table name
        var checkSum: Int = 0 // Check sum
        var offset: Int = 0    // Offset from beginning of file
        var length: Int = 0   // length of the table in bytes
    }

    private class NameLayout(
            var format: Int = 0,
            var count: Int = 0,
            var stringOffset: Int = 0,
            var records: MutableList<NameRecord> = LinkedList(),
    )

    private class NameRecord(
            var platformID: Int = 0, // 平台标识符<0:Unicode, 1:Mac, 2:ISO, 3:Windows, 4:Custom>
            var encodingID: Int = 0, // 编码标识符
            var languageID: Int = 0, // 语言标识符
            var nameID: Int = 0, // 名称标识符
            var length: Int = 0,// 名称字符串的长度
            var offset: Int = 0, // 名称字符串相对于stringOffset的字节偏移量
    )

    private class HeadLayout(
            var majorVersion: Int = 0,
            var minorVersion: Int = 0,
            var fontRevision: Int = 0,
            var checkSumAdjustment: Int = 0,
            var magicNumber: Int = 0,
            var flags: Int = 0,
            var unitsPerEm: Int = 0,
            var created: Long = 0,
            var modified: Long = 0,
            var xMin: Short = 0,
            var yMin: Short = 0,
            var xMax: Short = 0,
            var yMax: Short = 0,
            var macStyle: Int = 0,
            var lowestRecPPEM: Int = 0,
            var fontDirectionHint: Short = 0,
            var indexToLocFormat: Short = 0, // <0:loca是2字节数组, 1:loca是4字节数组>
            var glyphDataFormat: Short = 0,
    )

    private class MaxpLayout(
            var majorVersion: Int = 0,
            var minorVersion: Int = 0,
            var numGlyphs: Int = 0, // 字体中的字形数量
            var maxPoints: Int = 0,
            var maxContours: Int = 0,
            var maxCompositePoints: Int = 0,
            var maxCompositeContours: Int = 0,
            var maxZones: Int = 0,
            var maxTwilightPoints: Int = 0,
            var maxStorage: Int = 0,
            var maxFunctionDefs: Int = 0,
            var maxInstructionDefs: Int = 0,
            var maxStackElements: Int = 0,
            var maxSizeOfInstructions: Int = 0,
            var maxComponentElements: Int = 0,
            var maxComponentDepth: Int = 0,
    )

    private class CmapLayout(
            var version: Int = 0,
            var numTables: Int = 0,
            var records: MutableList<CmapRecord> = LinkedList(),
            var tables: MutableMap<Int, CmapFormat> = HashMap(),
    )

    private class CmapRecord(
            var platformID: Int = 0,
            var encodingID: Int = 0,
            var offset: Int = 0,
    )

    private open class CmapFormat {
        var format: Int = 0
        open var length: Int = 0
        open var language: Int = 0
        lateinit var glyphIdArray: IntArray
    }

    private class CmapFormat4 : CmapFormat() {
        var segCountX2: Int = 0
        var searchRange: Int = 0
        var entrySelector: Int = 0
        var rangeShift: Int = 0
        lateinit var endCode: IntArray
        var reservedPad: Int = 0
        lateinit var startCode: IntArray
        lateinit var idDelta: ShortArray
        lateinit var idRangeOffset: IntArray
//        override lateinit var glyphIdArray: IntArray
    }

    private class CmapFormat6(
            var firstCode: Int = 0,
            var entryCount: Int = 0,
    ) : CmapFormat()

    private class CmapFormat12(
            var reserved: Int = 0,
            override var length: Int = 0,
            override var language: Int = 0,
            var numGroups: Int = 0,
            var groups: MutableList<Triple<Int, Int, Int>>
    ) : CmapFormat()

    private class GlyfLayout(
            var numberOfContours: Short = 0, // 非负值为简单字型,负值为符合字型
            var xMin: Short = 0,
            var yMin: Short = 0,
            var xMax: Short = 0,
            var yMax: Short = 0,
            var instructionLength: Int = 0
            ) {
        lateinit var endPtsOfContours: IntArray // length=numberOfContours
        lateinit var instructions: IntArray // length=instructionLength
        lateinit var flags: ByteArray
        lateinit var xCoordinates: ShortArray // length = flags.length
        lateinit var yCoordinates: ShortArray // length = flags.length
    }

    private class ByteArrayReader(var buffer: ByteArray, var index: Int) {
        fun readUIntX(len: Long): Long {
            var result: Long = 0
            for (i in 0 until len) {
                result = result shl 8
                result = result or ((buffer[index++] and 0xFF.toByte()).toLong())
            }
            return result
        }

        fun readUInt64(): Long = readUIntX(8)

        fun readUInt32(): Int = readUIntX(4).toInt()

        fun readUInt16(): Int = readUIntX(2).toInt()

        fun readInt16(): Short = readUIntX(2).toShort()

        fun readUInt8(): Short = readUIntX(1).toShort()

        fun readStrings(len: Int, charset: Charset): String {
            val result = ByteArray(len)
            for (i in 0 until len) {
                result[i] = buffer[index++]
            }
            return String(result, charset)
        }

        fun getByte() = buffer[index++]

        fun getBytes(len: Int): IntArray {
            val result = IntArray(len)
            for (i in 0 until len) {
                result[i] = buffer[index++].toInt()
            }
            return result
        }

        fun getUInt16Array(len: Int): IntArray {
            val result = IntArray(len)
            for (i in 0 until len) {
                result[i] = readUInt16()
            }
            return result
        }

        fun getInt16Array(len: Int): ShortArray {
            val result = ShortArray(len)
            for (i in 0 until len) {
                result[i] = readInt16()
            }
            return result
        }
    }

    private val fontReader: ByteArrayReader = ByteArrayReader(buffer, 0)
    private val directories: MutableList<Directory> = LinkedList()
    private val name = NameLayout()
    private val cmap = CmapLayout()
    private val pps: Array<Pair<Int, Int>> = arrayOf(
            Pair.of(3, 10),
            Pair.of(0, 4),
            Pair.of(3, 1),
            Pair.of(1, 0),
            Pair.of(0, 3),
            Pair.of(0, 1)
    )
    private val codeToGlyph: MutableMap<Int, String> = HashMap()
    private val glyphToCode: MutableMap<String, Int> = HashMap()
    private var limitMix = 0
    private var limitMax = 0

    /**
     * 获取字体信息 (1=字体名称)
     *
     * @param nameId 传入十进制字体信息索引
     * @return 返回查询结果字符串
     */
    fun getNameById(nameId: Int): String {
        for (temp in directories) {
            if ("name" != temp.tag) {
                continue
            }
            fontReader.index = temp.offset
            break
        }
        for (record in name.records) {
            if (record.nameID != nameId) {
                continue
            }
            fontReader.index += name.stringOffset + record.offset
            return fontReader.readStrings(record.length, if (record.platformID == 1) StandardCharsets.UTF_8 else StandardCharsets.UTF_16BE)
        }
        return "error"
    }

    /**
     * 使用Unicode值查找轮廓索引
     *
     * @param code 传入Unicode十进制值
     * @return 返回十进制轮廓索引
     */
    private fun getGlyfIndex(code: Int): Int {
        if (code == 0) {
            return 0
        }
        var fmtKey = 0
        for (item in pps) {
            for (record in cmap.records) {
                if (item.left == record.platformID && item.right == record.encodingID) {
                    fmtKey = record.offset
                    break
                }
            }
            if (fmtKey > 0) {
                break
            }
        }
        if (fmtKey == 0) {
            return 0
        }
        var glyfID = 0
        val table = cmap.tables[fmtKey]
        when (table?.format) {
            0 -> {
                if (code < table.glyphIdArray.size) {
                    glyfID = table.glyphIdArray[code] and 0xFF
                }
            }
            4 -> {
                val tab = table as CmapFormat4
                if (code > tab.endCode[tab.endCode.size - 1]) {
                    return 0
                }
                // 二分法查找数值索引
                var start = 0
                var middle: Int
                var end = tab.endCode.size - 1
                while (start + 1 < end) {
                    middle = (start + end) / 2
                    if (tab.endCode[middle] <= code) {
                        start = middle
                    } else {
                        end = middle
                    }
                }
                if (tab.endCode[start] < code) {
                    ++start
                }
                if (code < tab.startCode[start]) {
                    return 0
                }
                glyfID = if (tab.idRangeOffset[start] != 0) {
                    tab.glyphIdArray[code - tab.startCode[start] + (tab.idRangeOffset[start] shr 1) - (tab.idRangeOffset.size - start)]
                } else {
                    code + tab.idDelta[start]
                }
                glyfID = glyfID and 0xFFFF
            }
            6 -> {
                val tab = table as CmapFormat6
                val index = code - tab.firstCode
                glyfID = if (index < 0 || index >= tab.glyphIdArray.size) {
                    0
                } else {
                    tab.glyphIdArray[index]
                }
            }
            12 -> {
                val tab = table as CmapFormat12
                if (code > tab.groups[tab.numGroups - 1].middle) {
                    return 0
                }
                // 二分法查找数值索引
                var start = 0
                var middle: Int
                var end = tab.numGroups - 1
                while (start + 1 < end) {
                    middle = (start + end) / 2
                    if (tab.groups[middle].left <= code) {
                        start = middle
                    } else {
                        end = middle
                    }
                }
                if (tab.groups[start].left <= code && code <= tab.groups[start].middle) {
                    glyfID = tab.groups[start].right + code - tab.groups[start].left
                }
            }
        }
        return glyfID
    }

    /**
     * 判断Unicode值是否在字体范围内
     * @param code 传入Unicode十进制值
     * @return 返回bool查询结果
     */
    fun inLimit(code: Char): Boolean = code.toInt() in limitMix until limitMax

    /**
     * 使用Unicode值获取轮廓数据
     *
     * @param key 传入Unicode十进制值
     * @return 返回轮廓数组的String值
     */
    fun getGlyfByCode(key: Int): String = codeToGlyph.getOrDefault(key, "")

    /**
     * 使用轮廓数据获取Unicode值
     *
     * @param val 传入轮廓数组的String值
     * @return 返回Unicode十进制值
     */
    fun getCodeByGlyf(`val`: String): Int = glyphToCode.getOrDefault(`val`, 0)

    /**
     * 构造函数 buffer 传入TTF字体二进制数组
     */
    init {
        // 获取文件头
        val fileHeader = Header()
        fileHeader.majorVersion = fontReader.readUInt16()
        fileHeader.minorVersion = fontReader.readUInt16()
        fileHeader.numOfTables = fontReader.readUInt16()
        fileHeader.searchRange = fontReader.readUInt16()
        fileHeader.entrySelector = fontReader.readUInt16()
        fileHeader.rangeShift = fontReader.readUInt16()
        // 获取目录
        for (i in 0 until fileHeader.numOfTables) {
            val d = Directory()
            d.tag = fontReader.readStrings(4, StandardCharsets.US_ASCII)
            d.checkSum = fontReader.readUInt32()
            d.offset = fontReader.readUInt32()
            d.length = fontReader.readUInt32()
            directories.add(d)
        }
        // 解析表 name (字体信息,包含版权、名称、作者等...)
        for (temp in directories) {
            if ("name" == temp.tag) {
                fontReader.index = temp.offset
                name.format = fontReader.readUInt16()
                name.count = fontReader.readUInt16()
                name.stringOffset = fontReader.readUInt16()
                for (i in 0 until name.count) {
                    val record = NameRecord()
                    record.platformID = fontReader.readUInt16()
                    record.encodingID = fontReader.readUInt16()
                    record.languageID = fontReader.readUInt16()
                    record.nameID = fontReader.readUInt16()
                    record.length = fontReader.readUInt16()
                    record.offset = fontReader.readUInt16()
                    name.records.add(record)
                }
            }
        }
        // 解析表 head (获取 head.indexToLocFormat)
        val head = HeadLayout()
        for (temp in directories) {
            if ("head" == temp.tag) {
                fontReader.index = temp.offset
                head.majorVersion = fontReader.readUInt16()
                head.minorVersion = fontReader.readUInt16()
                head.fontRevision = fontReader.readUInt32()
                head.checkSumAdjustment = fontReader.readUInt32()
                head.magicNumber = fontReader.readUInt32()
                head.flags = fontReader.readUInt16()
                head.unitsPerEm = fontReader.readUInt16()
                head.created = fontReader.readUInt64()
                head.modified = fontReader.readUInt64()
                head.xMin = fontReader.readInt16()
                head.yMin = fontReader.readInt16()
                head.xMax = fontReader.readInt16()
                head.yMax = fontReader.readInt16()
                head.macStyle = fontReader.readUInt16()
                head.lowestRecPPEM = fontReader.readUInt16()
                head.fontDirectionHint = fontReader.readInt16()
                head.indexToLocFormat = fontReader.readInt16()
                head.glyphDataFormat = fontReader.readInt16()
            }
        }
        // 解析表 maxp (获取 maxp.numGlyphs)
        val maxp = MaxpLayout()
        for (temp in directories) {
            if ("maxp" == temp.tag) {
                fontReader.index = temp.offset
                maxp.majorVersion = fontReader.readUInt16()
                maxp.minorVersion = fontReader.readUInt16()
                maxp.numGlyphs = fontReader.readUInt16()
                maxp.maxPoints = fontReader.readUInt16()
                maxp.maxContours = fontReader.readUInt16()
                maxp.maxCompositePoints = fontReader.readUInt16()
                maxp.maxCompositeContours = fontReader.readUInt16()
                maxp.maxZones = fontReader.readUInt16()
                maxp.maxTwilightPoints = fontReader.readUInt16()
                maxp.maxStorage = fontReader.readUInt16()
                maxp.maxFunctionDefs = fontReader.readUInt16()
                maxp.maxInstructionDefs = fontReader.readUInt16()
                maxp.maxStackElements = fontReader.readUInt16()
                maxp.maxSizeOfInstructions = fontReader.readUInt16()
                maxp.maxComponentElements = fontReader.readUInt16()
                maxp.maxComponentDepth = fontReader.readUInt16()
            }
        }
        // 解析表 loca (轮廓数据偏移地址表)
        val loca: MutableList<Int> = LinkedList()
        for (temp in directories) {
            if ("loca" == temp.tag) {
                fontReader.index = temp.offset
                val offset = if (head.indexToLocFormat == 0.toShort()) 2 else 4
                var i: Long = 0
                while (i < temp.length) {
                    loca.add(if (offset == 2) fontReader.readUInt16() shl 1 else fontReader.readUInt32())
                    i += offset
                }
            }
        }
        // 解析表 cmap (Unicode编码轮廓索引对照表)
        for (temp in directories) {
            if ("cmap" == temp.tag) {
                fontReader.index = temp.offset
                cmap.version = fontReader.readUInt16()
                cmap.numTables = fontReader.readUInt16()
                for (i in 0 until cmap.numTables) {
                    val record = CmapRecord()
                    record.platformID = fontReader.readUInt16()
                    record.encodingID = fontReader.readUInt16()
                    record.offset = fontReader.readUInt32()
                    cmap.records.add(record)
                }
                for (i in 0 until cmap.numTables) {
                    val fmtOffset = cmap.records[i].offset
                    fontReader.index = temp.offset + fmtOffset
                    val endIndex = fontReader.index
                    val format = fontReader.readUInt16()
                    if (cmap.tables.containsKey(fmtOffset)) {
                        continue
                    }
                    when (format) {
                        0 -> {
                            val f = CmapFormat()
                            f.format = format
                            f.length = fontReader.readUInt16()
                            f.language = fontReader.readUInt16()
                            f.glyphIdArray = fontReader.getBytes(f.length - 6)
                            cmap.tables[fmtOffset] = f
                        }
                        4 -> {
                            val f = CmapFormat4()
                            f.format = format
                            f.length = fontReader.readUInt16()
                            f.language = fontReader.readUInt16()
                            f.segCountX2 = fontReader.readUInt16()
                            val segCount = f.segCountX2 shr 1
                            f.searchRange = fontReader.readUInt16()
                            f.entrySelector = fontReader.readUInt16()
                            f.rangeShift = fontReader.readUInt16()
                            f.endCode = fontReader.getUInt16Array(segCount)
                            f.reservedPad = fontReader.readUInt16()
                            f.startCode = fontReader.getUInt16Array(segCount)
                            f.idDelta = fontReader.getInt16Array(segCount)
                            f.idRangeOffset = fontReader.getUInt16Array(segCount)
                            f.glyphIdArray = fontReader.getUInt16Array(endIndex + f.length - fontReader.index shr 1)
                            cmap.tables[fmtOffset] = f
                        }
                        6 -> {
                            val f = CmapFormat6()
                            f.format = format
                            f.length = fontReader.readUInt16()
                            f.language = fontReader.readUInt16()
                            f.firstCode = fontReader.readUInt16()
                            f.entryCount = fontReader.readUInt16()
                            f.glyphIdArray = fontReader.getUInt16Array(f.entryCount)
                            cmap.tables[fmtOffset] = f
                        }
                        12 -> {
                            val f = CmapFormat12(
                                    reserved = fontReader.readUInt16(),
                                    length = fontReader.readUInt32(),
                                    language = fontReader.readUInt32(),
                                    numGroups = fontReader.readUInt32(),
                                    groups = ArrayList(fontReader.readUInt32())
                            )
                            f.format = format
                            for (n in 0 until f.numGroups) {
                                (f.groups as ArrayList<Triple<Int, Int, Int>>).add(Triple.of(fontReader.readUInt32(), fontReader.readUInt32(), fontReader.readUInt32()))
                            }
                            cmap.tables[fmtOffset] = f
                        }
                    }
                }
            }
        }
        // 解析表 glyf (字体轮廓数据表)
        val glyf: MutableList<GlyfLayout> = LinkedList()
        for (temp in directories) {
            if ("glyf" == temp.tag) {
                fontReader.index = temp.offset
                for (i in 0 until maxp.numGlyphs) {
                    fontReader.index = temp.offset + loca[i]
                    val numberOfContours = fontReader.readInt16()
                    if (numberOfContours > 0) {
                        val g = GlyfLayout()
                        g.numberOfContours = numberOfContours
                        g.xMin = fontReader.readInt16()
                        g.yMin = fontReader.readInt16()
                        g.xMax = fontReader.readInt16()
                        g.yMax = fontReader.readInt16()
                        g.endPtsOfContours = fontReader.getUInt16Array(numberOfContours.toInt())
                        g.instructionLength = fontReader.readUInt16()
                        g.instructions = fontReader.getBytes(g.instructionLength)
                        val flagLength = g.endPtsOfContours[g.endPtsOfContours.size - 1] + 1
                        // 获取轮廓点描述标志
                        g.flags = ByteArray(flagLength).apply {
                            var n = 0
                            while (n < flagLength) {
                                this[n] = fontReader.getByte()
                                if ((this[n] and 0x08) != 0x00.toByte()) {
                                    for (m in fontReader.readUInt8() downTo 1) {
                                        this[++n] = this[n - 1]
                                    }
                                }
                                ++n
                            }
                        }
                        // 获取轮廓点描述x轴相对值
                        g.xCoordinates = ShortArray(flagLength)
                        for (n in 0 until flagLength) {
                            val same = (if (g.flags[n] and 0x10 != 0.toByte()) 1 else -1).toShort()
                            if (g.flags[n] and 0x02 != 0.toByte()) {
                                g.xCoordinates[n] = (same * fontReader.readUInt8()).toShort()
                            } else {
                                g.xCoordinates[n] = if (same.toInt() == 1) 0.toShort() else fontReader.readInt16()
                            }
                        }
                        // 获取轮廓点描述y轴相对值
                        g.yCoordinates = ShortArray(flagLength)
                        for (n in 0 until flagLength) {
                            val same = (if (g.flags[n] and 0x20 != 0.toByte()) 1 else -1).toShort()
                            if (g.flags[n] and 0x04 != 0.toByte()) {
                                g.yCoordinates[n] = (same * fontReader.readUInt8()).toShort()
                            } else {
                                g.yCoordinates[n] = if (same.toInt() == 1) 0.toShort() else fontReader.readInt16()
                            }
                        }
                        // 相对坐标转绝对坐标
//                        for (int n = 1; n < flagLength; ++n) {
//                            xCoordinates[n] += xCoordinates[n - 1];
//                            yCoordinates[n] += yCoordinates[n - 1];
//                        }
                        glyf.add(g)
                    } //else情况：复合字体暂未使用
                }
            }
        }

        // 建立Unicode&Glyph双向表
        var key = 0
        while (key < 130000) {
            if (key == 0xFF) {
                key = 0x3400
            }
            val gid = getGlyfIndex(key)
            if (gid == 0) {
                ++key
                continue
            }
            val sb = StringBuilder()
            // 字型数据转String，方便存HashMap
            for (b in glyf[gid].xCoordinates) {
                sb.append(b.toInt())
            }
            for (b in glyf[gid].yCoordinates) {
                sb.append(b.toInt())
            }
            val `val` = sb.toString()
            if (limitMix == 0) {
                limitMix = key
            }
            limitMax = key
            codeToGlyph[key] = `val`
            if (glyphToCode.containsKey(`val`)) {
                ++key
                continue
            }
            glyphToCode[`val`] = key
            ++key
        }
    }
}