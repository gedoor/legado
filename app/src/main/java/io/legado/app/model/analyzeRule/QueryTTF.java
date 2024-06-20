package io.legado.app.model.analyzeRule;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class QueryTTF {
    /**
     * 文件头
     *
     * @url <a href="https://learn.microsoft.com/zh-cn/typography/opentype/spec/otff">Microsoft opentype 字体文档</a>
     */
    private static class Header {
        /**
         * uint32   字体版本 0x00010000 (ttf)
         */
        public long sfntVersion;
        /**
         * uint16   Number of tables.
         */
        public int numTables;
        /**
         * uint16
         */
        public int searchRange;
        /**
         * uint16
         */
        public int entrySelector;
        /**
         * uint16
         */
        public int rangeShift;
    }

    /**
     * 数据表目录
     */
    private static class Directory {
        /**
         * uint32 (表标识符)
         */
        public String tableTag;
        /**
         * uint32 (该表的校验和)
         */
        public int checkSum;
        /**
         * uint32 (TTF文件 Bytes 数据索引 0 开始的偏移地址)
         */
        public int offset;
        /**
         * uint32 (该表的长度)
         */
        public int length;
    }

    private static class NameLayout {
        public int format;
        public int count;
        public int stringOffset;
        public LinkedList<NameRecord> records = new LinkedList<>();
    }

    private static class NameRecord {
        public int platformID;           // 平台标识符<0:Unicode, 1:Mac, 2:ISO, 3:Windows, 4:Custom>
        public int encodingID;           // 编码标识符
        public int languageID;           // 语言标识符
        public int nameID;               // 名称标识符
        public int length;               // 名称字符串的长度
        public int offset;               // 名称字符串相对于stringOffset的字节偏移量
    }

    /**
     * Font Header Table
     */
    private static class HeadLayout {
        /**
         * uint16
         */
        public int majorVersion;
        /**
         * uint16
         */
        public int minorVersion;
        /**
         * uint16
         */
        public int fontRevision;
        /**
         * uint32
         */
        public int checkSumAdjustment;
        /**
         * uint32
         */
        public int magicNumber;
        /**
         * uint16
         */
        public int flags;
        /**
         * uint16
         */
        public int unitsPerEm;
        /**
         * long
         */
        public long created;
        /**
         * long
         */
        public long modified;
        /**
         * int16
         */
        public short xMin;
        /**
         * int16
         */
        public short yMin;
        /**
         * int16
         */
        public short xMax;
        /**
         * int16
         */
        public short yMax;
        /**
         * uint16
         */
        public int macStyle;
        /**
         * uint16
         */
        public int lowestRecPPEM;
        /**
         * int16
         */
        public short fontDirectionHint;
        /**
         * int16
         * <p> 0 表示短偏移 (Offset16)，1 表示长偏移 (Offset32)。
         */
        public short indexToLocFormat;
        /**
         * int16
         */
        public short glyphDataFormat;
    }

    /**
     * Maximum Profile
     */
    private static class MaxpLayout {
        /**
         * uint32   高16位表示整数，低16位表示小数
         */
        public int version;
        /**
         * uint16   字体中的字形数量
         */
        public int numGlyphs;
        /**
         * uint16   非复合字形中包含的最大点数。点是构成字形轮廓的基本单位。
         */
        public int maxPoints;
        /**
         * uint16   非复合字形中包含的最大轮廓数。轮廓是由一系列点连接形成的封闭曲线。
         */
        public int maxContours;
        /**
         * uint16   复合字形中包含的最大点数。复合字形是由多个简单字形组合而成的。
         */
        public int maxCompositePoints;
        /**
         * uint16   复合字形中包含的最大轮廓数。
         */
        public int maxCompositeContours;
        /**
         * uint16
         */
        public int maxZones;
        /**
         * uint16
         */
        public int maxTwilightPoints;
        /**
         * uint16
         */
        public int maxStorage;
        /**
         * uint16
         */
        public int maxFunctionDefs;
        /**
         * uint16
         */
        public int maxInstructionDefs;
        /**
         * uint16
         */
        public int maxStackElements;
        /**
         * uint16
         */
        public int maxSizeOfInstructions;
        /**
         * uint16   任何复合字形在“顶层”引用的最大组件数。
         */
        public int maxComponentElements;
        /**
         * uint16   递归的最大层数；简单组件为1。
         */
        public int maxComponentDepth;
    }

    /**
     * 字符到字形索引映射表
     */
    private static class CmapLayout {
        /**
         * uint16
         */
        public int version;
        /**
         * uint16   后面的编码表的数量
         */
        public int numTables;
        public LinkedList<CmapRecord> records = new LinkedList<>();
        public HashMap<Integer, CmapFormat> tables = new HashMap<>();
    }

    /**
     * Encoding records and encodings
     */
    private static class CmapRecord {
        /**
         * uint16   Platform ID.
         * <p> 0、Unicode
         * <p> 1、Macintosh
         * <p> 2、ISO
         * <p> 3、Windows
         * <p> 4、Custom
         */
        public int platformID;
        /**
         * uint16   Platform-specific encoding ID.
         * <p> platform ID = 3
         * <p>  0、Symbol
         * <p>  1、Unicode BMP
         * <p>  2、ShiftJIS
         * <p>  3、PRC
         * <p>  4、Big5
         * <p>  5、Wansung
         * <p>  6、Johab
         * <p>  7、Reserved
         * <p>  8、Reserved
         * <p>  9、Reserved
         * <p> 10、Unicode full repertoire
         */
        public int encodingID;
        /**
         * uint32   从 cmap 表开头到子表的字节偏移量
         */
        public int offset;
    }

    private static class CmapFormat {
        /**
         * uint16
         * <p> cmapFormat 子表的格式类型
         */
        public int format;
        /**
         * uint16
         * <p> 这个 Format 表的长度（以字节为单位）
         */
        public int length;
        /**
         * uint16
         * <p> 仅 platformID=1 时有效
         */
        public int language;
        /**
         * uint16[256]
         * <p> 仅 Format=2
         * <p> 将高字节映射到 subHeaders 的数组：值为 subHeader 索引x8
         */
        public int[] subHeaderKeys;
        /**
         * uint16[]
         * <p> 仅 Format=2
         * <p> subHeader 子标头的可变长度数组
         * <p> 其结构为 uint16[][4]{ {uint16,uint16,int16,uint16}, ... }
         */
        public int[] subHeaders;
        /**
         * uint16   segCount x2
         * <p> 仅 Format=4
         * <p> seg段计数乘以 2。这是因为每个段用两个字节表示，所以这个值是实际段数的两倍。
         */
        public int segCountX2;
        /**
         * uint16
         * <p> 仅 Format=4
         * <p> 小于或等于段数的最大二次幂，再乘以 2。这是为二分查找优化搜索过程。
         */
        public int searchRange;
        /**
         * uint16
         * <p> 仅 Format=4
         * <p> 等于 log2(searchRange/2)，这是最大二次幂的对数。
         */
        public int entrySelector;
        /**
         * uint16
         * <p> 仅 Format=4
         * <p> segCount * 2 - searchRange 用于调整搜索范围的偏移。
         */
        public int rangeShift;
        /**
         * uint16[segCount]
         * <p> 仅 Format=4
         * <p> 每个段的结束字符码，最后一个是 0xFFFF，表示 Unicode 范围的结束。
         */
        public int[] endCode;
        /**
         * uint16
         * <p> 仅 Format=4
         * <p> 固定设置为 0，用于填充保留位以保持数据对齐。
         */
        public int reservedPad;
        /**
         * uint16[segCount]
         * <p> 仅 Format=4
         * <p> 每个段的起始字符码。
         */
        public int[] startCode;
        /**
         * int16[segCount]
         * <p> 仅 Format=4
         * <p> 用于计算字形索引的偏移值。该值被加到从 startCode 到 endCode 的所有字符码上，得到相应的字形索引。
         */
        public int[] idDelta;
        /**
         * uint16[segCount]
         * <p> 仅 Format=4
         * <p> 偏移到 glyphIdArray 中的起始位置，如果没有额外的字形索引映射，则为 0。
         */
        public int[] idRangeOffsets;
        /**
         * uint16
         * <p> 仅 Format=6
         * <p> 子范围的第一个字符代码。这是连续字符代码范围的起始点。
         */
        public int firstCode;
        /**
         * uint16
         * <p> 仅 Format=6
         * <p> 子范围中字符代码的数量。这表示从 firstCode 开始，连续多少个字符代码被包含
         */
        public int entryCount;
        /**
         * 字形索引数组
         * <p> Format=0 为 bye[256]数组
         * <p> Format>0 为 uint16[] 数组
         * <p> Format>12 为 uint32[] 数组
         * <p> @url <a href="https://learn.microsoft.com/zh-cn/typography/opentype/spec/cmap#language">Microsoft cmap文档</a>
         */
        public int[] glyphIdArray;
    }

    /**
     * 字形轮廓数据表
     */
    private static class GlyfLayout {
        /**
         * int16    非负值为简单字形的轮廓数,负值表示为复合字形
         */
        public short numberOfContours;
        /**
         * int16    Minimum x for coordinate data.
         */
        public short xMin;
        /**
         * int16    Minimum y for coordinate data.
         */
        public short yMin;
        /**
         * int16    Maximum x for coordinate data.
         */
        public short xMax;
        /**
         * int16    Maximum y for coordinate data.
         */
        public short yMax;
        /**
         * 简单字形数据
         */
        public GlyphTableBySimple glyphSimple;
        /**
         * 复合字形数据
         */
        public LinkedList<GlyphTableComponent> glyphComponent;
    }

    /**
     * 简单字形数据表
     */
    private static class GlyphTableBySimple {
        /**
         * uint16[numberOfContours]
         */
        int[] endPtsOfContours;
        /**
         * uint16
         */
        int instructionLength;
        /**
         * uint8[instructionLength]
         */
        int[] instructions;
        /**
         * uint8[variable]
         * <p> bit0: 该点位于曲线上
         * <p> bit1: < 1:xCoordinate为uint8 >
         * <p> bit2: < 1:yCoordinate为uint8 >
         * <p> bit3: < 1:下一个uint8为此条目之后插入的附加逻辑标志条目的数量 >
         * <p> bit4: < bit1=1时表示符号[1.正,0.负]; bit1=0时[1.x坐标重复一次,0.x坐标读为int16] >
         * <p> bit5: < bit2=1时表示符号[1.正,0.负]; bit2=0时[1.y坐标重复一次,0.y坐标读为int16] >
         * <p> bit6: 字形描述中的轮廓可能会重叠
         * <p> bit7: 保留位,无意义
         */
        int[] flags;
        /**
         * uint8[]  when(flags&0x02==0x02)
         * int16[]  when(flags&0x12==0x00)
         */
        int[] xCoordinates;
        /**
         * uint8[]  when(flags&0x04==0x02)
         * int16[]  when(flags&0x24==0x00)
         */
        int[] yCoordinates;
    }

    /**
     * 复合字形数据表
     */
    private static class GlyphTableComponent {
        /**
         * uint16
         * <p> bit0: < 1:argument是16bit，0:argument是8bit >
         * <p> bit1: < 1:argument是有符号值，0:argument是无符号值 >
         * <p> bit3: 该组件有一个缩放比例，否则比例为1.0
         * <p> bit5: 表示在此字形之后还有字形
         */
        int flags;
        /**
         * uint16
         */
        int glyphIndex;
        /**
         * x-offset
         * <p>  uint8 when flags&0x03==0
         * <p>   int8 when flags&0x03==1
         * <p> uint16 when flags&0x03==2
         * <p>  int16 when flags&0x03==3
         */
        int argument1;
        /**
         * y-offset
         * <p>  uint8 when flags&0x03==0
         * <p>   int8 when flags&0x03==1
         * <p> uint16 when flags&0x03==2
         * <p>  int16 when flags&0x03==3
         */
        int argument2;
        /**
         * uint16
         * <p> 值类型为 F2DOT14 的组件缩放X比例值
         */
        float xScale;
        /**
         * uint16
         * <p> 值类型为 F2DOT14 的2x2变换矩阵01值
         */
        float scale01;
        /**
         * uint16
         * <p> 值类型为 F2DOT14 的2x2变换矩阵10值
         */
        float scale10;
        /**
         * uint16
         * <p> 值类型为 F2DOT14 的组件缩放Y比例值
         */
        float yScale;
    }

    private static class BufferReader {
        private final ByteBuffer byteBuffer;

        public BufferReader(byte[] buffer, int index) {
            this.byteBuffer = ByteBuffer.wrap(buffer);
            this.byteBuffer.order(ByteOrder.BIG_ENDIAN); // 设置为大端模式
            this.byteBuffer.position(index); // 设置起始索引
        }

        public void position(int index) {
            byteBuffer.position(index); // 设置起始索引
        }

        public int position() {
            return byteBuffer.position();
        }

        public long ReadUInt64() {
            return byteBuffer.getLong();
        }

        public int ReadUInt32() {
            return byteBuffer.getInt();
        }

        public int ReadInt32() {
            return byteBuffer.getInt();
        }

        public int ReadUInt16() {
            return byteBuffer.getShort() & 0xFFFF;
        }

        public short ReadInt16() {
            return byteBuffer.getShort();
        }

        public short ReadUInt8() {
            return (short) (byteBuffer.get() & 0xFF);
        }

        public byte ReadInt8() {
            return byteBuffer.get();
        }

        public byte[] ReadByteArray(int len) {
            assert len >= 0;
            byte[] result = new byte[len];
            byteBuffer.get(result);
            return result;
        }

        public int[] ReadUInt8Array(int len) {
            assert len >= 0;
            var result = new int[len];
            for (int i = 0; i < len; ++i) result[i] = byteBuffer.get() & 0xFF;
            return result;
        }

        public int[] ReadInt16Array(int len) {
            assert len >= 0;
            var result = new int[len];
            for (int i = 0; i < len; ++i) result[i] = byteBuffer.getShort();
            return result;
        }

        public int[] ReadUInt16Array(int len) {
            assert len >= 0;
            var result = new int[len];
            for (int i = 0; i < len; ++i) result[i] = byteBuffer.getShort() & 0xFFFF;
            return result;
        }

        public int[] ReadInt32Array(int len) {
            assert len >= 0;
            var result = new int[len];
            for (int i = 0; i < len; ++i) result[i] = byteBuffer.getInt();
            return result;
        }
    }

    private final Header fileHeader = new Header();
    private final HashMap<String, Directory> directorys = new HashMap<>();
    private final NameLayout name = new NameLayout();
    private final HeadLayout head = new HeadLayout();
    private final MaxpLayout maxp = new MaxpLayout();
    private final CmapLayout Cmap = new CmapLayout();
    private final int[][] pps = new int[][]{{3, 10}, {0, 4}, {3, 1}, {1, 0}, {0, 3}, {0, 1}};

    private void readNameTable(byte[] buffer) {
        var dataTable = directorys.get("name");
        assert dataTable != null;
        var reader = new BufferReader(buffer, dataTable.offset);
        name.format = reader.ReadUInt16();
        name.count = reader.ReadUInt16();
        name.stringOffset = reader.ReadUInt16();
        for (int i = 0; i < name.count; ++i) {
            NameRecord record = new NameRecord();
            record.platformID = reader.ReadUInt16();
            record.encodingID = reader.ReadUInt16();
            record.languageID = reader.ReadUInt16();
            record.nameID = reader.ReadUInt16();
            record.length = reader.ReadUInt16();
            record.offset = reader.ReadUInt16();
            name.records.add(record);
        }
    }

    private void readHeadTable(byte[] buffer) {
        var dataTable = directorys.get("head");
        assert dataTable != null;
        var reader = new BufferReader(buffer, dataTable.offset);
        head.majorVersion = reader.ReadUInt16();
        head.minorVersion = reader.ReadUInt16();
        head.fontRevision = reader.ReadUInt32();
        head.checkSumAdjustment = reader.ReadUInt32();
        head.magicNumber = reader.ReadUInt32();
        head.flags = reader.ReadUInt16();
        head.unitsPerEm = reader.ReadUInt16();
        head.created = reader.ReadUInt64();
        head.modified = reader.ReadUInt64();
        head.xMin = reader.ReadInt16();
        head.yMin = reader.ReadInt16();
        head.xMax = reader.ReadInt16();
        head.yMax = reader.ReadInt16();
        head.macStyle = reader.ReadUInt16();
        head.lowestRecPPEM = reader.ReadUInt16();
        head.fontDirectionHint = reader.ReadInt16();
        head.indexToLocFormat = reader.ReadInt16();
        head.glyphDataFormat = reader.ReadInt16();
    }

    /**
     * glyfId到glyphData的索引
     * <p> 根据定义，索引零指向“丢失的字符”。
     * <p> loca.length = maxp.numGlyphs + 1;
     */
    private int[] loca;

    private void readLocaTable(byte[] buffer) {
        var dataTable = directorys.get("loca");
        assert dataTable != null;
        var reader = new BufferReader(buffer, dataTable.offset);
        if (head.indexToLocFormat == 0) {
            loca = reader.ReadUInt16Array(dataTable.length / 2);
            // 当loca表数据长度为Uint16时,需要翻倍
            for (var i = 0; i < loca.length; i++) loca[i] *= 2;
        } else {
            loca = reader.ReadInt32Array(dataTable.length / 4);
        }
    }

    private void readCmapTable(byte[] buffer) {
        var dataTable = directorys.get("cmap");
        assert dataTable != null;
        var reader = new BufferReader(buffer, dataTable.offset);
        Cmap.version = reader.ReadUInt16();
        Cmap.numTables = reader.ReadUInt16();
        for (int i = 0; i < Cmap.numTables; ++i) {
            CmapRecord record = new CmapRecord();
            record.platformID = reader.ReadUInt16();
            record.encodingID = reader.ReadUInt16();
            record.offset = reader.ReadUInt32();
            Cmap.records.add(record);
        }

        for (var formatTable : Cmap.records) {
            int fmtOffset = formatTable.offset;
            if (Cmap.tables.containsKey(fmtOffset)) continue;
            reader.position(dataTable.offset + fmtOffset);

            CmapFormat f = new CmapFormat();
            f.format = reader.ReadUInt16();
            f.length = reader.ReadUInt16();
            f.language = reader.ReadUInt16();
            switch (f.format) {
                case 0: {
                    f.glyphIdArray = reader.ReadUInt8Array(f.length - 6);
                    // 记录 unicode->glyphId 映射表
                    int unicodeInclusive = 0;
                    int unicodeExclusive = f.glyphIdArray.length;
                    for (; unicodeInclusive < unicodeExclusive; unicodeInclusive++) {
                        if (f.glyphIdArray[unicodeInclusive] == 0) continue; // 排除轮廓索引为0的Unicode
                        unicodeToGlyphId.put(unicodeInclusive, f.glyphIdArray[unicodeInclusive]);
                    }
                    break;
                }
                case 4: {
                    f.segCountX2 = reader.ReadUInt16();
                    int segCount = f.segCountX2 / 2;
                    f.searchRange = reader.ReadUInt16();
                    f.entrySelector = reader.ReadUInt16();
                    f.rangeShift = reader.ReadUInt16();
                    f.endCode = reader.ReadUInt16Array(segCount);
                    f.reservedPad = reader.ReadUInt16();
                    f.startCode = reader.ReadUInt16Array(segCount);
                    f.idDelta = reader.ReadInt16Array(segCount);
                    f.idRangeOffsets = reader.ReadUInt16Array(segCount);
                    // 一个包含字形索引的数组，其长度是任意的，取决于映射的复杂性和字体中的字符数量。
                    int glyphIdArrayLength = (f.length - 16 - (segCount * 8)) / 2;
                    f.glyphIdArray = reader.ReadUInt16Array(glyphIdArrayLength);

                    // 记录 unicode->glyphId 映射表
                    for (int segmentIndex = 0; segmentIndex < segCount; segmentIndex++) {
                        int unicodeInclusive = f.startCode[segmentIndex];
                        int unicodeExclusive = f.endCode[segmentIndex];
                        int idDelta = f.idDelta[segmentIndex];
                        int idRangeOffset = f.idRangeOffsets[segmentIndex];
                        for (int unicode = unicodeInclusive; unicode <= unicodeExclusive; unicode++) {
                            int glyphId = 0;
                            if (idRangeOffset == 0) {
                                glyphId = (unicode + idDelta) & 0xFFFF;
                            } else {
                                int gIndex = (idRangeOffset / 2) + unicode - unicodeInclusive + segmentIndex - segCount;
                                if (gIndex < glyphIdArrayLength) glyphId = f.glyphIdArray[gIndex] + idDelta;
                            }
                            if (glyphId == 0) continue; // 排除轮廓索引为0的Unicode
                            unicodeToGlyphId.put(unicode, glyphId);
                        }
                    }
                    break;
                }
                case 6: {
                    f.firstCode = reader.ReadUInt16();
                    f.entryCount = reader.ReadUInt16();
                    // 范围内字符代码的字形索引值数组。
                    f.glyphIdArray = reader.ReadUInt16Array(f.entryCount);

                    // 记录 unicode->glyphId 映射表
                    int unicodeIndex = f.firstCode;
                    int unicodeCount = f.entryCount;
                    for (int gIndex = 0; gIndex < unicodeCount; gIndex++) {
                        unicodeToGlyphId.put(unicodeIndex, f.glyphIdArray[gIndex]);
                        unicodeIndex++;
                    }
                    break;
                }
                default:
                    break;
            }
            Cmap.tables.put(fmtOffset, f);
        }
    }

    private void readMaxpTable(byte[] buffer) {
        var dataTable = directorys.get("maxp");
        assert dataTable != null;
        var reader = new BufferReader(buffer, dataTable.offset);
        maxp.version = reader.ReadUInt32();
        maxp.numGlyphs = reader.ReadUInt16();
        maxp.maxPoints = reader.ReadUInt16();
        maxp.maxContours = reader.ReadUInt16();
        maxp.maxCompositePoints = reader.ReadUInt16();
        maxp.maxCompositeContours = reader.ReadUInt16();
        maxp.maxZones = reader.ReadUInt16();
        maxp.maxTwilightPoints = reader.ReadUInt16();
        maxp.maxStorage = reader.ReadUInt16();
        maxp.maxFunctionDefs = reader.ReadUInt16();
        maxp.maxInstructionDefs = reader.ReadUInt16();
        maxp.maxStackElements = reader.ReadUInt16();
        maxp.maxSizeOfInstructions = reader.ReadUInt16();
        maxp.maxComponentElements = reader.ReadUInt16();
        maxp.maxComponentDepth = reader.ReadUInt16();
    }

    /**
     * 字形轮廓表 数组
     */
    private GlyfLayout[] glyfArray;

    private void readGlyfTable(byte[] buffer) {
        var dataTable = directorys.get("glyf");
        assert dataTable != null;
        int glyfCount = maxp.numGlyphs;
        glyfArray = new GlyfLayout[glyfCount];  // 创建字形容器

        var reader = new BufferReader(buffer, 0);
        for (int index = 0; index < glyfCount; index++) {
            if (loca[index] == loca[index + 1]) continue;   // 当前loca与下一个loca相同，表示这个字形不存在
            int offset = dataTable.offset + loca[index];
            // 读GlyphHeaders
            var glyph = new GlyfLayout();
            reader.position(offset);
            glyph.numberOfContours = reader.ReadInt16();
            if (glyph.numberOfContours > maxp.maxContours) continue; // 如果字形轮廓数大于非复合字形中包含的最大轮廓数，则说明该字形无效。
            glyph.xMin = reader.ReadInt16();
            glyph.yMin = reader.ReadInt16();
            glyph.xMax = reader.ReadInt16();
            glyph.yMax = reader.ReadInt16();

            // 轮廓数为0时，不需要解析轮廓数据
            if (glyph.numberOfContours == 0) continue;
            // 读Glyph轮廓数据
            if (glyph.numberOfContours > 0) {
                // 简单轮廓
                glyph.glyphSimple = new GlyphTableBySimple();
                glyph.glyphSimple.endPtsOfContours = reader.ReadUInt16Array(glyph.numberOfContours);
                glyph.glyphSimple.instructionLength = reader.ReadUInt16();
                glyph.glyphSimple.instructions = reader.ReadUInt8Array(glyph.glyphSimple.instructionLength);
                int flagLength = glyph.glyphSimple.endPtsOfContours[glyph.glyphSimple.endPtsOfContours.length - 1] + 1;
                // 获取轮廓点描述标志
                glyph.glyphSimple.flags = new int[flagLength];
                for (int n = 0; n < flagLength; ++n) {
                    var glyphSimpleFlag = reader.ReadUInt8();
                    glyph.glyphSimple.flags[n] = glyphSimpleFlag;
                    if ((glyphSimpleFlag & 0x08) == 0x08) {
                        for (int m = reader.ReadUInt8(); m > 0; --m) {
                            glyph.glyphSimple.flags[++n] = glyphSimpleFlag;
                        }
                    }
                }
                // 获取轮廓点描述x轴相对值
                glyph.glyphSimple.xCoordinates = new int[flagLength];
                for (int n = 0; n < flagLength; ++n) {
                    switch (glyph.glyphSimple.flags[n] & 0x12) {
                        case 0x02:
                            glyph.glyphSimple.xCoordinates[n] = -1 * reader.ReadUInt8();
                            break;
                        case 0x12:
                            glyph.glyphSimple.xCoordinates[n] = reader.ReadUInt8();
                            break;
                        case 0x10:
                            glyph.glyphSimple.xCoordinates[n] = 0;  // 点位数据重复上一次数据，那么相对数据变化量就是0
                            break;
                        case 0x00:
                            glyph.glyphSimple.xCoordinates[n] = reader.ReadInt16();
                            break;
                    }
                }
                // 获取轮廓点描述y轴相对值
                glyph.glyphSimple.yCoordinates = new int[flagLength];
                for (int n = 0; n < flagLength; ++n) {
                    switch (glyph.glyphSimple.flags[n] & 0x24) {
                        case 0x04:
                            glyph.glyphSimple.yCoordinates[n] = -1 * reader.ReadUInt8();
                            break;
                        case 0x24:
                            glyph.glyphSimple.yCoordinates[n] = reader.ReadUInt8();
                            break;
                        case 0x20:
                            glyph.glyphSimple.yCoordinates[n] = 0;  // 点位数据重复上一次数据，那么相对数据变化量就是0
                            break;
                        case 0x00:
                            glyph.glyphSimple.yCoordinates[n] = reader.ReadInt16();
                            break;
                    }
                }
            } else {
                // 复合轮廓
                glyph.glyphComponent = new LinkedList<>();
                while (true) {
                    var glyphTableComponent = new GlyphTableComponent();
                    glyphTableComponent.flags = reader.ReadUInt16();
                    glyphTableComponent.glyphIndex = reader.ReadUInt16();
                    switch (glyphTableComponent.flags & 0b11) {
                        case 0b00:
                            glyphTableComponent.argument1 = reader.ReadUInt8();
                            glyphTableComponent.argument2 = reader.ReadUInt8();
                            break;
                        case 0b10:
                            glyphTableComponent.argument1 = reader.ReadInt8();
                            glyphTableComponent.argument2 = reader.ReadInt8();
                            break;
                        case 0b01:
                            glyphTableComponent.argument1 = reader.ReadUInt16();
                            glyphTableComponent.argument2 = reader.ReadUInt16();
                            break;
                        case 0b11:
                            glyphTableComponent.argument1 = reader.ReadInt16();
                            glyphTableComponent.argument2 = reader.ReadInt16();
                            break;
                    }
                    switch (glyphTableComponent.flags & 0b11001000) {
                        case 0b00001000:
                            // 有单一比例
                            glyphTableComponent.yScale = glyphTableComponent.xScale = ((float) reader.ReadUInt16()) / 16384.0f;
                            break;
                        case 0b01000000:
                            // 有X和Y的独立比例
                            glyphTableComponent.xScale = ((float) reader.ReadUInt16()) / 16384.0f;
                            glyphTableComponent.yScale = ((float) reader.ReadUInt16()) / 16384.0f;
                            break;
                        case 0b10000000:
                            // 有2x2变换矩阵
                            glyphTableComponent.xScale = ((float) reader.ReadUInt16()) / 16384.0f;
                            glyphTableComponent.scale01 = ((float) reader.ReadUInt16()) / 16384.0f;
                            glyphTableComponent.scale10 = ((float) reader.ReadUInt16()) / 16384.0f;
                            glyphTableComponent.yScale = ((float) reader.ReadUInt16()) / 16384.0f;
                            break;
                    }
                    glyph.glyphComponent.add(glyphTableComponent);
                    if ((glyphTableComponent.flags & 0x20) == 0) break;
                }
            }
            glyfArray[index] = glyph;
        }
    }

    /**
     * 使用轮廓索引值获取轮廓数据
     *
     * @param glyfId 轮廓索引
     * @return 轮廓数据
     */
    public String getGlyfById(int glyfId) {
        var glyph = glyfArray[glyfId];
        if (glyph == null) return null;    // 过滤不存在的字体轮廓
        String glyphString;
        if (glyph.numberOfContours >= 0) {
            // 简单字形
            int dataCount = glyph.glyphSimple.flags.length;
            String[] coordinateArray = new String[dataCount];
            for (int i = 0; i < dataCount; i++) {
                coordinateArray[i] = glyph.glyphSimple.xCoordinates[i] + "," + glyph.glyphSimple.yCoordinates[i];
            }
            glyphString = String.join("|", coordinateArray);
        } else {
            // 复合字形
            LinkedList<String> glyphIdList = new LinkedList<>();
            for (var g : glyph.glyphComponent) {
                glyphIdList.add("{" +
                        "flags:" + g.flags + "," +
                        "glyphIndex:" + g.glyphIndex + "," +
                        "arg1:" + g.argument1 + "," +
                        "arg2:" + g.argument2 + "," +
                        "xScale:" + g.xScale + "," +
                        "scale01:" + g.scale01 + "," +
                        "scale10:" + g.scale10 + "," +
                        "yScale:" + g.yScale + "}");
            }
            glyphString = "[" + String.join(",", glyphIdList) + "]";
        }
        return glyphString;
    }

    /**
     * 构造函数
     *
     * @param buffer 传入TTF字体二进制数组
     */
    public QueryTTF(final byte[] buffer) {
        var fontReader = new BufferReader(buffer, 0);
//        Log.i("QueryTTF", "读文件头"); // 获取文件头
        fileHeader.sfntVersion = fontReader.ReadUInt32();
        fileHeader.numTables = fontReader.ReadUInt16();
        fileHeader.searchRange = fontReader.ReadUInt16();
        fileHeader.entrySelector = fontReader.ReadUInt16();
        fileHeader.rangeShift = fontReader.ReadUInt16();
        // 获取目录
        for (int i = 0; i < fileHeader.numTables; ++i) {
            Directory d = new Directory();
            d.tableTag = new String(fontReader.ReadByteArray(4), StandardCharsets.US_ASCII);
            d.checkSum = fontReader.ReadUInt32();
            d.offset = fontReader.ReadUInt32();
            d.length = fontReader.ReadUInt32();
            directorys.put(d.tableTag, d);
        }

//        Log.i("QueryTTF", "解析表 name"); // 字体信息,包含版权、名称、作者等...
        readNameTable(buffer);
//        Log.i("QueryTTF", "解析表 head"); // 获取 head.indexToLocFormat
        readHeadTable(buffer);
//        Log.i("QueryTTF", "解析表 cmap"); // Unicode编码->轮廓索引 对照表
        readCmapTable(buffer);
//        Log.i("QueryTTF", "解析表 loca"); // 轮廓数据偏移地址表
        readLocaTable(buffer);
//        Log.i("QueryTTF", "解析表 maxp"); // 获取 maxp.numGlyphs 字体轮廓数量
        readMaxpTable(buffer);
//        Log.i("QueryTTF", "解析表 glyf"); // 字体轮廓数据表,需要解析loca,maxp表后计算
        readGlyfTable(buffer);
//        Log.i("QueryTTF", "建立Unicode&Glyph映射表");
        int glyfArrayLength = glyfArray.length;
        for (var item : unicodeToGlyphId.entrySet()) {
            int key = item.getKey();
            int val = item.getValue();
            if (val >= glyfArrayLength) continue;
            String glyfString = getGlyfById(val);
            unicodeToGlyph.put(key, glyfString);
            if (glyfString == null) continue;   // null 不能用作hashmap的key
            glyphToUnicode.put(glyfString, key);
        }
//        Log.i("QueryTTF", "字体处理完成");
    }

    public final HashMap<Integer, String> unicodeToGlyph = new HashMap<>();
    public final HashMap<String, Integer> glyphToUnicode = new HashMap<>();
    public final HashMap<Integer, Integer> unicodeToGlyphId = new HashMap<>();

    /**
     * 使用 Unicode 值获查询廓索引
     *
     * @param unicode 传入 Unicode 值
     * @return 轮廓索引
     */
    public int getGlyfIdByUnicode(int unicode) {
        var result = unicodeToGlyphId.get(unicode);
        if (result == null) return 0; // 如果找不到Unicode对应的轮廓索引，就返回默认值0
        return result;
    }

    /**
     * 使用 Unicode 值查询轮廓数据
     *
     * @param unicode 传入 Unicode 值
     * @return 轮廓数据
     */
    public String getGlyfByUnicode(int unicode) {
        return unicodeToGlyph.get(unicode);
    }

    /**
     * 使用轮廓数据反查 Unicode 值
     *
     * @param glyph 传入轮廓数据
     * @return Unicode
     */
    public int getUnicodeByGlyf(String glyph) {
        var result = glyphToUnicode.get(glyph);
        if (result == null) return 0; // 如果轮廓数据找不到对应的Unicode，就返回默认值0
        return result;
    }

    /**
     * Unicode 空白字符判断
     *
     * @param unicode 字符的 Unicode 值
     * @return true:是空白字符; false:非空白字符
     */
    public boolean isBlankUnicode(int unicode) {
        return switch (unicode) {
            case 0x0009,    // 水平制表符 (Horizontal Tab)
                    0x0020,    // 空格 (Space)
                    0x00A0,    // 不中断空格 (No-Break Space)
                    0x2002,    // En空格 (En Space)
                    0x2003,    // Em空格 (Em Space)
                    0x2007,    // 刚性空格 (Figure Space)
                    0x200A,    // 发音修饰字母的连字符 (Hair Space)
                    0x200B,    // 零宽空格 (Zero Width Space)
                    0x200C,    // 零宽不连字 (Zero Width Non-Joiner)
                    0x200D,    // 零宽连字 (Zero Width Joiner)
                    0x202F,    // 狭窄不中断空格 (Narrow No-Break Space)
                    0x205F     // 中等数学空格 (Medium Mathematical Space)
                    -> true;
            default -> false;
        };
    }
}
