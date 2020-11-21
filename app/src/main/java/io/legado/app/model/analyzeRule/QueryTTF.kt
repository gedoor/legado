package io.legado.app.model.analyzeRule

import io.legado.app.help.JsExtensions
import java.nio.charset.Charset
import kotlin.experimental.and

/**
 * 解析TTF字体
 * @see <a href="https://docs.microsoft.com/en-us/typography/opentype/spec/">获取详情</a>
 * @see <a href="https://photopea.github.io/Typr.js/demo/index.html">基于Javascript的TTF解析器</a>
 */
@ExperimentalUnsignedTypes
class QueryTTF(var Font: ByteArray) : JsExtensions {
    private class Header {
        var majorVersion: UShort = 0u
        var minorVersion: UShort = 0u
        var numOfTables: UShort = 0u
        var searchRange: UShort = 0u
        var entrySelector: UShort = 0u
        var rangeShift: UShort = 0u
    }

    private class Directory {
        var tag: String = ""
        var checkSum: UInt = 0u
        var offset: UInt = 0u
        var length: UInt = 0u
    }

    private class NameLayout {
        var format: UShort = 0u
        var count: UShort = 0u
        var stringOffset: UShort = 0u
        var records = ArrayList<NameRecord>()
    }

    private class NameRecord {
        var platformID: UShort = 0u
        var encodingID: UShort = 0u
        var languageID: UShort = 0u
        var nameID: UShort = 0u
        var length: UShort = 0u
        var offset: UShort = 0u
    }

    private class HeadLayout {
        var majorVersion: UShort = 0u
        var minorVersion: UShort = 0u
        var fontRevision: UInt = 0u
        var checkSumAdjustment: UInt = 0u
        var magicNumber: UInt = 0u
        var flags: UShort = 0u
        var unitsPerEm: UShort = 0u
        var created: ULong = 0u
        var modified: ULong = 0u
        var xMin: Short = 0
        var yMin: Short = 0
        var xMax: Short = 0
        var yMax: Short = 0
        var macStyle: UShort = 0u
        var lowestRecPPEM: UShort = 0u
        var fontDirectionHint: Short = 0
        var indexToLocFormat: Short = 0
        var glyphDataFormat: Short = 0
    }

    private class MaxpLayout {
        var majorVersion: UShort = 0u
        var minorVersion: UShort = 0u
        var numGlyphs: UShort = 0u // 字体中的字形数量
        var maxPoints: UShort = 0u
        var maxContours: UShort = 0u
        var maxCompositePoints: UShort = 0u
        var maxCompositeContours: UShort = 0u
        var maxZones: UShort = 0u
        var maxTwilightPoints: UShort = 0u
        var maxStorage: UShort = 0u
        var maxFunctionDefs: UShort = 0u
        var maxInstructionDefs: UShort = 0u
        var maxStackElements: UShort = 0u
        var maxSizeOfInstructions: UShort = 0u
        var maxComponentElements: UShort = 0u
        var maxComponentDepth: UShort = 0u
    }

    private class CmapLayout {
        var version: UShort = 0u
        var numTables: UShort = 0u
        var records = ArrayList<CmapRecord>()
        var tables = mutableMapOf<UInt, Any>()
    }

    private class CmapRecord {
        var platformID: UShort = 0u
        var encodingID: UShort = 0u
        var offset: UInt = 0u
    }

    private class CmapFormat0  {
        var format: UShort = 0u
        var length: UShort = 0u
        var language: UShort = 0u
        var glyphIdArray = ArrayList<Byte>()
    }

    private class CmapFormat4 {
        var format: UShort = 0u
        var length: UShort = 0u
        var language: UShort = 0u
        var segCountX2: UShort = 0u
        var searchRange: UShort = 0u
        var entrySelector: UShort = 0u
        var rangeShift: UShort = 0u
        lateinit var endCode: ArrayList<UShort>              // UInt16[]
        var reservedPad: UShort = 0u
        lateinit var startCode: ArrayList<UShort>            // UInt16[]
        lateinit var idDelta: ArrayList<Short>
        lateinit var idRangeOffset: ArrayList<UShort>        // UInt16[]
        var glyphIdArray = ArrayList<UShort>()
    }

