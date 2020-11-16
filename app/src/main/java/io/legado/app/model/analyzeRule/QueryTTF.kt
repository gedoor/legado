package io.legado.app.model.analyzeRule

import io.legado.app.help.JsExtensions
import kotlin.experimental.and

/**
 * 解析TTF字体
 * @see <a href="https://docs.microsoft.com/en-us/typography/opentype/spec/">获取详情</a>
 * @see <a href="https://photopea.github.io/Typr.js/demo/index.html">基于Javascript的TTF解析器</a>
 */
@Suppress("unused", "RedundantExplicitType", "MemberVisibilityCanBePrivate")
@ExperimentalUnsignedTypes
class QueryTTF(var font: ByteArray) : JsExtensions {
    data class Index(var num: Int)

    class FileHeader {
        var majorVersion: UShort = 0u
        var minorVersion: UShort = 0u
        var numOfTables: UShort = 0u
        var searchRange: UShort = 0u
        var entrySelector: UShort = 0u
        var rangeShift: UShort = 0u
    }

    class TableDirectory {
        var tag: String = ""
        var checkSum: UInt = 0u
        var offset: UInt = 0u
        var length: UInt = 0u
        lateinit var data: ByteArray
    }

    class NameTable {
        var format: UShort = 0u
        var count: UShort = 0u
        var stringOffset: UShort = 0u
        var records = ArrayList<NameRecord>()
    }

    class NameRecord {
        var platformID: UShort = 0u
        var encodingID: UShort = 0u
        var languageID: UShort = 0u
        var nameID: UShort = 0u
        var length: UShort = 0u
        var offset: UShort = 0u
        lateinit var nameBuffer: ByteArray
    }

    class HeadTable {
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

    class MaxpTable {
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

    class CmapTable {
        var version: UShort = 0u
        var numTables: UShort = 0u
        var records = ArrayList<EncodingRecord>()
        var tables = mutableMapOf<Int, Any>()
    }

    class EncodingRecord {
        var platformID: UShort = 0u
        var encodingID: UShort = 0u
        var offset: UInt = 0u
    }

    class Format0 {
        var format: UShort = 0u
        var length: UShort = 0u
        var language: UShort = 0u
        var glyphIdArray = ByteArray(256)
    }

    class Format4 {
        var format: UShort = 0u
        var length: UShort = 0u
        var language: UShort = 0u
        var segCountX2: UShort = 0u
        var searchRange: UShort = 0u
        var entrySelector: UShort = 0u
        var rangeShift: UShort = 0u
        lateinit var endCode: IntArray              // UInt16[]
        var reservedPad: UShort = 0u
        lateinit var startCode: IntArray            // UInt16[]
        lateinit var idDelta: ShortArray
        lateinit var idRangeOffset: IntArray        // UInt16[]
        var glyphIdArray = ArrayList<UShort>()
    }

    class GlyfTable {
        var numberOfContours: Short = 0
        var xMin: Short = 0
        var yMin: Short = 0
        var xMax: Short = 0
        var yMax: Short = 0
        lateinit var endPtsOfContours: IntArray     // UInt16[]
        var instructionLength: Int = 0              // UInt16
        lateinit var instructions: ByteArray
        lateinit var flags: ByteArray
        lateinit var xCoordinates: Array<Short>
        lateinit var yCoordinates: Array<Short>
    }

    var header = FileHeader()

    var tables = mutableMapOf<String, TableDirectory>()

    var name = NameTable()

    var head = HeadTable()

    var maxp = MaxpTable()

    var loca = ArrayList<UInt>()

    var cmap = CmapTable()

    var glyf = ArrayList<GlyfTable>()

    // ByteArray转Long整数
    private fun byteArrToUintx(buff: ByteArray): Long {
        var result: Long = 0
        var n: Int = 0
        var i: Int = buff.size
        while (i > 0) {
            result = result or buff[--i].toLong().shl(n)
            n += 8
        }
        return result
    }

    // 索引变量
    private var index = 0

    // 从索引index开始拷贝指定长度的数组
    private fun ByteArray.copyOfIndex(length: Int): ByteArray {
        val fromIndex = index
        index += length
        return this.copyOfRange(fromIndex, fromIndex + length)
    }

