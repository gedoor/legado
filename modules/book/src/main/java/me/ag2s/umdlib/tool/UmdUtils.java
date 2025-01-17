
package me.ag2s.umdlib.tool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.zip.Inflater;
import java.nio.charset.StandardCharsets;


public class UmdUtils {

    private static final int EOF = -1;
    private static final int BUFFER_SIZE = 8 * 1024;


    /**
     * 将字符串编码成Unicode形式的byte[]
     *
     * @param s 要编码的字符串
     * @return 编码好的byte[]
     */
    public static byte[] stringToUnicodeBytes(String s) {
        if (s == null) {
            throw new NullPointerException();
        }

        return s.getBytes(StandardCharsets.UTF_16LE);
    }

    /**
     * 将编码成Unicode形式的byte[]解码成原始字符串
     *
     * @param bytes 编码成Unicode形式的byte[]
     * @return 原始字符串
     */
    public static String unicodeBytesToString(byte[] bytes) {
        //修复一些文件属性空值的问题
        if (bytes==null){
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_16LE);
    }

    /**
     * 将byte[]转化成Hex形式
     *
     * @param bArr byte[]
     * @return 目标HEX字符串
     */
    public static String toHex(byte[] bArr) {
        StringBuilder sb = new StringBuilder(bArr.length);
        String sTmp;

        for (int i = 0; i < bArr.length; i++) {
            sTmp = Integer.toHexString(0xFF & bArr[i]);
            if (sTmp.length() < 2)
                sb.append(0);
            sb.append(sTmp.toUpperCase());
        }

        return sb.toString();
    }

    /**
     * 解压缩zip的byte[]
     *
     * @param compress zippered byte[]
     * @return decompressed byte[]
     * @throws Exception 解码时失败时
     */
    public static byte[] decompress(byte[] compress) throws Exception {
        Inflater inflater = new Inflater();
        inflater.reset();
        inflater.setInput(compress);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(compress.length);
        try (baos) {
            byte[] buff = new byte[BUFFER_SIZE];
            while (!inflater.finished()) {
                int count = inflater.inflate(buff);
                baos.write(buff, 0, count);
            }
        }
        inflater.end();
        return baos.toByteArray();
    }


    public static void saveFile(File f, byte[] content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(content);
            bos.flush();
        }
    }

    public static byte[] readFile(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BufferedInputStream bis = new BufferedInputStream(fis);
            int ch;
            while ((ch = bis.read()) >= 0) {
                baos.write(ch);
            }
            baos.flush();
            return baos.toByteArray();
        }
    }

    private static final Random random = new Random();

    public static byte[] genRandomBytes(int len) {
        if (len <= 0) {
            throw new IllegalArgumentException("Length must > 0: " + len);
        }
        byte[] ret = new byte[len];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (byte) random.nextInt(256);
        }
        return ret;
    }

}
