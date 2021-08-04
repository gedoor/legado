package me.ag2s.umdlib.domain;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

import me.ag2s.umdlib.tool.UmdUtils;
import me.ag2s.umdlib.tool.WrapOutputStream;

/**
 * It includes all titles and contents of each chapter in the UMD file.
 * And the content has been compressed by zlib.
 * 
 * @author Ray Liang (liangguanhui@qq.com)
 * 2009-12-20
 */
public class UmdChapters {
	
	private static final int DEFAULT_CHUNK_INIT_SIZE = 32768;
	private int TotalContentLen;

	public List<byte[]> getTitles() {
		return titles;
	}

	private List<byte[]> titles = new ArrayList<>();
	public List<Integer> contentLengths = new ArrayList<>();
	public ByteArrayOutputStream contents = new ByteArrayOutputStream();

	public void addTitle(String s){
		titles.add(UmdUtils.stringToUnicodeBytes(s));
	}
	public void addTitle(byte[] s){
      titles.add(s);
	}
	public void addContentLength(Integer integer){
		contentLengths.add(integer);
	}
	public int getContentLength(int index){
		return contentLengths.get(index);
	}

	public byte[] getContent(int index) {
		int st=contentLengths.get(index);
		byte[] b=contents.toByteArray();
		int end=index+1<contentLengths.size()?contentLengths.get(index+1): getTotalContentLen();
		System.out.println("总长度:"+contents.size());
		System.out.println("起始值:"+st);
		System.out.println("结束值:"+end);
		byte[] bAr=new byte[end-st];
		System.arraycopy(b,st,bAr,0,bAr.length);
		return bAr;

	}
	public String getContentString(int index) {
		return UmdUtils.unicodeBytesToString(getContent(index)).replace((char) 0x2029, '\n');

	}
	public String getTitle(int index){
		return UmdUtils.unicodeBytesToString(titles.get(index));
	}

	
	public void buildChapters(WrapOutputStream wos) throws IOException {
		writeChaptersHead(wos);
		writeChaptersContentOffset(wos);
		writeChaptersTitles(wos);
		writeChaptersChunks(wos);
	}
	
	private void writeChaptersHead(WrapOutputStream wos) throws IOException {
		wos.writeBytes('#', 0x0b, 0, 0, 0x09);
		wos.writeInt(contents.size());
	}
	
	private void writeChaptersContentOffset(WrapOutputStream wos) throws IOException {
		wos.writeBytes('#', 0x83, 0, 0, 0x09);
		byte[] rb = UmdUtils.genRandomBytes(4);
		wos.writeBytes(rb); //random numbers
		wos.write('$');
		wos.writeBytes(rb); //random numbers
		
		wos.writeInt(contentLengths.size() * 4 + 9);  // about the count of chapters
		int offset = 0;
		for (Integer n : contentLengths) {
			wos.writeInt(offset);
			offset += n;
		}
	}
	
	private void writeChaptersTitles(WrapOutputStream wos) throws IOException {
		wos.writeBytes('#', 0x84, 0, 0x01, 0x09);
		byte[] rb = UmdUtils.genRandomBytes(4);
		wos.writeBytes(rb); //random numbers
		wos.write('$');
		wos.writeBytes(rb); //random numbers
		
		int totalTitlesLen = 0;
		for (byte[] t : titles) {
			totalTitlesLen += t.length;
		}
		
		// about the length of the titles
		wos.writeInt(totalTitlesLen + titles.size() + 9);  
		
		for (byte[] t : titles) {
			wos.writeByte(t.length);
			wos.write(t);
		}
	}
	
	private void writeChaptersChunks(WrapOutputStream wos) throws IOException {
		byte[] allContents = contents.toByteArray();
		
		byte[] zero16 = new byte[16];
		Arrays.fill(zero16, 0, zero16.length, (byte) 0);
		
		// write each package of content
		int startPos = 0;
		int len = 0;
		int left = 0;
		int chunkCnt = 0;
		ByteArrayOutputStream bos = new ByteArrayOutputStream(DEFAULT_CHUNK_INIT_SIZE + 256);
		List<byte[]> chunkRbList = new ArrayList<byte[]>();
		
		while(startPos < allContents.length) {
			left = allContents.length - startPos;
			len = DEFAULT_CHUNK_INIT_SIZE < left ? DEFAULT_CHUNK_INIT_SIZE : left; 
			
			bos.reset();
			DeflaterOutputStream zos = new DeflaterOutputStream(bos);
			zos.write(allContents, startPos, len);
			zos.close();
			byte[] chunk = bos.toByteArray();
			
			byte[] rb = UmdUtils.genRandomBytes(4);
			wos.writeByte('$');
			wos.writeBytes(rb);  // 4 random
			chunkRbList.add(rb);
			wos.writeInt(chunk.length + 9);
			wos.write(chunk);
			
			// end of each chunk
			wos.writeBytes('#', 0xF1, 0, 0, 0x15);
			wos.write(zero16);
			
			startPos += len;
			chunkCnt++;
		}
		
		// end of all chunks
		wos.writeBytes('#', 0x81, 0, 0x01, 0x09);
		wos.writeBytes(0, 0, 0, 0); //random numbers
		wos.write('$');
		wos.writeBytes(0, 0, 0, 0); //random numbers
		wos.writeInt(chunkCnt * 4 + 9);
		for (int i = chunkCnt - 1; i >= 0; i--) {
			// random. They are as the same as random numbers in the begin of each chunk
			// use desc order to output these random
			wos.writeBytes(chunkRbList.get(i));
		}
	}
	
	public void addChapter(String title, String content) {
		titles.add(UmdUtils.stringToUnicodeBytes(title));
		byte[] b = UmdUtils.stringToUnicodeBytes(content);
		contentLengths.add(b.length);
		try {
			contents.write(b);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void addFile(File f, String title) throws IOException {
		byte[] temp = UmdUtils.readFile(f);
		String s = new String(temp);
		addChapter(title, s);
	}
	
	public void addFile(File f) throws IOException {
		String s = f.getName();
		int idx = s.lastIndexOf('.');
		if (idx >= 0) {
			s = s.substring(0, idx);
		}
		addFile(f, s);
	}
	
	public void clearChapters() {
		titles.clear();
		contentLengths.clear();
		contents.reset();
	}

	public int getTotalContentLen() {
		return TotalContentLen;
	}

	public void setTotalContentLen(int totalContentLen) {
		TotalContentLen = totalContentLen;
	}
}
