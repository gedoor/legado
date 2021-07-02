package me.ag2s.umdlib.domain;


import java.io.IOException;

import me.ag2s.umdlib.tool.UmdUtils;
import me.ag2s.umdlib.tool.WrapOutputStream;

/**
 * Header of UMD file.
 * It includes a lot of properties of header.
 * All the properties are String type.
 * 
 * @author Ray Liang (liangguanhui@qq.com)
 * 2009-12-20
 */
public class UmdHeader {
	public byte getUmdType() {
		return umdType;
	}

	public void setUmdType(byte umdType) {
		this.umdType = umdType;
	}

	private byte umdType;
	private String title;

	private String author;
	
	private String year;
	
	private String month;
	
	private String day;
	
	private String bookType;

	private String bookMan;
	
	private String shopKeeper;
	private final static byte B_type_umd = (byte) 0x01;
	private final static byte B_type_title = (byte) 0x02;
	private final static byte B_type_author = (byte) 0x03;
	private final static byte B_type_year = (byte) 0x04;
	private final static byte B_type_month = (byte) 0x05;
	private final static byte B_type_day = (byte) 0x06;
	private final static byte B_type_bookType = (byte) 0x07;
	private final static byte B_type_bookMan = (byte) 0x08;
	private final static byte B_type_shopKeeper = (byte) 0x09;
	
	public void buildHeader(WrapOutputStream wos) throws IOException {
		wos.writeBytes(0x89, 0x9b, 0x9a, 0xde); // UMD file type flags
		wos.writeByte('#');
		wos.writeBytes(0x01, 0x00, 0x00, 0x08); // Unknown
		wos.writeByte(0x01); //0x01 is text type; while 0x02 is Image type.
		wos.writeBytes(UmdUtils.genRandomBytes(2)); //random number
		
		// start properties output
		buildType(wos, B_type_title, getTitle());
		buildType(wos, B_type_author, getAuthor());
		buildType(wos, B_type_year, getYear());
		buildType(wos, B_type_month, getMonth());
		buildType(wos, B_type_day, getDay());
		buildType(wos, B_type_bookType, getBookType());
		buildType(wos, B_type_bookMan, getBookMan());
		buildType(wos, B_type_shopKeeper, getShopKeeper());
	}
	
	public void buildType(WrapOutputStream wos, byte type, String content) throws IOException {
		if (content == null || content.length() == 0) {
			return;
		}
		
		wos.writeBytes('#', type, 0, 0);
		
		byte[] temp = UmdUtils.stringToUnicodeBytes(content);
		wos.writeByte(temp.length + 5);
		wos.write(temp);
	}



	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getBookMan() {
		return bookMan;
	}

	public void setBookMan(String bookMan) {
		this.bookMan = bookMan;
	}

	public String getShopKeeper() {
		return shopKeeper;
	}

	public void setShopKeeper(String shopKeeper) {
		this.shopKeeper = shopKeeper;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getMonth() {
		return month;
	}

	public void setMonth(String month) {
		this.month = month;
	}

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public String getBookType() {
		return bookType;
	}

	public void setBookType(String bookType) {
		this.bookType = bookType;
	}
	
	@Override
	public String toString() {
		return "UmdHeader{" +
				"umdType=" + umdType +
				", title='" + title + '\'' +
				", author='" + author + '\'' +
				", year='" + year + '\'' +
				", month='" + month + '\'' +
				", day='" + day + '\'' +
				", bookType='" + bookType + '\'' +
				", bookMan='" + bookMan + '\'' +
				", shopKeeper='" + shopKeeper + '\'' +
				'}';
	}
}
