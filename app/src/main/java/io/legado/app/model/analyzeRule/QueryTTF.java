package io.legado.app.model.analyzeRule;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"FieldCanBeLocal", "rawtypes", "StatementWithEmptyBody", "unused"})
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
        public List<Triple<Integer, Integer, Integer>> groups;
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
        public int Index;
        public byte[] Buffer;

        public ByteArrayReader(byte[] buffer, int index) {
            Buffer = buffer;
            Index = index;
        }

        public long ReadUIntX(long len) {
            long result = 0;
            for (long i = 0; i < len; ++i) {
                result <<= 8;
                result |= Buffer[Index++] & 0xFF;
            }
            return result;
        }

        public long ReadUInt64() {
            return ReadUIntX(8);
        }

        public int ReadUInt32() {
            return (int) ReadUIntX(4);
        }

        public int ReadUInt16() {
            return (int) ReadUIntX(2);
        }

        public short ReadInt16() {
            return (short) ReadUIntX(2);
        }

        public short ReadUInt8() {
            return (short) ReadUIntX(1);
        }


        public String ReadStrings(int len, Charset charset) {
            byte[] result = len > 0 ? new byte[len] : null;
            for (int i = 0; i < len; ++i) result[i] = Buffer[Index++];
            return new String(result, charset);
        }

        public byte GetByte() {
            return Buffer[Index++];
        }

        public byte[] GetBytes(int len) {
            byte[] result = len > 0 ? new byte[len] : null;
            for (int i = 0; i < len; ++i) result[i] = Buffer[Index++];
            return result;
        }

        public int[] GetUInt16Array(int len) {
            int[] result = len > 0 ? new int[len] : null;
            for (int i = 0; i < len; ++i) result[i] = ReadUInt16();
            return result;
        }

        public short[] GetInt16Array(int len) {
            short[] result = len > 0 ? new short[len] : null;
            for (int i = 0; i < len; ++i) result[i] = ReadInt16();
            return result;
        }
    }

    private final ByteArrayReader FontReader;
    private final Header fileHeader = new Header();
    private final List<Directory> Directorys = new LinkedList<>();
    private final NameLayout Name = new NameLayout();
    private final HeadLayout head = new HeadLayout();
    private final MaxpLayout maxp = new MaxpLayout();
    private final List<Integer> loca = new LinkedList<>();
    private final CmapLayout Cmap = new CmapLayout();
    private final List<GlyfLayout> glyf = new LinkedList<>();
    private final Pair[] pps = new Pair[]{
            Pair.of(3, 10),
            Pair.of(0, 4),
            Pair.of(3, 1),
            Pair.of(1, 0),
            Pair.of(0, 3),
            Pair.of(0, 1)
    };

    public final Map<Integer, String> CodeToGlyph = new HashMap<>();
    public final Map<String, Integer> GlyphToCode = new HashMap<>();
    private int LimitMix = 0;
    private int LimitMax = 0;

    /**
     * 构造函数
     *
     * @param buffer 传入TTF字体二进制数组
     */
    public QueryTTF(byte[] buffer) {
        FontReader = new ByteArrayReader(buffer, 0);
        // 获取文件头
        fileHeader.majorVersion = FontReader.ReadUInt16();
        fileHeader.minorVersion = FontReader.ReadUInt16();
        fileHeader.numOfTables = FontReader.ReadUInt16();
        fileHeader.searchRange = FontReader.ReadUInt16();
        fileHeader.entrySelector = FontReader.ReadUInt16();
        fileHeader.rangeShift = FontReader.ReadUInt16();
        // 获取目录
        for (int i = 0; i < fileHeader.numOfTables; ++i) {
            Directory d = new Directory();
            d.tag = FontReader.ReadStrings(4, StandardCharsets.US_ASCII);
            d.checkSum = FontReader.ReadUInt32();
            d.offset = FontReader.ReadUInt32();
            d.length = FontReader.ReadUInt32();
            Directorys.add(d);
        }
        // 解析表 name (字体信息,包含版权、名称、作者等...)
        for (Directory Temp : Directorys) {
            if (Temp.tag.equals("name")) {
                FontReader.Index = Temp.offset;
                Name.format = FontReader.ReadUInt16();
                Name.count = FontReader.ReadUInt16();
                Name.stringOffset = FontReader.ReadUInt16();
                for (int i = 0; i < Name.count; ++i) {
                    NameRecord record = new NameRecord();
                    record.platformID = FontReader.ReadUInt16();
                    record.encodingID = FontReader.ReadUInt16();
                    record.languageID = FontReader.ReadUInt16();
                    record.nameID = FontReader.ReadUInt16();
                    record.length = FontReader.ReadUInt16();
                    record.offset = FontReader.ReadUInt16();
                    Name.records.add(record);
                }
            }
        }
        // 解析表 head (获取 head.indexToLocFormat)
        for (Directory Temp : Directorys) {
            if (Temp.tag.equals("head")) {
                FontReader.Index = Temp.offset;
                head.majorVersion = FontReader.ReadUInt16();
                head.minorVersion = FontReader.ReadUInt16();
                head.fontRevision = FontReader.ReadUInt32();
                head.checkSumAdjustment = FontReader.ReadUInt32();
                head.magicNumber = FontReader.ReadUInt32();
                head.flags = FontReader.ReadUInt16();
                head.unitsPerEm = FontReader.ReadUInt16();
                head.created = FontReader.ReadUInt64();
                head.modified = FontReader.ReadUInt64();
                head.xMin = FontReader.ReadInt16();
                head.yMin = FontReader.ReadInt16();
                head.xMax = FontReader.ReadInt16();
                head.yMax = FontReader.ReadInt16();
                head.macStyle = FontReader.ReadUInt16();
                head.lowestRecPPEM = FontReader.ReadUInt16();
                head.fontDirectionHint = FontReader.ReadInt16();
                head.indexToLocFormat = FontReader.ReadInt16();
                head.glyphDataFormat = FontReader.ReadInt16();
            }
        }
        // 解析表 maxp (获取 maxp.numGlyphs)
        for (Directory Temp : Directorys) {
            if (Temp.tag.equals("maxp")) {
                FontReader.Index = Temp.offset;
                maxp.majorVersion = FontReader.ReadUInt16();
                maxp.minorVersion = FontReader.ReadUInt16();
                maxp.numGlyphs = FontReader.ReadUInt16();
                maxp.maxPoints = FontReader.ReadUInt16();
                maxp.maxContours = FontReader.ReadUInt16();
                maxp.maxCompositePoints = FontReader.ReadUInt16();
                maxp.maxCompositeContours = FontReader.ReadUInt16();
                maxp.maxZones = FontReader.ReadUInt16();
                maxp.maxTwilightPoints = FontReader.ReadUInt16();
                maxp.maxStorage = FontReader.ReadUInt16();
                maxp.maxFunctionDefs = FontReader.ReadUInt16();
                maxp.maxInstructionDefs = FontReader.ReadUInt16();
                maxp.maxStackElements = FontReader.ReadUInt16();
                maxp.maxSizeOfInstructions = FontReader.ReadUInt16();
                maxp.maxComponentElements = FontReader.ReadUInt16();
                maxp.maxComponentDepth = FontReader.ReadUInt16();
            }
        }
        // 解析表 loca (轮廓数据偏移地址表)
        for (Directory Temp : Directorys) {
            if (Temp.tag.equals("loca")) {
                FontReader.Index = Temp.offset;
                int offset = head.indexToLocFormat == 0 ? 2 : 4;
                for (long i = 0; i < Temp.length; i += offset) {
                    loca.add(offset == 2 ? FontReader.ReadUInt16() << 1 : FontReader.ReadUInt32());
                }
            }
        }
        // 解析表 cmap (Unicode编码轮廓索引对照表)
        for (Directory Temp : Directorys) {
            if (Temp.tag.equals("cmap")) {
                FontReader.Index = Temp.offset;
                Cmap.version = FontReader.ReadUInt16();
                Cmap.numTables = FontReader.ReadUInt16();

                for (int i = 0; i < Cmap.numTables; ++i) {
                    CmapRecord record = new CmapRecord();
                    record.platformID = FontReader.ReadUInt16();
                    record.encodingID = FontReader.ReadUInt16();
                    record.offset = FontReader.ReadUInt32();
                    Cmap.records.add(record);
                }
                for (int i = 0; i < Cmap.numTables; ++i) {
                    int fmtOffset = Cmap.records.get(i).offset;
                    FontReader.Index = Temp.offset + fmtOffset;
                    int EndIndex = FontReader.Index;

                    int format = FontReader.ReadUInt16();
                    if (Cmap.tables.containsKey(fmtOffset)) continue;
                    if (format == 0) {
                        CmapFormat f = new CmapFormat();
                        f.format = format;
                        f.length = FontReader.ReadUInt16();
                        f.language = FontReader.ReadUInt16();
                        f.glyphIdArray = FontReader.GetBytes(f.length - 6);
                        Cmap.tables.put(fmtOffset, f);
                    } else if (format == 4) {
                        CmapFormat4 f = new CmapFormat4();
                        f.format = format;
                        f.length = FontReader.ReadUInt16();
                        f.language = FontReader.ReadUInt16();
                        f.segCountX2 = FontReader.ReadUInt16();
                        int segCount = f.segCountX2 >> 1;
                        f.searchRange = FontReader.ReadUInt16();
                        f.entrySelector = FontReader.ReadUInt16();
                        f.rangeShift = FontReader.ReadUInt16();
                        f.endCode = FontReader.GetUInt16Array(segCount);
                        f.reservedPad = FontReader.ReadUInt16();
                        f.startCode = FontReader.GetUInt16Array(segCount);
                        f.idDelta = FontReader.GetInt16Array(segCount);
                        f.idRangeOffset = FontReader.GetUInt16Array(segCount);
                        f.glyphIdArray = FontReader.GetUInt16Array((EndIndex + f.length - FontReader.Index) >> 1);
                        Cmap.tables.put(fmtOffset, f);
                    } else if (format == 6) {
                        CmapFormat6 f = new CmapFormat6();
                        f.format = format;
                        f.length = FontReader.ReadUInt16();
                        f.language = FontReader.ReadUInt16();
                        f.firstCode = FontReader.ReadUInt16();
                        f.entryCount = FontReader.ReadUInt16();
                        f.glyphIdArray = FontReader.GetUInt16Array(f.entryCount);
                        Cmap.tables.put(fmtOffset, f);
                    } else if (format == 12) {
                        CmapFormat12 f = new CmapFormat12();
                        f.format = format;
                        f.reserved = FontReader.ReadUInt16();
                        f.length = FontReader.ReadUInt32();
                        f.language = FontReader.ReadUInt32();
                        f.numGroups = FontReader.ReadUInt32();
                        f.groups = new ArrayList<>(f.numGroups);
                        for (int n = 0; n < f.numGroups; ++n) {
                            f.groups.add(Triple.of(FontReader.ReadUInt32(), FontReader.ReadUInt32(), FontReader.ReadUInt32()));
                        }
                        Cmap.tables.put(fmtOffset, f);
                    }
                }
            }
        }
        // 解析表 glyf (字体轮廓数据表)
        for (Directory Temp : Directorys) {
            if (Temp.tag.equals("glyf")) {
                FontReader.Index = Temp.offset;
                for (int i = 0; i < maxp.numGlyphs; ++i) {
                    FontReader.Index = Temp.offset + loca.get(i);

                    short numberOfContours = FontReader.ReadInt16();
                    if (numberOfContours > 0) {
                        GlyfLayout g = new GlyfLayout();
                        g.numberOfContours = numberOfContours;
                        g.xMin = FontReader.ReadInt16();
                        g.yMin = FontReader.ReadInt16();
                        g.xMax = FontReader.ReadInt16();
                        g.yMax = FontReader.ReadInt16();
                        g.endPtsOfContours = FontReader.GetUInt16Array(numberOfContours);
                        g.instructionLength = FontReader.ReadUInt16();
                        g.instructions = FontReader.GetBytes(g.instructionLength);
                        int flagLength = g.endPtsOfContours[g.endPtsOfContours.length - 1] + 1;
                        // 获取轮廓点描述标志
                        g.flags = new byte[flagLength];
                        for (int n = 0; n < flagLength; ++n) {
                            g.flags[n] = FontReader.GetByte();
                            if ((g.flags[n] & 0x08) != 0x00) {
                                for (int m = FontReader.ReadUInt8(); m > 0; --m) {
                                    g.flags[++n] = g.flags[n - 1];
                                }
                            }
                        }
                        // 获取轮廓点描述x轴相对值
                        g.xCoordinates = new short[flagLength];
                        for (int n = 0; n < flagLength; ++n) {
                            short same = (short) ((g.flags[n] & 0x10) != 0 ? 1 : -1);
                            if ((g.flags[n] & 0x02) != 0) {
                                g.xCoordinates[n] = (short) (same * FontReader.ReadUInt8());
                            } else {
                                g.xCoordinates[n] = same == 1 ? (short) 0 : FontReader.ReadInt16();
                            }
                        }
                        // 获取轮廓点描述y轴相对值
                        g.yCoordinates = new short[flagLength];
                        for (int n = 0; n < flagLength; ++n) {
                            short same = (short) ((g.flags[n] & 0x20) != 0 ? 1 : -1);
                            if ((g.flags[n] & 0x04) != 0) {
                                g.yCoordinates[n] = (short) (same * FontReader.ReadUInt8());
                            } else {
                                g.yCoordinates[n] = same == 1 ? (short) 0 : FontReader.ReadInt16();
                            }
                        }
                        // 相对坐标转绝对坐标
//                        for (int n = 1; n < flagLength; ++n) {
//                            xCoordinates[n] += xCoordinates[n - 1];
//                            yCoordinates[n] += yCoordinates[n - 1];
//                        }

                        glyf.add(g);
                    } else {
                        // 复合字体暂未使用
                    }
                }
            }
        }

        // 建立Unicode&Glyph双向表
        for (int key = 0; key < 130000; ++key) {
            if (key == 0xFF) key = 0x3400;
            int gid = getGlyfIndex(key);
            if (gid == 0) continue;
            StringBuilder sb = new StringBuilder();
            // 字型数据转String，方便存HashMap
            for (short b : glyf.get(gid).xCoordinates) sb.append(b);
            for (short b : glyf.get(gid).yCoordinates) sb.append(b);
            String val = sb.toString();
            if (LimitMix == 0) LimitMix = key;
            LimitMax = key;
            CodeToGlyph.put(key, val);
            if (GlyphToCode.containsKey(val)) continue;
            GlyphToCode.put(val, key);
        }
    }

    /**
     * 获取字体信息 (1=字体名称)
     *
     * @param nameId 传入十进制字体信息索引
     * @return 返回查询结果字符串
     */
    public String getNameById(int nameId) {
        for (Directory Temp : Directorys) {
            if (!Temp.tag.equals("name")) continue;
            FontReader.Index = Temp.offset;
            break;
        }
        for (NameRecord record : Name.records) {
            if (record.nameID != nameId) continue;
            FontReader.Index += Name.stringOffset + record.offset;
            return FontReader.ReadStrings(record.length, record.platformID == 1 ? StandardCharsets.UTF_8 : StandardCharsets.UTF_16BE);
        }
        return "error";
    }

    /**
     * 使用Unicode值查找轮廓索引
     *
     * @param code 传入Unicode十进制值
     * @return 返回十进制轮廓索引
     */
    private int getGlyfIndex(int code) {
        if (code == 0) return 0;
        int fmtKey = 0;
        for (@SuppressWarnings("unchecked") Pair<Integer, Integer> item : pps) {
            for (CmapRecord record : Cmap.records) {
                if ((item.getLeft() == record.platformID) && (item.getRight() == record.encodingID)) {
                    fmtKey = record.offset;
                    break;
                }
            }
            if (fmtKey > 0) break;
        }
        if (fmtKey == 0) return 0;

        int glyfID = 0;
        CmapFormat table = Cmap.tables.get(fmtKey);
        int fmt = table.format;
        if (fmt == 0) {
            if (code < table.glyphIdArray.length) glyfID = table.glyphIdArray[code] & 0xFF;
        } else if (fmt == 4) {
            CmapFormat4 tab = (CmapFormat4) table;
            if (code > tab.endCode[tab.endCode.length - 1]) return 0;
            // 二分法查找数值索引
            int start = 0, middle, end = tab.endCode.length - 1;
            while (start + 1 < end) {
                middle = (start + end) / 2;
                if (tab.endCode[middle] <= code) start = middle;
                else end = middle;
            }
            if (tab.endCode[start] < code) ++start;
            if (code < tab.startCode[start]) return 0;
            if (tab.idRangeOffset[start] != 0) {
                glyfID = tab.glyphIdArray[code - tab.startCode[start] + (tab.idRangeOffset[start] >> 1) - (tab.idRangeOffset.length - start)];
            } else glyfID = code + tab.idDelta[start];
            glyfID &= 0xFFFF;
        } else if (fmt == 6) {
            CmapFormat6 tab = (CmapFormat6) table;
            int index = code - tab.firstCode;
            if (index < 0 || index >= tab.glyphIdArray.length) glyfID = 0;
            else glyfID = tab.glyphIdArray[index];
        } else if (fmt == 12) {
            CmapFormat12 tab = (CmapFormat12) table;
            if (code > tab.groups.get(tab.numGroups - 1).getMiddle()) return 0;
            // 二分法查找数值索引
            int start = 0, middle, end = tab.numGroups - 1;
            while (start + 1 < end) {
                middle = (start + end) / 2;
                if (tab.groups.get(middle).getLeft() <= code) start = middle;
                else end = middle;
            }
            if (tab.groups.get(start).getLeft() <= code && code <= tab.groups.get(start).getMiddle()) {
                glyfID = tab.groups.get(start).getRight() + code - tab.groups.get(start).getLeft();
            }
        }
        return glyfID;
    }

    /**
     * 判断Unicode值是否在字体范围内
     *
     * @param code 传入Unicode十进制值
     * @return 返回bool查询结果
     */
    public boolean inLimit(char code) {
        return (LimitMix <= code) && (code < LimitMax);
    }

    /**
     * 使用Unicode值获取轮廓数据
     *
     * @param key 传入Unicode十进制值
     * @return 返回轮廓数组的String值
     */
    public String getGlyfByCode(int key) {
        return CodeToGlyph.getOrDefault(key, "");
    }

    /**
     * 使用轮廓数据获取Unicode值
     *
     * @param val 传入轮廓数组的String值
     * @return 返回Unicode十进制值
     */
    public int getCodeByGlyf(String val) {
        return GlyphToCode.getOrDefault(val, 0);
    }
}