    // 从索引fromIndex开始拷贝指定长度的数组
    private fun ByteArray.copyOfIndex(fromIndex: Int, length: Int): ByteArray {
        return this.copyOfRange(fromIndex, fromIndex + length)
    }

    init {

        // 解析文件头
        header = FileHeader()
        // 跳过不需要的数据条目
        index = 4
        header.numOfTables = byteArrToUintx(font.copyOfIndex(2)).toUShort()

        // 获取数据表
        index = 12
        for (i in 0 until header.numOfTables.toInt()) {
            val table = TableDirectory()
            table.tag = font.copyOfIndex(4).toString(Charsets.US_ASCII)
            table.checkSum = byteArrToUintx(font.copyOfIndex(4)).toUInt()
            table.offset = byteArrToUintx(font.copyOfIndex(4)).toUInt()
            table.length = byteArrToUintx(font.copyOfIndex(4)).toUInt()
            table.data = font.copyOfIndex(table.offset.toInt(), table.length.toInt())
            tables[table.tag] = table
        }

        // 解析表 name (字体信息,包含版权、名称、作者等...)
        run {
            val data = tables["name"]!!.data
            index = 0
            name.format = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            name.count = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            name.stringOffset = byteArrToUintx(data.copyOfIndex(2)).toUShort()

            for (i in 0 until name.count.toInt()) {
                val record = NameRecord()
                record.platformID = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                record.encodingID = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                record.languageID = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                record.nameID = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                record.length = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                record.offset = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                record.nameBuffer = data.copyOfIndex(
                    (name.stringOffset + record.offset).toInt(),
                    record.length.toInt()
                )
                name.records.add(record)
            }
        }

        // 解析表 head (获取 head.indexToLocFormat)
        run {
            val data = tables["head"]!!.data
            index = 0
            head.majorVersion = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            head.minorVersion = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            head.fontRevision = byteArrToUintx(data.copyOfIndex(4)).toUInt()
            head.checkSumAdjustment = byteArrToUintx(data.copyOfIndex(4)).toUInt()
            head.magicNumber = byteArrToUintx(data.copyOfIndex(4)).toUInt()
            head.flags = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            head.unitsPerEm = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            head.created = byteArrToUintx(data.copyOfIndex(8)).toULong()
            head.modified = byteArrToUintx(data.copyOfIndex(8)).toULong()
            head.xMin = byteArrToUintx(data.copyOfIndex(2)).toShort()
            head.yMin = byteArrToUintx(data.copyOfIndex(2)).toShort()
            head.xMax = byteArrToUintx(data.copyOfIndex(2)).toShort()
            head.yMax = byteArrToUintx(data.copyOfIndex(2)).toShort()
            head.macStyle = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            head.lowestRecPPEM = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            head.fontDirectionHint = byteArrToUintx(data.copyOfIndex(2)).toShort()
            head.indexToLocFormat = byteArrToUintx(data.copyOfIndex(2)).toShort()
            head.glyphDataFormat = byteArrToUintx(data.copyOfIndex(2)).toShort()
        }

        // 解析表 maxp (获取 maxp.numGlyphs)
        run {
            val data = tables["maxp"]!!.data
            index = 0
            maxp.majorVersion = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.minorVersion = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.numGlyphs = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.maxPoints = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.maxContours = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.maxCompositePoints = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.maxCompositeContours = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.maxZones = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.maxTwilightPoints = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.maxStorage = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.maxFunctionDefs = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.maxInstructionDefs = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.maxStackElements = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.maxSizeOfInstructions = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.maxComponentElements = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            maxp.maxComponentDepth = byteArrToUintx(data.copyOfIndex(2)).toUShort()
        }

        // 解析表 loca (轮廓数据偏移地址表)
        run {
            val data = tables["maxp"]!!.data
            val offset = if (head.indexToLocFormat.equals(0)) 2 else 4
            index = 0
            while (index < data.size) {
                loca.add((byteArrToUintx(data.copyOfIndex(offset)) * (if (offset == 4) 1 else 2)).toUInt())
                index += offset
            }
        }

        // 解析表 cmap (Unicode编码轮廓索引对照表)
        run {
            val data = tables["cmap"]!!.data
            index = 0
            cmap.version = byteArrToUintx(data.copyOfIndex(2)).toUShort()
            cmap.numTables = byteArrToUintx(data.copyOfIndex(2)).toUShort()

            for (i in 0 until cmap.numTables.toInt()) {
                val record = EncodingRecord()
                record.platformID = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                record.encodingID = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                record.offset = byteArrToUintx(data.copyOfIndex(4)).toUInt()
                cmap.records.add(record)
                val tmpIndex = index   // 缓存索引

                index = record.offset.toInt()
                val fmt = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                val len = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                val lang = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                if (fmt.equals(0)) {
                    val ft = Format0()
                    ft.format = fmt
                    ft.length = len
                    ft.language = lang
                    ft.glyphIdArray = data.copyOfIndex(index, len.toInt() - 6)
                    cmap.tables[fmt.toInt()] = ft
                } else if (fmt.equals(4)) {
                    val ft = Format4()
                    ft.format = fmt
                    ft.length = len
                    ft.language = lang
                    ft.segCountX2 = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                    val segCount = ft.segCountX2.toInt() / 2
                    ft.searchRange = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                    ft.entrySelector = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                    ft.rangeShift = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                    ft.endCode = IntArray(segCount)
                    for (n in 0 until segCount) {
                        ft.endCode[n] = byteArrToUintx(data.copyOfIndex(2)).toInt()
                    }
                    ft.reservedPad = byteArrToUintx(data.copyOfIndex(2)).toUShort()
                    ft.startCode = IntArray(segCount)
                    for (n in 0 until segCount) {
                        ft.startCode[n] = byteArrToUintx(data.copyOfIndex(2)).toInt()
                    }
                    ft.idDelta = ShortArray(segCount)
                    for (n in 0 until segCount) {
                        ft.idDelta[n] = byteArrToUintx(data.copyOfIndex(2)).toShort()
                    }
                    ft.idRangeOffset = IntArray(segCount)
                    for (n in 0 until segCount) {
                        ft.idRangeOffset[n] = byteArrToUintx(data.copyOfIndex(2)).toInt()
                    }
                    while (index < len.toInt()) {
                        ft.glyphIdArray.add(byteArrToUintx(data.copyOfIndex(2)).toUShort())
                    }
                    cmap.tables[fmt.toInt()] = ft
                }
                index = tmpIndex   // 读取缓存的索引
            }
        }

        // 解析表 glyf (字体轮廓数据表)
        run {
            val data = tables["glyf"]!!.data
            for (i in 0 until maxp.numGlyphs.toInt()) {
                index = loca[i].toInt()
                val numberOfContours = byteArrToUintx(data.copyOfIndex(2)).toShort()
                val xMin = byteArrToUintx(data.copyOfIndex(2)).toShort()
                val yMin = byteArrToUintx(data.copyOfIndex(2)).toShort()
                val xMax = byteArrToUintx(data.copyOfIndex(2)).toShort()
                val yMax = byteArrToUintx(data.copyOfIndex(2)).toShort()
                if (numberOfContours > 0) {
                    val g = GlyfTable()
                    g.numberOfContours = numberOfContours
                    g.xMin = xMin
                    g.yMin = yMin
                    g.xMax = xMax
                    g.yMax = yMax
                    g.endPtsOfContours = IntArray(numberOfContours.toInt())
                    for (n in 0 until numberOfContours) {
                        g.endPtsOfContours[n] =
                            byteArrToUintx(data.copyOfIndex(2)).toInt() // 这里数据源为UShort
                    }
                    g.instructionLength =
                        byteArrToUintx(data.copyOfIndex(2)).toInt() // 这里数据源为UShort
                    g.instructions = data.copyOfIndex(g.instructionLength)
                    val flagLength = g.endPtsOfContours.last() + 1
                    g.flags = ByteArray(flagLength)
                    var n = 0
                    while (n < flagLength) {
                        val flag = data.copyOfIndex(1).first()
                        g.flags[n] = flag
                        if (!(flag and 0x08).equals(0)) {
                            var j = data.copyOfIndex(1).first()
                            while (j > 0) {
                                --j
                                g.flags[++n] = flag
                            }
                        }
                        ++n
                    }
                    // 获取x轴相对坐标
                    g.xCoordinates = Array(flagLength) { 0 }
                    for (n in 0 until flagLength) {
                        val xByte = !(g.flags[n] and 0x02).equals(0)
                        val xSame = !(g.flags[n] and 0x10).equals(0)
                        if (xByte) {
                            g.xCoordinates[n] =
                                ((if (xSame) 1 else -1) * data.copyOfIndex(1).first()).toShort()
                        } else {
                            if (xSame) g.xCoordinates[n] = 0
                            else {
                                g.xCoordinates[n] = byteArrToUintx(data.copyOfIndex(2)).toShort()
                            }
                        }
                    }
                    // 获取y轴相对坐标
                    g.yCoordinates = Array(flagLength) { 0 }
                    for (n in 0 until flagLength) {
                        val yByte = !(g.flags[n] and 0x04).equals(0)
                        val ySame = !(g.flags[n] and 0x20).equals(0)
                        if (yByte) {
                            g.yCoordinates[n] =
                                ((if (ySame) 1 else -1) * data.copyOfIndex(1).first()).toShort()
                        } else {
                            if (ySame) g.yCoordinates[n] = 0
                            else {
                                g.yCoordinates[n] = byteArrToUintx(data.copyOfIndex(2)).toShort()
                            }
                        }
                    }
                    /*
                    PS:因为不需要绘制字体，转换就省了
                    // 相对坐标转绝对坐标
                    for (n in 0 until flagLength) {
                        g.xCoordinates[n] = (g.xCoordinates[n] + g.xCoordinates[n - 1]).toShort()
                        g.yCoordinates[n] = (g.yCoordinates[n] + g.yCoordinates[n - 1]).toShort()
                    }
                    */

                    glyf.add(g)
                } else {
                    // 复合字体暂不处理,以后用到再加
                }
            }
        }
    }

