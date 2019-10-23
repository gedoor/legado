package io.legado.app.utils;

/**
 * Created by newbiechen on 2018/1/1.
 */

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 将字符串转化为MD5
 */

public class MD5Utils {

    public static String strToMd5By32(String str) {
        if (str == null) return null;
        String reStr = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(str.getBytes());
            StringBuilder stringBuffer = new StringBuilder();
            for (byte b : bytes) {
                int bt = b & 0xff;
                if (bt < 16) {
                    stringBuffer.append(0);
                }
                stringBuffer.append(Integer.toHexString(bt));
            }
            reStr = stringBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return reStr;
    }

    public static String strToMd5By16(String str) {
        String reStr = strToMd5By32(str);
        if (reStr != null) {
            reStr = reStr.substring(8, 24);
        }
        return reStr;
    }
}
