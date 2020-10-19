package io.legado.app.utils;

import androidx.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static android.text.TextUtils.isEmpty;

/**
 * <Detect encoding .> Copyright (C) <2009> <Fluck,ACC http://androidos.cc/dev>
 * <p>
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * EncodingDetect.java<br>
 * 自动获取文件的编码
 *
 * @author Billows.Van
 * @version 1.0
 * @since Create on 2010-01-27 11:19:00
 */
@SuppressWarnings("ALL")
public class EncodingDetect {

    public static String getHtmlEncode(@NonNull byte[] bytes) {
        try {
            Document doc = Jsoup.parse(new String(bytes, StandardCharsets.UTF_8));
            Elements metaTags = doc.getElementsByTag("meta");
            String charsetStr;
            for (Element metaTag : metaTags) {
                charsetStr = metaTag.attr("charset");
                if (!isEmpty(charsetStr)) {
                    return charsetStr;
                }
                String content = metaTag.attr("content");
                String http_equiv = metaTag.attr("http-equiv");
                if (http_equiv.toLowerCase().equals("content-type")) {
                    if (content.toLowerCase().contains("charset")) {
                        charsetStr = content.substring(content.toLowerCase().indexOf("charset") + "charset=".length());
                    } else {
                        charsetStr = content.substring(content.toLowerCase().indexOf(";") + 1);
                    }
                    if (!isEmpty(charsetStr)) {
                        return charsetStr;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return getEncode(bytes);
    }

    public static String getEncode(@NonNull byte[] bytes) {
        int len = Math.min(bytes.length, 2000);
        byte[] cBytes = new byte[len];
        System.arraycopy(bytes, 0, cBytes, 0, len);
        BytesEncodingDetect bytesEncodingDetect = new BytesEncodingDetect();
        String code = BytesEncodingDetect.javaname[bytesEncodingDetect.detectEncoding(cBytes)];
        // UTF-16LE 特殊处理
        if ("Unicode".equals(code)) {
            if (cBytes[0] == -1) {
                code = "UTF-16LE";
            }
        }
        return code;
    }

    /**
     * 得到文件的编码
     */
    public static String getEncode(@NonNull String filePath) {
        BytesEncodingDetect s = new BytesEncodingDetect();
        String fileCode = BytesEncodingDetect.javaname[s
                .detectEncoding(new File(filePath))];

        // UTF-16LE 特殊处理
        if ("Unicode".equals(fileCode)) {
            byte[] tempByte = BytesEncodingDetect.getFileBytes(new File(
                    filePath));
            if (tempByte[0] == -1) {
                fileCode = "UTF-16LE";
            }
        }
        return fileCode;
    }

    /**
     * 得到文件的编码
     */
    public static String getEncode(@NonNull File file) {
        BytesEncodingDetect s = new BytesEncodingDetect();
        String fileCode = BytesEncodingDetect.javaname[s.detectEncoding(file)];
        // UTF-16LE 特殊处理
        if ("Unicode".equals(fileCode)) {
            byte[] tempByte = BytesEncodingDetect.getFileBytes(file);
            if (tempByte[0] == -1) {
                fileCode = "UTF-16LE";
            }
        }
        return fileCode;
    }

}

@SuppressWarnings("ALL")
class BytesEncodingDetect extends Encoding {
    // Frequency tables to hold the GB, Big5, and EUC-TW character
    // frequencies
    private int[][] GBFreq;

    private int[][] GBKFreq;

    private int[][] Big5Freq;

    private int[][] Big5PFreq;

    private int[][] EUC_TWFreq;

    private int[][] KRFreq;

    private int[][] JPFreq;

    public boolean debug;

    BytesEncodingDetect() {
        super();
        debug = false;
        GBFreq = new int[94][94];
        GBKFreq = new int[126][191];
        Big5Freq = new int[94][158];
        Big5PFreq = new int[126][191];
        EUC_TWFreq = new int[94][94];
        KRFreq = new int[94][94];
        JPFreq = new int[94][94];
        // Initialize the Frequency Table for GB, GBK, Big5, EUC-TW, KR, JP
        initialize_frequencies();
    }

    /**
     * Function : detectEncoding Aruguments: URL Returns : One of the encodings
     * from the Encoding enumeration (GB2312, HZ, BIG5, EUC_TW, ASCII, or OTHER)
     * Description: This function looks at the URL contents and assigns it a
     * probability score for each encoding type. The encoding type with the
     * highest probability is returned.
     */
    public int detectEncoding(URL testurl) {
        byte[] rawtext = new byte[10000];
        int bytesread = 0, byteoffset = 0;
        int guess = OTHER;
        InputStream chinesestream;
        try {
            chinesestream = testurl.openStream();
            while ((bytesread = chinesestream.read(rawtext, byteoffset,
                    rawtext.length - byteoffset)) > 0) {
                byteoffset += bytesread;
            }
            chinesestream.close();
            guess = detectEncoding(rawtext);
        } catch (Exception e) {
            System.err.println("Error loading or using URL " + e.toString());
            guess = -1;
        }
        return guess;
    }

    /**
     * Function : detectEncoding Aruguments: File Returns : One of the encodings
     * from the Encoding enumeration (GB2312, HZ, BIG5, EUC_TW, ASCII, or OTHER)
     * Description: This function looks at the file and assigns it a probability
     * score for each encoding type. The encoding type with the highest
     * probability is returned.
     */
    int detectEncoding(File testfile) {
        byte[] rawtext = getFileBytes(testfile);
        return detectEncoding(rawtext);
    }

    static byte[] getFileBytes(File testfile) {
        FileInputStream chinesefile;
        byte[] rawtext;
        rawtext = new byte[2000];
        try {
            chinesefile = new FileInputStream(testfile);
            chinesefile.read(rawtext);
            chinesefile.close();
        } catch (Exception e) {
            System.err.println("Error: " + e);
        }
        return rawtext;
    }


    /**
     * Function : detectEncoding Aruguments: byte array Returns : One of the
     * encodings from the Encoding enumeration (GB2312, HZ, BIG5, EUC_TW, ASCII,
     * or OTHER) Description: This function looks at the byte array and assigns
     * it a probability score for each encoding type. The encoding type with the
     * highest probability is returned.
     */
    int detectEncoding(byte[] rawtext) {
        int[] scores;
        int index, maxscore = 0;
        int encoding_guess = OTHER;
        scores = new int[TOTALTYPES];
        // Assign Scores
        scores[GB2312] = gb2312_probability(rawtext);
        scores[GBK] = gbk_probability(rawtext);
        scores[GB18030] = gb18030_probability(rawtext);
        scores[HZ] = hz_probability(rawtext);
        scores[BIG5] = big5_probability(rawtext);
        scores[CNS11643] = euc_tw_probability(rawtext);
        scores[ISO2022CN] = iso_2022_cn_probability(rawtext);
        scores[UTF8] = utf8_probability(rawtext);
        scores[UNICODE] = utf16_probability(rawtext);
        scores[EUC_KR] = euc_kr_probability(rawtext);
        scores[CP949] = cp949_probability(rawtext);
        scores[JOHAB] = 0;
        scores[ISO2022KR] = iso_2022_kr_probability(rawtext);
        scores[ASCII] = ascii_probability(rawtext);
        scores[SJIS] = sjis_probability(rawtext);
        scores[EUC_JP] = euc_jp_probability(rawtext);
        scores[ISO2022JP] = iso_2022_jp_probability(rawtext);
        scores[UNICODET] = 0;
        scores[UNICODES] = 0;
        scores[ISO2022CN_GB] = 0;
        scores[ISO2022CN_CNS] = 0;
        scores[OTHER] = 0;
        // Tabulate Scores
        for (index = 0; index < TOTALTYPES; index++) {
            if (debug)
                System.err.println("Encoding " + nicename[index] + " score "
                        + scores[index]);
            if (scores[index] > maxscore) {
                encoding_guess = index;
                maxscore = scores[index];
            }
        }
        // Return OTHER if nothing scored above 50
        if (maxscore <= 50) {
            encoding_guess = OTHER;
        }
        return encoding_guess;
    }

    /*
     * Function: gb2312_probability Argument: pointer to byte array Returns :
     * number from 0 to 100 representing probability text in array uses GB-2312
     * encoding
     */
    int gb2312_probability(byte[] rawtext) {
        int i, rawtextlen = 0;
        int dbchars = 1, gbchars = 1;
        long gbfreq = 0, totalfreq = 1;
        float rangeval = 0, freqval = 0;
        int row, column;
        // Stage 1: Check to see if characters fit into acceptable ranges
        rawtextlen = rawtext.length;
        for (i = 0; i < rawtextlen - 1; i++) {
            // System.err.println(rawtext[i]);
            if (rawtext[i] < 0) {
                dbchars++;
                if ((byte) 0xA1 <= rawtext[i] && rawtext[i] <= (byte) 0xF7
                        && (byte) 0xA1 <= rawtext[i + 1]
                        && rawtext[i + 1] <= (byte) 0xFE) {
                    gbchars++;
                    totalfreq += 500;
                    row = rawtext[i] + 256 - 0xA1;
                    column = rawtext[i + 1] + 256 - 0xA1;
                    if (GBFreq[row][column] != 0) {
                        gbfreq += GBFreq[row][column];
                    } else if (15 <= row && row < 55) {
                        // In GB high-freq character range
                        gbfreq += 200;
                    }
                }
                i++;
            }
        }
        rangeval = 50 * ((float) gbchars / (float) dbchars);
        freqval = 50 * ((float) gbfreq / (float) totalfreq);
        return (int) (rangeval + freqval);
    }

    /*
     * Function: gbk_probability Argument: pointer to byte array Returns :
     * number from 0 to 100 representing probability text in array uses GBK
     * encoding
     */
    int gbk_probability(byte[] rawtext) {
        int i, rawtextlen = 0;
        int dbchars = 1, gbchars = 1;
        long gbfreq = 0, totalfreq = 1;
        float rangeval = 0, freqval = 0;
        int row, column;
        // Stage 1: Check to see if characters fit into acceptable ranges
        rawtextlen = rawtext.length;
        for (i = 0; i < rawtextlen - 1; i++) {
            // System.err.println(rawtext[i]);
            if (rawtext[i] < 0) {
                dbchars++;
                if ((byte) 0xA1 <= rawtext[i] && rawtext[i] <= (byte) 0xF7
                        && // Original GB range
                        (byte) 0xA1 <= rawtext[i + 1]
                        && rawtext[i + 1] <= (byte) 0xFE) {
                    gbchars++;
                    totalfreq += 500;
                    row = rawtext[i] + 256 - 0xA1;
                    column = rawtext[i + 1] + 256 - 0xA1;
                    // System.out.println("original row " + row + " column " +
                    // column);
                    if (GBFreq[row][column] != 0) {
                        gbfreq += GBFreq[row][column];
                    } else if (15 <= row && row < 55) {
                        gbfreq += 200;
                    }
                } else if ((byte) 0x81 <= rawtext[i]
                        && rawtext[i] <= (byte) 0xFE && // Extended GB range
                        (((byte) 0x80 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFE) || ((byte) 0x40 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0x7E))) {
                    gbchars++;
                    totalfreq += 500;
                    row = rawtext[i] + 256 - 0x81;
                    if (0x40 <= rawtext[i + 1] && rawtext[i + 1] <= 0x7E) {
                        column = rawtext[i + 1] - 0x40;
                    } else {
                        column = rawtext[i + 1] + 256 - 0x40;
                    }
                    // System.out.println("extended row " + row + " column " +
                    // column + " rawtext[i] " + rawtext[i]);
                    if (GBKFreq[row][column] != 0) {
                        gbfreq += GBKFreq[row][column];
                    }
                }
                i++;
            }
        }
        rangeval = 50 * ((float) gbchars / (float) dbchars);
        freqval = 50 * ((float) gbfreq / (float) totalfreq);
        // For regular GB files, this would give the same score, so I handicap
        // it slightly
        return (int) (rangeval + freqval) - 1;
    }

    /*
     * Function: gb18030_probability Argument: pointer to byte array Returns :
     * number from 0 to 100 representing probability text in array uses GBK
     * encoding
     */
    int gb18030_probability(byte[] rawtext) {
        int i, rawtextlen = 0;
        int dbchars = 1, gbchars = 1;
        long gbfreq = 0, totalfreq = 1;
        float rangeval = 0, freqval = 0;
        int row, column;
        // Stage 1: Check to see if characters fit into acceptable ranges
        rawtextlen = rawtext.length;
        for (i = 0; i < rawtextlen - 1; i++) {
            // System.err.println(rawtext[i]);
            if (rawtext[i] < 0) {
                dbchars++;
                if ((byte) 0xA1 <= rawtext[i] && rawtext[i] <= (byte) 0xF7
                        && // Original GB range
                        i + 1 < rawtextlen && (byte) 0xA1 <= rawtext[i + 1]
                        && rawtext[i + 1] <= (byte) 0xFE) {
                    gbchars++;
                    totalfreq += 500;
                    row = rawtext[i] + 256 - 0xA1;
                    column = rawtext[i + 1] + 256 - 0xA1;
                    // System.out.println("original row " + row + " column " +
                    // column);
                    if (GBFreq[row][column] != 0) {
                        gbfreq += GBFreq[row][column];
                    } else if (15 <= row && row < 55) {
                        gbfreq += 200;
                    }
                } else if ((byte) 0x81 <= rawtext[i]
                        && rawtext[i] <= (byte) 0xFE
                        && // Extended GB range
                        i + 1 < rawtextlen
                        && (((byte) 0x80 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFE) || ((byte) 0x40 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0x7E))) {
                    gbchars++;
                    totalfreq += 500;
                    row = rawtext[i] + 256 - 0x81;
                    if (0x40 <= rawtext[i + 1] && rawtext[i + 1] <= 0x7E) {
                        column = rawtext[i + 1] - 0x40;
                    } else {
                        column = rawtext[i + 1] + 256 - 0x40;
                    }
                    // System.out.println("extended row " + row + " column " +
                    // column + " rawtext[i] " + rawtext[i]);
                    if (GBKFreq[row][column] != 0) {
                        gbfreq += GBKFreq[row][column];
                    }
                } else if ((byte) 0x81 <= rawtext[i]
                        && rawtext[i] <= (byte) 0xFE
                        && // Extended GB range
                        i + 3 < rawtextlen && (byte) 0x30 <= rawtext[i + 1]
                        && rawtext[i + 1] <= (byte) 0x39
                        && (byte) 0x81 <= rawtext[i + 2]
                        && rawtext[i + 2] <= (byte) 0xFE
                        && (byte) 0x30 <= rawtext[i + 3]
                        && rawtext[i + 3] <= (byte) 0x39) {
                    gbchars++;
                }
                i++;
            }
        }
        rangeval = 50 * ((float) gbchars / (float) dbchars);
        freqval = 50 * ((float) gbfreq / (float) totalfreq);
        // For regular GB files, this would give the same score, so I handicap
        // it slightly
        return (int) (rangeval + freqval) - 1;
    }

    /*
     * Function: hz_probability Argument: byte array Returns : number from 0 to
     * 100 representing probability text in array uses HZ encoding
     */
    int hz_probability(byte[] rawtext) {
        int i, rawtextlen;
        int hzchars = 0, dbchars = 1;
        long hzfreq = 0, totalfreq = 1;
        float rangeval = 0, freqval = 0;
        int hzstart = 0, hzend = 0;
        int row, column;
        rawtextlen = rawtext.length;
        for (i = 0; i < rawtextlen; i++) {
            if (rawtext[i] == '~') {
                if (rawtext[i + 1] == '{') {
                    hzstart++;
                    i += 2;
                    while (i < rawtextlen - 1) {
                        if (rawtext[i] == 0x0A || rawtext[i] == 0x0D) {
                            break;
                        } else if (rawtext[i] == '~' && rawtext[i + 1] == '}') {
                            hzend++;
                            i++;
                            break;
                        } else if ((0x21 <= rawtext[i] && rawtext[i] <= 0x77)
                                && (0x21 <= rawtext[i + 1] && rawtext[i + 1] <= 0x77)) {
                            hzchars += 2;
                            row = rawtext[i] - 0x21;
                            column = rawtext[i + 1] - 0x21;
                            totalfreq += 500;
                            if (GBFreq[row][column] != 0) {
                                hzfreq += GBFreq[row][column];
                            } else if (15 <= row && row < 55) {
                                hzfreq += 200;
                            }
                        } else if ((0xA1 <= rawtext[i] && rawtext[i] <= 0xF7)
                                && (0xA1 <= rawtext[i + 1] && rawtext[i + 1] <= 0xF7)) {
                            hzchars += 2;
                            row = rawtext[i] + 256 - 0xA1;
                            column = rawtext[i + 1] + 256 - 0xA1;
                            totalfreq += 500;
                            if (GBFreq[row][column] != 0) {
                                hzfreq += GBFreq[row][column];
                            } else if (15 <= row && row < 55) {
                                hzfreq += 200;
                            }
                        }
                        dbchars += 2;
                        i += 2;
                    }
                } else if (rawtext[i + 1] == '}') {
                    hzend++;
                    i++;
                } else if (rawtext[i + 1] == '~') {
                    i++;
                }
            }
        }
        if (hzstart > 4) {
            rangeval = 50;
        } else if (hzstart > 1) {
            rangeval = 41;
        } else if (hzstart > 0) { // Only 39 in case the sequence happened to
            // occur
            rangeval = 39; // in otherwise non-Hz text
        } else {
            rangeval = 0;
        }
        freqval = 50 * ((float) hzfreq / (float) totalfreq);
        return (int) (rangeval + freqval);
    }

    /**
     * Function: big5_probability Argument: byte array Returns : number from 0
     * to 100 representing probability text in array uses Big5 encoding
     */
    int big5_probability(byte[] rawtext) {
        int i, rawtextlen = 0;
        int dbchars = 1, bfchars = 1;
        float rangeval = 0, freqval = 0;
        long bffreq = 0, totalfreq = 1;
        int row, column;
        // Check to see if characters fit into acceptable ranges
        rawtextlen = rawtext.length;
        for (i = 0; i < rawtextlen - 1; i++) {
            if (rawtext[i] < 0) {
                dbchars++;
                if ((byte) 0xA1 <= rawtext[i]
                        && rawtext[i] <= (byte) 0xF9
                        && (((byte) 0x40 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0x7E) || ((byte) 0xA1 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFE))) {
                    bfchars++;
                    totalfreq += 500;
                    row = rawtext[i] + 256 - 0xA1;
                    if (0x40 <= rawtext[i + 1] && rawtext[i + 1] <= 0x7E) {
                        column = rawtext[i + 1] - 0x40;
                    } else {
                        column = rawtext[i + 1] + 256 - 0x61;
                    }
                    if (Big5Freq[row][column] != 0) {
                        bffreq += Big5Freq[row][column];
                    } else if (3 <= row && row <= 37) {
                        bffreq += 200;
                    }
                }
                i++;
            }
        }
        rangeval = 50 * ((float) bfchars / (float) dbchars);
        freqval = 50 * ((float) bffreq / (float) totalfreq);
        return (int) (rangeval + freqval);
    }

    /*
     * Function: big5plus_probability Argument: pointer to unsigned char array
     * Returns : number from 0 to 100 representing probability text in array
     * uses Big5+ encoding
     */
    int big5plus_probability(byte[] rawtext) {
        int i, rawtextlen = 0;
        int dbchars = 1, bfchars = 1;
        long bffreq = 0, totalfreq = 1;
        float rangeval = 0, freqval = 0;
        int row, column;
        // Stage 1: Check to see if characters fit into acceptable ranges
        rawtextlen = rawtext.length;
        for (i = 0; i < rawtextlen - 1; i++) {
            // System.err.println(rawtext[i]);
            if (rawtext[i] < 128) {
                dbchars++;
                if (0xA1 <= rawtext[i]
                        && rawtext[i] <= 0xF9
                        && // Original Big5 range
                        ((0x40 <= rawtext[i + 1] && rawtext[i + 1] <= 0x7E) || (0xA1 <= rawtext[i + 1] && rawtext[i + 1] <= 0xFE))) {
                    bfchars++;
                    totalfreq += 500;
                    row = rawtext[i] - 0xA1;
                    if (0x40 <= rawtext[i + 1] && rawtext[i + 1] <= 0x7E) {
                        column = rawtext[i + 1] - 0x40;
                    } else {
                        column = rawtext[i + 1] - 0x61;
                    }
                    // System.out.println("original row " + row + " column " +
                    // column);
                    if (Big5Freq[row][column] != 0) {
                        bffreq += Big5Freq[row][column];
                    } else if (3 <= row && row < 37) {
                        bffreq += 200;
                    }
                } else if (0x81 <= rawtext[i]
                        && rawtext[i] <= 0xFE
                        && // Extended Big5 range
                        ((0x40 <= rawtext[i + 1] && rawtext[i + 1] <= 0x7E) || (0x80 <= rawtext[i + 1] && rawtext[i + 1] <= 0xFE))) {
                    bfchars++;
                    totalfreq += 500;
                    row = rawtext[i] - 0x81;
                    if (0x40 <= rawtext[i + 1] && rawtext[i + 1] <= 0x7E) {
                        column = rawtext[i + 1] - 0x40;
                    } else {
                        column = rawtext[i + 1] - 0x40;
                    }
                    // System.out.println("extended row " + row + " column " +
                    // column + " rawtext[i] " + rawtext[i]);
                    if (Big5PFreq[row][column] != 0) {
                        bffreq += Big5PFreq[row][column];
                    }
                }
                i++;
            }
        }
        rangeval = 50 * ((float) bfchars / (float) dbchars);
        freqval = 50 * ((float) bffreq / (float) totalfreq);
        // For regular Big5 files, this would give the same score, so I handicap
        // it slightly
        return (int) (rangeval + freqval) - 1;
    }

    /*
     * Function: euc_tw_probability Argument: byte array Returns : number from 0
     * to 100 representing probability text in array uses EUC-TW (CNS 11643)
     * encoding
     */
    int euc_tw_probability(byte[] rawtext) {
        int i, rawtextlen = 0;
        int dbchars = 1, cnschars = 1;
        long cnsfreq = 0, totalfreq = 1;
        float rangeval = 0, freqval = 0;
        int row, column;
        // Check to see if characters fit into acceptable ranges
        // and have expected frequency of use
        rawtextlen = rawtext.length;
        for (i = 0; i < rawtextlen - 1; i++) {
            if (rawtext[i] < 0) { // high bit set
                dbchars++;
                if (i + 3 < rawtextlen && (byte) 0x8E == rawtext[i]
                        && (byte) 0xA1 <= rawtext[i + 1]
                        && rawtext[i + 1] <= (byte) 0xB0
                        && (byte) 0xA1 <= rawtext[i + 2]
                        && rawtext[i + 2] <= (byte) 0xFE
                        && (byte) 0xA1 <= rawtext[i + 3]
                        && rawtext[i + 3] <= (byte) 0xFE) { // Planes 1 - 16
                    cnschars++;
                    // System.out.println("plane 2 or above CNS char");
                    // These are all less frequent chars so just ignore freq
                    i += 3;
                } else if ((byte) 0xA1 <= rawtext[i]
                        && rawtext[i] <= (byte) 0xFE
                        && // Plane 1
                        (byte) 0xA1 <= rawtext[i + 1]
                        && rawtext[i + 1] <= (byte) 0xFE) {
                    cnschars++;
                    totalfreq += 500;
                    row = rawtext[i] + 256 - 0xA1;
                    column = rawtext[i + 1] + 256 - 0xA1;
                    if (EUC_TWFreq[row][column] != 0) {
                        cnsfreq += EUC_TWFreq[row][column];
                    } else if (35 <= row && row <= 92) {
                        cnsfreq += 150;
                    }
                    i++;
                }
            }
        }
        rangeval = 50 * ((float) cnschars / (float) dbchars);
        freqval = 50 * ((float) cnsfreq / (float) totalfreq);
        return (int) (rangeval + freqval);
    }

    /*
     * Function: iso_2022_cn_probability Argument: byte array Returns : number
     * from 0 to 100 representing probability text in array uses ISO 2022-CN
     * encoding WORKS FOR BASIC CASES, BUT STILL NEEDS MORE WORK
     */
    int iso_2022_cn_probability(byte[] rawtext) {
        int i, rawtextlen = 0;
        int dbchars = 1, isochars = 1;
        long isofreq = 0, totalfreq = 1;
        float rangeval = 0, freqval = 0;
        int row, column;
        // Check to see if characters fit into acceptable ranges
        // and have expected frequency of use
        rawtextlen = rawtext.length;
        for (i = 0; i < rawtextlen - 1; i++) {
            if (rawtext[i] == (byte) 0x1B && i + 3 < rawtextlen) { // Escape
                // char ESC
                if (rawtext[i + 1] == (byte) 0x24 && rawtext[i + 2] == 0x29
                        && rawtext[i + 3] == (byte) 0x41) { // GB Escape $ ) A
                    i += 4;
                    while (rawtext[i] != (byte) 0x1B) {
                        dbchars++;
                        if ((0x21 <= rawtext[i] && rawtext[i] <= 0x77)
                                && (0x21 <= rawtext[i + 1] && rawtext[i + 1] <= 0x77)) {
                            isochars++;
                            row = rawtext[i] - 0x21;
                            column = rawtext[i + 1] - 0x21;
                            totalfreq += 500;
                            if (GBFreq[row][column] != 0) {
                                isofreq += GBFreq[row][column];
                            } else if (15 <= row && row < 55) {
                                isofreq += 200;
                            }
                            i++;
                        }
                        i++;
                    }
                } else if (i + 3 < rawtextlen && rawtext[i + 1] == (byte) 0x24
                        && rawtext[i + 2] == (byte) 0x29
                        && rawtext[i + 3] == (byte) 0x47) {
                    // CNS Escape $ ) G
                    i += 4;
                    while (rawtext[i] != (byte) 0x1B) {
                        dbchars++;
                        if ((byte) 0x21 <= rawtext[i]
                                && rawtext[i] <= (byte) 0x7E
                                && (byte) 0x21 <= rawtext[i + 1]
                                && rawtext[i + 1] <= (byte) 0x7E) {
                            isochars++;
                            totalfreq += 500;
                            row = rawtext[i] - 0x21;
                            column = rawtext[i + 1] - 0x21;
                            if (EUC_TWFreq[row][column] != 0) {
                                isofreq += EUC_TWFreq[row][column];
                            } else if (35 <= row && row <= 92) {
                                isofreq += 150;
                            }
                            i++;
                        }
                        i++;
                    }
                }
                if (rawtext[i] == (byte) 0x1B && i + 2 < rawtextlen
                        && rawtext[i + 1] == (byte) 0x28
                        && rawtext[i + 2] == (byte) 0x42) { // ASCII:
                    // ESC
                    // ( B
                    i += 2;
                }
            }
        }
        rangeval = 50 * ((float) isochars / (float) dbchars);
        freqval = 50 * ((float) isofreq / (float) totalfreq);
        // System.out.println("isochars dbchars isofreq totalfreq " + isochars +
        // " " + dbchars + " " + isofreq + " " + totalfreq + "
        // " + rangeval + " " + freqval);
        return (int) (rangeval + freqval);
        // return 0;
    }

    /*
     * Function: utf8_probability Argument: byte array Returns : number from 0
     * to 100 representing probability text in array uses UTF-8 encoding of
     * Unicode
     */
    int utf8_probability(byte[] rawtext) {
        int score = 0;
        int i, rawtextlen = 0;
        int goodbytes = 0, asciibytes = 0;
        // Maybe also use UTF8 Byte Order Mark: EF BB BF
        // Check to see if characters fit into acceptable ranges
        rawtextlen = rawtext.length;
        for (i = 0; i < rawtextlen; i++) {
            if ((rawtext[i] & (byte) 0x7F) == rawtext[i]) { // One byte
                asciibytes++;
                // Ignore ASCII, can throw off count
            } else if (-64 <= rawtext[i] && rawtext[i] <= -33
                    && // Two bytes
                    i + 1 < rawtextlen && -128 <= rawtext[i + 1]
                    && rawtext[i + 1] <= -65) {
                goodbytes += 2;
                i++;
            } else if (-32 <= rawtext[i]
                    && rawtext[i] <= -17
                    && // Three bytes
                    i + 2 < rawtextlen && -128 <= rawtext[i + 1]
                    && rawtext[i + 1] <= -65 && -128 <= rawtext[i + 2]
                    && rawtext[i + 2] <= -65) {
                goodbytes += 3;
                i += 2;
            }
        }
        if (asciibytes == rawtextlen) {
            return 0;
        }
        score = (int) (100 * ((float) goodbytes / (float) (rawtextlen - asciibytes)));
        // System.out.println("rawtextlen " + rawtextlen + " goodbytes " +
        // goodbytes + " asciibytes " + asciibytes + " score " +
        // score);
        // If not above 98, reduce to zero to prevent coincidental matches
        // Allows for some (few) bad formed sequences
        if (score > 98) {
            return score;
        } else if (score > 95 && goodbytes > 30) {
            return score;
        } else {
            return 0;
        }
    }

    /*
     * Function: utf16_probability Argument: byte array Returns : number from 0
     * to 100 representing probability text in array uses UTF-16 encoding of
     * Unicode, guess based on BOM // NOT VERY GENERAL, NEEDS MUCH MORE WORK
     */
    int utf16_probability(byte[] rawtext) {
        if (rawtext.length > 1
                && ((byte) 0xFE == rawtext[0] && (byte) 0xFF == rawtext[1]) || // Big-endian
                ((byte) 0xFF == rawtext[0] && (byte) 0xFE == rawtext[1])) { // Little-endian
            return 100;
        }
        return 0;
    }

    /*
     * Function: ascii_probability Argument: byte array Returns : number from 0
     * to 100 representing probability text in array uses all ASCII Description:
     * Sees if array has any characters not in ASCII range, if so, score is
     * reduced
     */
    int ascii_probability(byte[] rawtext) {
        int score = 75;
        int i, rawtextlen;
        rawtextlen = rawtext.length;
        for (i = 0; i < rawtextlen; i++) {
            if (rawtext[i] < 0) {
                score = score - 5;
            } else if (rawtext[i] == (byte) 0x1B) { // ESC (used by ISO 2022)
                score = score - 5;
            }
            if (score <= 0) {
                return 0;
            }
        }
        return score;
    }

    /*
     * Function: euc_kr__probability Argument: pointer to byte array Returns :
     * number from 0 to 100 representing probability text in array uses EUC-KR
     * encoding
     */
    int euc_kr_probability(byte[] rawtext) {
        int i, rawtextlen = 0;
        int dbchars = 1, krchars = 1;
        long krfreq = 0, totalfreq = 1;
        float rangeval = 0, freqval = 0;
        int row, column;
        // Stage 1: Check to see if characters fit into acceptable ranges
        rawtextlen = rawtext.length;
        for (i = 0; i < rawtextlen - 1; i++) {
            // System.err.println(rawtext[i]);
            if (rawtext[i] < 0) {
                dbchars++;
                if ((byte) 0xA1 <= rawtext[i] && rawtext[i] <= (byte) 0xFE
                        && (byte) 0xA1 <= rawtext[i + 1]
                        && rawtext[i + 1] <= (byte) 0xFE) {
                    krchars++;
                    totalfreq += 500;
                    row = rawtext[i] + 256 - 0xA1;
                    column = rawtext[i + 1] + 256 - 0xA1;
                    if (KRFreq[row][column] != 0) {
                        krfreq += KRFreq[row][column];
                    } else if (15 <= row && row < 55) {
                        krfreq += 0;
                    }
                }
                i++;
            }
        }
        rangeval = 50 * ((float) krchars / (float) dbchars);
        freqval = 50 * ((float) krfreq / (float) totalfreq);
        return (int) (rangeval + freqval);
    }

    /*
     * Function: cp949__probability Argument: pointer to byte array Returns :
     * number from 0 to 100 representing probability text in array uses Cp949
     * encoding
     */
    int cp949_probability(byte[] rawtext) {
        int i, rawtextlen = 0;
        int dbchars = 1, krchars = 1;
        long krfreq = 0, totalfreq = 1;
        float rangeval = 0, freqval = 0;
        int row, column;
        // Stage 1: Check to see if characters fit into acceptable ranges
        rawtextlen = rawtext.length;
        for (i = 0; i < rawtextlen - 1; i++) {
            // System.err.println(rawtext[i]);
            if (rawtext[i] < 0) {
                dbchars++;
                if ((byte) 0x81 <= rawtext[i]
                        && rawtext[i] <= (byte) 0xFE
                        && ((byte) 0x41 <= rawtext[i + 1]
                        && rawtext[i + 1] <= (byte) 0x5A
                        || (byte) 0x61 <= rawtext[i + 1]
                        && rawtext[i + 1] <= (byte) 0x7A || (byte) 0x81 <= rawtext[i + 1]
                        && rawtext[i + 1] <= (byte) 0xFE)) {
                    krchars++;
                    totalfreq += 500;
                    if ((byte) 0xA1 <= rawtext[i] && rawtext[i] <= (byte) 0xFE
                            && (byte) 0xA1 <= rawtext[i + 1]
                            && rawtext[i + 1] <= (byte) 0xFE) {
                        row = rawtext[i] + 256 - 0xA1;
                        column = rawtext[i + 1] + 256 - 0xA1;
                        if (KRFreq[row][column] != 0) {
                            krfreq += KRFreq[row][column];
                        }
                    }
                }
                i++;
            }
        }
        rangeval = 50 * ((float) krchars / (float) dbchars);
        freqval = 50 * ((float) krfreq / (float) totalfreq);
        return (int) (rangeval + freqval);
    }

    int iso_2022_kr_probability(byte[] rawtext) {
        int i;
        for (i = 0; i < rawtext.length; i++) {
            if (i + 3 < rawtext.length && rawtext[i] == 0x1b
                    && (char) rawtext[i + 1] == '$'
                    && (char) rawtext[i + 2] == ')'
                    && (char) rawtext[i + 3] == 'C') {
                return 100;
            }
        }
        return 0;
    }

    /*
     * Function: euc_jp_probability Argument: pointer to byte array Returns :
     * number from 0 to 100 representing probability text in array uses EUC-JP
     * encoding
     */
    int euc_jp_probability(byte[] rawtext) {
        int i, rawtextlen = 0;
        int dbchars = 1, jpchars = 1;
        long jpfreq = 0, totalfreq = 1;
        float rangeval = 0, freqval = 0;
        int row, column;
        // Stage 1: Check to see if characters fit into acceptable ranges
        rawtextlen = rawtext.length;
        for (i = 0; i < rawtextlen - 1; i++) {
            // System.err.println(rawtext[i]);
            if (rawtext[i] < 0) {
                dbchars++;
                if ((byte) 0xA1 <= rawtext[i] && rawtext[i] <= (byte) 0xFE
                        && (byte) 0xA1 <= rawtext[i + 1]
                        && rawtext[i + 1] <= (byte) 0xFE) {
                    jpchars++;
                    totalfreq += 500;
                    row = rawtext[i] + 256 - 0xA1;
                    column = rawtext[i + 1] + 256 - 0xA1;
                    if (JPFreq[row][column] != 0) {
                        jpfreq += JPFreq[row][column];
                    } else if (15 <= row && row < 55) {
                        jpfreq += 0;
                    }
                }
                i++;
            }
        }
        rangeval = 50 * ((float) jpchars / (float) dbchars);
        freqval = 50 * ((float) jpfreq / (float) totalfreq);
        return (int) (rangeval + freqval);
    }

    int iso_2022_jp_probability(byte[] rawtext) {
        int i;
        for (i = 0; i < rawtext.length; i++) {
            if (i + 2 < rawtext.length && rawtext[i] == 0x1b
                    && (char) rawtext[i + 1] == '$'
                    && (char) rawtext[i + 2] == 'B') {
                return 100;
            }
        }
        return 0;
    }

    /*
     * Function: sjis_probability Argument: pointer to byte array Returns :
     * number from 0 to 100 representing probability text in array uses
     * Shift-JIS encoding
     */
    int sjis_probability(byte[] rawtext) {
        int i, rawtextlen = 0;
        int dbchars = 1, jpchars = 1;
        long jpfreq = 0, totalfreq = 1;
        float rangeval = 0, freqval = 0;
        int row, column, adjust;
        // Stage 1: Check to see if characters fit into acceptable ranges
        rawtextlen = rawtext.length;
        for (i = 0; i < rawtextlen - 1; i++) {
            // System.err.println(rawtext[i]);
            if (rawtext[i] < 0) {
                dbchars++;
                if (i + 1 < rawtext.length
                        && (((byte) 0x81 <= rawtext[i] && rawtext[i] <= (byte) 0x9F) || ((byte) 0xE0 <= rawtext[i] && rawtext[i] <= (byte) 0xEF))
                        && (((byte) 0x40 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0x7E) || ((byte) 0x80 <= rawtext[i + 1] && rawtext[i + 1] <= (byte) 0xFC))) {
                    jpchars++;
                    totalfreq += 500;
                    row = rawtext[i] + 256;
                    column = rawtext[i + 1] + 256;
                    if (column < 0x9f) {
                        adjust = 1;
                    } else {
                        adjust = 0;
                    }
                    if (row < 0xa0) {
                        row = ((row - 0x70) << 1) - adjust;
                    } else {
                        row = ((row - 0xb0) << 1) - adjust;
                    }
                    row -= 0x20;
                    column = 0x20;
                    // System.out.println("original row " + row + " column " +
                    // column);
                    if (row < JPFreq.length && column < JPFreq[row].length
                            && JPFreq[row][column] != 0) {
                        jpfreq += JPFreq[row][column];
                    }
                    i++;
                } else if ((byte) 0xA1 <= rawtext[i]
                        && rawtext[i] <= (byte) 0xDF) {
                    // half-width katakana, convert to full-width
                }
            }
        }
        rangeval = 50 * ((float) jpchars / (float) dbchars);
        freqval = 50 * ((float) jpfreq / (float) totalfreq);
        // For regular GB files, this would give the same score, so I handicap
        // it slightly
        return (int) (rangeval + freqval) - 1;
    }

    void initialize_frequencies() {
        int i, j;
        for (i = 93; i >= 0; i--) {
            for (j = 93; j >= 0; j--) {
                GBFreq[i][j] = 0;
            }
        }
        for (i = 125; i >= 0; i--) {
            for (j = 190; j >= 0; j--) {
                GBKFreq[i][j] = 0;
            }
        }
        for (i = 93; i >= 0; i--) {
            for (j = 157; j >= 0; j--) {
                Big5Freq[i][j] = 0;
            }
        }
        for (i = 125; i >= 0; i--) {
            for (j = 190; j >= 0; j--) {
                Big5PFreq[i][j] = 0;
            }
        }
        for (i = 93; i >= 0; i--) {
            for (j = 93; j >= 0; j--) {
                EUC_TWFreq[i][j] = 0;
            }
        }
        for (i = 93; i >= 0; i--) {
            for (j = 93; j >= 0; j--) {
                JPFreq[i][j] = 0;
            }
        }
        GBFreq[20][35] = 599;
        GBFreq[49][26] = 598;
        GBFreq[41][38] = 597;
        GBFreq[17][26] = 596;
        GBFreq[32][42] = 595;
        GBFreq[39][42] = 594;
        GBFreq[45][49] = 593;
        GBFreq[51][57] = 592;
        GBFreq[50][47] = 591;
        GBFreq[42][90] = 590;
        GBFreq[52][65] = 589;
        GBFreq[53][47] = 588;
        GBFreq[19][82] = 587;
        GBFreq[31][19] = 586;
        GBFreq[40][46] = 585;
        GBFreq[24][89] = 584;
        GBFreq[23][85] = 583;
        GBFreq[20][28] = 582;
        GBFreq[42][20] = 581;
        GBFreq[34][38] = 580;
        GBFreq[45][9] = 579;
        GBFreq[54][50] = 578;
        GBFreq[25][44] = 577;
        GBFreq[35][66] = 576;
        GBFreq[20][55] = 575;
        GBFreq[18][85] = 574;
        GBFreq[20][31] = 573;
        GBFreq[49][17] = 572;
        GBFreq[41][16] = 571;
        GBFreq[35][73] = 570;
        GBFreq[20][34] = 569;
        GBFreq[29][44] = 568;
        GBFreq[35][38] = 567;
        GBFreq[49][9] = 566;
        GBFreq[46][33] = 565;
        GBFreq[49][51] = 564;
        GBFreq[40][89] = 563;
        GBFreq[26][64] = 562;
        GBFreq[54][51] = 561;
        GBFreq[54][36] = 560;
        GBFreq[39][4] = 559;
        GBFreq[53][13] = 558;
        GBFreq[24][92] = 557;
        GBFreq[27][49] = 556;
        GBFreq[48][6] = 555;
        GBFreq[21][51] = 554;
        GBFreq[30][40] = 553;
        GBFreq[42][92] = 552;
        GBFreq[31][78] = 551;
        GBFreq[25][82] = 550;
        GBFreq[47][0] = 549;
        GBFreq[34][19] = 548;
        GBFreq[47][35] = 547;
        GBFreq[21][63] = 546;
        GBFreq[43][75] = 545;
        GBFreq[21][87] = 544;
        GBFreq[35][59] = 543;
        GBFreq[25][34] = 542;
        GBFreq[21][27] = 541;
        GBFreq[39][26] = 540;
        GBFreq[34][26] = 539;
        GBFreq[39][52] = 538;
        GBFreq[50][57] = 537;
        GBFreq[37][79] = 536;
        GBFreq[26][24] = 535;
        GBFreq[22][1] = 534;
        GBFreq[18][40] = 533;
        GBFreq[41][33] = 532;
        GBFreq[53][26] = 531;
        GBFreq[54][86] = 530;
        GBFreq[20][16] = 529;
        GBFreq[46][74] = 528;
        GBFreq[30][19] = 527;
        GBFreq[45][35] = 526;
        GBFreq[45][61] = 525;
        GBFreq[30][9] = 524;
        GBFreq[41][53] = 523;
        GBFreq[41][13] = 522;
        GBFreq[50][34] = 521;
        GBFreq[53][86] = 520;
        GBFreq[47][47] = 519;
        GBFreq[22][28] = 518;
        GBFreq[50][53] = 517;
        GBFreq[39][70] = 516;
        GBFreq[38][15] = 515;
        GBFreq[42][88] = 514;
        GBFreq[16][29] = 513;
        GBFreq[27][90] = 512;
        GBFreq[29][12] = 511;
        GBFreq[44][22] = 510;
        GBFreq[34][69] = 509;
        GBFreq[24][10] = 508;
        GBFreq[44][11] = 507;
        GBFreq[39][92] = 506;
        GBFreq[49][48] = 505;
        GBFreq[31][46] = 504;
        GBFreq[19][50] = 503;
        GBFreq[21][14] = 502;
        GBFreq[32][28] = 501;
        GBFreq[18][3] = 500;
        GBFreq[53][9] = 499;
        GBFreq[34][80] = 498;
        GBFreq[48][88] = 497;
        GBFreq[46][53] = 496;
        GBFreq[22][53] = 495;
        GBFreq[28][10] = 494;
        GBFreq[44][65] = 493;
        GBFreq[20][10] = 492;
        GBFreq[40][76] = 491;
        GBFreq[47][8] = 490;
        GBFreq[50][74] = 489;
        GBFreq[23][62] = 488;
        GBFreq[49][65] = 487;
        GBFreq[28][87] = 486;
        GBFreq[15][48] = 485;
        GBFreq[22][7] = 484;
        GBFreq[19][42] = 483;
        GBFreq[41][20] = 482;
        GBFreq[26][55] = 481;
        GBFreq[21][93] = 480;
        GBFreq[31][76] = 479;
        GBFreq[34][31] = 478;
        GBFreq[20][66] = 477;
        GBFreq[51][33] = 476;
        GBFreq[34][86] = 475;
        GBFreq[37][67] = 474;
        GBFreq[53][53] = 473;
        GBFreq[40][88] = 472;
        GBFreq[39][10] = 471;
        GBFreq[24][3] = 470;
        GBFreq[27][25] = 469;
        GBFreq[26][15] = 468;
        GBFreq[21][88] = 467;
        GBFreq[52][62] = 466;
        GBFreq[46][81] = 465;
        GBFreq[38][72] = 464;
        GBFreq[17][30] = 463;
        GBFreq[52][92] = 462;
        GBFreq[34][90] = 461;
        GBFreq[21][7] = 460;
        GBFreq[36][13] = 459;
        GBFreq[45][41] = 458;
        GBFreq[32][5] = 457;
        GBFreq[26][89] = 456;
        GBFreq[23][87] = 455;
        GBFreq[20][39] = 454;
        GBFreq[27][23] = 453;
        GBFreq[25][59] = 452;
        GBFreq[49][20] = 451;
        GBFreq[54][77] = 450;
        GBFreq[27][67] = 449;
        GBFreq[47][33] = 448;
        GBFreq[41][17] = 447;
        GBFreq[19][81] = 446;
        GBFreq[16][66] = 445;
        GBFreq[45][26] = 444;
        GBFreq[49][81] = 443;
        GBFreq[53][55] = 442;
        GBFreq[16][26] = 441;
        GBFreq[54][62] = 440;
        GBFreq[20][70] = 439;
        GBFreq[42][35] = 438;
        GBFreq[20][57] = 437;
        GBFreq[34][36] = 436;
        GBFreq[46][63] = 435;
        GBFreq[19][45] = 434;
        GBFreq[21][10] = 433;
        GBFreq[52][93] = 432;
        GBFreq[25][2] = 431;
        GBFreq[30][57] = 430;
        GBFreq[41][24] = 429;
        GBFreq[28][43] = 428;
        GBFreq[45][86] = 427;
        GBFreq[51][56] = 426;
        GBFreq[37][28] = 425;
        GBFreq[52][69] = 424;
        GBFreq[43][92] = 423;
        GBFreq[41][31] = 422;
        GBFreq[37][87] = 421;
        GBFreq[47][36] = 420;
        GBFreq[16][16] = 419;
        GBFreq[40][56] = 418;
        GBFreq[24][55] = 417;
        GBFreq[17][1] = 416;
        GBFreq[35][57] = 415;
        GBFreq[27][50] = 414;
        GBFreq[26][14] = 413;
        GBFreq[50][40] = 412;
        GBFreq[39][19] = 411;
        GBFreq[19][89] = 410;
        GBFreq[29][91] = 409;
        GBFreq[17][89] = 408;
        GBFreq[39][74] = 407;
        GBFreq[46][39] = 406;
        GBFreq[40][28] = 405;
        GBFreq[45][68] = 404;
        GBFreq[43][10] = 403;
        GBFreq[42][13] = 402;
        GBFreq[44][81] = 401;
        GBFreq[41][47] = 400;
        GBFreq[48][58] = 399;
        GBFreq[43][68] = 398;
        GBFreq[16][79] = 397;
        GBFreq[19][5] = 396;
        GBFreq[54][59] = 395;
        GBFreq[17][36] = 394;
        GBFreq[18][0] = 393;
        GBFreq[41][5] = 392;
        GBFreq[41][72] = 391;
        GBFreq[16][39] = 390;
        GBFreq[54][0] = 389;
        GBFreq[51][16] = 388;
        GBFreq[29][36] = 387;
        GBFreq[47][5] = 386;
        GBFreq[47][51] = 385;
        GBFreq[44][7] = 384;
        GBFreq[35][30] = 383;
        GBFreq[26][9] = 382;
        GBFreq[16][7] = 381;
        GBFreq[32][1] = 380;
        GBFreq[33][76] = 379;
        GBFreq[34][91] = 378;
        GBFreq[52][36] = 377;
        GBFreq[26][77] = 376;
        GBFreq[35][48] = 375;
        GBFreq[40][80] = 374;
        GBFreq[41][92] = 373;
        GBFreq[27][93] = 372;
        GBFreq[15][17] = 371;
        GBFreq[16][76] = 370;
        GBFreq[51][12] = 369;
        GBFreq[18][20] = 368;
        GBFreq[15][54] = 367;
        GBFreq[50][5] = 366;
        GBFreq[33][22] = 365;
        GBFreq[37][57] = 364;
        GBFreq[28][47] = 363;
        GBFreq[42][31] = 362;
        GBFreq[18][2] = 361;
        GBFreq[43][64] = 360;
        GBFreq[23][47] = 359;
        GBFreq[28][79] = 358;
        GBFreq[25][45] = 357;
        GBFreq[23][91] = 356;
        GBFreq[22][19] = 355;
        GBFreq[25][46] = 354;
        GBFreq[22][36] = 353;
        GBFreq[54][85] = 352;
        GBFreq[46][20] = 351;
        GBFreq[27][37] = 350;
        GBFreq[26][81] = 349;
        GBFreq[42][29] = 348;
        GBFreq[31][90] = 347;
        GBFreq[41][59] = 346;
        GBFreq[24][65] = 345;
        GBFreq[44][84] = 344;
        GBFreq[24][90] = 343;
        GBFreq[38][54] = 342;
        GBFreq[28][70] = 341;
        GBFreq[27][15] = 340;
        GBFreq[28][80] = 339;
        GBFreq[29][8] = 338;
        GBFreq[45][80] = 337;
        GBFreq[53][37] = 336;
        GBFreq[28][65] = 335;
        GBFreq[23][86] = 334;
        GBFreq[39][45] = 333;
        GBFreq[53][32] = 332;
        GBFreq[38][68] = 331;
        GBFreq[45][78] = 330;
        GBFreq[43][7] = 329;
        GBFreq[46][82] = 328;
        GBFreq[27][38] = 327;
        GBFreq[16][62] = 326;
        GBFreq[24][17] = 325;
        GBFreq[22][70] = 324;
        GBFreq[52][28] = 323;
        GBFreq[23][40] = 322;
        GBFreq[28][50] = 321;
        GBFreq[42][91] = 320;
        GBFreq[47][76] = 319;
        GBFreq[15][42] = 318;
        GBFreq[43][55] = 317;
        GBFreq[29][84] = 316;
        GBFreq[44][90] = 315;
        GBFreq[53][16] = 314;
        GBFreq[22][93] = 313;
        GBFreq[34][10] = 312;
        GBFreq[32][53] = 311;
        GBFreq[43][65] = 310;
        GBFreq[28][7] = 309;
        GBFreq[35][46] = 308;
        GBFreq[21][39] = 307;
        GBFreq[44][18] = 306;
        GBFreq[40][10] = 305;
        GBFreq[54][53] = 304;
        GBFreq[38][74] = 303;
        GBFreq[28][26] = 302;
        GBFreq[15][13] = 301;
        GBFreq[39][34] = 300;
        GBFreq[39][46] = 299;
        GBFreq[42][66] = 298;
        GBFreq[33][58] = 297;
        GBFreq[15][56] = 296;
        GBFreq[18][51] = 295;
        GBFreq[49][68] = 294;
        GBFreq[30][37] = 293;
        GBFreq[51][84] = 292;
        GBFreq[51][9] = 291;
        GBFreq[40][70] = 290;
        GBFreq[41][84] = 289;
        GBFreq[28][64] = 288;
        GBFreq[32][88] = 287;
        GBFreq[24][5] = 286;
        GBFreq[53][23] = 285;
        GBFreq[42][27] = 284;
        GBFreq[22][38] = 283;
        GBFreq[32][86] = 282;
        GBFreq[34][30] = 281;
        GBFreq[38][63] = 280;
        GBFreq[24][59] = 279;
        GBFreq[22][81] = 278;
        GBFreq[32][11] = 277;
        GBFreq[51][21] = 276;
        GBFreq[54][41] = 275;
        GBFreq[21][50] = 274;
        GBFreq[23][89] = 273;
        GBFreq[19][87] = 272;
        GBFreq[26][7] = 271;
        GBFreq[30][75] = 270;
        GBFreq[43][84] = 269;
        GBFreq[51][25] = 268;
        GBFreq[16][67] = 267;
        GBFreq[32][9] = 266;
        GBFreq[48][51] = 265;
        GBFreq[39][7] = 264;
        GBFreq[44][88] = 263;
        GBFreq[52][24] = 262;
        GBFreq[23][34] = 261;
        GBFreq[32][75] = 260;
        GBFreq[19][10] = 259;
        GBFreq[28][91] = 258;
        GBFreq[32][83] = 257;
        GBFreq[25][75] = 256;
        GBFreq[53][45] = 255;
        GBFreq[29][85] = 254;
        GBFreq[53][59] = 253;
        GBFreq[16][2] = 252;
        GBFreq[19][78] = 251;
        GBFreq[15][75] = 250;
        GBFreq[51][42] = 249;
        GBFreq[45][67] = 248;
        GBFreq[15][74] = 247;
        GBFreq[25][81] = 246;
        GBFreq[37][62] = 245;
        GBFreq[16][55] = 244;
        GBFreq[18][38] = 243;
        GBFreq[23][23] = 242;
        GBFreq[38][30] = 241;
        GBFreq[17][28] = 240;
        GBFreq[44][73] = 239;
        GBFreq[23][78] = 238;
        GBFreq[40][77] = 237;
        GBFreq[38][87] = 236;
        GBFreq[27][19] = 235;
        GBFreq[38][82] = 234;
        GBFreq[37][22] = 233;
        GBFreq[41][30] = 232;
        GBFreq[54][9] = 231;
        GBFreq[32][30] = 230;
        GBFreq[30][52] = 229;
        GBFreq[40][84] = 228;
        GBFreq[53][57] = 227;
        GBFreq[27][27] = 226;
        GBFreq[38][64] = 225;
        GBFreq[18][43] = 224;
        GBFreq[23][69] = 223;
        GBFreq[28][12] = 222;
        GBFreq[50][78] = 221;
        GBFreq[50][1] = 220;
        GBFreq[26][88] = 219;
        GBFreq[36][40] = 218;
        GBFreq[33][89] = 217;
        GBFreq[41][28] = 216;
        GBFreq[31][77] = 215;
        GBFreq[46][1] = 214;
        GBFreq[47][19] = 213;
        GBFreq[35][55] = 212;
        GBFreq[41][21] = 211;
        GBFreq[27][10] = 210;
        GBFreq[32][77] = 209;
        GBFreq[26][37] = 208;
        GBFreq[20][33] = 207;
        GBFreq[41][52] = 206;
        GBFreq[32][18] = 205;
        GBFreq[38][13] = 204;
        GBFreq[20][18] = 203;
        GBFreq[20][24] = 202;
        GBFreq[45][19] = 201;
        GBFreq[18][53] = 200;

        Big5Freq[9][89] = 600;
        Big5Freq[11][15] = 599;
        Big5Freq[3][66] = 598;
        Big5Freq[6][121] = 597;
        Big5Freq[3][0] = 596;
        Big5Freq[5][82] = 595;
        Big5Freq[3][42] = 594;
        Big5Freq[5][34] = 593;
        Big5Freq[3][8] = 592;
        Big5Freq[3][6] = 591;
        Big5Freq[3][67] = 590;
        Big5Freq[7][139] = 589;
        Big5Freq[23][137] = 588;
        Big5Freq[12][46] = 587;
        Big5Freq[4][8] = 586;
        Big5Freq[4][41] = 585;
        Big5Freq[18][47] = 584;
        Big5Freq[12][114] = 583;
        Big5Freq[6][1] = 582;
        Big5Freq[22][60] = 581;
        Big5Freq[5][46] = 580;
        Big5Freq[11][79] = 579;
        Big5Freq[3][23] = 578;
        Big5Freq[7][114] = 577;
        Big5Freq[29][102] = 576;
        Big5Freq[19][14] = 575;
        Big5Freq[4][133] = 574;
        Big5Freq[3][29] = 573;
        Big5Freq[4][109] = 572;
        Big5Freq[14][127] = 571;
        Big5Freq[5][48] = 570;
        Big5Freq[13][104] = 569;
        Big5Freq[3][132] = 568;
        Big5Freq[26][64] = 567;
        Big5Freq[7][19] = 566;
        Big5Freq[4][12] = 565;
        Big5Freq[11][124] = 564;
        Big5Freq[7][89] = 563;
        Big5Freq[15][124] = 562;
        Big5Freq[4][108] = 561;
        Big5Freq[19][66] = 560;
        Big5Freq[3][21] = 559;
        Big5Freq[24][12] = 558;
        Big5Freq[28][111] = 557;
        Big5Freq[12][107] = 556;
        Big5Freq[3][112] = 555;
        Big5Freq[8][113] = 554;
        Big5Freq[5][40] = 553;
        Big5Freq[26][145] = 552;
        Big5Freq[3][48] = 551;
        Big5Freq[3][70] = 550;
        Big5Freq[22][17] = 549;
        Big5Freq[16][47] = 548;
        Big5Freq[3][53] = 547;
        Big5Freq[4][24] = 546;
        Big5Freq[32][120] = 545;
        Big5Freq[24][49] = 544;
        Big5Freq[24][142] = 543;
        Big5Freq[18][66] = 542;
        Big5Freq[29][150] = 541;
        Big5Freq[5][122] = 540;
        Big5Freq[5][114] = 539;
        Big5Freq[3][44] = 538;
        Big5Freq[10][128] = 537;
        Big5Freq[15][20] = 536;
        Big5Freq[13][33] = 535;
        Big5Freq[14][87] = 534;
        Big5Freq[3][126] = 533;
        Big5Freq[4][53] = 532;
        Big5Freq[4][40] = 531;
        Big5Freq[9][93] = 530;
        Big5Freq[15][137] = 529;
        Big5Freq[10][123] = 528;
        Big5Freq[4][56] = 527;
        Big5Freq[5][71] = 526;
        Big5Freq[10][8] = 525;
        Big5Freq[5][16] = 524;
        Big5Freq[5][146] = 523;
        Big5Freq[18][88] = 522;
        Big5Freq[24][4] = 521;
        Big5Freq[20][47] = 520;
        Big5Freq[5][33] = 519;
        Big5Freq[9][43] = 518;
        Big5Freq[20][12] = 517;
        Big5Freq[20][13] = 516;
        Big5Freq[5][156] = 515;
        Big5Freq[22][140] = 514;
        Big5Freq[8][146] = 513;
        Big5Freq[21][123] = 512;
        Big5Freq[4][90] = 511;
        Big5Freq[5][62] = 510;
        Big5Freq[17][59] = 509;
        Big5Freq[10][37] = 508;
        Big5Freq[18][107] = 507;
        Big5Freq[14][53] = 506;
        Big5Freq[22][51] = 505;
        Big5Freq[8][13] = 504;
        Big5Freq[5][29] = 503;
        Big5Freq[9][7] = 502;
        Big5Freq[22][14] = 501;
        Big5Freq[8][55] = 500;
        Big5Freq[33][9] = 499;
        Big5Freq[16][64] = 498;
        Big5Freq[7][131] = 497;
        Big5Freq[34][4] = 496;
        Big5Freq[7][101] = 495;
        Big5Freq[11][139] = 494;
        Big5Freq[3][135] = 493;
        Big5Freq[7][102] = 492;
        Big5Freq[17][13] = 491;
        Big5Freq[3][20] = 490;
        Big5Freq[27][106] = 489;
        Big5Freq[5][88] = 488;
        Big5Freq[6][33] = 487;
        Big5Freq[5][139] = 486;
        Big5Freq[6][0] = 485;
        Big5Freq[17][58] = 484;
        Big5Freq[5][133] = 483;
        Big5Freq[9][107] = 482;
        Big5Freq[23][39] = 481;
        Big5Freq[5][23] = 480;
        Big5Freq[3][79] = 479;
        Big5Freq[32][97] = 478;
        Big5Freq[3][136] = 477;
        Big5Freq[4][94] = 476;
        Big5Freq[21][61] = 475;
        Big5Freq[23][123] = 474;
        Big5Freq[26][16] = 473;
        Big5Freq[24][137] = 472;
        Big5Freq[22][18] = 471;
        Big5Freq[5][1] = 470;
        Big5Freq[20][119] = 469;
        Big5Freq[3][7] = 468;
        Big5Freq[10][79] = 467;
        Big5Freq[15][105] = 466;
        Big5Freq[3][144] = 465;
        Big5Freq[12][80] = 464;
        Big5Freq[15][73] = 463;
        Big5Freq[3][19] = 462;
        Big5Freq[8][109] = 461;
        Big5Freq[3][15] = 460;
        Big5Freq[31][82] = 459;
        Big5Freq[3][43] = 458;
        Big5Freq[25][119] = 457;
        Big5Freq[16][111] = 456;
        Big5Freq[7][77] = 455;
        Big5Freq[3][95] = 454;
        Big5Freq[24][82] = 453;
        Big5Freq[7][52] = 452;
        Big5Freq[9][151] = 451;
        Big5Freq[3][129] = 450;
        Big5Freq[5][87] = 449;
        Big5Freq[3][55] = 448;
        Big5Freq[8][153] = 447;
        Big5Freq[4][83] = 446;
        Big5Freq[3][114] = 445;
        Big5Freq[23][147] = 444;
        Big5Freq[15][31] = 443;
        Big5Freq[3][54] = 442;
        Big5Freq[11][122] = 441;
        Big5Freq[4][4] = 440;
        Big5Freq[34][149] = 439;
        Big5Freq[3][17] = 438;
        Big5Freq[21][64] = 437;
        Big5Freq[26][144] = 436;
        Big5Freq[4][62] = 435;
        Big5Freq[8][15] = 434;
        Big5Freq[35][80] = 433;
        Big5Freq[7][110] = 432;
        Big5Freq[23][114] = 431;
        Big5Freq[3][108] = 430;
        Big5Freq[3][62] = 429;
        Big5Freq[21][41] = 428;
        Big5Freq[15][99] = 427;
        Big5Freq[5][47] = 426;
        Big5Freq[4][96] = 425;
        Big5Freq[20][122] = 424;
        Big5Freq[5][21] = 423;
        Big5Freq[4][157] = 422;
        Big5Freq[16][14] = 421;
        Big5Freq[3][117] = 420;
        Big5Freq[7][129] = 419;
        Big5Freq[4][27] = 418;
        Big5Freq[5][30] = 417;
        Big5Freq[22][16] = 416;
        Big5Freq[5][64] = 415;
        Big5Freq[17][99] = 414;
        Big5Freq[17][57] = 413;
        Big5Freq[8][105] = 412;
        Big5Freq[5][112] = 411;
        Big5Freq[20][59] = 410;
        Big5Freq[6][129] = 409;
        Big5Freq[18][17] = 408;
        Big5Freq[3][92] = 407;
        Big5Freq[28][118] = 406;
        Big5Freq[3][109] = 405;
        Big5Freq[31][51] = 404;
        Big5Freq[13][116] = 403;
        Big5Freq[6][15] = 402;
        Big5Freq[36][136] = 401;
        Big5Freq[12][74] = 400;
        Big5Freq[20][88] = 399;
        Big5Freq[36][68] = 398;
        Big5Freq[3][147] = 397;
        Big5Freq[15][84] = 396;
        Big5Freq[16][32] = 395;
        Big5Freq[16][58] = 394;
        Big5Freq[7][66] = 393;
        Big5Freq[23][107] = 392;
        Big5Freq[9][6] = 391;
        Big5Freq[12][86] = 390;
        Big5Freq[23][112] = 389;
        Big5Freq[37][23] = 388;
        Big5Freq[3][138] = 387;
        Big5Freq[20][68] = 386;
        Big5Freq[15][116] = 385;
        Big5Freq[18][64] = 384;
        Big5Freq[12][139] = 383;
        Big5Freq[11][155] = 382;
        Big5Freq[4][156] = 381;
        Big5Freq[12][84] = 380;
        Big5Freq[18][49] = 379;
        Big5Freq[25][125] = 378;
        Big5Freq[25][147] = 377;
        Big5Freq[15][110] = 376;
        Big5Freq[19][96] = 375;
        Big5Freq[30][152] = 374;
        Big5Freq[6][31] = 373;
        Big5Freq[27][117] = 372;
        Big5Freq[3][10] = 371;
        Big5Freq[6][131] = 370;
        Big5Freq[13][112] = 369;
        Big5Freq[36][156] = 368;
        Big5Freq[4][60] = 367;
        Big5Freq[15][121] = 366;
        Big5Freq[4][112] = 365;
        Big5Freq[30][142] = 364;
        Big5Freq[23][154] = 363;
        Big5Freq[27][101] = 362;
        Big5Freq[9][140] = 361;
        Big5Freq[3][89] = 360;
        Big5Freq[18][148] = 359;
        Big5Freq[4][69] = 358;
        Big5Freq[16][49] = 357;
        Big5Freq[6][117] = 356;
        Big5Freq[36][55] = 355;
        Big5Freq[5][123] = 354;
        Big5Freq[4][126] = 353;
        Big5Freq[4][119] = 352;
        Big5Freq[9][95] = 351;
        Big5Freq[5][24] = 350;
        Big5Freq[16][133] = 349;
        Big5Freq[10][134] = 348;
        Big5Freq[26][59] = 347;
        Big5Freq[6][41] = 346;
        Big5Freq[6][146] = 345;
        Big5Freq[19][24] = 344;
        Big5Freq[5][113] = 343;
        Big5Freq[10][118] = 342;
        Big5Freq[34][151] = 341;
        Big5Freq[9][72] = 340;
        Big5Freq[31][25] = 339;
        Big5Freq[18][126] = 338;
        Big5Freq[18][28] = 337;
        Big5Freq[4][153] = 336;
        Big5Freq[3][84] = 335;
        Big5Freq[21][18] = 334;
        Big5Freq[25][129] = 333;
        Big5Freq[6][107] = 332;
        Big5Freq[12][25] = 331;
        Big5Freq[17][109] = 330;
        Big5Freq[7][76] = 329;
        Big5Freq[15][15] = 328;
        Big5Freq[4][14] = 327;
        Big5Freq[23][88] = 326;
        Big5Freq[18][2] = 325;
        Big5Freq[6][88] = 324;
        Big5Freq[16][84] = 323;
        Big5Freq[12][48] = 322;
        Big5Freq[7][68] = 321;
        Big5Freq[5][50] = 320;
        Big5Freq[13][54] = 319;
        Big5Freq[7][98] = 318;
        Big5Freq[11][6] = 317;
        Big5Freq[9][80] = 316;
        Big5Freq[16][41] = 315;
        Big5Freq[7][43] = 314;
        Big5Freq[28][117] = 313;
        Big5Freq[3][51] = 312;
        Big5Freq[7][3] = 311;
        Big5Freq[20][81] = 310;
        Big5Freq[4][2] = 309;
        Big5Freq[11][16] = 308;
        Big5Freq[10][4] = 307;
        Big5Freq[10][119] = 306;
        Big5Freq[6][142] = 305;
        Big5Freq[18][51] = 304;
        Big5Freq[8][144] = 303;
        Big5Freq[10][65] = 302;
        Big5Freq[11][64] = 301;
        Big5Freq[11][130] = 300;
        Big5Freq[9][92] = 299;
        Big5Freq[18][29] = 298;
        Big5Freq[18][78] = 297;
        Big5Freq[18][151] = 296;
        Big5Freq[33][127] = 295;
        Big5Freq[35][113] = 294;
        Big5Freq[10][155] = 293;
        Big5Freq[3][76] = 292;
        Big5Freq[36][123] = 291;
        Big5Freq[13][143] = 290;
        Big5Freq[5][135] = 289;
        Big5Freq[23][116] = 288;
        Big5Freq[6][101] = 287;
        Big5Freq[14][74] = 286;
        Big5Freq[7][153] = 285;
        Big5Freq[3][101] = 284;
        Big5Freq[9][74] = 283;
        Big5Freq[3][156] = 282;
        Big5Freq[4][147] = 281;
        Big5Freq[9][12] = 280;
        Big5Freq[18][133] = 279;
        Big5Freq[4][0] = 278;
        Big5Freq[7][155] = 277;
        Big5Freq[9][144] = 276;
        Big5Freq[23][49] = 275;
        Big5Freq[5][89] = 274;
        Big5Freq[10][11] = 273;
        Big5Freq[3][110] = 272;
        Big5Freq[3][40] = 271;
        Big5Freq[29][115] = 270;
        Big5Freq[9][100] = 269;
        Big5Freq[21][67] = 268;
        Big5Freq[23][145] = 267;
        Big5Freq[10][47] = 266;
        Big5Freq[4][31] = 265;
        Big5Freq[4][81] = 264;
        Big5Freq[22][62] = 263;
        Big5Freq[4][28] = 262;
        Big5Freq[27][39] = 261;
        Big5Freq[27][54] = 260;
        Big5Freq[32][46] = 259;
        Big5Freq[4][76] = 258;
        Big5Freq[26][15] = 257;
        Big5Freq[12][154] = 256;
        Big5Freq[9][150] = 255;
        Big5Freq[15][17] = 254;
        Big5Freq[5][129] = 253;
        Big5Freq[10][40] = 252;
        Big5Freq[13][37] = 251;
        Big5Freq[31][104] = 250;
        Big5Freq[3][152] = 249;
        Big5Freq[5][22] = 248;
        Big5Freq[8][48] = 247;
        Big5Freq[4][74] = 246;
        Big5Freq[6][17] = 245;
        Big5Freq[30][82] = 244;
        Big5Freq[4][116] = 243;
        Big5Freq[16][42] = 242;
        Big5Freq[5][55] = 241;
        Big5Freq[4][64] = 240;
        Big5Freq[14][19] = 239;
        Big5Freq[35][82] = 238;
        Big5Freq[30][139] = 237;
        Big5Freq[26][152] = 236;
        Big5Freq[32][32] = 235;
        Big5Freq[21][102] = 234;
        Big5Freq[10][131] = 233;
        Big5Freq[9][128] = 232;
        Big5Freq[3][87] = 231;
        Big5Freq[4][51] = 230;
        Big5Freq[10][15] = 229;
        Big5Freq[4][150] = 228;
        Big5Freq[7][4] = 227;
        Big5Freq[7][51] = 226;
        Big5Freq[7][157] = 225;
        Big5Freq[4][146] = 224;
        Big5Freq[4][91] = 223;
        Big5Freq[7][13] = 222;
        Big5Freq[17][116] = 221;
        Big5Freq[23][21] = 220;
        Big5Freq[5][106] = 219;
        Big5Freq[14][100] = 218;
        Big5Freq[10][152] = 217;
        Big5Freq[14][89] = 216;
        Big5Freq[6][138] = 215;
        Big5Freq[12][157] = 214;
        Big5Freq[10][102] = 213;
        Big5Freq[19][94] = 212;
        Big5Freq[7][74] = 211;
        Big5Freq[18][128] = 210;
        Big5Freq[27][111] = 209;
        Big5Freq[11][57] = 208;
        Big5Freq[3][131] = 207;
        Big5Freq[30][23] = 206;
        Big5Freq[30][126] = 205;
        Big5Freq[4][36] = 204;
        Big5Freq[26][124] = 203;
        Big5Freq[4][19] = 202;
        Big5Freq[9][152] = 201;

        Big5PFreq[41][122] = 600;
        Big5PFreq[35][0] = 599;
        Big5PFreq[43][15] = 598;
        Big5PFreq[35][99] = 597;
        Big5PFreq[35][6] = 596;
        Big5PFreq[35][8] = 595;
        Big5PFreq[38][154] = 594;
        Big5PFreq[37][34] = 593;
        Big5PFreq[37][115] = 592;
        Big5PFreq[36][12] = 591;
        Big5PFreq[18][77] = 590;
        Big5PFreq[35][100] = 589;
        Big5PFreq[35][42] = 588;
        Big5PFreq[120][75] = 587;
        Big5PFreq[35][23] = 586;
        Big5PFreq[13][72] = 585;
        Big5PFreq[0][67] = 584;
        Big5PFreq[39][172] = 583;
        Big5PFreq[22][182] = 582;
        Big5PFreq[15][186] = 581;
        Big5PFreq[15][165] = 580;
        Big5PFreq[35][44] = 579;
        Big5PFreq[40][13] = 578;
        Big5PFreq[38][1] = 577;
        Big5PFreq[37][33] = 576;
        Big5PFreq[36][24] = 575;
        Big5PFreq[56][4] = 574;
        Big5PFreq[35][29] = 573;
        Big5PFreq[9][96] = 572;
        Big5PFreq[37][62] = 571;
        Big5PFreq[48][47] = 570;
        Big5PFreq[51][14] = 569;
        Big5PFreq[39][122] = 568;
        Big5PFreq[44][46] = 567;
        Big5PFreq[35][21] = 566;
        Big5PFreq[36][8] = 565;
        Big5PFreq[36][141] = 564;
        Big5PFreq[3][81] = 563;
        Big5PFreq[37][155] = 562;
        Big5PFreq[42][84] = 561;
        Big5PFreq[36][40] = 560;
        Big5PFreq[35][103] = 559;
        Big5PFreq[11][84] = 558;
        Big5PFreq[45][33] = 557;
        Big5PFreq[121][79] = 556;
        Big5PFreq[2][77] = 555;
        Big5PFreq[36][41] = 554;
        Big5PFreq[37][47] = 553;
        Big5PFreq[39][125] = 552;
        Big5PFreq[37][26] = 551;
        Big5PFreq[35][48] = 550;
        Big5PFreq[35][28] = 549;
        Big5PFreq[35][159] = 548;
        Big5PFreq[37][40] = 547;
        Big5PFreq[35][145] = 546;
        Big5PFreq[37][147] = 545;
        Big5PFreq[46][160] = 544;
        Big5PFreq[37][46] = 543;
        Big5PFreq[50][99] = 542;
        Big5PFreq[52][13] = 541;
        Big5PFreq[10][82] = 540;
        Big5PFreq[35][169] = 539;
        Big5PFreq[35][31] = 538;
        Big5PFreq[47][31] = 537;
        Big5PFreq[18][79] = 536;
        Big5PFreq[16][113] = 535;
        Big5PFreq[37][104] = 534;
        Big5PFreq[39][134] = 533;
        Big5PFreq[36][53] = 532;
        Big5PFreq[38][0] = 531;
        Big5PFreq[4][86] = 530;
        Big5PFreq[54][17] = 529;
        Big5PFreq[43][157] = 528;
        Big5PFreq[35][165] = 527;
        Big5PFreq[69][147] = 526;
        Big5PFreq[117][95] = 525;
        Big5PFreq[35][162] = 524;
        Big5PFreq[35][17] = 523;
        Big5PFreq[36][142] = 522;
        Big5PFreq[36][4] = 521;
        Big5PFreq[37][166] = 520;
        Big5PFreq[35][168] = 519;
        Big5PFreq[35][19] = 518;
        Big5PFreq[37][48] = 517;
        Big5PFreq[42][37] = 516;
        Big5PFreq[40][146] = 515;
        Big5PFreq[36][123] = 514;
        Big5PFreq[22][41] = 513;
        Big5PFreq[20][119] = 512;
        Big5PFreq[2][74] = 511;
        Big5PFreq[44][113] = 510;
        Big5PFreq[35][125] = 509;
        Big5PFreq[37][16] = 508;
        Big5PFreq[35][20] = 507;
        Big5PFreq[35][55] = 506;
        Big5PFreq[37][145] = 505;
        Big5PFreq[0][88] = 504;
        Big5PFreq[3][94] = 503;
        Big5PFreq[6][65] = 502;
        Big5PFreq[26][15] = 501;
        Big5PFreq[41][126] = 500;
        Big5PFreq[36][129] = 499;
        Big5PFreq[31][75] = 498;
        Big5PFreq[19][61] = 497;
        Big5PFreq[35][128] = 496;
        Big5PFreq[29][79] = 495;
        Big5PFreq[36][62] = 494;
        Big5PFreq[37][189] = 493;
        Big5PFreq[39][109] = 492;
        Big5PFreq[39][135] = 491;
        Big5PFreq[72][15] = 490;
        Big5PFreq[47][106] = 489;
        Big5PFreq[54][14] = 488;
        Big5PFreq[24][52] = 487;
        Big5PFreq[38][162] = 486;
        Big5PFreq[41][43] = 485;
        Big5PFreq[37][121] = 484;
        Big5PFreq[14][66] = 483;
        Big5PFreq[37][30] = 482;
        Big5PFreq[35][7] = 481;
        Big5PFreq[49][58] = 480;
        Big5PFreq[43][188] = 479;
        Big5PFreq[24][66] = 478;
        Big5PFreq[35][171] = 477;
        Big5PFreq[40][186] = 476;
        Big5PFreq[39][164] = 475;
        Big5PFreq[78][186] = 474;
        Big5PFreq[8][72] = 473;
        Big5PFreq[36][190] = 472;
        Big5PFreq[35][53] = 471;
        Big5PFreq[35][54] = 470;
        Big5PFreq[22][159] = 469;
        Big5PFreq[35][9] = 468;
        Big5PFreq[41][140] = 467;
        Big5PFreq[37][22] = 466;
        Big5PFreq[48][97] = 465;
        Big5PFreq[50][97] = 464;
        Big5PFreq[36][127] = 463;
        Big5PFreq[37][23] = 462;
        Big5PFreq[40][55] = 461;
        Big5PFreq[35][43] = 460;
        Big5PFreq[26][22] = 459;
        Big5PFreq[35][15] = 458;
        Big5PFreq[72][179] = 457;
        Big5PFreq[20][129] = 456;
        Big5PFreq[52][101] = 455;
        Big5PFreq[35][12] = 454;
        Big5PFreq[42][156] = 453;
        Big5PFreq[15][157] = 452;
        Big5PFreq[50][140] = 451;
        Big5PFreq[26][28] = 450;
        Big5PFreq[54][51] = 449;
        Big5PFreq[35][112] = 448;
        Big5PFreq[36][116] = 447;
        Big5PFreq[42][11] = 446;
        Big5PFreq[37][172] = 445;
        Big5PFreq[37][29] = 444;
        Big5PFreq[44][107] = 443;
        Big5PFreq[50][17] = 442;
        Big5PFreq[39][107] = 441;
        Big5PFreq[19][109] = 440;
        Big5PFreq[36][60] = 439;
        Big5PFreq[49][132] = 438;
        Big5PFreq[26][16] = 437;
        Big5PFreq[43][155] = 436;
        Big5PFreq[37][120] = 435;
        Big5PFreq[15][159] = 434;
        Big5PFreq[43][6] = 433;
        Big5PFreq[45][188] = 432;
        Big5PFreq[35][38] = 431;
        Big5PFreq[39][143] = 430;
        Big5PFreq[48][144] = 429;
        Big5PFreq[37][168] = 428;
        Big5PFreq[37][1] = 427;
        Big5PFreq[36][109] = 426;
        Big5PFreq[46][53] = 425;
        Big5PFreq[38][54] = 424;
        Big5PFreq[36][0] = 423;
        Big5PFreq[72][33] = 422;
        Big5PFreq[42][8] = 421;
        Big5PFreq[36][31] = 420;
        Big5PFreq[35][150] = 419;
        Big5PFreq[118][93] = 418;
        Big5PFreq[37][61] = 417;
        Big5PFreq[0][85] = 416;
        Big5PFreq[36][27] = 415;
        Big5PFreq[35][134] = 414;
        Big5PFreq[36][145] = 413;
        Big5PFreq[6][96] = 412;
        Big5PFreq[36][14] = 411;
        Big5PFreq[16][36] = 410;
        Big5PFreq[15][175] = 409;
        Big5PFreq[35][10] = 408;
        Big5PFreq[36][189] = 407;
        Big5PFreq[35][51] = 406;
        Big5PFreq[35][109] = 405;
        Big5PFreq[35][147] = 404;
        Big5PFreq[35][180] = 403;
        Big5PFreq[72][5] = 402;
        Big5PFreq[36][107] = 401;
        Big5PFreq[49][116] = 400;
        Big5PFreq[73][30] = 399;
        Big5PFreq[6][90] = 398;
        Big5PFreq[2][70] = 397;
        Big5PFreq[17][141] = 396;
        Big5PFreq[35][62] = 395;
        Big5PFreq[16][180] = 394;
        Big5PFreq[4][91] = 393;
        Big5PFreq[15][171] = 392;
        Big5PFreq[35][177] = 391;
        Big5PFreq[37][173] = 390;
        Big5PFreq[16][121] = 389;
        Big5PFreq[35][5] = 388;
        Big5PFreq[46][122] = 387;
        Big5PFreq[40][138] = 386;
        Big5PFreq[50][49] = 385;
        Big5PFreq[36][152] = 384;
        Big5PFreq[13][43] = 383;
        Big5PFreq[9][88] = 382;
        Big5PFreq[36][159] = 381;
        Big5PFreq[27][62] = 380;
        Big5PFreq[40][18] = 379;
        Big5PFreq[17][129] = 378;
        Big5PFreq[43][97] = 377;
        Big5PFreq[13][131] = 376;
        Big5PFreq[46][107] = 375;
        Big5PFreq[60][64] = 374;
        Big5PFreq[36][179] = 373;
        Big5PFreq[37][55] = 372;
        Big5PFreq[41][173] = 371;
        Big5PFreq[44][172] = 370;
        Big5PFreq[23][187] = 369;
        Big5PFreq[36][149] = 368;
        Big5PFreq[17][125] = 367;
        Big5PFreq[55][180] = 366;
        Big5PFreq[51][129] = 365;
        Big5PFreq[36][51] = 364;
        Big5PFreq[37][122] = 363;
        Big5PFreq[48][32] = 362;
        Big5PFreq[51][99] = 361;
        Big5PFreq[54][16] = 360;
        Big5PFreq[41][183] = 359;
        Big5PFreq[37][179] = 358;
        Big5PFreq[38][179] = 357;
        Big5PFreq[35][143] = 356;
        Big5PFreq[37][24] = 355;
        Big5PFreq[40][177] = 354;
        Big5PFreq[47][117] = 353;
        Big5PFreq[39][52] = 352;
        Big5PFreq[22][99] = 351;
        Big5PFreq[40][142] = 350;
        Big5PFreq[36][49] = 349;
        Big5PFreq[38][17] = 348;
        Big5PFreq[39][188] = 347;
        Big5PFreq[36][186] = 346;
        Big5PFreq[35][189] = 345;
        Big5PFreq[41][7] = 344;
        Big5PFreq[18][91] = 343;
        Big5PFreq[43][137] = 342;
        Big5PFreq[35][142] = 341;
        Big5PFreq[35][117] = 340;
        Big5PFreq[39][138] = 339;
        Big5PFreq[16][59] = 338;
        Big5PFreq[39][174] = 337;
        Big5PFreq[55][145] = 336;
        Big5PFreq[37][21] = 335;
        Big5PFreq[36][180] = 334;
        Big5PFreq[37][156] = 333;
        Big5PFreq[49][13] = 332;
        Big5PFreq[41][107] = 331;
        Big5PFreq[36][56] = 330;
        Big5PFreq[53][8] = 329;
        Big5PFreq[22][114] = 328;
        Big5PFreq[5][95] = 327;
        Big5PFreq[37][0] = 326;
        Big5PFreq[26][183] = 325;
        Big5PFreq[22][66] = 324;
        Big5PFreq[35][58] = 323;
        Big5PFreq[48][117] = 322;
        Big5PFreq[36][102] = 321;
        Big5PFreq[22][122] = 320;
        Big5PFreq[35][11] = 319;
        Big5PFreq[46][19] = 318;
        Big5PFreq[22][49] = 317;
        Big5PFreq[48][166] = 316;
        Big5PFreq[41][125] = 315;
        Big5PFreq[41][1] = 314;
        Big5PFreq[35][178] = 313;
        Big5PFreq[41][12] = 312;
        Big5PFreq[26][167] = 311;
        Big5PFreq[42][152] = 310;
        Big5PFreq[42][46] = 309;
        Big5PFreq[42][151] = 308;
        Big5PFreq[20][135] = 307;
        Big5PFreq[37][162] = 306;
        Big5PFreq[37][50] = 305;
        Big5PFreq[22][185] = 304;
        Big5PFreq[36][166] = 303;
        Big5PFreq[19][40] = 302;
        Big5PFreq[22][107] = 301;
        Big5PFreq[22][102] = 300;
        Big5PFreq[57][162] = 299;
        Big5PFreq[22][124] = 298;
        Big5PFreq[37][138] = 297;
        Big5PFreq[37][25] = 296;
        Big5PFreq[0][69] = 295;
        Big5PFreq[43][172] = 294;
        Big5PFreq[42][167] = 293;
        Big5PFreq[35][120] = 292;
        Big5PFreq[41][128] = 291;
        Big5PFreq[2][88] = 290;
        Big5PFreq[20][123] = 289;
        Big5PFreq[35][123] = 288;
        Big5PFreq[36][28] = 287;
        Big5PFreq[42][188] = 286;
        Big5PFreq[42][164] = 285;
        Big5PFreq[42][4] = 284;
        Big5PFreq[43][57] = 283;
        Big5PFreq[39][3] = 282;
        Big5PFreq[42][3] = 281;
        Big5PFreq[57][158] = 280;
        Big5PFreq[35][146] = 279;
        Big5PFreq[24][54] = 278;
        Big5PFreq[13][110] = 277;
        Big5PFreq[23][132] = 276;
        Big5PFreq[26][102] = 275;
        Big5PFreq[55][178] = 274;
        Big5PFreq[17][117] = 273;
        Big5PFreq[41][161] = 272;
        Big5PFreq[38][150] = 271;
        Big5PFreq[10][71] = 270;
        Big5PFreq[47][60] = 269;
        Big5PFreq[16][114] = 268;
        Big5PFreq[21][47] = 267;
        Big5PFreq[39][101] = 266;
        Big5PFreq[18][45] = 265;
        Big5PFreq[40][121] = 264;
        Big5PFreq[45][41] = 263;
        Big5PFreq[22][167] = 262;
        Big5PFreq[26][149] = 261;
        Big5PFreq[15][189] = 260;
        Big5PFreq[41][177] = 259;
        Big5PFreq[46][36] = 258;
        Big5PFreq[20][40] = 257;
        Big5PFreq[41][54] = 256;
        Big5PFreq[3][87] = 255;
        Big5PFreq[40][16] = 254;
        Big5PFreq[42][15] = 253;
        Big5PFreq[11][83] = 252;
        Big5PFreq[0][94] = 251;
        Big5PFreq[122][81] = 250;
        Big5PFreq[41][26] = 249;
        Big5PFreq[36][34] = 248;
        Big5PFreq[44][148] = 247;
        Big5PFreq[35][3] = 246;
        Big5PFreq[36][114] = 245;
        Big5PFreq[42][112] = 244;
        Big5PFreq[35][183] = 243;
        Big5PFreq[49][73] = 242;
        Big5PFreq[39][2] = 241;
        Big5PFreq[38][121] = 240;
        Big5PFreq[44][114] = 239;
        Big5PFreq[49][32] = 238;
        Big5PFreq[1][65] = 237;
        Big5PFreq[38][25] = 236;
        Big5PFreq[39][4] = 235;
        Big5PFreq[42][62] = 234;
        Big5PFreq[35][40] = 233;
        Big5PFreq[24][2] = 232;
        Big5PFreq[53][49] = 231;
        Big5PFreq[41][133] = 230;
        Big5PFreq[43][134] = 229;
        Big5PFreq[3][83] = 228;
        Big5PFreq[38][158] = 227;
        Big5PFreq[24][17] = 226;
        Big5PFreq[52][59] = 225;
        Big5PFreq[38][41] = 224;
        Big5PFreq[37][127] = 223;
        Big5PFreq[22][175] = 222;
        Big5PFreq[44][30] = 221;
        Big5PFreq[47][178] = 220;
        Big5PFreq[43][99] = 219;
        Big5PFreq[19][4] = 218;
        Big5PFreq[37][97] = 217;
        Big5PFreq[38][181] = 216;
        Big5PFreq[45][103] = 215;
        Big5PFreq[1][86] = 214;
        Big5PFreq[40][15] = 213;
        Big5PFreq[22][136] = 212;
        Big5PFreq[75][165] = 211;
        Big5PFreq[36][15] = 210;
        Big5PFreq[46][80] = 209;
        Big5PFreq[59][55] = 208;
        Big5PFreq[37][108] = 207;
        Big5PFreq[21][109] = 206;
        Big5PFreq[24][165] = 205;
        Big5PFreq[79][158] = 204;
        Big5PFreq[44][139] = 203;
        Big5PFreq[36][124] = 202;
        Big5PFreq[42][185] = 201;
        Big5PFreq[39][186] = 200;
        Big5PFreq[22][128] = 199;
        Big5PFreq[40][44] = 198;
        Big5PFreq[41][105] = 197;
        Big5PFreq[1][70] = 196;
        Big5PFreq[1][68] = 195;
        Big5PFreq[53][22] = 194;
        Big5PFreq[36][54] = 193;
        Big5PFreq[47][147] = 192;
        Big5PFreq[35][36] = 191;
        Big5PFreq[35][185] = 190;
        Big5PFreq[45][37] = 189;
        Big5PFreq[43][163] = 188;
        Big5PFreq[56][115] = 187;
        Big5PFreq[38][164] = 186;
        Big5PFreq[35][141] = 185;
        Big5PFreq[42][132] = 184;
        Big5PFreq[46][120] = 183;
        Big5PFreq[69][142] = 182;
        Big5PFreq[38][175] = 181;
        Big5PFreq[22][112] = 180;
        Big5PFreq[38][142] = 179;
        Big5PFreq[40][37] = 178;
        Big5PFreq[37][109] = 177;
        Big5PFreq[40][144] = 176;
        Big5PFreq[44][117] = 175;
        Big5PFreq[35][181] = 174;
        Big5PFreq[26][105] = 173;
        Big5PFreq[16][48] = 172;
        Big5PFreq[44][122] = 171;
        Big5PFreq[12][86] = 170;
        Big5PFreq[84][53] = 169;
        Big5PFreq[17][44] = 168;
        Big5PFreq[59][54] = 167;
        Big5PFreq[36][98] = 166;
        Big5PFreq[45][115] = 165;
        Big5PFreq[73][9] = 164;
        Big5PFreq[44][123] = 163;
        Big5PFreq[37][188] = 162;
        Big5PFreq[51][117] = 161;
        Big5PFreq[15][156] = 160;
        Big5PFreq[36][155] = 159;
        Big5PFreq[44][25] = 158;
        Big5PFreq[38][12] = 157;
        Big5PFreq[38][140] = 156;
        Big5PFreq[23][4] = 155;
        Big5PFreq[45][149] = 154;
        Big5PFreq[22][189] = 153;
        Big5PFreq[38][147] = 152;
        Big5PFreq[27][5] = 151;
        Big5PFreq[22][42] = 150;
        Big5PFreq[3][68] = 149;
        Big5PFreq[39][51] = 148;
        Big5PFreq[36][29] = 147;
        Big5PFreq[20][108] = 146;
        Big5PFreq[50][57] = 145;
        Big5PFreq[55][104] = 144;
        Big5PFreq[22][46] = 143;
        Big5PFreq[18][164] = 142;
        Big5PFreq[50][159] = 141;
        Big5PFreq[85][131] = 140;
        Big5PFreq[26][79] = 139;
        Big5PFreq[38][100] = 138;
        Big5PFreq[53][112] = 137;
        Big5PFreq[20][190] = 136;
        Big5PFreq[14][69] = 135;
        Big5PFreq[23][11] = 134;
        Big5PFreq[40][114] = 133;
        Big5PFreq[40][148] = 132;
        Big5PFreq[53][130] = 131;
        Big5PFreq[36][2] = 130;
        Big5PFreq[66][82] = 129;
        Big5PFreq[45][166] = 128;
        Big5PFreq[4][88] = 127;
        Big5PFreq[16][57] = 126;
        Big5PFreq[22][116] = 125;
        Big5PFreq[36][108] = 124;
        Big5PFreq[13][48] = 123;
        Big5PFreq[54][12] = 122;
        Big5PFreq[40][136] = 121;
        Big5PFreq[36][128] = 120;
        Big5PFreq[23][6] = 119;
        Big5PFreq[38][125] = 118;
        Big5PFreq[45][154] = 117;
        Big5PFreq[51][127] = 116;
        Big5PFreq[44][163] = 115;
        Big5PFreq[16][173] = 114;
        Big5PFreq[43][49] = 113;
        Big5PFreq[20][112] = 112;
        Big5PFreq[15][168] = 111;
        Big5PFreq[35][129] = 110;
        Big5PFreq[20][45] = 109;
        Big5PFreq[38][10] = 108;
        Big5PFreq[57][171] = 107;
        Big5PFreq[44][190] = 106;
        Big5PFreq[40][56] = 105;
        Big5PFreq[36][156] = 104;
        Big5PFreq[3][88] = 103;
        Big5PFreq[50][122] = 102;
        Big5PFreq[36][7] = 101;
        Big5PFreq[39][43] = 100;
        Big5PFreq[15][166] = 99;
        Big5PFreq[42][136] = 98;
        Big5PFreq[22][131] = 97;
        Big5PFreq[44][23] = 96;
        Big5PFreq[54][147] = 95;
        Big5PFreq[41][32] = 94;
        Big5PFreq[23][121] = 93;
        Big5PFreq[39][108] = 92;
        Big5PFreq[2][78] = 91;
        Big5PFreq[40][155] = 90;
        Big5PFreq[55][51] = 89;
        Big5PFreq[19][34] = 88;
        Big5PFreq[48][128] = 87;
        Big5PFreq[48][159] = 86;
        Big5PFreq[20][70] = 85;
        Big5PFreq[34][71] = 84;
        Big5PFreq[16][31] = 83;
        Big5PFreq[42][157] = 82;
        Big5PFreq[20][44] = 81;
        Big5PFreq[11][92] = 80;
        Big5PFreq[44][180] = 79;
        Big5PFreq[84][33] = 78;
        Big5PFreq[16][116] = 77;
        Big5PFreq[61][163] = 76;
        Big5PFreq[35][164] = 75;
        Big5PFreq[36][42] = 74;
        Big5PFreq[13][40] = 73;
        Big5PFreq[43][176] = 72;
        Big5PFreq[2][66] = 71;
        Big5PFreq[20][133] = 70;
        Big5PFreq[36][65] = 69;
        Big5PFreq[38][33] = 68;
        Big5PFreq[12][91] = 67;
        Big5PFreq[36][26] = 66;
        Big5PFreq[15][174] = 65;
        Big5PFreq[77][32] = 64;
        Big5PFreq[16][1] = 63;
        Big5PFreq[25][86] = 62;
        Big5PFreq[17][13] = 61;
        Big5PFreq[5][75] = 60;
        Big5PFreq[36][52] = 59;
        Big5PFreq[51][164] = 58;
        Big5PFreq[12][85] = 57;
        Big5PFreq[39][168] = 56;
        Big5PFreq[43][16] = 55;
        Big5PFreq[40][69] = 54;
        Big5PFreq[26][108] = 53;
        Big5PFreq[51][56] = 52;
        Big5PFreq[16][37] = 51;
        Big5PFreq[40][29] = 50;
        Big5PFreq[46][171] = 49;
        Big5PFreq[40][128] = 48;
        Big5PFreq[72][114] = 47;
        Big5PFreq[21][103] = 46;
        Big5PFreq[22][44] = 45;
        Big5PFreq[40][115] = 44;
        Big5PFreq[43][7] = 43;
        Big5PFreq[43][153] = 42;
        Big5PFreq[17][20] = 41;
        Big5PFreq[16][49] = 40;
        Big5PFreq[36][57] = 39;
        Big5PFreq[18][38] = 38;
        Big5PFreq[45][184] = 37;
        Big5PFreq[37][167] = 36;
        Big5PFreq[26][106] = 35;
        Big5PFreq[61][121] = 34;
        Big5PFreq[89][140] = 33;
        Big5PFreq[46][61] = 32;
        Big5PFreq[39][163] = 31;
        Big5PFreq[40][62] = 30;
        Big5PFreq[38][165] = 29;
        Big5PFreq[47][37] = 28;
        Big5PFreq[18][155] = 27;
        Big5PFreq[20][33] = 26;
        Big5PFreq[29][90] = 25;
        Big5PFreq[20][103] = 24;
        Big5PFreq[37][51] = 23;
        Big5PFreq[57][0] = 22;
        Big5PFreq[40][31] = 21;
        Big5PFreq[45][32] = 20;
        Big5PFreq[59][23] = 19;
        Big5PFreq[18][47] = 18;
        Big5PFreq[45][134] = 17;
        Big5PFreq[37][59] = 16;
        Big5PFreq[21][128] = 15;
        Big5PFreq[36][106] = 14;
        Big5PFreq[31][39] = 13;
        Big5PFreq[40][182] = 12;
        Big5PFreq[52][155] = 11;
        Big5PFreq[42][166] = 10;
        Big5PFreq[35][27] = 9;
        Big5PFreq[38][3] = 8;
        Big5PFreq[13][44] = 7;
        Big5PFreq[58][157] = 6;
        Big5PFreq[47][51] = 5;
        Big5PFreq[41][37] = 4;
        Big5PFreq[41][172] = 3;
        Big5PFreq[51][165] = 2;
        Big5PFreq[15][161] = 1;
        Big5PFreq[24][181] = 0;
        EUC_TWFreq[48][49] = 599;
        EUC_TWFreq[35][65] = 598;
        EUC_TWFreq[41][27] = 597;
        EUC_TWFreq[35][0] = 596;
        EUC_TWFreq[39][19] = 595;
        EUC_TWFreq[35][42] = 594;
        EUC_TWFreq[38][66] = 593;
        EUC_TWFreq[35][8] = 592;
        EUC_TWFreq[35][6] = 591;
        EUC_TWFreq[35][66] = 590;
        EUC_TWFreq[43][14] = 589;
        EUC_TWFreq[69][80] = 588;
        EUC_TWFreq[50][48] = 587;
        EUC_TWFreq[36][71] = 586;
        EUC_TWFreq[37][10] = 585;
        EUC_TWFreq[60][52] = 584;
        EUC_TWFreq[51][21] = 583;
        EUC_TWFreq[40][2] = 582;
        EUC_TWFreq[67][35] = 581;
        EUC_TWFreq[38][78] = 580;
        EUC_TWFreq[49][18] = 579;
        EUC_TWFreq[35][23] = 578;
        EUC_TWFreq[42][83] = 577;
        EUC_TWFreq[79][47] = 576;
        EUC_TWFreq[61][82] = 575;
        EUC_TWFreq[38][7] = 574;
        EUC_TWFreq[35][29] = 573;
        EUC_TWFreq[37][77] = 572;
        EUC_TWFreq[54][67] = 571;
        EUC_TWFreq[38][80] = 570;
        EUC_TWFreq[52][74] = 569;
        EUC_TWFreq[36][37] = 568;
        EUC_TWFreq[74][8] = 567;
        EUC_TWFreq[41][83] = 566;
        EUC_TWFreq[36][75] = 565;
        EUC_TWFreq[49][63] = 564;
        EUC_TWFreq[42][58] = 563;
        EUC_TWFreq[56][33] = 562;
        EUC_TWFreq[37][76] = 561;
        EUC_TWFreq[62][39] = 560;
        EUC_TWFreq[35][21] = 559;
        EUC_TWFreq[70][19] = 558;
        EUC_TWFreq[77][88] = 557;
        EUC_TWFreq[51][14] = 556;
        EUC_TWFreq[36][17] = 555;
        EUC_TWFreq[44][51] = 554;
        EUC_TWFreq[38][72] = 553;
        EUC_TWFreq[74][90] = 552;
        EUC_TWFreq[35][48] = 551;
        EUC_TWFreq[35][69] = 550;
        EUC_TWFreq[66][86] = 549;
        EUC_TWFreq[57][20] = 548;
        EUC_TWFreq[35][53] = 547;
        EUC_TWFreq[36][87] = 546;
        EUC_TWFreq[84][67] = 545;
        EUC_TWFreq[70][56] = 544;
        EUC_TWFreq[71][54] = 543;
        EUC_TWFreq[60][70] = 542;
        EUC_TWFreq[80][1] = 541;
        EUC_TWFreq[39][59] = 540;
        EUC_TWFreq[39][51] = 539;
        EUC_TWFreq[35][44] = 538;
        EUC_TWFreq[48][4] = 537;
        EUC_TWFreq[55][24] = 536;
        EUC_TWFreq[52][4] = 535;
        EUC_TWFreq[54][26] = 534;
        EUC_TWFreq[36][31] = 533;
        EUC_TWFreq[37][22] = 532;
        EUC_TWFreq[37][9] = 531;
        EUC_TWFreq[46][0] = 530;
        EUC_TWFreq[56][46] = 529;
        EUC_TWFreq[47][93] = 528;
        EUC_TWFreq[37][25] = 527;
        EUC_TWFreq[39][8] = 526;
        EUC_TWFreq[46][73] = 525;
        EUC_TWFreq[38][48] = 524;
        EUC_TWFreq[39][83] = 523;
        EUC_TWFreq[60][92] = 522;
        EUC_TWFreq[70][11] = 521;
        EUC_TWFreq[63][84] = 520;
        EUC_TWFreq[38][65] = 519;
        EUC_TWFreq[45][45] = 518;
        EUC_TWFreq[63][49] = 517;
        EUC_TWFreq[63][50] = 516;
        EUC_TWFreq[39][93] = 515;
        EUC_TWFreq[68][20] = 514;
        EUC_TWFreq[44][84] = 513;
        EUC_TWFreq[66][34] = 512;
        EUC_TWFreq[37][58] = 511;
        EUC_TWFreq[39][0] = 510;
        EUC_TWFreq[59][1] = 509;
        EUC_TWFreq[47][8] = 508;
        EUC_TWFreq[61][17] = 507;
        EUC_TWFreq[53][87] = 506;
        EUC_TWFreq[67][26] = 505;
        EUC_TWFreq[43][46] = 504;
        EUC_TWFreq[38][61] = 503;
        EUC_TWFreq[45][9] = 502;
        EUC_TWFreq[66][83] = 501;
        EUC_TWFreq[43][88] = 500;
        EUC_TWFreq[85][20] = 499;
        EUC_TWFreq[57][36] = 498;
        EUC_TWFreq[43][6] = 497;
        EUC_TWFreq[86][77] = 496;
        EUC_TWFreq[42][70] = 495;
        EUC_TWFreq[49][78] = 494;
        EUC_TWFreq[36][40] = 493;
        EUC_TWFreq[42][71] = 492;
        EUC_TWFreq[58][49] = 491;
        EUC_TWFreq[35][20] = 490;
        EUC_TWFreq[76][20] = 489;
        EUC_TWFreq[39][25] = 488;
        EUC_TWFreq[40][34] = 487;
        EUC_TWFreq[39][76] = 486;
        EUC_TWFreq[40][1] = 485;
        EUC_TWFreq[59][0] = 484;
        EUC_TWFreq[39][70] = 483;
        EUC_TWFreq[46][14] = 482;
        EUC_TWFreq[68][77] = 481;
        EUC_TWFreq[38][55] = 480;
        EUC_TWFreq[35][78] = 479;
        EUC_TWFreq[84][44] = 478;
        EUC_TWFreq[36][41] = 477;
        EUC_TWFreq[37][62] = 476;
        EUC_TWFreq[65][67] = 475;
        EUC_TWFreq[69][66] = 474;
        EUC_TWFreq[73][55] = 473;
        EUC_TWFreq[71][49] = 472;
        EUC_TWFreq[66][87] = 471;
        EUC_TWFreq[38][33] = 470;
        EUC_TWFreq[64][61] = 469;
        EUC_TWFreq[35][7] = 468;
        EUC_TWFreq[47][49] = 467;
        EUC_TWFreq[56][14] = 466;
        EUC_TWFreq[36][49] = 465;
        EUC_TWFreq[50][81] = 464;
        EUC_TWFreq[55][76] = 463;
        EUC_TWFreq[35][19] = 462;
        EUC_TWFreq[44][47] = 461;
        EUC_TWFreq[35][15] = 460;
        EUC_TWFreq[82][59] = 459;
        EUC_TWFreq[35][43] = 458;
        EUC_TWFreq[73][0] = 457;
        EUC_TWFreq[57][83] = 456;
        EUC_TWFreq[42][46] = 455;
        EUC_TWFreq[36][0] = 454;
        EUC_TWFreq[70][88] = 453;
        EUC_TWFreq[42][22] = 452;
        EUC_TWFreq[46][58] = 451;
        EUC_TWFreq[36][34] = 450;
        EUC_TWFreq[39][24] = 449;
        EUC_TWFreq[35][55] = 448;
        EUC_TWFreq[44][91] = 447;
        EUC_TWFreq[37][51] = 446;
        EUC_TWFreq[36][19] = 445;
        EUC_TWFreq[69][90] = 444;
        EUC_TWFreq[55][35] = 443;
        EUC_TWFreq[35][54] = 442;
        EUC_TWFreq[49][61] = 441;
        EUC_TWFreq[36][67] = 440;
        EUC_TWFreq[88][34] = 439;
        EUC_TWFreq[35][17] = 438;
        EUC_TWFreq[65][69] = 437;
        EUC_TWFreq[74][89] = 436;
        EUC_TWFreq[37][31] = 435;
        EUC_TWFreq[43][48] = 434;
        EUC_TWFreq[89][27] = 433;
        EUC_TWFreq[42][79] = 432;
        EUC_TWFreq[69][57] = 431;
        EUC_TWFreq[36][13] = 430;
        EUC_TWFreq[35][62] = 429;
        EUC_TWFreq[65][47] = 428;
        EUC_TWFreq[56][8] = 427;
        EUC_TWFreq[38][79] = 426;
        EUC_TWFreq[37][64] = 425;
        EUC_TWFreq[64][64] = 424;
        EUC_TWFreq[38][53] = 423;
        EUC_TWFreq[38][31] = 422;
        EUC_TWFreq[56][81] = 421;
        EUC_TWFreq[36][22] = 420;
        EUC_TWFreq[43][4] = 419;
        EUC_TWFreq[36][90] = 418;
        EUC_TWFreq[38][62] = 417;
        EUC_TWFreq[66][85] = 416;
        EUC_TWFreq[39][1] = 415;
        EUC_TWFreq[59][40] = 414;
        EUC_TWFreq[58][93] = 413;
        EUC_TWFreq[44][43] = 412;
        EUC_TWFreq[39][49] = 411;
        EUC_TWFreq[64][2] = 410;
        EUC_TWFreq[41][35] = 409;
        EUC_TWFreq[60][22] = 408;
        EUC_TWFreq[35][91] = 407;
        EUC_TWFreq[78][1] = 406;
        EUC_TWFreq[36][14] = 405;
        EUC_TWFreq[82][29] = 404;
        EUC_TWFreq[52][86] = 403;
        EUC_TWFreq[40][16] = 402;
        EUC_TWFreq[91][52] = 401;
        EUC_TWFreq[50][75] = 400;
        EUC_TWFreq[64][30] = 399;
        EUC_TWFreq[90][78] = 398;
        EUC_TWFreq[36][52] = 397;
        EUC_TWFreq[55][87] = 396;
        EUC_TWFreq[57][5] = 395;
        EUC_TWFreq[57][31] = 394;
        EUC_TWFreq[42][35] = 393;
        EUC_TWFreq[69][50] = 392;
        EUC_TWFreq[45][8] = 391;
        EUC_TWFreq[50][87] = 390;
        EUC_TWFreq[69][55] = 389;
        EUC_TWFreq[92][3] = 388;
        EUC_TWFreq[36][43] = 387;
        EUC_TWFreq[64][10] = 386;
        EUC_TWFreq[56][25] = 385;
        EUC_TWFreq[60][68] = 384;
        EUC_TWFreq[51][46] = 383;
        EUC_TWFreq[50][0] = 382;
        EUC_TWFreq[38][30] = 381;
        EUC_TWFreq[50][85] = 380;
        EUC_TWFreq[60][54] = 379;
        EUC_TWFreq[73][6] = 378;
        EUC_TWFreq[73][28] = 377;
        EUC_TWFreq[56][19] = 376;
        EUC_TWFreq[62][69] = 375;
        EUC_TWFreq[81][66] = 374;
        EUC_TWFreq[40][32] = 373;
        EUC_TWFreq[76][31] = 372;
        EUC_TWFreq[35][10] = 371;
        EUC_TWFreq[41][37] = 370;
        EUC_TWFreq[52][82] = 369;
        EUC_TWFreq[91][72] = 368;
        EUC_TWFreq[37][29] = 367;
        EUC_TWFreq[56][30] = 366;
        EUC_TWFreq[37][80] = 365;
        EUC_TWFreq[81][56] = 364;
        EUC_TWFreq[70][3] = 363;
        EUC_TWFreq[76][15] = 362;
        EUC_TWFreq[46][47] = 361;
        EUC_TWFreq[35][88] = 360;
        EUC_TWFreq[61][58] = 359;
        EUC_TWFreq[37][37] = 358;
        EUC_TWFreq[57][22] = 357;
        EUC_TWFreq[41][23] = 356;
        EUC_TWFreq[90][66] = 355;
        EUC_TWFreq[39][60] = 354;
        EUC_TWFreq[38][0] = 353;
        EUC_TWFreq[37][87] = 352;
        EUC_TWFreq[46][2] = 351;
        EUC_TWFreq[38][56] = 350;
        EUC_TWFreq[58][11] = 349;
        EUC_TWFreq[48][10] = 348;
        EUC_TWFreq[74][4] = 347;
        EUC_TWFreq[40][42] = 346;
        EUC_TWFreq[41][52] = 345;
        EUC_TWFreq[61][92] = 344;
        EUC_TWFreq[39][50] = 343;
        EUC_TWFreq[47][88] = 342;
        EUC_TWFreq[88][36] = 341;
        EUC_TWFreq[45][73] = 340;
        EUC_TWFreq[82][3] = 339;
        EUC_TWFreq[61][36] = 338;
        EUC_TWFreq[60][33] = 337;
        EUC_TWFreq[38][27] = 336;
        EUC_TWFreq[35][83] = 335;
        EUC_TWFreq[65][24] = 334;
        EUC_TWFreq[73][10] = 333;
        EUC_TWFreq[41][13] = 332;
        EUC_TWFreq[50][27] = 331;
        EUC_TWFreq[59][50] = 330;
        EUC_TWFreq[42][45] = 329;
        EUC_TWFreq[55][19] = 328;
        EUC_TWFreq[36][77] = 327;
        EUC_TWFreq[69][31] = 326;
        EUC_TWFreq[60][7] = 325;
        EUC_TWFreq[40][88] = 324;
        EUC_TWFreq[57][56] = 323;
        EUC_TWFreq[50][50] = 322;
        EUC_TWFreq[42][37] = 321;
        EUC_TWFreq[38][82] = 320;
        EUC_TWFreq[52][25] = 319;
        EUC_TWFreq[42][67] = 318;
        EUC_TWFreq[48][40] = 317;
        EUC_TWFreq[45][81] = 316;
        EUC_TWFreq[57][14] = 315;
        EUC_TWFreq[42][13] = 314;
        EUC_TWFreq[78][0] = 313;
        EUC_TWFreq[35][51] = 312;
        EUC_TWFreq[41][67] = 311;
        EUC_TWFreq[64][23] = 310;
        EUC_TWFreq[36][65] = 309;
        EUC_TWFreq[48][50] = 308;
        EUC_TWFreq[46][69] = 307;
        EUC_TWFreq[47][89] = 306;
        EUC_TWFreq[41][48] = 305;
        EUC_TWFreq[60][56] = 304;
        EUC_TWFreq[44][82] = 303;
        EUC_TWFreq[47][35] = 302;
        EUC_TWFreq[49][3] = 301;
        EUC_TWFreq[49][69] = 300;
        EUC_TWFreq[45][93] = 299;
        EUC_TWFreq[60][34] = 298;
        EUC_TWFreq[60][82] = 297;
        EUC_TWFreq[61][61] = 296;
        EUC_TWFreq[86][42] = 295;
        EUC_TWFreq[89][60] = 294;
        EUC_TWFreq[48][31] = 293;
        EUC_TWFreq[35][75] = 292;
        EUC_TWFreq[91][39] = 291;
        EUC_TWFreq[53][19] = 290;
        EUC_TWFreq[39][72] = 289;
        EUC_TWFreq[69][59] = 288;
        EUC_TWFreq[41][7] = 287;
        EUC_TWFreq[54][13] = 286;
        EUC_TWFreq[43][28] = 285;
        EUC_TWFreq[36][6] = 284;
        EUC_TWFreq[45][75] = 283;
        EUC_TWFreq[36][61] = 282;
        EUC_TWFreq[38][21] = 281;
        EUC_TWFreq[45][14] = 280;
        EUC_TWFreq[61][43] = 279;
        EUC_TWFreq[36][63] = 278;
        EUC_TWFreq[43][30] = 277;
        EUC_TWFreq[46][51] = 276;
        EUC_TWFreq[68][87] = 275;
        EUC_TWFreq[39][26] = 274;
        EUC_TWFreq[46][76] = 273;
        EUC_TWFreq[36][15] = 272;
        EUC_TWFreq[35][40] = 271;
        EUC_TWFreq[79][60] = 270;
        EUC_TWFreq[46][7] = 269;
        EUC_TWFreq[65][72] = 268;
        EUC_TWFreq[69][88] = 267;
        EUC_TWFreq[47][18] = 266;
        EUC_TWFreq[37][0] = 265;
        EUC_TWFreq[37][49] = 264;
        EUC_TWFreq[67][37] = 263;
        EUC_TWFreq[36][91] = 262;
        EUC_TWFreq[75][48] = 261;
        EUC_TWFreq[75][63] = 260;
        EUC_TWFreq[83][87] = 259;
        EUC_TWFreq[37][44] = 258;
        EUC_TWFreq[73][54] = 257;
        EUC_TWFreq[51][61] = 256;
        EUC_TWFreq[46][57] = 255;
        EUC_TWFreq[55][21] = 254;
        EUC_TWFreq[39][66] = 253;
        EUC_TWFreq[47][11] = 252;
        EUC_TWFreq[52][8] = 251;
        EUC_TWFreq[82][81] = 250;
        EUC_TWFreq[36][57] = 249;
        EUC_TWFreq[38][54] = 248;
        EUC_TWFreq[43][81] = 247;
        EUC_TWFreq[37][42] = 246;
        EUC_TWFreq[40][18] = 245;
        EUC_TWFreq[80][90] = 244;
        EUC_TWFreq[37][84] = 243;
        EUC_TWFreq[57][15] = 242;
        EUC_TWFreq[38][87] = 241;
        EUC_TWFreq[37][32] = 240;
        EUC_TWFreq[53][53] = 239;
        EUC_TWFreq[89][29] = 238;
        EUC_TWFreq[81][53] = 237;
        EUC_TWFreq[75][3] = 236;
        EUC_TWFreq[83][73] = 235;
        EUC_TWFreq[66][13] = 234;
        EUC_TWFreq[48][7] = 233;
        EUC_TWFreq[46][35] = 232;
        EUC_TWFreq[35][86] = 231;
        EUC_TWFreq[37][20] = 230;
        EUC_TWFreq[46][80] = 229;
        EUC_TWFreq[38][24] = 228;
        EUC_TWFreq[41][68] = 227;
        EUC_TWFreq[42][21] = 226;
        EUC_TWFreq[43][32] = 225;
        EUC_TWFreq[38][20] = 224;
        EUC_TWFreq[37][59] = 223;
        EUC_TWFreq[41][77] = 222;
        EUC_TWFreq[59][57] = 221;
        EUC_TWFreq[68][59] = 220;
        EUC_TWFreq[39][43] = 219;
        EUC_TWFreq[54][39] = 218;
        EUC_TWFreq[48][28] = 217;
        EUC_TWFreq[54][28] = 216;
        EUC_TWFreq[41][44] = 215;
        EUC_TWFreq[51][64] = 214;
        EUC_TWFreq[47][72] = 213;
        EUC_TWFreq[62][67] = 212;
        EUC_TWFreq[42][43] = 211;
        EUC_TWFreq[61][38] = 210;
        EUC_TWFreq[76][25] = 209;
        EUC_TWFreq[48][91] = 208;
        EUC_TWFreq[36][36] = 207;
        EUC_TWFreq[80][32] = 206;
        EUC_TWFreq[81][40] = 205;
        EUC_TWFreq[37][5] = 204;
        EUC_TWFreq[74][69] = 203;
        EUC_TWFreq[36][82] = 202;
        EUC_TWFreq[46][59] = 201;

        GBKFreq[52][132] = 600;
        GBKFreq[73][135] = 599;
        GBKFreq[49][123] = 598;
        GBKFreq[77][146] = 597;
        GBKFreq[81][123] = 596;
        GBKFreq[82][144] = 595;
        GBKFreq[51][179] = 594;
        GBKFreq[83][154] = 593;
        GBKFreq[71][139] = 592;
        GBKFreq[64][139] = 591;
        GBKFreq[85][144] = 590;
        GBKFreq[52][125] = 589;
        GBKFreq[88][25] = 588;
        GBKFreq[81][106] = 587;
        GBKFreq[81][148] = 586;
        GBKFreq[62][137] = 585;
        GBKFreq[94][0] = 584;
        GBKFreq[1][64] = 583;
        GBKFreq[67][163] = 582;
        GBKFreq[20][190] = 581;
        GBKFreq[57][131] = 580;
        GBKFreq[29][169] = 579;
        GBKFreq[72][143] = 578;
        GBKFreq[0][173] = 577;
        GBKFreq[11][23] = 576;
        GBKFreq[61][141] = 575;
        GBKFreq[60][123] = 574;
        GBKFreq[81][114] = 573;
        GBKFreq[82][131] = 572;
        GBKFreq[67][156] = 571;
        GBKFreq[71][167] = 570;
        GBKFreq[20][50] = 569;
        GBKFreq[77][132] = 568;
        GBKFreq[84][38] = 567;
        GBKFreq[26][29] = 566;
        GBKFreq[74][187] = 565;
        GBKFreq[62][116] = 564;
        GBKFreq[67][135] = 563;
        GBKFreq[5][86] = 562;
        GBKFreq[72][186] = 561;
        GBKFreq[75][161] = 560;
        GBKFreq[78][130] = 559;
        GBKFreq[94][30] = 558;
        GBKFreq[84][72] = 557;
        GBKFreq[1][67] = 556;
        GBKFreq[75][172] = 555;
        GBKFreq[74][185] = 554;
        GBKFreq[53][160] = 553;
        GBKFreq[123][14] = 552;
        GBKFreq[79][97] = 551;
        GBKFreq[85][110] = 550;
        GBKFreq[78][171] = 549;
        GBKFreq[52][131] = 548;
        GBKFreq[56][100] = 547;
        GBKFreq[50][182] = 546;
        GBKFreq[94][64] = 545;
        GBKFreq[106][74] = 544;
        GBKFreq[11][102] = 543;
        GBKFreq[53][124] = 542;
        GBKFreq[24][3] = 541;
        GBKFreq[86][148] = 540;
        GBKFreq[53][184] = 539;
        GBKFreq[86][147] = 538;
        GBKFreq[96][161] = 537;
        GBKFreq[82][77] = 536;
        GBKFreq[59][146] = 535;
        GBKFreq[84][126] = 534;
        GBKFreq[79][132] = 533;
        GBKFreq[85][123] = 532;
        GBKFreq[71][101] = 531;
        GBKFreq[85][106] = 530;
        GBKFreq[6][184] = 529;
        GBKFreq[57][156] = 528;
        GBKFreq[75][104] = 527;
        GBKFreq[50][137] = 526;
        GBKFreq[79][133] = 525;
        GBKFreq[76][108] = 524;
        GBKFreq[57][142] = 523;
        GBKFreq[84][130] = 522;
        GBKFreq[52][128] = 521;
        GBKFreq[47][44] = 520;
        GBKFreq[52][152] = 519;
        GBKFreq[54][104] = 518;
        GBKFreq[30][47] = 517;
        GBKFreq[71][123] = 516;
        GBKFreq[52][107] = 515;
        GBKFreq[45][84] = 514;
        GBKFreq[107][118] = 513;
        GBKFreq[5][161] = 512;
        GBKFreq[48][126] = 511;
        GBKFreq[67][170] = 510;
        GBKFreq[43][6] = 509;
        GBKFreq[70][112] = 508;
        GBKFreq[86][174] = 507;
        GBKFreq[84][166] = 506;
        GBKFreq[79][130] = 505;
        GBKFreq[57][141] = 504;
        GBKFreq[81][178] = 503;
        GBKFreq[56][187] = 502;
        GBKFreq[81][162] = 501;
        GBKFreq[53][104] = 500;
        GBKFreq[123][35] = 499;
        GBKFreq[70][169] = 498;
        GBKFreq[69][164] = 497;
        GBKFreq[109][61] = 496;
        GBKFreq[73][130] = 495;
        GBKFreq[62][134] = 494;
        GBKFreq[54][125] = 493;
        GBKFreq[79][105] = 492;
        GBKFreq[70][165] = 491;
        GBKFreq[71][189] = 490;
        GBKFreq[23][147] = 489;
        GBKFreq[51][139] = 488;
        GBKFreq[47][137] = 487;
        GBKFreq[77][123] = 486;
        GBKFreq[86][183] = 485;
        GBKFreq[63][173] = 484;
        GBKFreq[79][144] = 483;
        GBKFreq[84][159] = 482;
        GBKFreq[60][91] = 481;
        GBKFreq[66][187] = 480;
        GBKFreq[73][114] = 479;
        GBKFreq[85][56] = 478;
        GBKFreq[71][149] = 477;
        GBKFreq[84][189] = 476;
        GBKFreq[104][31] = 475;
        GBKFreq[83][82] = 474;
        GBKFreq[68][35] = 473;
        GBKFreq[11][77] = 472;
        GBKFreq[15][155] = 471;
        GBKFreq[83][153] = 470;
        GBKFreq[71][1] = 469;
        GBKFreq[53][190] = 468;
        GBKFreq[50][135] = 467;
        GBKFreq[3][147] = 466;
        GBKFreq[48][136] = 465;
        GBKFreq[66][166] = 464;
        GBKFreq[55][159] = 463;
        GBKFreq[82][150] = 462;
        GBKFreq[58][178] = 461;
        GBKFreq[64][102] = 460;
        GBKFreq[16][106] = 459;
        GBKFreq[68][110] = 458;
        GBKFreq[54][14] = 457;
        GBKFreq[60][140] = 456;
        GBKFreq[91][71] = 455;
        GBKFreq[54][150] = 454;
        GBKFreq[78][177] = 453;
        GBKFreq[78][117] = 452;
        GBKFreq[104][12] = 451;
        GBKFreq[73][150] = 450;
        GBKFreq[51][142] = 449;
        GBKFreq[81][145] = 448;
        GBKFreq[66][183] = 447;
        GBKFreq[51][178] = 446;
        GBKFreq[75][107] = 445;
        GBKFreq[65][119] = 444;
        GBKFreq[69][176] = 443;
        GBKFreq[59][122] = 442;
        GBKFreq[78][160] = 441;
        GBKFreq[85][183] = 440;
        GBKFreq[105][16] = 439;
        GBKFreq[73][110] = 438;
        GBKFreq[104][39] = 437;
        GBKFreq[119][16] = 436;
        GBKFreq[76][162] = 435;
        GBKFreq[67][152] = 434;
        GBKFreq[82][24] = 433;
        GBKFreq[73][121] = 432;
        GBKFreq[83][83] = 431;
        GBKFreq[82][145] = 430;
        GBKFreq[49][133] = 429;
        GBKFreq[94][13] = 428;
        GBKFreq[58][139] = 427;
        GBKFreq[74][189] = 426;
        GBKFreq[66][177] = 425;
        GBKFreq[85][184] = 424;
        GBKFreq[55][183] = 423;
        GBKFreq[71][107] = 422;
        GBKFreq[11][98] = 421;
        GBKFreq[72][153] = 420;
        GBKFreq[2][137] = 419;
        GBKFreq[59][147] = 418;
        GBKFreq[58][152] = 417;
        GBKFreq[55][144] = 416;
        GBKFreq[73][125] = 415;
        GBKFreq[52][154] = 414;
        GBKFreq[70][178] = 413;
        GBKFreq[79][148] = 412;
        GBKFreq[63][143] = 411;
        GBKFreq[50][140] = 410;
        GBKFreq[47][145] = 409;
        GBKFreq[48][123] = 408;
        GBKFreq[56][107] = 407;
        GBKFreq[84][83] = 406;
        GBKFreq[59][112] = 405;
        GBKFreq[124][72] = 404;
        GBKFreq[79][99] = 403;
        GBKFreq[3][37] = 402;
        GBKFreq[114][55] = 401;
        GBKFreq[85][152] = 400;
        GBKFreq[60][47] = 399;
        GBKFreq[65][96] = 398;
        GBKFreq[74][110] = 397;
        GBKFreq[86][182] = 396;
        GBKFreq[50][99] = 395;
        GBKFreq[67][186] = 394;
        GBKFreq[81][74] = 393;
        GBKFreq[80][37] = 392;
        GBKFreq[21][60] = 391;
        GBKFreq[110][12] = 390;
        GBKFreq[60][162] = 389;
        GBKFreq[29][115] = 388;
        GBKFreq[83][130] = 387;
        GBKFreq[52][136] = 386;
        GBKFreq[63][114] = 385;
        GBKFreq[49][127] = 384;
        GBKFreq[83][109] = 383;
        GBKFreq[66][128] = 382;
        GBKFreq[78][136] = 381;
        GBKFreq[81][180] = 380;
        GBKFreq[76][104] = 379;
        GBKFreq[56][156] = 378;
        GBKFreq[61][23] = 377;
        GBKFreq[4][30] = 376;
        GBKFreq[69][154] = 375;
        GBKFreq[100][37] = 374;
        GBKFreq[54][177] = 373;
        GBKFreq[23][119] = 372;
        GBKFreq[71][171] = 371;
        GBKFreq[84][146] = 370;
        GBKFreq[20][184] = 369;
        GBKFreq[86][76] = 368;
        GBKFreq[74][132] = 367;
        GBKFreq[47][97] = 366;
        GBKFreq[82][137] = 365;
        GBKFreq[94][56] = 364;
        GBKFreq[92][30] = 363;
        GBKFreq[19][117] = 362;
        GBKFreq[48][173] = 361;
        GBKFreq[2][136] = 360;
        GBKFreq[7][182] = 359;
        GBKFreq[74][188] = 358;
        GBKFreq[14][132] = 357;
        GBKFreq[62][172] = 356;
        GBKFreq[25][39] = 355;
        GBKFreq[85][129] = 354;
        GBKFreq[64][98] = 353;
        GBKFreq[67][127] = 352;
        GBKFreq[72][167] = 351;
        GBKFreq[57][143] = 350;
        GBKFreq[76][187] = 349;
        GBKFreq[83][181] = 348;
        GBKFreq[84][10] = 347;
        GBKFreq[55][166] = 346;
        GBKFreq[55][188] = 345;
        GBKFreq[13][151] = 344;
        GBKFreq[62][124] = 343;
        GBKFreq[53][136] = 342;
        GBKFreq[106][57] = 341;
        GBKFreq[47][166] = 340;
        GBKFreq[109][30] = 339;
        GBKFreq[78][114] = 338;
        GBKFreq[83][19] = 337;
        GBKFreq[56][162] = 336;
        GBKFreq[60][177] = 335;
        GBKFreq[88][9] = 334;
        GBKFreq[74][163] = 333;
        GBKFreq[52][156] = 332;
        GBKFreq[71][180] = 331;
        GBKFreq[60][57] = 330;
        GBKFreq[72][173] = 329;
        GBKFreq[82][91] = 328;
        GBKFreq[51][186] = 327;
        GBKFreq[75][86] = 326;
        GBKFreq[75][78] = 325;
        GBKFreq[76][170] = 324;
        GBKFreq[60][147] = 323;
        GBKFreq[82][75] = 322;
        GBKFreq[80][148] = 321;
        GBKFreq[86][150] = 320;
        GBKFreq[13][95] = 319;
        GBKFreq[0][11] = 318;
        GBKFreq[84][190] = 317;
        GBKFreq[76][166] = 316;
        GBKFreq[14][72] = 315;
        GBKFreq[67][144] = 314;
        GBKFreq[84][44] = 313;
        GBKFreq[72][125] = 312;
        GBKFreq[66][127] = 311;
        GBKFreq[60][25] = 310;
        GBKFreq[70][146] = 309;
        GBKFreq[79][135] = 308;
        GBKFreq[54][135] = 307;
        GBKFreq[60][104] = 306;
        GBKFreq[55][132] = 305;
        GBKFreq[94][2] = 304;
        GBKFreq[54][133] = 303;
        GBKFreq[56][190] = 302;
        GBKFreq[58][174] = 301;
        GBKFreq[80][144] = 300;
        GBKFreq[85][113] = 299;

        KRFreq[31][43] = 600;
        KRFreq[19][56] = 599;
        KRFreq[38][46] = 598;
        KRFreq[3][3] = 597;
        KRFreq[29][77] = 596;
        KRFreq[19][33] = 595;
        KRFreq[30][0] = 594;
        KRFreq[29][89] = 593;
        KRFreq[31][26] = 592;
        KRFreq[31][38] = 591;
        KRFreq[32][85] = 590;
        KRFreq[15][0] = 589;
        KRFreq[16][54] = 588;
        KRFreq[15][76] = 587;
        KRFreq[31][25] = 586;
        KRFreq[23][13] = 585;
        KRFreq[28][34] = 584;
        KRFreq[18][9] = 583;
        KRFreq[29][37] = 582;
        KRFreq[22][45] = 581;
        KRFreq[19][46] = 580;
        KRFreq[16][65] = 579;
        KRFreq[23][5] = 578;
        KRFreq[26][70] = 577;
        KRFreq[31][53] = 576;
        KRFreq[27][12] = 575;
        KRFreq[30][67] = 574;
        KRFreq[31][57] = 573;
        KRFreq[20][20] = 572;
        KRFreq[30][31] = 571;
        KRFreq[20][72] = 570;
        KRFreq[15][51] = 569;
        KRFreq[3][8] = 568;
        KRFreq[32][53] = 567;
        KRFreq[27][85] = 566;
        KRFreq[25][23] = 565;
        KRFreq[15][44] = 564;
        KRFreq[32][3] = 563;
        KRFreq[31][68] = 562;
        KRFreq[30][24] = 561;
        KRFreq[29][49] = 560;
        KRFreq[27][49] = 559;
        KRFreq[23][23] = 558;
        KRFreq[31][91] = 557;
        KRFreq[31][46] = 556;
        KRFreq[19][74] = 555;
        KRFreq[27][27] = 554;
        KRFreq[3][17] = 553;
        KRFreq[20][38] = 552;
        KRFreq[21][82] = 551;
        KRFreq[28][25] = 550;
        KRFreq[32][5] = 549;
        KRFreq[31][23] = 548;
        KRFreq[25][45] = 547;
        KRFreq[32][87] = 546;
        KRFreq[18][26] = 545;
        KRFreq[24][10] = 544;
        KRFreq[26][82] = 543;
        KRFreq[15][89] = 542;
        KRFreq[28][36] = 541;
        KRFreq[28][31] = 540;
        KRFreq[16][23] = 539;
        KRFreq[16][77] = 538;
        KRFreq[19][84] = 537;
        KRFreq[23][72] = 536;
        KRFreq[38][48] = 535;
        KRFreq[23][2] = 534;
        KRFreq[30][20] = 533;
        KRFreq[38][47] = 532;
        KRFreq[39][12] = 531;
        KRFreq[23][21] = 530;
        KRFreq[18][17] = 529;
        KRFreq[30][87] = 528;
        KRFreq[29][62] = 527;
        KRFreq[29][87] = 526;
        KRFreq[34][53] = 525;
        KRFreq[32][29] = 524;
        KRFreq[35][0] = 523;
        KRFreq[24][43] = 522;
        KRFreq[36][44] = 521;
        KRFreq[20][30] = 520;
        KRFreq[39][86] = 519;
        KRFreq[22][14] = 518;
        KRFreq[29][39] = 517;
        KRFreq[28][38] = 516;
        KRFreq[23][79] = 515;
        KRFreq[24][56] = 514;
        KRFreq[29][63] = 513;
        KRFreq[31][45] = 512;
        KRFreq[23][26] = 511;
        KRFreq[15][87] = 510;
        KRFreq[30][74] = 509;
        KRFreq[24][69] = 508;
        KRFreq[20][4] = 507;
        KRFreq[27][50] = 506;
        KRFreq[30][75] = 505;
        KRFreq[24][13] = 504;
        KRFreq[30][8] = 503;
        KRFreq[31][6] = 502;
        KRFreq[25][80] = 501;
        KRFreq[36][8] = 500;
        KRFreq[15][18] = 499;
        KRFreq[39][23] = 498;
        KRFreq[16][24] = 497;
        KRFreq[31][89] = 496;
        KRFreq[15][71] = 495;
        KRFreq[15][57] = 494;
        KRFreq[30][11] = 493;
        KRFreq[15][36] = 492;
        KRFreq[16][60] = 491;
        KRFreq[24][45] = 490;
        KRFreq[37][35] = 489;
        KRFreq[24][87] = 488;
        KRFreq[20][45] = 487;
        KRFreq[31][90] = 486;
        KRFreq[32][21] = 485;
        KRFreq[19][70] = 484;
        KRFreq[24][15] = 483;
        KRFreq[26][92] = 482;
        KRFreq[37][13] = 481;
        KRFreq[39][2] = 480;
        KRFreq[23][70] = 479;
        KRFreq[27][25] = 478;
        KRFreq[15][69] = 477;
        KRFreq[19][61] = 476;
        KRFreq[31][58] = 475;
        KRFreq[24][57] = 474;
        KRFreq[36][74] = 473;
        KRFreq[21][6] = 472;
        KRFreq[30][44] = 471;
        KRFreq[15][91] = 470;
        KRFreq[27][16] = 469;
        KRFreq[29][42] = 468;
        KRFreq[33][86] = 467;
        KRFreq[29][41] = 466;
        KRFreq[20][68] = 465;
        KRFreq[25][47] = 464;
        KRFreq[22][0] = 463;
        KRFreq[18][14] = 462;
        KRFreq[31][28] = 461;
        KRFreq[15][2] = 460;
        KRFreq[23][76] = 459;
        KRFreq[38][32] = 458;
        KRFreq[29][82] = 457;
        KRFreq[21][86] = 456;
        KRFreq[24][62] = 455;
        KRFreq[31][64] = 454;
        KRFreq[38][26] = 453;
        KRFreq[32][86] = 452;
        KRFreq[22][32] = 451;
        KRFreq[19][59] = 450;
        KRFreq[34][18] = 449;
        KRFreq[18][54] = 448;
        KRFreq[38][63] = 447;
        KRFreq[36][23] = 446;
        KRFreq[35][35] = 445;
        KRFreq[32][62] = 444;
        KRFreq[28][35] = 443;
        KRFreq[27][13] = 442;
        KRFreq[31][59] = 441;
        KRFreq[29][29] = 440;
        KRFreq[15][64] = 439;
        KRFreq[26][84] = 438;
        KRFreq[21][90] = 437;
        KRFreq[20][24] = 436;
        KRFreq[16][18] = 435;
        KRFreq[22][23] = 434;
        KRFreq[31][14] = 433;
        KRFreq[15][1] = 432;
        KRFreq[18][63] = 431;
        KRFreq[19][10] = 430;
        KRFreq[25][49] = 429;
        KRFreq[36][57] = 428;
        KRFreq[20][22] = 427;
        KRFreq[15][15] = 426;
        KRFreq[31][51] = 425;
        KRFreq[24][60] = 424;
        KRFreq[31][70] = 423;
        KRFreq[15][7] = 422;
        KRFreq[28][40] = 421;
        KRFreq[18][41] = 420;
        KRFreq[15][38] = 419;
        KRFreq[32][0] = 418;
        KRFreq[19][51] = 417;
        KRFreq[34][62] = 416;
        KRFreq[16][27] = 415;
        KRFreq[20][70] = 414;
        KRFreq[22][33] = 413;
        KRFreq[26][73] = 412;
        KRFreq[20][79] = 411;
        KRFreq[23][6] = 410;
        KRFreq[24][85] = 409;
        KRFreq[38][51] = 408;
        KRFreq[29][88] = 407;
        KRFreq[38][55] = 406;
        KRFreq[32][32] = 405;
        KRFreq[27][18] = 404;
        KRFreq[23][87] = 403;
        KRFreq[35][6] = 402;
        KRFreq[34][27] = 401;
        KRFreq[39][35] = 400;
        KRFreq[30][88] = 399;
        KRFreq[32][92] = 398;
        KRFreq[32][49] = 397;
        KRFreq[24][61] = 396;
        KRFreq[18][74] = 395;
        KRFreq[23][77] = 394;
        KRFreq[23][50] = 393;
        KRFreq[23][32] = 392;
        KRFreq[23][36] = 391;
        KRFreq[38][38] = 390;
        KRFreq[29][86] = 389;
        KRFreq[36][15] = 388;
        KRFreq[31][50] = 387;
        KRFreq[15][86] = 386;
        KRFreq[39][13] = 385;
        KRFreq[34][26] = 384;
        KRFreq[19][34] = 383;
        KRFreq[16][3] = 382;
        KRFreq[26][93] = 381;
        KRFreq[19][67] = 380;
        KRFreq[24][72] = 379;
        KRFreq[29][17] = 378;
        KRFreq[23][24] = 377;
        KRFreq[25][19] = 376;
        KRFreq[18][65] = 375;
        KRFreq[30][78] = 374;
        KRFreq[27][52] = 373;
        KRFreq[22][18] = 372;
        KRFreq[16][38] = 371;
        KRFreq[21][26] = 370;
        KRFreq[34][20] = 369;
        KRFreq[15][42] = 368;
        KRFreq[16][71] = 367;
        KRFreq[17][17] = 366;
        KRFreq[24][71] = 365;
        KRFreq[18][84] = 364;
        KRFreq[15][40] = 363;
        KRFreq[31][62] = 362;
        KRFreq[15][8] = 361;
        KRFreq[16][69] = 360;
        KRFreq[29][79] = 359;
        KRFreq[38][91] = 358;
        KRFreq[31][92] = 357;
        KRFreq[20][77] = 356;
        KRFreq[3][16] = 355;
        KRFreq[27][87] = 354;
        KRFreq[16][25] = 353;
        KRFreq[36][33] = 352;
        KRFreq[37][76] = 351;
        KRFreq[30][12] = 350;
        KRFreq[26][75] = 349;
        KRFreq[25][14] = 348;
        KRFreq[32][26] = 347;
        KRFreq[23][22] = 346;
        KRFreq[20][90] = 345;
        KRFreq[19][8] = 344;
        KRFreq[38][41] = 343;
        KRFreq[34][2] = 342;
        KRFreq[39][4] = 341;
        KRFreq[27][89] = 340;
        KRFreq[28][41] = 339;
        KRFreq[28][44] = 338;
        KRFreq[24][92] = 337;
        KRFreq[34][65] = 336;
        KRFreq[39][14] = 335;
        KRFreq[21][38] = 334;
        KRFreq[19][31] = 333;
        KRFreq[37][39] = 332;
        KRFreq[33][41] = 331;
        KRFreq[38][4] = 330;
        KRFreq[23][80] = 329;
        KRFreq[25][24] = 328;
        KRFreq[37][17] = 327;
        KRFreq[22][16] = 326;
        KRFreq[22][46] = 325;
        KRFreq[33][91] = 324;
        KRFreq[24][89] = 323;
        KRFreq[30][52] = 322;
        KRFreq[29][38] = 321;
        KRFreq[38][85] = 320;
        KRFreq[15][12] = 319;
        KRFreq[27][58] = 318;
        KRFreq[29][52] = 317;
        KRFreq[37][38] = 316;
        KRFreq[34][41] = 315;
        KRFreq[31][65] = 314;
        KRFreq[29][53] = 313;
        KRFreq[22][47] = 312;
        KRFreq[22][19] = 311;
        KRFreq[26][0] = 310;
        KRFreq[37][86] = 309;
        KRFreq[35][4] = 308;
        KRFreq[36][54] = 307;
        KRFreq[20][76] = 306;
        KRFreq[30][9] = 305;
        KRFreq[30][33] = 304;
        KRFreq[23][17] = 303;
        KRFreq[23][33] = 302;
        KRFreq[38][52] = 301;
        KRFreq[15][19] = 300;
        KRFreq[28][45] = 299;
        KRFreq[29][78] = 298;
        KRFreq[23][15] = 297;
        KRFreq[33][5] = 296;
        KRFreq[17][40] = 295;
        KRFreq[30][83] = 294;
        KRFreq[18][1] = 293;
        KRFreq[30][81] = 292;
        KRFreq[19][40] = 291;
        KRFreq[24][47] = 290;
        KRFreq[17][56] = 289;
        KRFreq[39][80] = 288;
        KRFreq[30][46] = 287;
        KRFreq[16][61] = 286;
        KRFreq[26][78] = 285;
        KRFreq[26][57] = 284;
        KRFreq[20][46] = 283;
        KRFreq[25][15] = 282;
        KRFreq[25][91] = 281;
        KRFreq[21][83] = 280;
        KRFreq[30][77] = 279;
        KRFreq[35][30] = 278;
        KRFreq[30][34] = 277;
        KRFreq[20][69] = 276;
        KRFreq[35][10] = 275;
        KRFreq[29][70] = 274;
        KRFreq[22][50] = 273;
        KRFreq[18][0] = 272;
        KRFreq[22][64] = 271;
        KRFreq[38][65] = 270;
        KRFreq[22][70] = 269;
        KRFreq[24][58] = 268;
        KRFreq[19][66] = 267;
        KRFreq[30][59] = 266;
        KRFreq[37][14] = 265;
        KRFreq[16][56] = 264;
        KRFreq[29][85] = 263;
        KRFreq[31][15] = 262;
        KRFreq[36][84] = 261;
        KRFreq[39][15] = 260;
        KRFreq[39][90] = 259;
        KRFreq[18][12] = 258;
        KRFreq[21][93] = 257;
        KRFreq[24][66] = 256;
        KRFreq[27][90] = 255;
        KRFreq[25][90] = 254;
        KRFreq[22][24] = 253;
        KRFreq[36][67] = 252;
        KRFreq[33][90] = 251;
        KRFreq[15][60] = 250;
        KRFreq[23][85] = 249;
        KRFreq[34][1] = 248;
        KRFreq[39][37] = 247;
        KRFreq[21][18] = 246;
        KRFreq[34][4] = 245;
        KRFreq[28][33] = 244;
        KRFreq[15][13] = 243;
        KRFreq[32][22] = 242;
        KRFreq[30][76] = 241;
        KRFreq[20][21] = 240;
        KRFreq[38][66] = 239;
        KRFreq[32][55] = 238;
        KRFreq[32][89] = 237;
        KRFreq[25][26] = 236;
        KRFreq[16][80] = 235;
        KRFreq[15][43] = 234;
        KRFreq[38][54] = 233;
        KRFreq[39][68] = 232;
        KRFreq[22][88] = 231;
        KRFreq[21][84] = 230;
        KRFreq[21][17] = 229;
        KRFreq[20][28] = 228;
        KRFreq[32][1] = 227;
        KRFreq[33][87] = 226;
        KRFreq[38][71] = 225;
        KRFreq[37][47] = 224;
        KRFreq[18][77] = 223;
        KRFreq[37][58] = 222;
        KRFreq[34][74] = 221;
        KRFreq[32][54] = 220;
        KRFreq[27][33] = 219;
        KRFreq[32][93] = 218;
        KRFreq[23][51] = 217;
        KRFreq[20][57] = 216;
        KRFreq[22][37] = 215;
        KRFreq[39][10] = 214;
        KRFreq[39][17] = 213;
        KRFreq[33][4] = 212;
        KRFreq[32][84] = 211;
        KRFreq[34][3] = 210;
        KRFreq[28][27] = 209;
        KRFreq[15][79] = 208;
        KRFreq[34][21] = 207;
        KRFreq[34][69] = 206;
        KRFreq[21][62] = 205;
        KRFreq[36][24] = 204;
        KRFreq[16][89] = 203;
        KRFreq[18][48] = 202;
        KRFreq[38][15] = 201;
        KRFreq[36][58] = 200;
        KRFreq[21][56] = 199;
        KRFreq[34][48] = 198;
        KRFreq[21][15] = 197;
        KRFreq[39][3] = 196;
        KRFreq[16][44] = 195;
        KRFreq[18][79] = 194;
        KRFreq[25][13] = 193;
        KRFreq[29][47] = 192;
        KRFreq[38][88] = 191;
        KRFreq[20][71] = 190;
        KRFreq[16][58] = 189;
        KRFreq[35][57] = 188;
        KRFreq[29][30] = 187;
        KRFreq[29][23] = 186;
        KRFreq[34][93] = 185;
        KRFreq[30][85] = 184;
        KRFreq[15][80] = 183;
        KRFreq[32][78] = 182;
        KRFreq[37][82] = 181;
        KRFreq[22][40] = 180;
        KRFreq[21][69] = 179;
        KRFreq[26][85] = 178;
        KRFreq[31][31] = 177;
        KRFreq[28][64] = 176;
        KRFreq[38][13] = 175;
        KRFreq[25][2] = 174;
        KRFreq[22][34] = 173;
        KRFreq[28][28] = 172;
        KRFreq[24][91] = 171;
        KRFreq[33][74] = 170;
        KRFreq[29][40] = 169;
        KRFreq[15][77] = 168;
        KRFreq[32][80] = 167;
        KRFreq[30][41] = 166;
        KRFreq[23][30] = 165;
        KRFreq[24][63] = 164;
        KRFreq[30][53] = 163;
        KRFreq[39][70] = 162;
        KRFreq[23][61] = 161;
        KRFreq[37][27] = 160;
        KRFreq[16][55] = 159;
        KRFreq[22][74] = 158;
        KRFreq[26][50] = 157;
        KRFreq[16][10] = 156;
        KRFreq[34][63] = 155;
        KRFreq[35][14] = 154;
        KRFreq[17][7] = 153;
        KRFreq[15][59] = 152;
        KRFreq[27][23] = 151;
        KRFreq[18][70] = 150;
        KRFreq[32][56] = 149;
        KRFreq[37][87] = 148;
        KRFreq[17][61] = 147;
        KRFreq[18][83] = 146;
        KRFreq[23][86] = 145;
        KRFreq[17][31] = 144;
        KRFreq[23][83] = 143;
        KRFreq[35][2] = 142;
        KRFreq[18][64] = 141;
        KRFreq[27][43] = 140;
        KRFreq[32][42] = 139;
        KRFreq[25][76] = 138;
        KRFreq[19][85] = 137;
        KRFreq[37][81] = 136;
        KRFreq[38][83] = 135;
        KRFreq[35][7] = 134;
        KRFreq[16][51] = 133;
        KRFreq[27][22] = 132;
        KRFreq[16][76] = 131;
        KRFreq[22][4] = 130;
        KRFreq[38][84] = 129;
        KRFreq[17][83] = 128;
        KRFreq[24][46] = 127;
        KRFreq[33][15] = 126;
        KRFreq[20][48] = 125;
        KRFreq[17][30] = 124;
        KRFreq[30][93] = 123;
        KRFreq[28][11] = 122;
        KRFreq[28][30] = 121;
        KRFreq[15][62] = 120;
        KRFreq[17][87] = 119;
        KRFreq[32][81] = 118;
        KRFreq[23][37] = 117;
        KRFreq[30][22] = 116;
        KRFreq[32][66] = 115;
        KRFreq[33][78] = 114;
        KRFreq[21][4] = 113;
        KRFreq[31][17] = 112;
        KRFreq[39][61] = 111;
        KRFreq[18][76] = 110;
        KRFreq[15][85] = 109;
        KRFreq[31][47] = 108;
        KRFreq[19][57] = 107;
        KRFreq[23][55] = 106;
        KRFreq[27][29] = 105;
        KRFreq[29][46] = 104;
        KRFreq[33][0] = 103;
        KRFreq[16][83] = 102;
        KRFreq[39][78] = 101;
        KRFreq[32][77] = 100;
        KRFreq[36][25] = 99;
        KRFreq[34][19] = 98;
        KRFreq[38][49] = 97;
        KRFreq[19][25] = 96;
        KRFreq[23][53] = 95;
        KRFreq[28][43] = 94;
        KRFreq[31][44] = 93;
        KRFreq[36][34] = 92;
        KRFreq[16][34] = 91;
        KRFreq[35][1] = 90;
        KRFreq[19][87] = 89;
        KRFreq[18][53] = 88;
        KRFreq[29][54] = 87;
        KRFreq[22][41] = 86;
        KRFreq[38][18] = 85;
        KRFreq[22][2] = 84;
        KRFreq[20][3] = 83;
        KRFreq[39][69] = 82;
        KRFreq[30][29] = 81;
        KRFreq[28][19] = 80;
        KRFreq[29][90] = 79;
        KRFreq[17][86] = 78;
        KRFreq[15][9] = 77;
        KRFreq[39][73] = 76;
        KRFreq[15][37] = 75;
        KRFreq[35][40] = 74;
        KRFreq[33][77] = 73;
        KRFreq[27][86] = 72;
        KRFreq[36][79] = 71;
        KRFreq[23][18] = 70;
        KRFreq[34][87] = 69;
        KRFreq[39][24] = 68;
        KRFreq[26][8] = 67;
        KRFreq[33][48] = 66;
        KRFreq[39][30] = 65;
        KRFreq[33][28] = 64;
        KRFreq[16][67] = 63;
        KRFreq[31][78] = 62;
        KRFreq[32][23] = 61;
        KRFreq[24][55] = 60;
        KRFreq[30][68] = 59;
        KRFreq[18][60] = 58;
        KRFreq[15][17] = 57;
        KRFreq[23][34] = 56;
        KRFreq[20][49] = 55;
        KRFreq[15][78] = 54;
        KRFreq[24][14] = 53;
        KRFreq[19][41] = 52;
        KRFreq[31][55] = 51;
        KRFreq[21][39] = 50;
        KRFreq[35][9] = 49;
        KRFreq[30][15] = 48;
        KRFreq[20][52] = 47;
        KRFreq[35][71] = 46;
        KRFreq[20][7] = 45;
        KRFreq[29][72] = 44;
        KRFreq[37][77] = 43;
        KRFreq[22][35] = 42;
        KRFreq[20][61] = 41;
        KRFreq[31][60] = 40;
        KRFreq[20][93] = 39;
        KRFreq[27][92] = 38;
        KRFreq[28][16] = 37;
        KRFreq[36][26] = 36;
        KRFreq[18][89] = 35;
        KRFreq[21][63] = 34;
        KRFreq[22][52] = 33;
        KRFreq[24][65] = 32;
        KRFreq[31][8] = 31;
        KRFreq[31][49] = 30;
        KRFreq[33][30] = 29;
        KRFreq[37][15] = 28;
        KRFreq[18][18] = 27;
        KRFreq[25][50] = 26;
        KRFreq[29][20] = 25;
        KRFreq[35][48] = 24;
        KRFreq[38][75] = 23;
        KRFreq[26][83] = 22;
        KRFreq[21][87] = 21;
        KRFreq[27][71] = 20;
        KRFreq[32][91] = 19;
        KRFreq[25][73] = 18;
        KRFreq[16][84] = 17;
        KRFreq[25][31] = 16;
        KRFreq[17][90] = 15;
        KRFreq[18][40] = 14;
        KRFreq[17][77] = 13;
        KRFreq[17][35] = 12;
        KRFreq[23][52] = 11;
        KRFreq[23][35] = 10;
        KRFreq[16][5] = 9;
        KRFreq[23][58] = 8;
        KRFreq[19][60] = 7;
        KRFreq[30][32] = 6;
        KRFreq[38][34] = 5;
        KRFreq[23][4] = 4;
        KRFreq[23][1] = 3;
        KRFreq[27][57] = 2;
        KRFreq[39][38] = 1;
        KRFreq[32][33] = 0;
        JPFreq[3][74] = 600;
        JPFreq[3][45] = 599;
        JPFreq[3][3] = 598;
        JPFreq[3][24] = 597;
        JPFreq[3][30] = 596;
        JPFreq[3][42] = 595;
        JPFreq[3][46] = 594;
        JPFreq[3][39] = 593;
        JPFreq[3][11] = 592;
        JPFreq[3][37] = 591;
        JPFreq[3][38] = 590;
        JPFreq[3][31] = 589;
        JPFreq[3][41] = 588;
        JPFreq[3][5] = 587;
        JPFreq[3][10] = 586;
        JPFreq[3][75] = 585;
        JPFreq[3][65] = 584;
        JPFreq[3][72] = 583;
        JPFreq[37][91] = 582;
        JPFreq[0][27] = 581;
        JPFreq[3][18] = 580;
        JPFreq[3][22] = 579;
        JPFreq[3][61] = 578;
        JPFreq[3][14] = 577;
        JPFreq[24][80] = 576;
        JPFreq[4][82] = 575;
        JPFreq[17][80] = 574;
        JPFreq[30][44] = 573;
        JPFreq[3][73] = 572;
        JPFreq[3][64] = 571;
        JPFreq[38][14] = 570;
        JPFreq[33][70] = 569;
        JPFreq[3][1] = 568;
        JPFreq[3][16] = 567;
        JPFreq[3][35] = 566;
        JPFreq[3][40] = 565;
        JPFreq[4][74] = 564;
        JPFreq[4][24] = 563;
        JPFreq[42][59] = 562;
        JPFreq[3][7] = 561;
        JPFreq[3][71] = 560;
        JPFreq[3][12] = 559;
        JPFreq[15][75] = 558;
        JPFreq[3][20] = 557;
        JPFreq[4][39] = 556;
        JPFreq[34][69] = 555;
        JPFreq[3][28] = 554;
        JPFreq[35][24] = 553;
        JPFreq[3][82] = 552;
        JPFreq[28][47] = 551;
        JPFreq[3][67] = 550;
        JPFreq[37][16] = 549;
        JPFreq[26][93] = 548;
        JPFreq[4][1] = 547;
        JPFreq[26][85] = 546;
        JPFreq[31][14] = 545;
        JPFreq[4][3] = 544;
        JPFreq[4][72] = 543;
        JPFreq[24][51] = 542;
        JPFreq[27][51] = 541;
        JPFreq[27][49] = 540;
        JPFreq[22][77] = 539;
        JPFreq[27][10] = 538;
        JPFreq[29][68] = 537;
        JPFreq[20][35] = 536;
        JPFreq[41][11] = 535;
        JPFreq[24][70] = 534;
        JPFreq[36][61] = 533;
        JPFreq[31][23] = 532;
        JPFreq[43][16] = 531;
        JPFreq[23][68] = 530;
        JPFreq[32][15] = 529;
        JPFreq[3][32] = 528;
        JPFreq[19][53] = 527;
        JPFreq[40][83] = 526;
        JPFreq[4][14] = 525;
        JPFreq[36][9] = 524;
        JPFreq[4][73] = 523;
        JPFreq[23][10] = 522;
        JPFreq[3][63] = 521;
        JPFreq[39][14] = 520;
        JPFreq[3][78] = 519;
        JPFreq[33][47] = 518;
        JPFreq[21][39] = 517;
        JPFreq[34][46] = 516;
        JPFreq[36][75] = 515;
        JPFreq[41][92] = 514;
        JPFreq[37][93] = 513;
        JPFreq[4][34] = 512;
        JPFreq[15][86] = 511;
        JPFreq[46][1] = 510;
        JPFreq[37][65] = 509;
        JPFreq[3][62] = 508;
        JPFreq[32][73] = 507;
        JPFreq[21][65] = 506;
        JPFreq[29][75] = 505;
        JPFreq[26][51] = 504;
        JPFreq[3][34] = 503;
        JPFreq[4][10] = 502;
        JPFreq[30][22] = 501;
        JPFreq[35][73] = 500;
        JPFreq[17][82] = 499;
        JPFreq[45][8] = 498;
        JPFreq[27][73] = 497;
        JPFreq[18][55] = 496;
        JPFreq[25][2] = 495;
        JPFreq[3][26] = 494;
        JPFreq[45][46] = 493;
        JPFreq[4][22] = 492;
        JPFreq[4][40] = 491;
        JPFreq[18][10] = 490;
        JPFreq[32][9] = 489;
        JPFreq[26][49] = 488;
        JPFreq[3][47] = 487;
        JPFreq[24][65] = 486;
        JPFreq[4][76] = 485;
        JPFreq[43][67] = 484;
        JPFreq[3][9] = 483;
        JPFreq[41][37] = 482;
        JPFreq[33][68] = 481;
        JPFreq[43][31] = 480;
        JPFreq[19][55] = 479;
        JPFreq[4][30] = 478;
        JPFreq[27][33] = 477;
        JPFreq[16][62] = 476;
        JPFreq[36][35] = 475;
        JPFreq[37][15] = 474;
        JPFreq[27][70] = 473;
        JPFreq[22][71] = 472;
        JPFreq[33][45] = 471;
        JPFreq[31][78] = 470;
        JPFreq[43][59] = 469;
        JPFreq[32][19] = 468;
        JPFreq[17][28] = 467;
        JPFreq[40][28] = 466;
        JPFreq[20][93] = 465;
        JPFreq[18][15] = 464;
        JPFreq[4][23] = 463;
        JPFreq[3][23] = 462;
        JPFreq[26][64] = 461;
        JPFreq[44][92] = 460;
        JPFreq[17][27] = 459;
        JPFreq[3][56] = 458;
        JPFreq[25][38] = 457;
        JPFreq[23][31] = 456;
        JPFreq[35][43] = 455;
        JPFreq[4][54] = 454;
        JPFreq[35][19] = 453;
        JPFreq[22][47] = 452;
        JPFreq[42][0] = 451;
        JPFreq[23][28] = 450;
        JPFreq[46][33] = 449;
        JPFreq[36][85] = 448;
        JPFreq[31][12] = 447;
        JPFreq[3][76] = 446;
        JPFreq[4][75] = 445;
        JPFreq[36][56] = 444;
        JPFreq[4][64] = 443;
        JPFreq[25][77] = 442;
        JPFreq[15][52] = 441;
        JPFreq[33][73] = 440;
        JPFreq[3][55] = 439;
        JPFreq[43][82] = 438;
        JPFreq[27][82] = 437;
        JPFreq[20][3] = 436;
        JPFreq[40][51] = 435;
        JPFreq[3][17] = 434;
        JPFreq[27][71] = 433;
        JPFreq[4][52] = 432;
        JPFreq[44][48] = 431;
        JPFreq[27][2] = 430;
        JPFreq[17][39] = 429;
        JPFreq[31][8] = 428;
        JPFreq[44][54] = 427;
        JPFreq[43][18] = 426;
        JPFreq[43][77] = 425;
        JPFreq[4][61] = 424;
        JPFreq[19][91] = 423;
        JPFreq[31][13] = 422;
        JPFreq[44][71] = 421;
        JPFreq[20][0] = 420;
        JPFreq[23][87] = 419;
        JPFreq[21][14] = 418;
        JPFreq[29][13] = 417;
        JPFreq[3][58] = 416;
        JPFreq[26][18] = 415;
        JPFreq[4][47] = 414;
        JPFreq[4][18] = 413;
        JPFreq[3][53] = 412;
        JPFreq[26][92] = 411;
        JPFreq[21][7] = 410;
        JPFreq[4][37] = 409;
        JPFreq[4][63] = 408;
        JPFreq[36][51] = 407;
        JPFreq[4][32] = 406;
        JPFreq[28][73] = 405;
        JPFreq[4][50] = 404;
        JPFreq[41][60] = 403;
        JPFreq[23][1] = 402;
        JPFreq[36][92] = 401;
        JPFreq[15][41] = 400;
        JPFreq[21][71] = 399;
        JPFreq[41][30] = 398;
        JPFreq[32][76] = 397;
        JPFreq[17][34] = 396;
        JPFreq[26][15] = 395;
        JPFreq[26][25] = 394;
        JPFreq[31][77] = 393;
        JPFreq[31][3] = 392;
        JPFreq[46][34] = 391;
        JPFreq[27][84] = 390;
        JPFreq[23][8] = 389;
        JPFreq[16][0] = 388;
        JPFreq[28][80] = 387;
        JPFreq[26][54] = 386;
        JPFreq[33][18] = 385;
        JPFreq[31][20] = 384;
        JPFreq[31][62] = 383;
        JPFreq[30][41] = 382;
        JPFreq[33][30] = 381;
        JPFreq[45][45] = 380;
        JPFreq[37][82] = 379;
        JPFreq[15][33] = 378;
        JPFreq[20][12] = 377;
        JPFreq[18][5] = 376;
        JPFreq[28][86] = 375;
        JPFreq[30][19] = 374;
        JPFreq[42][43] = 373;
        JPFreq[36][31] = 372;
        JPFreq[17][93] = 371;
        JPFreq[4][15] = 370;
        JPFreq[21][20] = 369;
        JPFreq[23][21] = 368;
        JPFreq[28][72] = 367;
        JPFreq[4][20] = 366;
        JPFreq[26][55] = 365;
        JPFreq[21][5] = 364;
        JPFreq[19][16] = 363;
        JPFreq[23][64] = 362;
        JPFreq[40][59] = 361;
        JPFreq[37][26] = 360;
        JPFreq[26][56] = 359;
        JPFreq[4][12] = 358;
        JPFreq[33][71] = 357;
        JPFreq[32][39] = 356;
        JPFreq[38][40] = 355;
        JPFreq[22][74] = 354;
        JPFreq[3][25] = 353;
        JPFreq[15][48] = 352;
        JPFreq[41][82] = 351;
        JPFreq[41][9] = 350;
        JPFreq[25][48] = 349;
        JPFreq[31][71] = 348;
        JPFreq[43][29] = 347;
        JPFreq[26][80] = 346;
        JPFreq[4][5] = 345;
        JPFreq[18][71] = 344;
        JPFreq[29][0] = 343;
        JPFreq[43][43] = 342;
        JPFreq[23][81] = 341;
        JPFreq[4][42] = 340;
        JPFreq[44][28] = 339;
        JPFreq[23][93] = 338;
        JPFreq[17][81] = 337;
        JPFreq[25][25] = 336;
        JPFreq[41][23] = 335;
        JPFreq[34][35] = 334;
        JPFreq[4][53] = 333;
        JPFreq[28][36] = 332;
        JPFreq[4][41] = 331;
        JPFreq[25][60] = 330;
        JPFreq[23][20] = 329;
        JPFreq[3][43] = 328;
        JPFreq[24][79] = 327;
        JPFreq[29][41] = 326;
        JPFreq[30][83] = 325;
        JPFreq[3][50] = 324;
        JPFreq[22][18] = 323;
        JPFreq[18][3] = 322;
        JPFreq[39][30] = 321;
        JPFreq[4][28] = 320;
        JPFreq[21][64] = 319;
        JPFreq[4][68] = 318;
        JPFreq[17][71] = 317;
        JPFreq[27][0] = 316;
        JPFreq[39][28] = 315;
        JPFreq[30][13] = 314;
        JPFreq[36][70] = 313;
        JPFreq[20][82] = 312;
        JPFreq[33][38] = 311;
        JPFreq[44][87] = 310;
        JPFreq[34][45] = 309;
        JPFreq[4][26] = 308;
        JPFreq[24][44] = 307;
        JPFreq[38][67] = 306;
        JPFreq[38][6] = 305;
        JPFreq[30][68] = 304;
        JPFreq[15][89] = 303;
        JPFreq[24][93] = 302;
        JPFreq[40][41] = 301;
        JPFreq[38][3] = 300;
        JPFreq[28][23] = 299;
        JPFreq[26][17] = 298;
        JPFreq[4][38] = 297;
        JPFreq[22][78] = 296;
        JPFreq[15][37] = 295;
        JPFreq[25][85] = 294;
        JPFreq[4][9] = 293;
        JPFreq[4][7] = 292;
        JPFreq[27][53] = 291;
        JPFreq[39][29] = 290;
        JPFreq[41][43] = 289;
        JPFreq[25][62] = 288;
        JPFreq[4][48] = 287;
        JPFreq[28][28] = 286;
        JPFreq[21][40] = 285;
        JPFreq[36][73] = 284;
        JPFreq[26][39] = 283;
        JPFreq[22][54] = 282;
        JPFreq[33][5] = 281;
        JPFreq[19][21] = 280;
        JPFreq[46][31] = 279;
        JPFreq[20][64] = 278;
        JPFreq[26][63] = 277;
        JPFreq[22][23] = 276;
        JPFreq[25][81] = 275;
        JPFreq[4][62] = 274;
        JPFreq[37][31] = 273;
        JPFreq[40][52] = 272;
        JPFreq[29][79] = 271;
        JPFreq[41][48] = 270;
        JPFreq[31][57] = 269;
        JPFreq[32][92] = 268;
        JPFreq[36][36] = 267;
        JPFreq[27][7] = 266;
        JPFreq[35][29] = 265;
        JPFreq[37][34] = 264;
        JPFreq[34][42] = 263;
        JPFreq[27][15] = 262;
        JPFreq[33][27] = 261;
        JPFreq[31][38] = 260;
        JPFreq[19][79] = 259;
        JPFreq[4][31] = 258;
        JPFreq[4][66] = 257;
        JPFreq[17][32] = 256;
        JPFreq[26][67] = 255;
        JPFreq[16][30] = 254;
        JPFreq[26][46] = 253;
        JPFreq[24][26] = 252;
        JPFreq[35][10] = 251;
        JPFreq[18][37] = 250;
        JPFreq[3][19] = 249;
        JPFreq[33][69] = 248;
        JPFreq[31][9] = 247;
        JPFreq[45][29] = 246;
        JPFreq[3][15] = 245;
        JPFreq[18][54] = 244;
        JPFreq[3][44] = 243;
        JPFreq[31][29] = 242;
        JPFreq[18][45] = 241;
        JPFreq[38][28] = 240;
        JPFreq[24][12] = 239;
        JPFreq[35][82] = 238;
        JPFreq[17][43] = 237;
        JPFreq[28][9] = 236;
        JPFreq[23][25] = 235;
        JPFreq[44][37] = 234;
        JPFreq[23][75] = 233;
        JPFreq[23][92] = 232;
        JPFreq[0][24] = 231;
        JPFreq[19][74] = 230;
        JPFreq[45][32] = 229;
        JPFreq[16][72] = 228;
        JPFreq[16][93] = 227;
        JPFreq[45][13] = 226;
        JPFreq[24][8] = 225;
        JPFreq[25][47] = 224;
        JPFreq[28][26] = 223;
        JPFreq[43][81] = 222;
        JPFreq[32][71] = 221;
        JPFreq[18][41] = 220;
        JPFreq[26][62] = 219;
        JPFreq[41][24] = 218;
        JPFreq[40][11] = 217;
        JPFreq[43][57] = 216;
        JPFreq[34][53] = 215;
        JPFreq[20][32] = 214;
        JPFreq[34][43] = 213;
        JPFreq[41][91] = 212;
        JPFreq[29][57] = 211;
        JPFreq[15][43] = 210;
        JPFreq[22][89] = 209;
        JPFreq[33][83] = 208;
        JPFreq[43][20] = 207;
        JPFreq[25][58] = 206;
        JPFreq[30][30] = 205;
        JPFreq[4][56] = 204;
        JPFreq[17][64] = 203;
        JPFreq[23][0] = 202;
        JPFreq[44][12] = 201;
        JPFreq[25][37] = 200;
        JPFreq[35][13] = 199;
        JPFreq[20][30] = 198;
        JPFreq[21][84] = 197;
        JPFreq[29][14] = 196;
        JPFreq[30][5] = 195;
        JPFreq[37][2] = 194;
        JPFreq[4][78] = 193;
        JPFreq[29][78] = 192;
        JPFreq[29][84] = 191;
        JPFreq[32][86] = 190;
        JPFreq[20][68] = 189;
        JPFreq[30][39] = 188;
        JPFreq[15][69] = 187;
        JPFreq[4][60] = 186;
        JPFreq[20][61] = 185;
        JPFreq[41][67] = 184;
        JPFreq[16][35] = 183;
        JPFreq[36][57] = 182;
        JPFreq[39][80] = 181;
        JPFreq[4][59] = 180;
        JPFreq[4][44] = 179;
        JPFreq[40][54] = 178;
        JPFreq[30][8] = 177;
        JPFreq[44][30] = 176;
        JPFreq[31][93] = 175;
        JPFreq[31][47] = 174;
        JPFreq[16][70] = 173;
        JPFreq[21][0] = 172;
        JPFreq[17][35] = 171;
        JPFreq[21][67] = 170;
        JPFreq[44][18] = 169;
        JPFreq[36][29] = 168;
        JPFreq[18][67] = 167;
        JPFreq[24][28] = 166;
        JPFreq[36][24] = 165;
        JPFreq[23][5] = 164;
        JPFreq[31][65] = 163;
        JPFreq[26][59] = 162;
        JPFreq[28][2] = 161;
        JPFreq[39][69] = 160;
        JPFreq[42][40] = 159;
        JPFreq[37][80] = 158;
        JPFreq[15][66] = 157;
        JPFreq[34][38] = 156;
        JPFreq[28][48] = 155;
        JPFreq[37][77] = 154;
        JPFreq[29][34] = 153;
        JPFreq[33][12] = 152;
        JPFreq[4][65] = 151;
        JPFreq[30][31] = 150;
        JPFreq[27][92] = 149;
        JPFreq[4][2] = 148;
        JPFreq[4][51] = 147;
        JPFreq[23][77] = 146;
        JPFreq[4][35] = 145;
        JPFreq[3][13] = 144;
        JPFreq[26][26] = 143;
        JPFreq[44][4] = 142;
        JPFreq[39][53] = 141;
        JPFreq[20][11] = 140;
        JPFreq[40][33] = 139;
        JPFreq[45][7] = 138;
        JPFreq[4][70] = 137;
        JPFreq[3][49] = 136;
        JPFreq[20][59] = 135;
        JPFreq[21][12] = 134;
        JPFreq[33][53] = 133;
        JPFreq[20][14] = 132;
        JPFreq[37][18] = 131;
        JPFreq[18][17] = 130;
        JPFreq[36][23] = 129;
        JPFreq[18][57] = 128;
        JPFreq[26][74] = 127;
        JPFreq[35][2] = 126;
        JPFreq[38][58] = 125;
        JPFreq[34][68] = 124;
        JPFreq[29][81] = 123;
        JPFreq[20][69] = 122;
        JPFreq[39][86] = 121;
        JPFreq[4][16] = 120;
        JPFreq[16][49] = 119;
        JPFreq[15][72] = 118;
        JPFreq[26][35] = 117;
        JPFreq[32][14] = 116;
        JPFreq[40][90] = 115;
        JPFreq[33][79] = 114;
        JPFreq[35][4] = 113;
        JPFreq[23][33] = 112;
        JPFreq[19][19] = 111;
        JPFreq[31][41] = 110;
        JPFreq[44][1] = 109;
        JPFreq[22][56] = 108;
        JPFreq[31][27] = 107;
        JPFreq[32][18] = 106;
        JPFreq[27][32] = 105;
        JPFreq[37][39] = 104;
        JPFreq[42][11] = 103;
        JPFreq[29][71] = 102;
        JPFreq[32][58] = 101;
        JPFreq[46][10] = 100;
        JPFreq[17][30] = 99;
        JPFreq[38][15] = 98;
        JPFreq[29][60] = 97;
        JPFreq[4][11] = 96;
        JPFreq[38][31] = 95;
        JPFreq[40][79] = 94;
        JPFreq[28][49] = 93;
        JPFreq[28][84] = 92;
        JPFreq[26][77] = 91;
        JPFreq[22][32] = 90;
        JPFreq[33][17] = 89;
        JPFreq[23][18] = 88;
        JPFreq[32][64] = 87;
        JPFreq[4][6] = 86;
        JPFreq[33][51] = 85;
        JPFreq[44][77] = 84;
        JPFreq[29][5] = 83;
        JPFreq[46][25] = 82;
        JPFreq[19][58] = 81;
        JPFreq[4][46] = 80;
        JPFreq[15][71] = 79;
        JPFreq[18][58] = 78;
        JPFreq[26][45] = 77;
        JPFreq[45][66] = 76;
        JPFreq[34][10] = 75;
        JPFreq[19][37] = 74;
        JPFreq[33][65] = 73;
        JPFreq[44][52] = 72;
        JPFreq[16][38] = 71;
        JPFreq[36][46] = 70;
        JPFreq[20][26] = 69;
        JPFreq[30][37] = 68;
        JPFreq[4][58] = 67;
        JPFreq[43][2] = 66;
        JPFreq[30][18] = 65;
        JPFreq[19][35] = 64;
        JPFreq[15][68] = 63;
        JPFreq[3][36] = 62;
        JPFreq[35][40] = 61;
        JPFreq[36][32] = 60;
        JPFreq[37][14] = 59;
        JPFreq[17][11] = 58;
        JPFreq[19][78] = 57;
        JPFreq[37][11] = 56;
        JPFreq[28][63] = 55;
        JPFreq[29][61] = 54;
        JPFreq[33][3] = 53;
        JPFreq[41][52] = 52;
        JPFreq[33][63] = 51;
        JPFreq[22][41] = 50;
        JPFreq[4][19] = 49;
        JPFreq[32][41] = 48;
        JPFreq[24][4] = 47;
        JPFreq[31][28] = 46;
        JPFreq[43][30] = 45;
        JPFreq[17][3] = 44;
        JPFreq[43][70] = 43;
        JPFreq[34][19] = 42;
        JPFreq[20][77] = 41;
        JPFreq[18][83] = 40;
        JPFreq[17][15] = 39;
        JPFreq[23][61] = 38;
        JPFreq[40][27] = 37;
        JPFreq[16][48] = 36;
        JPFreq[39][78] = 35;
        JPFreq[41][53] = 34;
        JPFreq[40][91] = 33;
        JPFreq[40][72] = 32;
        JPFreq[18][52] = 31;
        JPFreq[35][66] = 30;
        JPFreq[39][93] = 29;
        JPFreq[19][48] = 28;
        JPFreq[26][36] = 27;
        JPFreq[27][25] = 26;
        JPFreq[42][71] = 25;
        JPFreq[42][85] = 24;
        JPFreq[26][48] = 23;
        JPFreq[28][15] = 22;
        JPFreq[3][66] = 21;
        JPFreq[25][24] = 20;
        JPFreq[27][43] = 19;
        JPFreq[27][78] = 18;
        JPFreq[45][43] = 17;
        JPFreq[27][72] = 16;
        JPFreq[40][29] = 15;
        JPFreq[41][0] = 14;
        JPFreq[19][57] = 13;
        JPFreq[15][59] = 12;
        JPFreq[29][29] = 11;
        JPFreq[4][25] = 10;
        JPFreq[21][42] = 9;
        JPFreq[23][35] = 8;
        JPFreq[33][1] = 7;
        JPFreq[4][57] = 6;
        JPFreq[17][60] = 5;
        JPFreq[25][19] = 4;
        JPFreq[22][65] = 3;
        JPFreq[42][29] = 2;
        JPFreq[27][66] = 1;
        JPFreq[26][89] = 0;
    }
}

@SuppressWarnings("ALL")
class Encoding {
    // Supported Encoding Types
    static int GB2312 = 0;
    static int GBK = 1;
    static int GB18030 = 2;
    static int HZ = 3;
    static int BIG5 = 4;
    static int CNS11643 = 5;
    static int UTF8 = 6;
    static int UTF8T = 7;
    static int UTF8S = 8;
    static int UNICODE = 9;
    static int UNICODET = 10;
    static int UNICODES = 11;
    static int ISO2022CN = 12;
    static int ISO2022CN_CNS = 13;
    static int ISO2022CN_GB = 14;
    static int EUC_KR = 15;
    static int CP949 = 16;
    static int ISO2022KR = 17;
    static int JOHAB = 18;
    static int SJIS = 19;
    static int EUC_JP = 20;
    static int ISO2022JP = 21;
    static int ASCII = 22;
    static int OTHER = 23;
    static int TOTALTYPES = 24;

    public final static int SIMP = 0;

    public final static int TRAD = 1;

    // Names of the encodings as understood by Java
    static String[] javaname;

    // Names of the encodings for human viewing
    static String[] nicename;

    // Names of charsets as used in charset parameter of HTML Meta tag
    static String[] htmlname;

    // Constructor
    Encoding() {
        javaname = new String[TOTALTYPES];
        nicename = new String[TOTALTYPES];
        htmlname = new String[TOTALTYPES];
        // Assign encoding names
        javaname[GB2312] = "GB2312";
        javaname[GBK] = "GBK";
        javaname[GB18030] = "GB18030";
        javaname[HZ] = "ASCII"; // What to put here? Sun doesn't support HZ
        javaname[ISO2022CN_GB] = "ISO2022CN_GB";
        javaname[BIG5] = "BIG5";
        javaname[CNS11643] = "EUC-TW";
        javaname[ISO2022CN_CNS] = "ISO2022CN_CNS";
        javaname[ISO2022CN] = "ISO2022CN";
        javaname[UTF8] = "UTF-8";
        javaname[UTF8T] = "UTF-8";
        javaname[UTF8S] = "UTF-8";
        javaname[UNICODE] = "Unicode";
        javaname[UNICODET] = "Unicode";
        javaname[UNICODES] = "Unicode";
        javaname[EUC_KR] = "EUC_KR";
        javaname[CP949] = "MS949";
        javaname[ISO2022KR] = "ISO2022KR";
        javaname[JOHAB] = "Johab";
        javaname[SJIS] = "SJIS";
        javaname[EUC_JP] = "EUC_JP";
        javaname[ISO2022JP] = "ISO2022JP";
        javaname[ASCII] = "ASCII";
        javaname[OTHER] = "ISO8859_1";
        // Assign encoding names
        htmlname[GB2312] = "GB2312";
        htmlname[GBK] = "GBK";
        htmlname[GB18030] = "GB18030";
        htmlname[HZ] = "HZ-GB-2312";
        htmlname[ISO2022CN_GB] = "ISO-2022-CN-EXT";
        htmlname[BIG5] = "BIG5";
        htmlname[CNS11643] = "EUC-TW";
        htmlname[ISO2022CN_CNS] = "ISO-2022-CN-EXT";
        htmlname[ISO2022CN] = "ISO-2022-CN";
        htmlname[UTF8] = "UTF-8";
        htmlname[UTF8T] = "UTF-8";
        htmlname[UTF8S] = "UTF-8";
        htmlname[UNICODE] = "UTF-16";
        htmlname[UNICODET] = "UTF-16";
        htmlname[UNICODES] = "UTF-16";
        htmlname[EUC_KR] = "EUC-KR";
        htmlname[CP949] = "x-windows-949";
        htmlname[ISO2022KR] = "ISO-2022-KR";
        htmlname[JOHAB] = "x-Johab";
        htmlname[SJIS] = "Shift_JIS";
        htmlname[EUC_JP] = "EUC-JP";
        htmlname[ISO2022JP] = "ISO-2022-JP";
        htmlname[ASCII] = "ASCII";
        htmlname[OTHER] = "ISO8859-1";
        // Assign Human readable names
        nicename[GB2312] = "GB-2312";
        nicename[GBK] = "GBK";
        nicename[GB18030] = "GB18030";
        nicename[HZ] = "HZ";
        nicename[ISO2022CN_GB] = "ISO2022CN-GB";
        nicename[BIG5] = "Big5";
        nicename[CNS11643] = "CNS11643";
        nicename[ISO2022CN_CNS] = "ISO2022CN-CNS";
        nicename[ISO2022CN] = "ISO2022 CN";
        nicename[UTF8] = "UTF-8";
        nicename[UTF8T] = "UTF-8 (Trad)";
        nicename[UTF8S] = "UTF-8 (Simp)";
        nicename[UNICODE] = "Unicode";
        nicename[UNICODET] = "Unicode (Trad)";
        nicename[UNICODES] = "Unicode (Simp)";
        nicename[EUC_KR] = "EUC-KR";
        nicename[CP949] = "CP949";
        nicename[ISO2022KR] = "ISO 2022 KR";
        nicename[JOHAB] = "Johab";
        nicename[SJIS] = "Shift-JIS";
        nicename[EUC_JP] = "EUC-JP";
        nicename[ISO2022JP] = "ISO 2022 JP";
        nicename[ASCII] = "ASCII";
        nicename[OTHER] = "OTHER";
    }

}