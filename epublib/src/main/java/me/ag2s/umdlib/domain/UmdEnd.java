package me.ag2s.umdlib.domain;

import java.io.IOException;

import me.ag2s.umdlib.tool.WrapOutputStream;

/**
 * End part of UMD book, nothing to be special
 * 
 * @author Ray Liang (liangguanhui@qq.com)
 * 2009-12-20
 */
public class UmdEnd {
	
	public void buildEnd(WrapOutputStream wos) throws IOException {
		wos.writeBytes('#', 0x0C, 0, 0x01, 0x09);
		wos.writeInt(wos.getWritten() + 4);
	}

}
