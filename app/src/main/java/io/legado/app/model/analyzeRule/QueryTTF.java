package io.legado.app.model.analyzeRule;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"FieldCanBeLocal", "StatementWithEmptyBody", "unused"})
public class QueryTTF {
    private static class Header {
        public int majorVersion;
        public int minorVersion;
        public int numOfTables;
        public int searchRange;
        public int entrySelector;
        public int rangeShift;
    }

    private static class Directory {
        public String tag;          // table name
        public int checkSum;       // Check sum
        public int offset;         // Offset from beginning of file
        public int length;         // length of the table in bytes
    }

    private static class NameLayout {
        public int format;
        public int count;
        public int stringOffset;
        public List<NameRecord> records = new LinkedList<>();
    }

    private static class NameRecord {
        public int platformID;           // 平台标识符<0:Unicode, 1:Mac, 2:ISO, 3:Windows, 4:Custom>
        public int encodingID;           // 编码标识符
        public int languageID;           // 语言标识符
        public int nameID;               // 名称标识符
        public int length;               // 名称字符串的长度
        public int offset;               // 名称字符串相对于stringOffset的字节偏移量
    }

    private static class HeadLayout {
        public int majorVersion;
        public int minorVersion;
        public int fontRevision;
        public int checkSumAdjustment;
        public int magicNumber;
        public int flags;
        public int unitsPerEm;
        public long created;
        public long modified;
        public short xMin;
        public short yMin;
        public short xMax;
        public short yMax;
        public int macStyle;
        public int lowestRecPPEM;
        public short fontDirectionHint;
        public short indexToLocFormat;      // <0:loca是2字节数组, 1:loca是4字节数组>
        public short glyphDataFormat;
    }

    private static class MaxpLayout {
        public int majorVersion;
        public int minorVersion;
        public int numGlyphs;                // 字体中的字形数量
        public int maxPoints;
        public int maxContours;
        public int maxCompositePoints;
        public int maxCompositeContours;
        public int maxZones;
        public int maxTwilightPoints;
        public int maxStorage;
        public int maxFunctionDefs;
        public int maxInstructionDefs;
        public int maxStackElements;
        public int maxSizeOfInstructions;
        public int maxComponentElements;
        public int maxComponentDepth;
    }

    private static class CmapLayout {
        public int version;
        public int numTables;
        public List<CmapRecord> records = new LinkedList<>();
        public Map<Integer, CmapFormat> tables = new HashMap<>();
    }

    private static class CmapRecord {
        public int platformID;
        public int encodingID;
        public int offset;
    }

    private static class CmapFormat {
        public int format;
        public int length;
        public int language;
        public byte[] glyphIdArray;
    }

    private static class CmapFormat4 extends CmapFormat {
        public int segCountX2;
        public int searchRange;
        public int entrySelector;
        public int rangeShift;
        public int[] endCode;
        public int reservedPad;
        public int[] startCode;
        public short[] idDelta;
        public int[] idRangeOffset;
        public int[] glyphIdArray;
    }

    private static class CmapFormat6 extends CmapFormat {
        public int firstCode;
        public int entryCount;
        public int[] glyphIdArray;
    }

    private static class CmapFormat12 extends CmapFormat {
        public int reserved;
        public int length;
        public int language;
        public int numGroups;
        public List<int[]> groups;
    }

    private static class GlyfLayout {
        public short numberOfContours;      // 非负值为简单字型,负值为符合字型
        public short xMin;
        public short yMin;
        public short xMax;
        public short yMax;
        public int[] endPtsOfContours;   // length=numberOfContours
        public int instructionLength;
        public byte[] instructions;         // length=instructionLength
        public byte[] flags;
        public short[] xCoordinates;        // length = flags.length
        public short[] yCoordinates;        // length = flags.length
    }

    private static class ByteArrayReader {
        private final byte[] buffer;
        public final ByteBuffer byteBuffer;