    private class CmapFormat6 {
        var format: UShort = 0u
        var length: UShort = 0u
        var language: UShort = 0u
        var firstCode: UShort = 0u
        var entryCount: UShort = 0u
        var glyphIdArray = ArrayList<UShort>()
    }

    private class CmapFormat12 {
        var format: UShort = 0u
        var reserved: UShort = 0u
        var length: UInt = 0u
        var language: UInt = 0u
        var numGroups: UInt = 0u
        lateinit var groups: ArrayList<Triple<UInt, UInt, UInt>>
    }

    private class GlyfLayout {
        var numberOfContours: Short = 0
        var xMin: Short = 0
        var yMin: Short = 0
        var xMax: Short = 0
        var yMax: Short = 0
        lateinit var endPtsOfContours: ArrayList<UShort>    // UInt16[]
        var instructionLength: UShort = 0u                  // UInt16
        lateinit var instructions: ArrayList<Byte>
        lateinit var flags: ArrayList<Byte>
        lateinit var xCoordinates: ArrayList<Short>
        lateinit var yCoordinates: ArrayList<Short>
    }

    @Suppress("unused")
    private class ByteArrayReader(var Buffer: ByteArray, var Index: Int) {
        fun readUIntX(len: Long): ULong {
            var result: ULong = 0u
            for (i in 0 until len) {
                result.shl(8)
                result = result or Buffer[Index++].toULong()
            }
            return result
        }

        fun readUInt64(): ULong {
            return readUIntX(8)
        }

        fun readInt64(): Long {
            return readUIntX(8).toLong()
        }

        fun readUInt32(): UInt {
            return readUIntX(4).toUInt()
        }

        fun readInt32(): Int {
            return readUIntX(4).toInt()
        }

        fun readUInt16(): UShort {
            return readUIntX(2).toUShort()
        }

        fun readInt16(): Short {
            return readUIntX(2).toShort()
        }

        fun readStrings(len: Int, charset: Charset): String {
            if (len <= 0) return ""
            val result = ByteArray(len)
            for (i in 0 until len) {
                result[i] = Buffer[Index++]
            }
            return result.toString(charset)
        }

        fun getByte(): Byte {
            return Buffer[Index++]
        }

        fun getBytes(len: Int): ArrayList<Byte> {
            if (len <= 0) return ArrayList(0)
            val result = ArrayList<Byte>(len)
            for (i in 0 until len) {
                result[i] = Buffer[Index++]
            }
            return result
        }

        fun getUInt16Array(len: Int): ArrayList<UShort> {
            if (len <= 0) return ArrayList(0)
            val result = ArrayList<UShort>(len)
            for (i in 0 until len) {
                result[i] = readUInt16()
            }
            return result
        }

        fun getInt16Array(len: Int): ArrayList<Short> {
            if (len <= 0) return ArrayList(0)
            val result = ArrayList<Short>(len)
            for (i in 0 until len) {
                result[i] = readInt16()
            }
            return result
        }
    }

    private var fontReader: ByteArrayReader
    private var fileHeader = Header()
    private var directorys = ArrayList<Directory>()
    private var name = NameLayout()
    private var head = HeadLayout()
    private var maxp = MaxpLayout()
    private var loca = ArrayList<UInt>()
    private var cmap = CmapLayout()
    private var glyf = ArrayList<GlyfLayout>()
    private var unicodeMap = mutableMapOf<Int, ArrayList<Short>>()

