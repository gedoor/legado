package me.ag2s.epublib.zip;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.OsConstants;

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class AndroidRandomReadableFile implements DataInput, Closeable {
    private ParcelFileDescriptor pfd;
    private FileInputStream fis;
    private DataInputStream dis;
    public long pos = 0;

    public AndroidRandomReadableFile(@NonNull Context context, @NonNull Uri treeUri) throws FileNotFoundException {
        try {
            pfd = context.getContentResolver().openFileDescriptor(treeUri, "r");
            fis = new FileInputStream(pfd.getFileDescriptor());
            dis = new DataInputStream(fis);
        } catch (FileNotFoundException e) {
            throw e;
        }
    }

    public final FileDescriptor getFD() {
        return pfd.getFileDescriptor();
    }

    public final FileChannel getChannel() {
        if (fis == null || pos != getPos()) {
            fis = new FileInputStream(pfd.getFileDescriptor());
        }
        return fis.getChannel();
    }

    public final FileInputStream getFileInputStream() {
        if (fis == null || pos != getPos()) {
            fis = new FileInputStream(pfd.getFileDescriptor());
        }
        return fis;
    }

    public int read() throws IOException {
        byte[] b = new byte[1];
        return (read(b, 0, 1) != -1) ? b[0] & 0xff : -1;
    }

    public int read(byte[] b) {
        try {
            return read(b, 0, b.length);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return android.system.Os.read(pfd.getFileDescriptor(), b, off, len);
        } catch (Exception e) {
            return -1;
        }
    }


    private void syncInputStream() {
        fis = new FileInputStream(pfd.getFileDescriptor());
        dis = new DataInputStream(fis);
    }


    public void seek(long pos) throws IOException {
        try {
            android.system.Os.lseek(pfd.getFileDescriptor(), pos, OsConstants.SEEK_SET);
        } catch (Exception e) {
            throw new IOException(e);
        }

    }

    public long length() throws IOException {
        try {
            return android.system.Os.lseek(pfd.getFileDescriptor(), 0, OsConstants.SEEK_END);
        } catch (Exception e) {
            throw new IOException(e);
        }

    }

    public long getPos() {
        try {
            return android.system.Os.lseek(pfd.getFileDescriptor(), 0, OsConstants.SEEK_CUR);
        } catch (Exception e) {
            return -1;
        }

    }

    @Override
    public void readFully(byte[] b) throws IOException {
        try {
            android.system.Os.read(pfd.getFileDescriptor(), b, 0, b.length);
        } catch (ErrnoException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        try {
            android.system.Os.read(pfd.getFileDescriptor(), b, off, len);
            //syncInputStream();
        } catch (ErrnoException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int skipBytes(int n) throws IOException {
        try {
            byte[] b = new byte[n];
            return android.system.Os.read(pfd.getFileDescriptor(), b, 0, b.length);
        } catch (Exception e) {
            return -1;
        }

    }

    @Override
    public boolean readBoolean() throws IOException {
        int ch = this.read();
        if (ch < 0)
            throw new EOFException();
        return (ch != 0);

    }

    @Override
    public byte readByte() throws IOException {
        int ch = this.read();
        if (ch < 0)
            throw new EOFException();
        return (byte) (ch);
    }


    @Override
    public int readUnsignedByte() throws IOException {
        int ch = this.read();
        if (ch < 0)
            throw new EOFException();
        return ch;
    }

    private final byte[] readBuffer = new byte[8];

    @Override
    public short readShort() throws IOException {
        readFully(readBuffer, 0, 2);
        return ByteBuffer.wrap(readBuffer).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        readFully(readBuffer, 0, 2);
        return ByteBuffer.wrap(readBuffer).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get() & 0xffff;
    }

    @Override
    public char readChar() throws IOException {
        readFully(readBuffer, 0, 2);
        return (char) ByteBuffer.wrap(readBuffer).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get();
    }

    @Override
    public int readInt() throws IOException {
        readFully(readBuffer, 0, 4);
        return ByteBuffer.wrap(readBuffer).order(ByteOrder.BIG_ENDIAN).asIntBuffer().get();
    }

    @Override
    public long readLong() throws IOException {
        readFully(readBuffer, 0, 8);
        return (((long) readBuffer[0] << 56) +
                ((long) (readBuffer[1] & 255) << 48) +
                ((long) (readBuffer[2] & 255) << 40) +
                ((long) (readBuffer[3] & 255) << 32) +
                ((long) (readBuffer[4] & 255) << 24) +
                ((readBuffer[5] & 255) << 16) +
                ((readBuffer[6] & 255) << 8) +
                ((readBuffer[7] & 255) << 0));
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    private char[] lineBuffer;

    @Deprecated
    @Override
    public String readLine() throws IOException {
        char[] buf = lineBuffer;

        if (buf == null) {
            buf = lineBuffer = new char[128];
        }

        int room = buf.length;
        int offset = 0;
        int c;

        loop:
        while (true) {
            switch (c = this.read()) {
                case -1:
                case '\n':
                    break loop;

                case '\r':
                    int c2 = this.read();
                    break loop;

                default:
                    if (--room < 0) {
                        buf = new char[offset + 128];
                        room = buf.length - offset - 1;
                        System.arraycopy(lineBuffer, 0, buf, 0, offset);
                        lineBuffer = buf;
                    }
                    buf[offset++] = (char) c;
                    break;
            }
        }
        if ((c == -1) && (offset == 0)) {
            return null;
        }
        return String.copyValueOf(buf, 0, offset);
    }

    @Override
    public String readUTF() throws IOException {
        if (pos != getPos()) {
            syncInputStream();
        }
        return dis.readUTF();
    }

//    public final static String readUTF(DataInput in) throws IOException {
//        int utflen = in.readUnsignedShort();
//        byte[] bytearr = null;
//        char[] chararr = null;
//        if (in instanceof DataInputStream) {
//            DataInputStream dis = (DataInputStream)in;
//            if (dis.bytearr.length < utflen){
//                dis.bytearr = new byte[utflen*2];
//                dis.chararr = new char[utflen*2];
//            }
//            chararr = dis.chararr;
//            bytearr = dis.bytearr;
//        } else {
//            bytearr = new byte[utflen];
//            chararr = new char[utflen];
//        }
//
//        int c, char2, char3;
//        int count = 0;
//        int chararr_count=0;
//
//        in.readFully(bytearr, 0, utflen);
//
//        while (count < utflen) {
//            c = (int) bytearr[count] & 0xff;
//            if (c > 127) break;
//            count++;
//            chararr[chararr_count++]=(char)c;
//        }
//
//        while (count < utflen) {
//            c = (int) bytearr[count] & 0xff;
//            switch (c >> 4) {
//                case 0: case 1: case 2: case 3: case 4: case 5: case 6: case 7:
//                    /* 0xxxxxxx*/
//                    count++;
//                    chararr[chararr_count++]=(char)c;
//                    break;
//                case 12: case 13:
//                    /* 110x xxxx   10xx xxxx*/
//                    count += 2;
//                    if (count > utflen)
//                        throw new UTFDataFormatException(
//                                "malformed input: partial character at end");
//                    char2 = (int) bytearr[count-1];
//                    if ((char2 & 0xC0) != 0x80)
//                        throw new UTFDataFormatException(
//                                "malformed input around byte " + count);
//                    chararr[chararr_count++]=(char)(((c & 0x1F) << 6) |
//                            (char2 & 0x3F));
//                    break;
//                case 14:
//                    /* 1110 xxxx  10xx xxxx  10xx xxxx */
//                    count += 3;
//                    if (count > utflen)
//                        throw new UTFDataFormatException(
//                                "malformed input: partial character at end");
//                    char2 = (int) bytearr[count-2];
//                    char3 = (int) bytearr[count-1];
//                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80))
//                        throw new UTFDataFormatException(
//                                "malformed input around byte " + (count-1));
//                    chararr[chararr_count++]=(char)(((c     & 0x0F) << 12) |
//                            ((char2 & 0x3F) << 6)  |
//                            ((char3 & 0x3F) << 0));
//                    break;
//                default:
//                    /* 10xx xxxx,  1111 xxxx */
//                    throw new UTFDataFormatException(
//                            "malformed input around byte " + count);
//            }
//        }
//        // The number of chars produced may be less than utflen
//        return new String(chararr, 0, chararr_count);
//    }

    @Override
    public void close() throws IOException {
        try {
            dis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            pfd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