        public ByteArrayReader(byte[] buffer, int index) {
            this.buffer = buffer;
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
            if (len < 0) throw new IllegalArgumentException("Length must not be negative");
            byte[] result = new byte[len];
            byteBuffer.get(result);
            return result;
        }

        public short[] ReadInt16Array(int len) {
            if (len < 0) throw new IllegalArgumentException("Length must not be negative");
            var result = new short[len];
            for (int i = 0; i < len; ++i) result[i] = byteBuffer.getShort();
            return result;
        }

        public int[] ReadUInt16Array(int len) {
            if (len < 0) throw new IllegalArgumentException("Length must not be negative");
            var result = new int[len];
            for (int i = 0; i < len; ++i) result[i] = byteBuffer.getShort() & 0xFFFF;
            return result;
        }
    }

    private final Header fileHeader = new Header();
    private final Map<String, Directory> directorys = new HashMap<>();
    private final NameLayout name = new NameLayout();
    private final HeadLayout head = new HeadLayout();
    private final MaxpLayout maxp = new MaxpLayout();
    private final List<Integer> loca = new LinkedList<>();
    private final CmapLayout Cmap = new CmapLayout();
    private final ConcurrentHashMap<Integer, String> glyf = new ConcurrentHashMap<>();
    private final int[][] pps = new int[][]{
            {3, 10},
            {0, 4},
            {3, 1},
            {1, 0},
            {0, 3},
            {0, 1}
    };

    public final Map<Integer, String> unicodeToGlyph = new HashMap<>();
    public final Map<String, Integer> glyphToUnicode = new HashMap<>();
    public final Map<Integer, Integer> unicodeToGlyphIndex = new HashMap<>();

