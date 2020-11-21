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

    private class CmapFormat0 {
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

    private class ByteArrayReader(var Buffer: ByteArray, var Index: Int) {
        fun ReadUIntX(len: Long): ULong {
            var result: ULong = 0u
            for (i in 0 until len) {
                result.shl(8)
                result = result or Buffer[Index++].toULong()
            }
            return result
        }

        fun ReadUInt64(): ULong {
            return ReadUIntX(8)
        }

        fun ReadInt64(): Long {
            return ReadUIntX(8).toLong()
        }

        fun ReadUInt32(): UInt {
            return ReadUIntX(4).toUInt()
        }

        fun ReadInt32(): Int {
            return ReadUIntX(4).toInt()
        }

        fun ReadUInt16(): UShort {
            return ReadUIntX(2).toUShort()
        }

        fun ReadInt16(): Short {
            return ReadUIntX(2).toShort()
        }

        fun ReadStrings(len: Int, charset: Charset): String {
            if (len <= 0) return ""
            val result = ByteArray(len)
            for (i in 0 until len) {
                result[i] = Buffer[Index++]
            }
            return result.toString(charset)
        }

        fun GetByte(): Byte {
            return Buffer[Index++]
        }

        fun GetBytes(len: Int): ArrayList<Byte> {
            if (len <= 0) return ArrayList(0)
            val result = ArrayList<Byte>(len)
            for (i in 0 until len) {
                result[i] = Buffer[Index++]
            }
            return result
        }

        fun GetUInt16Array(len: Int): ArrayList<UShort> {
            if (len <= 0) return ArrayList(0)
            val result = ArrayList<UShort>(len)
            for (i in 0 until len) {
                result[i] = ReadUInt16()
            }
            return result
        }

        fun GetInt16Array(len: Int): ArrayList<Short> {
            if (len <= 0) return ArrayList(0)
            val result = ArrayList<Short>(len)
            for (i in 0 until len) {
                result[i] = ReadInt16()
            }
            return result
        }
    }

    private var FontReader: ByteArrayReader
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
        FontReader = ByteArrayReader(Font, 0)
        // 获取文件头
        fileHeader.majorVersion = FontReader.ReadUInt16()
        fileHeader.minorVersion = FontReader.ReadUInt16()
        fileHeader.numOfTables = FontReader.ReadUInt16()
        fileHeader.searchRange = FontReader.ReadUInt16()
        fileHeader.entrySelector = FontReader.ReadUInt16()
        fileHeader.rangeShift = FontReader.ReadUInt16()
        // 获取目录
        for (i in 0 until fileHeader.numOfTables.toInt()) {
            val tag = FontReader.ReadStrings(4, Charsets.US_ASCII)
            val t = Directory()
            t.tag = tag
            t.checkSum = FontReader.ReadUInt32()
            t.offset = FontReader.ReadUInt32()
            t.length = FontReader.ReadUInt32()
            directorys.add(t)
        }
        // 解析表 name (字体信息,包含版权、名称、作者等...)
        for (Temp in directorys) {
            if (Temp.tag == "name") {
                FontReader.Index = Temp.offset.toInt()
                name.format = FontReader.ReadUInt16()
                name.count = FontReader.ReadUInt16()
                name.stringOffset = FontReader.ReadUInt16()

                for (i in 0 until name.count.toInt()) {
                    val record = NameRecord()
                    record.platformID = FontReader.ReadUInt16()
                    record.encodingID = FontReader.ReadUInt16()
                    record.languageID = FontReader.ReadUInt16()
                    record.nameID = FontReader.ReadUInt16()
                    record.length = FontReader.ReadUInt16()
                    record.offset = FontReader.ReadUInt16()
                    name.records.add(record)
                }
            }
        }
        // 解析表 head (获取 head.indexToLocFormat)
        for (Temp in directorys) {
            if (Temp.tag == "head") {
                FontReader.Index = Temp.offset.toInt()
                head.majorVersion = FontReader.ReadUInt16()
                head.minorVersion = FontReader.ReadUInt16()
                head.fontRevision = FontReader.ReadUInt32()
                head.checkSumAdjustment = FontReader.ReadUInt32()
                head.magicNumber = FontReader.ReadUInt32()
                head.flags = FontReader.ReadUInt16()
                head.unitsPerEm = FontReader.ReadUInt16()
                head.created = FontReader.ReadUInt64()
                head.modified = FontReader.ReadUInt64()
                head.xMin = FontReader.ReadInt16()
                head.yMin = FontReader.ReadInt16()
                head.xMax = FontReader.ReadInt16()
                head.yMax = FontReader.ReadInt16()
                head.macStyle = FontReader.ReadUInt16()
                head.lowestRecPPEM = FontReader.ReadUInt16()
                head.fontDirectionHint = FontReader.ReadInt16()
                head.indexToLocFormat = FontReader.ReadInt16()
                head.glyphDataFormat = FontReader.ReadInt16()
            }
        }
        // 解析表 maxp (获取 maxp.numGlyphs)
        for (Temp in directorys) {
            if (Temp.tag == "maxp") {
                FontReader.Index = Temp.offset.toInt()
                maxp.majorVersion = FontReader.ReadUInt16()
                maxp.minorVersion = FontReader.ReadUInt16()
                maxp.numGlyphs = FontReader.ReadUInt16()
                maxp.maxPoints = FontReader.ReadUInt16()
                maxp.maxContours = FontReader.ReadUInt16()
                maxp.maxCompositePoints = FontReader.ReadUInt16()
                maxp.maxCompositeContours = FontReader.ReadUInt16()
                maxp.maxZones = FontReader.ReadUInt16()
                maxp.maxTwilightPoints = FontReader.ReadUInt16()
                maxp.maxStorage = FontReader.ReadUInt16()
                maxp.maxFunctionDefs = FontReader.ReadUInt16()
                maxp.maxInstructionDefs = FontReader.ReadUInt16()
                maxp.maxStackElements = FontReader.ReadUInt16()
                maxp.maxSizeOfInstructions = FontReader.ReadUInt16()
                maxp.maxComponentElements = FontReader.ReadUInt16()
                maxp.maxComponentDepth = FontReader.ReadUInt16()
            }
        }
        // 解析表 loca (轮廓数据偏移地址表)
        for (Temp in directorys) {
            if (Temp.tag == "loca") {
                FontReader.Index = Temp.offset.toInt()
                val offset: UInt = if (head.indexToLocFormat.toInt() == 0) 2u else 4u
                var i: UInt = 0u
                while (i < Temp.length) {
                    loca.add(if (offset == 2u) FontReader.ReadUInt16().toUInt().shl(1) else FontReader.ReadUInt32())
                    i += offset
                }
            }
        }
        // 解析表 cmap (Unicode编码轮廓索引对照表)
        for (Temp in directorys) {
            if (Temp.tag == "cmap") {
                FontReader.Index = Temp.offset.toInt()
                cmap.version = FontReader.ReadUInt16()
                cmap.numTables = FontReader.ReadUInt16()

                for (i in 0 until cmap.numTables.toInt()) {
                    val record = CmapRecord()
                    record.platformID = FontReader.ReadUInt16()
                    record.encodingID = FontReader.ReadUInt16()
                    record.offset = FontReader.ReadUInt32()
                    cmap.records.add(record)
                }
                for (i in 0 until cmap.numTables.toInt()) {
                    val fmtOffset = cmap.records[i].offset
                    FontReader.Index = (Temp.offset + fmtOffset).toInt()
                    val EndIndex = FontReader.Index

                    val format = FontReader.ReadUInt16()

                    if (cmap.tables.contains(fmtOffset.toInt())) continue
                    when {
                        format.equals(0) -> {
                            val fmt = CmapFormat0()
                            fmt.format = format
                            fmt.length = FontReader.ReadUInt16()
                            fmt.language = FontReader.ReadUInt16()
                            fmt.glyphIdArray = FontReader.GetBytes(fmt.length.toInt() - 6)
                            cmap.tables[fmtOffset] = fmt
                        }
                        format.equals(4) -> {
                            val fmt = CmapFormat4()
                            fmt.format = format
                            fmt.length = FontReader.ReadUInt16()
                            fmt.language = FontReader.ReadUInt16()
                            fmt.segCountX2 = FontReader.ReadUInt16()
                            val segCount = fmt.segCountX2.toInt() / 2
                            fmt.searchRange = FontReader.ReadUInt16()
                            fmt.entrySelector = FontReader.ReadUInt16()
                            fmt.rangeShift = FontReader.ReadUInt16()
                            fmt.endCode = FontReader.GetUInt16Array(segCount)
                            fmt.reservedPad = FontReader.ReadUInt16()
                            fmt.startCode = FontReader.GetUInt16Array(segCount)
                            fmt.idDelta = FontReader.GetInt16Array(segCount)
                            fmt.idRangeOffset = FontReader.GetUInt16Array(segCount)
                            fmt.glyphIdArray = FontReader.GetUInt16Array((fmt.length.toInt() - (FontReader.Index - EndIndex)) / 2)
                            cmap.tables[fmtOffset] = fmt
                        }
                        format.equals(6) -> {
                            val fmt = CmapFormat6()
                            fmt.format = format
                            fmt.length = FontReader.ReadUInt16()
                            fmt.language = FontReader.ReadUInt16()
                            fmt.firstCode = FontReader.ReadUInt16()
                            fmt.entryCount = FontReader.ReadUInt16()
                            fmt.glyphIdArray = FontReader.GetUInt16Array(fmt.entryCount.toInt())
                            cmap.tables[fmtOffset] = fmt
                        }
                        format.equals(12) -> {
                            val fmt = CmapFormat12()
                            fmt.format = format
                            fmt.reserved = FontReader.ReadUInt16()
                            fmt.length = FontReader.ReadUInt32()
                            fmt.language = FontReader.ReadUInt32()
                            fmt.numGroups = FontReader.ReadUInt32()
                            for (n in 0 until fmt.numGroups.toLong()) {
                                fmt.groups.add(Triple(FontReader.ReadUInt32(), FontReader.ReadUInt32(), FontReader.ReadUInt32()))
                            }
                            cmap.tables[fmtOffset] = fmt
                        }
                    }
                }
            }
        }
        // 解析表 glyf (字体轮廓数据表)
        for (Temp in directorys) {
            if (Temp.tag == "glyf") {
                FontReader.Index = Temp.offset.toInt()
                for (i in 0 until maxp.numGlyphs.toInt()) {
                    FontReader.Index = (Temp.offset + loca[i]).toInt()

                    val g = GlyfLayout()
                    g.numberOfContours = FontReader.ReadInt16()
                    g.xMin = FontReader.ReadInt16()
                    g.yMin = FontReader.ReadInt16()
                    g.xMax = FontReader.ReadInt16()
                    g.yMax = FontReader.ReadInt16()
                    if (g.numberOfContours > 0) {
                        g.endPtsOfContours = FontReader.GetUInt16Array(g.numberOfContours.toInt())
                        g.instructionLength = FontReader.ReadUInt16()
                        g.instructions = FontReader.GetBytes(g.instructionLength.toInt())
                        val flagLength = g.endPtsOfContours.last().toInt() + 1
                        // 获取轮廓点描述标志
                        g.flags = ArrayList(flagLength)
                        var n = 0
                        while (n < flagLength) {
                            g.flags[n] = FontReader.GetByte()
                            if ((g.flags[n].and(0x08)).toInt() != 0x00) {
                                for (m in FontReader.GetByte() downTo 1) {
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
                                g.xCoordinates[m] = (same * FontReader.GetByte()).toShort()
                            } else {
                                g.xCoordinates[m] = if (same == 1) 0 else FontReader.ReadInt16()
                            }
                        }
                        // 获取轮廓点描述y轴相对值
                        g.yCoordinates = ArrayList(flagLength)
                        for (m in 0 until flagLength) {
                            val same = if ((g.flags[m].and(0x20)).toInt() != 0) 1 else -1
                            if ((g.flags[m].and(0x04)).toInt() != 0) {
                                g.yCoordinates[n] = (same * FontReader.GetByte()).toShort()
                            } else {
                                g.yCoordinates[n] = if (same == 1) 0 else FontReader.ReadInt16()
                            }
                        }
                        /*
                        // 相对坐标转绝对坐标 (因不绘制字体,这里用不上)
                        for(m in 1 until flagLength)
                        {
                            g.xCoordinates[m] = (g.xCoordinates[m] + g.xCoordinates[m - 1]).toShort()
                            g.yCoordinates[m] = (g.yCoordinates[m] + g.yCoordinates[m - 1]).toShort()
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
            val gid = GetGlyfIndex(i).toInt()
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
    fun GetNameById(nameId: Int = 1): String {
        for (Temp in directorys) {
            if (Temp.tag != "name") continue
            FontReader.Index = Temp.offset.toInt()
            break
        }
        for (record in name.records) {
            if (record.nameID.toInt() != nameId) continue
            FontReader.Index = FontReader.Index + (name.stringOffset + record.offset).toInt()
            return FontReader.ReadStrings(record.length.toInt(), if (record.platformID.toInt() == 1) Charsets.UTF_8 else Charsets.UTF_16BE)
        }
        return "error"
    }

    var pps = arrayListOf<Pair<UShort, UShort>>(Pair(3u, 10u), Pair(0u, 4u), Pair(3u, 1u), Pair(1u, 0u), Pair(0u, 3u), Pair(0u, 1u))

    /**
     * 使用Unicode值查找轮廓索引
     */
    private fun GetGlyfIndex(code: Int): Long {
        if (code == 0) return 0
        var fmtKey: UInt = 0u
        for (item in pps) {
            for (record in cmap.records) {
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
            while (start + 1 < end) {
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
            while (start + 1 < end) {
                middle = (start + end) / 2
                if (tab.groups[middle].first.toInt() <= code) start = middle
                else end = middle
            }
            if (tab.groups[start].first.toInt() <= code && code <= tab.groups[start].second.toInt()) {
                glyfID = (tab.groups[start].third.toInt() + code - tab.groups[start].first.toInt()).toLong()
            }
        }
        return glyfID
    }

    /**
     * 使用轮廓数据获取Unicode值
     */
    public fun GetCodeByGlyf(inputGlyf: List<Short>): Int {
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
    public fun GetGlyfByCode(code: Int): ArrayList<Short> {
        if (code <= 0) return ArrayList()
        return unicodeMap.getOrDefault(code, ArrayList())
    }
}