    init {
        fontReader = ByteArrayReader(Font, 0)
        // 获取文件头
        fileHeader.majorVersion = fontReader.readUInt16()
        fileHeader.minorVersion = fontReader.readUInt16()
        fileHeader.numOfTables = fontReader.readUInt16()
        fileHeader.searchRange = fontReader.readUInt16()
        fileHeader.entrySelector = fontReader.readUInt16()
        fileHeader.rangeShift = fontReader.readUInt16()
        // 获取目录
        for (i in 0 until fileHeader.numOfTables.toInt()) {
            val tag = fontReader.readStrings(4, Charsets.US_ASCII)
            val t = Directory()
            t.tag = tag
            t.checkSum = fontReader.readUInt32()
            t.offset = fontReader.readUInt32()
            t.length = fontReader.readUInt32()
            directorys.add(t)
        }
        // 解析表 name (字体信息,包含版权、名称、作者等...)
        for (temp in directorys) {
            if (temp.tag == "name") {
                fontReader.Index = temp.offset.toInt()
                name.format = fontReader.readUInt16()
                name.count = fontReader.readUInt16()
                name.stringOffset = fontReader.readUInt16()

                for (i in 0 until name.count.toInt()) {
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
        for (temp in directorys) {
            if (temp.tag == "head") {
                fontReader.Index = temp.offset.toInt()
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
        for (temp in directorys) {
            if (temp.tag == "maxp") {
                fontReader.Index = temp.offset.toInt()
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
        for (temp in directorys) {
            if (temp.tag == "loca") {
                fontReader.Index = temp.offset.toInt()
                val offset: UInt = if (head.indexToLocFormat.toInt() == 0) 2u else 4u
                var i: UInt = 0u
                while (i < temp.length) {
                    loca.add(
                        if (offset == 2u) fontReader.readUInt16().toUInt()
                            .shl(1) else fontReader.readUInt32()
                    )
                    i += offset
                }
            }
        }
        // 解析表 cmap (Unicode编码轮廓索引对照表)
        for (temp in directorys) {
            if (temp.tag == "cmap") {
                fontReader.Index = temp.offset.toInt()
                cmap.version = fontReader.readUInt16()
                cmap.numTables = fontReader.readUInt16()

                for (i in 0 until cmap.numTables.toInt()) {
                    val record = CmapRecord()
                    record.platformID = fontReader.readUInt16()
                    record.encodingID = fontReader.readUInt16()
                    record.offset = fontReader.readUInt32()
                    cmap.records.add(record)
                }
                for (i in 0 until cmap.numTables.toInt()) {
                    val fmtOffset = cmap.records[i].offset
                    fontReader.Index = (temp.offset + fmtOffset).toInt()
                    val endIndex = fontReader.Index

                    val format = fontReader.readUInt16()

                    if (cmap.tables.contains(fmtOffset.toInt())) continue
                    when {
                        format.equals(0) -> {
                            val fmt = CmapFormat0()
                            fmt.format = format
                            fmt.length = fontReader.readUInt16()
                            fmt.language = fontReader.readUInt16()
                            fmt.glyphIdArray = fontReader.getBytes(fmt.length.toInt() - 6)
                            cmap.tables[fmtOffset] = fmt
                        }
                        format.equals(4) -> {
                            val fmt = CmapFormat4()
                            fmt.format = format
                            fmt.length = fontReader.readUInt16()
                            fmt.language = fontReader.readUInt16()
                            fmt.segCountX2 = fontReader.readUInt16()
                            val segCount = fmt.segCountX2.toInt() / 2
                            fmt.searchRange = fontReader.readUInt16()
                            fmt.entrySelector = fontReader.readUInt16()
                            fmt.rangeShift = fontReader.readUInt16()
                            fmt.endCode = fontReader.getUInt16Array(segCount)
                            fmt.reservedPad = fontReader.readUInt16()
                            fmt.startCode = fontReader.getUInt16Array(segCount)
                            fmt.idDelta = fontReader.getInt16Array(segCount)
                            fmt.idRangeOffset = fontReader.getUInt16Array(segCount)
                            fmt.glyphIdArray =
                                fontReader.getUInt16Array((fmt.length.toInt() - (fontReader.Index - endIndex)) / 2)
                            cmap.tables[fmtOffset] = fmt
                        }
                        format.equals(6) -> {
                            val fmt = CmapFormat6()
                            fmt.format = format
                            fmt.length = fontReader.readUInt16()
                            fmt.language = fontReader.readUInt16()
                            fmt.firstCode = fontReader.readUInt16()
                            fmt.entryCount = fontReader.readUInt16()
                            fmt.glyphIdArray = fontReader.getUInt16Array(fmt.entryCount.toInt())
                            cmap.tables[fmtOffset] = fmt
                        }
                        format.equals(12) -> {
                            val fmt = CmapFormat12()
                            fmt.format = format
                            fmt.reserved = fontReader.readUInt16()
                            fmt.length = fontReader.readUInt32()
                            fmt.language = fontReader.readUInt32()
                            fmt.numGroups = fontReader.readUInt32()
                            for (n in 0 until fmt.numGroups.toLong()) {
                                fmt.groups.add(
                                    Triple(
                                        fontReader.readUInt32(),
                                        fontReader.readUInt32(),
                                        fontReader.readUInt32()
                                    )
                                )
                            }
                            cmap.tables[fmtOffset] = fmt
                        }
                    }
                }
            }
        }
        // 解析表 glyf (字体轮廓数据表)
        for (temp in directorys) {
            if (temp.tag == "glyf") {
                fontReader.Index = temp.offset.toInt()
                for (i in 0 until maxp.numGlyphs.toInt()) {
                    fontReader.Index = (temp.offset + loca[i]).toInt()

                    val g = GlyfLayout()
                    g.numberOfContours = fontReader.readInt16()
                    g.xMin = fontReader.readInt16()
                    g.yMin = fontReader.readInt16()
                    g.xMax = fontReader.readInt16()
                    g.yMax = fontReader.readInt16()
                    if (g.numberOfContours > 0) {
                        g.endPtsOfContours = fontReader.getUInt16Array(g.numberOfContours.toInt())
                        g.instructionLength = fontReader.readUInt16()
                        g.instructions = fontReader.getBytes(g.instructionLength.toInt())
                        val flagLength = g.endPtsOfContours.last().toInt() + 1
                        // 获取轮廓点描述标志
                        g.flags = ArrayList(flagLength)
                        var n = 0
                        while (n < flagLength) {
                            g.flags[n] = fontReader.getByte()
                            if ((g.flags[n].and(0x08)).toInt() != 0x00) {
                                for (m in fontReader.getByte() downTo 1) {
                                    g.flags[++n] = g.flags[n - 1]
                                }
                            }
                            ++n
                        }
                        // 获取轮廓点描述x轴相对值
                        g.xCoordinates = ArrayList(flagLength)
                        for (m in 0 until flagLength) {
                            val same = if ((g.flags[m].and(0x10)).toInt() != 0) 1 else -1
                            if ((g.flags[m].and(0x02)).toInt() != 0) {
                                g.xCoordinates[m] = (same * fontReader.getByte()).toShort()
                            } else {
                                g.xCoordinates[m] = if (same == 1) 0 else fontReader.readInt16()
                            }
                        }
                        // 获取轮廓点描述y轴相对值
                        g.yCoordinates = ArrayList(flagLength)
                        for (m in 0 until flagLength) {
                            val same = if ((g.flags[m].and(0x20)).toInt() != 0) 1 else -1
                            if ((g.flags[m].and(0x04)).toInt() != 0) {
                                g.yCoordinates[n] = (same * fontReader.getByte()).toShort()
                            } else {
                                g.yCoordinates[n] = if (same == 1) 0 else fontReader.readInt16()
                            }
                        }
                        /*
                        // 相对坐标转绝对坐标 (因不绘制字体,这里用不上)
                        for(m in 1 until flagLength)
                        {
                            g.xCoordinates[m] = (g.xCoordinates[m] + g.xCoordinates[m - 1]).toShort();
                            g.yCoordinates[m] = (g.yCoordinates[m] + g.yCoordinates[m - 1]).toShort();
                        }
                        */

                        glyf.add(g)
                    } else {
                        // 复合字体暂未使用
                    }
                }
            }
        }

        // 建立Unicode&Glyf映射表
        for (i in 0..130000) {
            val gid = getGlyfIndex(i).toInt()
            if (gid == 0) continue
            if (unicodeMap.containsKey(gid)) continue
            val thisGlyf = ArrayList<Short>()
            thisGlyf.addAll(glyf[gid].xCoordinates)
            thisGlyf.addAll(glyf[gid].yCoordinates)
            unicodeMap[i] = thisGlyf
        }

    }


    /**
     * 获取字体信息 (默认获取字体名称,索引位1)
     */
    @Suppress("unused")
    fun getNameById(nameId: Int = 1): String {
        for (Temp in directorys) {
            if (Temp.tag != "name") continue
            fontReader.Index = Temp.offset.toInt()
            break
        }
        for (record in name.records) {
            if (record.nameID.toInt() != nameId) continue
            fontReader.Index = fontReader.Index + (name.stringOffset + record.offset).toInt()
            return fontReader.readStrings(
                record.length.toInt(),
                if (record.platformID.toInt() == 1) Charsets.UTF_8 else Charsets.UTF_16BE
            )
        }
        return "error"
    }

    var pps = arrayListOf<Pair<UShort, UShort>>(Pair(3u, 10u), Pair(0u, 4u), Pair(3u, 1u), Pair(1u, 0u), Pair(0u, 3u), Pair(0u, 1u))

    /**
     * 使用Unicode值查找轮廓索引
     */
    private fun getGlyfIndex(code: Int): Long {
        var fmtKey: UInt = 0u
        for (record in cmap.records) {
            for (item in pps) {
                if ((item.first == record.platformID) && (item.second == record.encodingID)) {
                    fmtKey = record.offset
                    break
                }
            }
            if (fmtKey > 0u) break
        }
        if (fmtKey == 0u) return 0

        var glyfID: Long = 0
        if (cmap.tables[fmtKey] is CmapFormat0) {
            val tab = cmap.tables[fmtKey] as CmapFormat0
            if (code >= tab.glyphIdArray.size) glyfID = 0
            else glyfID = tab.glyphIdArray[code].toLong()
        } else if (cmap.tables[fmtKey] is CmapFormat4) {
            val tab = cmap.tables[fmtKey] as CmapFormat4
            if (code > tab.endCode.last().toInt()) return 0
            // 二分法查找数值索引
            var start = 0
            var middle: Int
            var end = tab.endCode.size - 1
            while (start + 1 != end) {
                middle = (start + end) / 2
                if (tab.endCode[middle] <= code.toUInt()) start = middle
                else end = middle
            }
            if (tab.endCode[start] < code.toUInt()) ++start
            if (code.toUInt() < tab.startCode[start]) return 0
            glyfID = if (tab.idRangeOffset[start].toInt() != 0) {
                tab.glyphIdArray[code - tab.startCode[start].toInt() + (tab.idRangeOffset[start].toInt() / 2) - (tab.idRangeOffset.size - start)].toLong()
            } else (code + tab.idDelta[start]).toLong()
            glyfID = glyfID.and(0xFFFF)
        } else if (cmap.tables[fmtKey] is CmapFormat6) {
            val tab = cmap.tables[fmtKey] as CmapFormat6
            val index = code - tab.firstCode.toInt()
            glyfID = if (index < 0 || index >= tab.glyphIdArray.size) 0 else tab.glyphIdArray[index].toLong()
        } else if (cmap.tables[fmtKey] is CmapFormat12) {
            val tab = (cmap.tables[fmtKey] as CmapFormat12)
            if (code > tab.groups.last().second.toInt()) return 0
            // 二分法查找数值索引
            var start = 0
            var middle: Int
            var end = tab.groups.size - 1
            while (start + 1 != end) {
                middle = (start + end) / 2
                if (tab.groups[middle].first.toInt() <= code) start = middle
                else end = middle
            }
            if (tab.groups[start].first.toInt() <= code && code <= tab.groups[start].second.toInt()) {
                glyfID =
                    (tab.groups[start].third.toInt() + code - tab.groups[start].first.toInt()).toLong()
            }
        }
        return glyfID
    }

    /**
     * 使用轮廓数据获取Unicode值
     */
    @Suppress("unused")
    fun getCodeByGlyf(inputGlyf: List<Short>): Int {
        var unicodeVal = 0
        if (inputGlyf.isEmpty()) return 0
        for (g in unicodeMap) {
            if (inputGlyf.size != g.value.size) continue
            var isFound = true
            for (i in inputGlyf.indices) {
                if (inputGlyf[i] != g.value[i]) {
                    isFound = false
                    break
                }
            }
            if (isFound) unicodeVal = g.key
        }
        return unicodeVal
    }

    /**
     * 使用Unicode值获取轮廓数据
     */
    @Suppress("unused")
    fun getGlyfByCode(code: Int): ArrayList<Short> {
        if (code <= 0) return ArrayList()
        return unicodeMap.getOrDefault(code, ArrayList())
    }
}