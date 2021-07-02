package me.ag2s.umdlib.tool;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamReader {
    private InputStream is;

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    private long offset;
    private long size;

    private void incCount(int value) {
        int temp = (int) (offset + value);
        if (temp < 0) {
            temp = Integer.MAX_VALUE;
        }
        offset = temp;
    }
    public StreamReader(InputStream inputStream) throws IOException {
        this.is=inputStream;
        //this.size=inputStream.getChannel().size();
    }

    public short readUint8() throws IOException {
        byte[] b=new byte[1];
        is.read(b);
        incCount(1);
        return (short) ((b[0] & 0xFF));

    }

    public byte readByte() throws IOException {
        byte[] b=new byte[1];
        is.read(b);
        incCount(1);
        return b[0];
    }
    public byte[] readBytes(int len) throws IOException {
        if (len<1){
            System.out.println(len);
            throw new IllegalArgumentException("Length must > 0: " + len);
        }
        byte[] b=new byte[len];
        is.read(b);
        incCount(len);
        return b;
    }
    public String readHex(int len) throws IOException {
        if (len<1){
            System.out.println(len);
            throw new IllegalArgumentException("Length must > 0: " + len);
        }
        byte[] b=new byte[len];
        is.read(b);
        incCount(len);
       return UmdUtils.toHex(b);
    }

    public short readShort() throws IOException {
        byte[] b=new byte[2];
        is.read(b);
        incCount(2);
        short x = (short) (((b[0] & 0xFF) <<  8) | ((b[1] & 0xFF) <<  0));
        return x;
    }
    public short readShortLe() throws IOException {
        byte[] b=new byte[2];
        is.read(b);
        incCount(2);
        short x = (short) (((b[1] & 0xFF) <<  8) | ((b[0] & 0xFF) <<  0));
        return x;
    }
    public int readInt() throws IOException {
        byte[] b=new byte[4];
        is.read(b);
        incCount(4);
        int x = ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) |
                ((b[2] & 0xFF) <<  8) | ((b[3] & 0xFF) <<  0);
        return x;
    }
    public int readIntLe() throws IOException {
        byte[] b=new byte[4];
        is.read(b);
        incCount(4);
        int x = ((b[3] & 0xFF) << 24) | ((b[2] & 0xFF) << 16) |
                ((b[1] & 0xFF) <<  8) | ((b[0] & 0xFF) <<  0);
        return x;
    }
    public void skip(int len) throws IOException {
        readBytes(len);
    }


    public byte[] read(byte[] b) throws IOException {
        is.read(b);
        incCount(b.length);
        return b;
    }

    public byte[] read(byte[] b, int off, int len) throws IOException {
        is.read(b, off, len);
        incCount(len);
        return b;
    }


}
