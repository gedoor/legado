package me.ag2s.umdlib.domain;


import java.io.File;
import java.io.IOException;

import me.ag2s.umdlib.tool.UmdUtils;
import me.ag2s.umdlib.tool.WrapOutputStream;


/**
 * This is the cover part of the UMD file.
 * <P>
 * NOTICE: if the "coverData" is empty, it will be skipped when building UMD file.
 * </P>
 * There are 3 ways to load the image data:
 * <ol>
 *     <li>new constructor function of UmdCover.</li>
 *     <li>use UmdCover.load function.</li>
 *     <li>use UmdCover.initDefaultCover, it will generate a simple image with text.</li>
 * </ol>
 * @author Ray Liang (liangguanhui@qq.com)
 * 2009-12-20
 */
public class UmdCover {
	
	private static int DEFAULT_COVER_WIDTH = 120;
	private static int DEFAULT_COVER_HEIGHT = 160;
	
	private byte[] coverData;

	public UmdCover() {
	}

	public UmdCover(byte[] coverData) {
		this.coverData = coverData;
	}
	
	public void load(File f) throws IOException {
		this.coverData = UmdUtils.readFile(f);
	}

	public void load(String fileName) throws IOException {
		load(new File(fileName));
	}
	
	public void initDefaultCover(String title) throws IOException {
//		BufferedImage img = new BufferedImage(DEFAULT_COVER_WIDTH, DEFAULT_COVER_HEIGHT, BufferedImage.TYPE_INT_RGB);
//		Graphics g = img.getGraphics();
//		g.setColor(Color.BLACK);
//		g.fillRect(0, 0, img.getWidth(), img.getHeight());
//		g.setColor(Color.WHITE);
//		g.setFont(new Font("����", Font.PLAIN, 12));
//
//		FontMetrics fm = g.getFontMetrics();
//		int ascent = fm.getAscent();
//		int descent = fm.getDescent();
//		int strWidth = fm.stringWidth(title);
//		int x = (img.getWidth() - strWidth) / 2;
//		int y = (img.getHeight() - ascent - descent) / 2;
//		g.drawString(title, x, y);
//		g.dispose();
//
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//
//		JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(baos);
//		JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(img);
//		param.setQuality(0.5f, false);
//		encoder.setJPEGEncodeParam(param);
//		encoder.encode(img);
//
//		coverData = baos.toByteArray();
	}
	
	public void buildCover(WrapOutputStream wos) throws IOException {
		if (coverData == null || coverData.length == 0) {
			return;
		}
		wos.writeBytes('#', 0x82, 0, 0x01, 0x0A, 0x01);
		byte[] rb = UmdUtils.genRandomBytes(4);
		wos.writeBytes(rb); //random numbers
		wos.write('$');
		wos.writeBytes(rb); //random numbers
		wos.writeInt(coverData.length + 9);
		wos.write(coverData);
	}

	public byte[] getCoverData() {
		return coverData;
	}

	public void setCoverData(byte[] coverData) {
		this.coverData = coverData;
	}

}