    private void readNameTable(byte[] buffer) {
        var dataTable = Objects.requireNonNull(directorys.get("name"));
        var reader = new ByteArrayReader(buffer, dataTable.offset);
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
        var dataTable = Objects.requireNonNull(directorys.get("head"));
        var reader = new ByteArrayReader(buffer, dataTable.offset);
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

    private void readCmapTable(byte[] buffer) {
        var dataTable = Objects.requireNonNull(directorys.get("cmap"));
        var reader = new ByteArrayReader(buffer, dataTable.offset);
        Cmap.version = reader.ReadUInt16();
        Cmap.numTables = reader.ReadUInt16();
        for (int i = 0; i < Cmap.numTables; ++i) {
            CmapRecord record = new CmapRecord();
            record.platformID = reader.ReadUInt16();
            record.encodingID = reader.ReadUInt16();
            record.offset = reader.ReadUInt32();
            Cmap.records.add(record);
        }
        for (int i = 0; i < Cmap.numTables; ++i) {
            int fmtOffset = Cmap.records.get(i).offset;
            reader.position(dataTable.offset + fmtOffset);
            int EndIndex = reader.position();

            int format = reader.ReadUInt16();
            if (Cmap.tables.containsKey(fmtOffset)) continue;
            if (format == 0) {
                CmapFormat f = new CmapFormat();
                f.format = format;
                f.length = reader.ReadUInt16();
                f.language = reader.ReadUInt16();
                f.glyphIdArray = reader.ReadByteArray(f.length - 6);
                Cmap.tables.put(fmtOffset, f);
            } else if (format == 4) {
                CmapFormat4 f = new CmapFormat4();
                f.format = format;
                f.length = reader.ReadUInt16();
                f.language = reader.ReadUInt16();
                f.segCountX2 = reader.ReadUInt16();
                int segCount = f.segCountX2 >> 1;
                f.searchRange = reader.ReadUInt16();
                f.entrySelector = reader.ReadUInt16();
                f.rangeShift = reader.ReadUInt16();
                f.endCode = reader.ReadUInt16Array(segCount);
                f.reservedPad = reader.ReadUInt16();
                f.startCode = reader.ReadUInt16Array(segCount);
                f.idDelta = reader.ReadInt16Array(segCount);
                f.idRangeOffset = reader.ReadUInt16Array(segCount);
                f.glyphIdArray = reader.ReadUInt16Array((EndIndex + f.length - reader.position()) >> 1);
                Cmap.tables.put(fmtOffset, f);
            } else if (format == 6) {
                CmapFormat6 f = new CmapFormat6();
                f.format = format;
                f.length = reader.ReadUInt16();
                f.language = reader.ReadUInt16();
                f.firstCode = reader.ReadUInt16();
                f.entryCount = reader.ReadUInt16();
                f.glyphIdArray = reader.ReadUInt16Array(f.entryCount);
                Cmap.tables.put(fmtOffset, f);
            } else if (format == 12) {
                CmapFormat12 f = new CmapFormat12();
                f.format = format;
                f.reserved = reader.ReadUInt16();
                f.length = reader.ReadUInt32();
                f.language = reader.ReadUInt32();
                f.numGroups = reader.ReadUInt32();
                f.groups = new LinkedList<>();
                for (int n = 0; n < f.numGroups; ++n) {
                    f.groups.add(new int[]{reader.ReadUInt32(), reader.ReadUInt32(), reader.ReadUInt32()});
                }
                Cmap.tables.put(fmtOffset, f);
            }
        }
    }

    private void readLocaTable(byte[] buffer) {
        var dataTable = Objects.requireNonNull(directorys.get("loca"));
        var reader = new ByteArrayReader(buffer, dataTable.offset);
        var locaTableSize = dataTable.length;
        if (head.indexToLocFormat == 0) {
            for (long i = 0; i < locaTableSize; i += 2) loca.add(reader.ReadUInt16());
        } else {
            for (long i = 0; i < locaTableSize; i += 4) loca.add(reader.ReadUInt32());
        }
    }

    private void readMaxpTable(byte[] buffer) {
        var dataTable = Objects.requireNonNull(directorys.get("maxp"));
        var reader = new ByteArrayReader(buffer, dataTable.offset);
        maxp.majorVersion = reader.ReadUInt16();
        maxp.minorVersion = reader.ReadUInt16();
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

    private void readGlyfTable(byte[] buffer) {
        var dataTable = Objects.requireNonNull(directorys.get("glyf"));
        int glyfCount = maxp.numGlyphs;

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (int i = 0; i < glyfCount; i++) {
            final int index = i;
            executor.submit(() -> {
                var reader = new ByteArrayReader(buffer, dataTable.offset + loca.get(index));
                int glyfNextIndex = index + 1 < glyfCount ? (dataTable.offset + loca.get(index + 1)) : (dataTable.offset + dataTable.length);
                byte[] glyph;
                short numberOfContours = reader.ReadInt16();
                if (numberOfContours > 0) {
                    short g_xMin = reader.ReadInt16();
                    short g_yMin = reader.ReadInt16();
                    short g_xMax = reader.ReadInt16();
                    short g_yMax = reader.ReadInt16();
                    int[] endPtsOfContours = reader.ReadUInt16Array(numberOfContours);
                    int instructionLength = reader.ReadUInt16();
                    var instructions = reader.ReadByteArray(instructionLength);
                    int flagLength = endPtsOfContours[endPtsOfContours.length - 1] + 1;
                    // 获取轮廓点描述标志
                    var flags = new byte[flagLength];
                    for (int n = 0; n < flagLength; ++n) {
                        flags[n] = reader.ReadInt8();
                        if ((flags[n] & 0x08) != 0x00) {
                            for (int m = reader.ReadUInt8(); m > 0; --m) {
                                flags[++n] = flags[n - 1];
                            }
                        }
                    }
                    // 获取轮廓点描述x,y相对值
                    ByteBuffer xyCoordinatesBuffer = ByteBuffer.allocate(flagLength * 4);
                    // 获取轮廓点描述x轴相对值
                    for (int n = 0; n < flagLength; ++n) {
                        short same = (short) ((flags[n] & 0x10) != 0 ? 1 : -1);
                        if ((flags[n] & 0x02) != 0) {
                            xyCoordinatesBuffer.putShort((short) (same * reader.ReadUInt8()));
                        } else {
                            xyCoordinatesBuffer.putShort(same == 1 ? (short) 0 : reader.ReadInt16());
                        }
                    }
                    // 获取轮廓点描述y轴相对值
                    for (int n = 0; n < flagLength; ++n) {
                        short same = (short) ((flags[n] & 0x20) != 0 ? 1 : -1);
                        if ((flags[n] & 0x04) != 0) {
                            xyCoordinatesBuffer.putShort((short) (same * reader.ReadUInt8()));
                        } else {
                            xyCoordinatesBuffer.putShort(same == 1 ? (short) 0 : reader.ReadInt16());
                        }
                    }
                    // 保存轮廓点描述x,y相对值ByteArray
                    glyph = xyCoordinatesBuffer.array();
                } else {
                    // 复合字体未做详细处理
                    glyph = reader.ReadByteArray(glyfNextIndex - (reader.position() - 2));
                }
                glyf.put(index, getHexFromBytes(glyph));
            });
        }
        executor.shutdown();
        try {
            boolean b = executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e("queryTTF", "glyf表解析出错: " + e);
        }
    }

    /**
     * 构造函数
     *
     * @param buffer 传入TTF字体二进制数组
     */
    public QueryTTF(byte[] buffer) {
        var fontReader = new ByteArrayReader(buffer, 0);
        Log.i("[queryTTF]", "读文件头");
        // 获取文件头
        fileHeader.majorVersion = fontReader.ReadUInt16();
        fileHeader.minorVersion = fontReader.ReadUInt16();
        fileHeader.numOfTables = fontReader.ReadUInt16();
        fileHeader.searchRange = fontReader.ReadUInt16();
        fileHeader.entrySelector = fontReader.ReadUInt16();
        fileHeader.rangeShift = fontReader.ReadUInt16();
        // 获取目录
        for (int i = 0; i < fileHeader.numOfTables; ++i) {
            Directory d = new Directory();
            d.tag = new String(fontReader.ReadByteArray(4), StandardCharsets.US_ASCII);
            d.checkSum = fontReader.ReadUInt32();
            d.offset = fontReader.ReadUInt32();
            d.length = fontReader.ReadUInt32();
            directorys.put(d.tag, d);
        }

        Log.i("[queryTTF]", "解析表 name"); // 字体信息,包含版权、名称、作者等...
        readNameTable(buffer);
        Log.i("[queryTTF]", "解析表 head"); // 获取 head.indexToLocFormat
        readHeadTable(buffer);
        Log.i("[queryTTF]", "解析表 cmap"); // Unicode编码->轮廓索引 对照表
        readCmapTable(buffer);
        Log.i("[queryTTF]", "解析表 loca"); // 轮廓数据偏移地址表
        readLocaTable(buffer);
        Log.i("[queryTTF]", "解析表 maxp"); // 获取 maxp.numGlyphs 字体轮廓数量
        readMaxpTable(buffer);
        Log.i("[queryTTF]", "解析表 glyf"); // 字体轮廓数据表,需要解析loca,maxp表后计算
        readGlyfTable(buffer);

        Log.i("[queryTTF]", "创建Unicode&Glyph映射表");
        for (int key = 0; key < 130000; ++key) {
            Integer gid = queryGlyfIndex(key);
            if (gid >= glyf.size()) continue;
            unicodeToGlyphIndex.put(key, gid);
            var val = glyf.get(gid);
            unicodeToGlyph.put(key, val);
            if (glyphToUnicode.containsKey(val)) continue;
            glyphToUnicode.put(val, key);
        }
        Log.i("[queryTTF]", "字体处理完成");
    }

//    /**
//     * 获取字体信息 (1=字体名称)
//     *
//     * @param nameId 传入十进制字体信息索引
//     * @return 返回查询结果字符串
//     */
//    public String getNameById(int nameId) {
//        fontReader.index = Objects.requireNonNull(directorys.get("name")).offset;
//        for (NameRecord record : name.records) {
//            if (record.nameID != nameId) continue;
//            fontReader.index += name.stringOffset + record.offset;
//            return fontReader.ReadStrings(record.length, record.platformID == 1 ? StandardCharsets.UTF_8 : StandardCharsets.UTF_16BE);
//        }
//        return "error";
//    }

    /**
     * 使用Unicode值查找轮廓索引
     *
     * @param unicode 传入Unicode值
     * @return 返回十进制轮廓索引
     */
    public int queryGlyfIndex(int unicode) {
        if (unicode == 0) return 0;

        int fmtKey = 0;
        for (var item : pps) {
            for (CmapRecord record : Cmap.records) {
                if ((item[0] == record.platformID) && (item[1] == record.encodingID)) {
                    fmtKey = record.offset;
                    break;
                }
            }
            if (fmtKey > 0) break;
        }
        if (fmtKey == 0) return 0;

        int glyfID = 0;
        CmapFormat table = Cmap.tables.get(fmtKey);
        assert table != null;
        int fmt = table.format;
        if (fmt == 0) {
            if (unicode < table.glyphIdArray.length) glyfID = table.glyphIdArray[unicode] & 0xFF;
        } else if (fmt == 4) {
            CmapFormat4 tab = (CmapFormat4) table;
            if (unicode > tab.endCode[tab.endCode.length - 1]) return 0;
            // 二分法查找数值索引
            int start = 0, middle, end = tab.endCode.length - 1;
            while (start + 1 < end) {
                middle = (start + end) / 2;
                if (tab.endCode[middle] <= unicode) start = middle;
                else end = middle;
            }
            if (tab.endCode[start] < unicode) ++start;
            if (unicode < tab.startCode[start]) return 0;
            if (tab.idRangeOffset[start] != 0) {
                glyfID = tab.glyphIdArray[unicode - tab.startCode[start] + (tab.idRangeOffset[start] >> 1) - (tab.idRangeOffset.length - start)];
            } else glyfID = unicode + tab.idDelta[start];
            glyfID &= 0xFFFF;
        } else if (fmt == 6) {
            CmapFormat6 tab = (CmapFormat6) table;
            int index = unicode - tab.firstCode;
            if (0 <= index && index < tab.glyphIdArray.length) glyfID = tab.glyphIdArray[index];
        } else if (fmt == 12) {
            CmapFormat12 tab = (CmapFormat12) table;
            if (unicode > tab.groups.get(tab.numGroups - 1)[1]) return 0;
            for (int i = 0; i < tab.numGroups; i++) {
                if (tab.groups.get(i)[0] <= unicode && unicode <= tab.groups.get(i)[1]) {
                    glyfID = tab.groups.get(i)[2] + unicode - tab.groups.get(i)[0];
                    break;
                }
            }
        }
        return glyfID;
    }

    /**
     * 使用Unicode值获取轮廓索引
     *
     * @param unicode 传入Unicode值
     * @return 返回十进制轮廓索引
     */
    public int getGlyfIndex(int unicode) {
        var glyfIndex = unicodeToGlyphIndex.get(unicode);
        if (glyfIndex == null) return 0;
        return glyfIndex;
    }

    /**
     * 使用Unicode值获取轮廓数据
     *
     * @param unicode 传入Unicode值
     * @return 返回轮廓数组的String值
     */
    public String getGlyfByCode(int unicode) {
        return unicodeToGlyph.getOrDefault(unicode, "");
    }

    /**
     * 使用轮廓数据获取Unicode值
     *
     * @param glyph 传入轮廓数组的String值
     * @return 返回Unicode十进制值
     */
    public int getCodeByGlyf(String glyph) {
        //noinspection ConstantConditions
        return glyphToUnicode.getOrDefault(glyph, 0);
    }

    /**
     * 字体轮廓数据转Hex字符串
     *
     * @param glyph 字体轮廓数据
     * @return 返回轮廓数组的String值
     */
    public String getHexFromBytes(byte[] glyph) {
        if (glyph == null) return "";
        StringBuilder hexString = new StringBuilder();
        for (byte b : glyph) hexString.append(String.format("%02X", b));
        return hexString.toString();
    }
}