    /**
     * 获取字体信息（默认获取字体名称）
     * @see <a href="https://docs.microsoft.com/en-us/typography/opentype/spec/name#name-ids">获取详情</a>
     */
    fun getName(nameID: Int = 1): String {
        for (record in name.records) {
            if (!record.nameID.equals(nameID)) continue
            if (record.platformID.equals(1) && record.encodingID.equals(0)) {
                return record.nameBuffer.toString(Charsets.UTF_8)
            } else if (record.platformID.equals(3) && record.encodingID.equals(1)) {
                return record.nameBuffer.toString(Charsets.UTF_16BE)
            }
        }
        return "error"
    }

    /**
     * 获取字体轮廓 (fontCode:十进制Unicode字符编码)
     */
    fun getGlyf(fontCode: Int): ArrayList<Short> {
        var fontIndex = -1    // 轮廓索引
        val fmt4 = cmap.tables.getOrDefault(4, null)
        if (fmt4 != null) {
            val fmt = (fmt4 as Format4)
            val endCode = fmt.endCode
            for (i in endCode.indices) {
                if (endCode[i] == fontCode) {
                    fontIndex = i
                    break
                }
            }
            when {
                fontIndex == -1 -> fontIndex = 0
                fontCode < fmt.startCode[fontIndex] -> fontIndex = 0
                else -> {
                    fontIndex = if (fmt.idRangeOffset[fontIndex] != 0) {
                        val i = (fontCode - fmt.startCode[fontIndex]
                                + fmt.idRangeOffset[fontIndex].ushr(1)
                                - (fmt.idRangeOffset.size - fontIndex))
                        fmt.glyphIdArray[i].toInt()
                    } else fontCode + fmt.idDelta[fontIndex]
                    fontIndex = fontIndex and 0xFFFF
                }
            }
        } else {
            val fmt0 = cmap.tables.getOrDefault(0, null)
            if (fmt0 != null) {
                val fmt = (fmt0 as Format0)
                val glyphs = fmt.glyphIdArray
                fontIndex = if (fontCode > glyphs.size) 0 else glyphs[fontCode].toInt()
            }
        }

        val glyph = ArrayList<Short>()
        if (fontIndex > -1) {
            // 将字体的轮廓坐标合并到一起返回，方便使用
            glyph.addAll(glyf[fontIndex].xCoordinates)
            glyph.addAll(glyf[fontIndex].yCoordinates)
        }
        return glyph
    }

    /***
     * 获取字体轮廓 (fontCode:单个String字符)
     */
    fun GetGlyf(fontCode: String): ArrayList<Short> {
        return GetGlyf(fontCode[0].toInt())
    }
}