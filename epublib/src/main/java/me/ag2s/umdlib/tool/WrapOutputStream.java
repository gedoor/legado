package me.ag2s.umdlib.tool;

import java.io.IOException;
import java.io.OutputStream;

public class WrapOutputStream extends OutputStream {
	
	private OutputStream os;
	private int written;

	public WrapOutputStream(OutputStream os) {
		this.os = os;
	}
	
    private void incCount(int value) {
        int temp = written + value;
        if (temp < 0) {
            temp = Integer.MAX_VALUE;
        }
        written = temp;
    }
	
    // it is different from the writeInt of DataOutputStream
	public void writeInt(int v) throws IOException {
        os.write((v >>>  0) & 0xFF);
        os.write((v >>>  8) & 0xFF);
        os.write((v >>> 16) & 0xFF);
        os.write((v >>> 24) & 0xFF);
		incCount(4);
	}
	
	public void writeByte(byte b) throws IOException {
		write(b);
	}
	
	public void writeByte(int n) throws IOException {
		write(n);
	}
	
	public void writeBytes(byte ... bytes) throws IOException {
		write(bytes);
	}
	
	public void writeBytes(int ... vals) throws IOException {
		for (int v : vals) {
			write(v);
		}
	}

	public void write(byte[] b, int off, int len) throws IOException {
		os.write(b, off, len);
		incCount(len);
	}

	public void write(byte[] b) throws IOException {
		os.write(b);
		incCount(b.length);
	}

	public void write(int b) throws IOException {
		os.write(b);
		incCount(1);
	}
	
	/////////////////////////////////////////////////

	public void close() throws IOException {
		os.close();
	}

	public void flush() throws IOException {
		os.flush();
	}

	public boolean equals(Object obj) {
		return os.equals(obj);
	}

	public int hashCode() {
		return os.hashCode();
	}

	public String toString() {
		return os.toString();
	}

	public int getWritten() {
		return written;
	}

}
