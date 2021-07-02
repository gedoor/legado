
package me.ag2s.umdlib.tool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.zip.InflaterInputStream;


public class UmdUtils {

	private static final int EOF = -1;
	private static final int BUFFER_SIZE = 8 * 1024;


	/**
	 * 将字符串编码成Unicode形式的byte[]
	 * @param s 要编码的字符串
	 * @return 编码好的byte[]
	 */
	public static byte[] stringToUnicodeBytes(String s) {
		if (s == null) {
			throw new NullPointerException();
		}
		
		int len = s.length();
		byte[] ret = new byte[len * 2];
		int a, b, c;
		for (int i = 0; i < len; i++) {
			c = s.charAt(i);
			a = c >> 8;
			b = c & 0xFF;
			if (a < 0) {
				a += 0xFF;
			}
			if (b < 0) {
				b += 0xFF;
			}
			ret[i * 2] = (byte) b;
			ret[i * 2 + 1] = (byte) a;
		}
		return ret;
	}

	/**
	 * 将编码成Unicode形式的byte[]解码成原始字符串
	 * @param bytes 编码成Unicode形式的byte[]
	 * @return 原始字符串
	 */
	public static String unicodeBytesToString(byte[] bytes){
		char[] s=new char[bytes.length/2];
		StringBuilder sb=new StringBuilder();
		int a,b,c;
		for(int i=0;i<s.length;i++){
			a=bytes[i*2+1];
			b=bytes[i*2];
			c=(a&0xff)<<8|(b&0xff);
			if(c<0){
				c+=0xffff;
			}
			char[] c1=Character.toChars(c);
			sb.append(c1);

		}
		return sb.toString();
	}

	/**
	 * 将byte[]转化成Hex形式
	 * @param bArr byte[]
	 * @return 目标HEX字符串
	 */
	public static String toHex(byte[] bArr){
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
	 * @param compress zippered byte[]
	 * @return decompressed byte[]
	 * @throws Exception 解码时失败时
	 */
	public static byte[] decompress(byte[] compress) throws Exception {
		ByteArrayInputStream bais = new ByteArrayInputStream(compress);
		InflaterInputStream iis = new InflaterInputStream(bais);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int c = 0;
		byte[] buf = new byte[BUFFER_SIZE];
		while (true) {
			c = iis.read(buf);

			if (c == EOF)
				break;
			baos.write(buf, 0, c);
		}
		baos.flush();
		return baos.toByteArray();
	}



	
	public static void saveFile(File f, byte[] content) throws IOException {
		FileOutputStream fos = new FileOutputStream(f);
		try {
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			bos.write(content);
			bos.flush();
		} finally {
			fos.close();
		}
	}
	
	public static byte[] readFile(File f) throws IOException {
		FileInputStream fis = new FileInputStream(f);
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BufferedInputStream bis = new BufferedInputStream(fis);
			int ch;
			while ((ch = bis.read()) >= 0) {
				baos.write(ch);
			}
			baos.flush();
			return baos.toByteArray();
		} finally {
			fis.close();
		}
	}
	
	private static Random random = new Random();
	
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
