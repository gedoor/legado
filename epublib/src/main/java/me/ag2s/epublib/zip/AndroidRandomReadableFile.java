package me.ag2s.epublib.zip;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
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
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import me.ag2s.epublib.util.AndroidCloseGuard;


public class AndroidRandomReadableFile implements DataInput, Closeable {
    private final ParcelFileDescriptor pfd;
    private FileInputStream fis;
    private long pos = 0;

    private final AndroidCloseGuard guard = AndroidCloseGuard.getInstance();

    public AndroidRandomReadableFile(@NonNull Context context, @NonNull Uri treeUri) throws FileNotFoundException {
        pfd = context.getContentResolver().openFileDescriptor(treeUri, "r");
        fis = new FileInputStream(pfd.getFileDescriptor());
        //dis = new DataInputStream(fis);
        guard.open("close");

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
        if (fis == null || this.pos != getPos()) {
            this.pos = getPos();
            fis = new FileInputStream(pfd.getFileDescriptor());
        }
        return fis;
    }

    /**
     * Reads a byte of data from this input stream. This method blocks
     * if no input is yet available.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * file is reached.
     * @throws IOException if an I/O error occurs.
     */
    public int read() throws IOException {
        byte[] b = new byte[1];
        return (read(b, 0, 1) != -1) ? b[0] & 0xff : -1;
    }


    /**
     * Reads up to <code>b.length</code> bytes of data from this input
     * stream into an array of bytes. This method blocks until some input
     * is available.
     *
     * @param b the buffer into which the data is read.
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of
     * the file has been reached.
     * @throws IOException if an I/O error occurs.
     */
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);

    }

    /**
     * Reads up to <code>len</code> bytes of data from this input stream
     * into an array of bytes. If <code>len</code> is not zero, the method
     * blocks until some input is available; otherwise, no
     * bytes are read and <code>0</code> is returned.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes read.
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of
     * the file has been reached.
     * @throws NullPointerException      If <code>b</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException If <code>off</code> is negative,
     *                                   <code>len</code> is negative, or <code>len</code> is greater than
     *                                   <code>b.length - off</code>
     * @throws IOException               if an I/O error occurs.
     */
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return android.system.Os.read(pfd.getFileDescriptor(), b, off, len);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }


    private void syncInputStream() {
        this.pos = getPos();
        fis = new FileInputStream(pfd.getFileDescriptor());
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

    /**
     * Reads some bytes from an input
     * stream and stores them into the buffer
     * array {@code b}. The number of bytes
     * read is equal
     * to the length of {@code b}.
     * <p>
     * This method blocks until one of the
     * following conditions occurs:
     * <ul>
     * <li>{@code b.length}
     * bytes of input data are available, in which
     * case a normal return is made.
     *
     * <li>End of
     * file is detected, in which case an {@code EOFException}
     * is thrown.
     *
     * <li>An I/O error occurs, in
     * which case an {@code IOException} other
     * than {@code EOFException} is thrown.
     * </ul>
     * <p>
     * If {@code b} is {@code null},
     * a {@code NullPointerException} is thrown.
     * If {@code b.length} is zero, then
     * no bytes are read. Otherwise, the first
     * byte read is stored into element {@code b[0]},
     * the next one into {@code b[1]}, and
     * so on.
     * If an exception is thrown from
     * this method, then it may be that some but
     * not all bytes of {@code b} have been
     * updated with data from the input stream.
     *
     * @param b the buffer into which the data is read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public void readFully(byte[] b) throws IOException {
        try {
            android.system.Os.read(pfd.getFileDescriptor(), b, 0, b.length);
        } catch (ErrnoException e) {
            e.printStackTrace();
        }


    }

    /**
     * Reads {@code len}
     * bytes from
     * an input stream.
     * <p>
     * This method
     * blocks until one of the following conditions
     * occurs:
     * <ul>
     * <li>{@code len} bytes
     * of input data are available, in which case
     * a normal return is made.
     *
     * <li>End of file
     * is detected, in which case an {@code EOFException}
     * is thrown.
     *
     * <li>An I/O error occurs, in
     * which case an {@code IOException} other
     * than {@code EOFException} is thrown.
     * </ul>
     * <p>
     * If {@code b} is {@code null},
     * a {@code NullPointerException} is thrown.
     * If {@code off} is negative, or {@code len}
     * is negative, or {@code off+len} is
     * greater than the length of the array {@code b},
     * then an {@code IndexOutOfBoundsException}
     * is thrown.
     * If {@code len} is zero,
     * then no bytes are read. Otherwise, the first
     * byte read is stored into element {@code b[off]},
     * the next one into {@code b[off+1]},
     * and so on. The number of bytes read is,
     * at most, equal to {@code len}.
     *
     * @param b   the buffer into which the data is read.
     * @param off an int specifying the offset into the data.
     * @param len an int specifying the number of bytes to read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        try {
            android.system.Os.read(pfd.getFileDescriptor(), b, off, len);
            //syncInputStream();
        } catch (ErrnoException e) {
            throw new IOException(e);
        }
    }

    /**
     * Makes an attempt to skip over
     * {@code n} bytes
     * of data from the input
     * stream, discarding the skipped bytes. However,
     * it may skip
     * over some smaller number of
     * bytes, possibly zero. This may result from
     * any of a
     * number of conditions; reaching
     * end of file before {@code n} bytes
     * have been skipped is
     * only one possibility.
     * This method never throws an {@code EOFException}.
     * The actual
     * number of bytes skipped is returned.
     *
     * @param n the number of bytes to be skipped.
     * @return the number of bytes actually skipped.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int skipBytes(int n) throws IOException {
        try {
            byte[] b = new byte[n];
            return android.system.Os.read(pfd.getFileDescriptor(), b, 0, b.length);
        } catch (Exception e) {
            return -1;
        }

    }

    /**
     * Reads one input byte and returns
     * {@code true} if that byte is nonzero,
     * {@code false} if that byte is zero.
     * This method is suitable for reading
     * the byte written by the {@code writeBoolean}
     * method of interface {@code DataOutput}.
     *
     * @return the {@code boolean} value read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public boolean readBoolean() throws IOException {
        int ch = this.read();
        if (ch < 0)
            throw new EOFException();
        return (ch != 0);

    }

    /**
     * Reads and returns one input byte.
     * The byte is treated as a signed value in
     * the range {@code -128} through {@code 127},
     * inclusive.
     * This method is suitable for
     * reading the byte written by the {@code writeByte}
     * method of interface {@code DataOutput}.
     *
     * @return the 8-bit value read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public byte readByte() throws IOException {
        int ch = this.read();
        if (ch < 0)
            throw new EOFException();
        return (byte) (ch);
    }


    /**
     * Reads one input byte, zero-extends
     * it to type {@code int}, and returns
     * the result, which is therefore in the range
     * {@code 0}
     * through {@code 255}.
     * This method is suitable for reading
     * the byte written by the {@code writeByte}
     * method of interface {@code DataOutput}
     * if the argument to {@code writeByte}
     * was intended to be a value in the range
     * {@code 0} through {@code 255}.
     *
     * @return the unsigned 8-bit value read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public int readUnsignedByte() throws IOException {
        int ch = this.read();
        if (ch < 0)
            throw new EOFException();
        return ch;
    }

    private final byte[] readBuffer = new byte[8];

    /**
     * Reads two input bytes and returns
     * a {@code short} value. Let {@code a}
     * be the first byte read and {@code b}
     * be the second byte. The value
     * returned
     * is:
     * <pre>{@code (short)((a << 8) | (b & 0xff))
     * }</pre>
     * This method
     * is suitable for reading the bytes written
     * by the {@code writeShort} method of
     * interface {@code DataOutput}.
     *
     * @return the 16-bit value read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public short readShort() throws IOException {
        readFully(readBuffer, 0, 2);
        return ByteBuffer.wrap(readBuffer).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get();
    }

    /**
     * Reads two input bytes and returns
     * an {@code int} value in the range {@code 0}
     * through {@code 65535}. Let {@code a}
     * be the first byte read and
     * {@code b}
     * be the second byte. The value returned is:
     * <pre>{@code (((a & 0xff) << 8) | (b & 0xff))
     * }</pre>
     * This method is suitable for reading the bytes
     * written by the {@code writeShort} method
     * of interface {@code DataOutput}  if
     * the argument to {@code writeShort}
     * was intended to be a value in the range
     * {@code 0} through {@code 65535}.
     *
     * @return the unsigned 16-bit value read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public int readUnsignedShort() throws IOException {
        readFully(readBuffer, 0, 2);
        return ByteBuffer.wrap(readBuffer).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get() & 0xffff;
    }

    /**
     * Reads two input bytes and returns a {@code char} value.
     * Let {@code a}
     * be the first byte read and {@code b}
     * be the second byte. The value
     * returned is:
     * <pre>{@code (char)((a << 8) | (b & 0xff))
     * }</pre>
     * This method
     * is suitable for reading bytes written by
     * the {@code writeChar} method of interface
     * {@code DataOutput}.
     *
     * @return the {@code char} value read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public char readChar() throws IOException {
        readFully(readBuffer, 0, 2);
        return (char) ByteBuffer.wrap(readBuffer).order(ByteOrder.BIG_ENDIAN).asShortBuffer().get();
    }

    /**
     * Reads four input bytes and returns an
     * {@code int} value. Let {@code a-d}
     * be the first through fourth bytes read. The value returned is:
     * <pre>{@code
     * (((a & 0xff) << 24) | ((b & 0xff) << 16) |
     *  ((c & 0xff) <<  8) | (d & 0xff))
     * }</pre>
     * This method is suitable
     * for reading bytes written by the {@code writeInt}
     * method of interface {@code DataOutput}.
     *
     * @return the {@code int} value read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public int readInt() throws IOException {
        readFully(readBuffer, 0, 4);
        return ByteBuffer.wrap(readBuffer).order(ByteOrder.BIG_ENDIAN).asIntBuffer().get();
    }


    /**
     * Reads eight input bytes and returns
     * a {@code long} value. Let {@code a-h}
     * be the first through eighth bytes read.
     * The value returned is:
     * <pre>{@code
     * (((long)(a & 0xff) << 56) |
     *  ((long)(b & 0xff) << 48) |
     *  ((long)(c & 0xff) << 40) |
     *  ((long)(d & 0xff) << 32) |
     *  ((long)(e & 0xff) << 24) |
     *  ((long)(f & 0xff) << 16) |
     *  ((long)(g & 0xff) <<  8) |
     *  ((long)(h & 0xff)))
     * }</pre>
     * <p>
     * This method is suitable
     * for reading bytes written by the {@code writeLong}
     * method of interface {@code DataOutput}.
     *
     * @return the {@code long} value read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
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

    /**
     * Reads four input bytes and returns
     * a {@code float} value. It does this
     * by first constructing an {@code int}
     * value in exactly the manner
     * of the {@code readInt}
     * method, then converting this {@code int}
     * value to a {@code float} in
     * exactly the manner of the method {@code Float.intBitsToFloat}.
     * This method is suitable for reading
     * bytes written by the {@code writeFloat}
     * method of interface {@code DataOutput}.
     *
     * @return the {@code float} value read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * Reads eight input bytes and returns
     * a {@code double} value. It does this
     * by first constructing a {@code long}
     * value in exactly the manner
     * of the {@code readLong}
     * method, then converting this {@code long}
     * value to a {@code double} in exactly
     * the manner of the method {@code Double.longBitsToDouble}.
     * This method is suitable for reading
     * bytes written by the {@code writeDouble}
     * method of interface {@code DataOutput}.
     *
     * @return the {@code double} value read.
     * @throws EOFException if this stream reaches the end before reading
     *                      all the bytes.
     * @throws IOException  if an I/O error occurs.
     */
    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    private char[] lineBuffer;

    /**
     * See the general contract of the <code>readLine</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @return the next line of text from this input stream.
     * @throws IOException if an I/O error occurs.
     * @see java.io.BufferedReader#readLine()
     * @see java.io.FilterInputStream##in
     * @deprecated This method does not properly convert bytes to characters.
     * As of JDK&nbsp;1.1, the preferred way to read lines of text is via the
     * <code>BufferedReader.readLine()</code> method.  Programs that use the
     * <code>DataInputStream</code> class to read lines can be converted to use
     * the <code>BufferedReader</code> class by replacing code of the form:
     * <blockquote><pre>
     *     DataInputStream d =&nbsp;new&nbsp;DataInputStream(in);
     * </pre></blockquote>
     * with:
     * <blockquote><pre>
     *     BufferedReader d
     *          =&nbsp;new&nbsp;BufferedReader(new&nbsp;InputStreamReader(in));
     * </pre></blockquote>
     */
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

    /**
     * See the general contract of the <code>readUTF</code>
     * method of <code>DataInput</code>.
     * <p>
     * Bytes
     * for this operation are read from the contained
     * input stream.
     *
     * @return a Unicode string.
     * @throws EOFException           if this input stream reaches the end before
     *                                reading all the bytes.
     * @throws IOException            the stream has been closed and the contained
     *                                input stream does not support reading after close, or
     *                                another I/O error occurs.
     * @throws UTFDataFormatException if the bytes do not represent a valid
     *                                modified UTF-8 encoding of a string.
     * @see java.io.DataInputStream#readUTF(java.io.DataInput)
     */
    @Override
    public String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }


    @Override
    public void close() throws IOException {

        guard.close();

        if (Build.VERSION.SDK_INT >= 28) {
            //Reference.reachabilityFence(this);
        }
        try {
            if (fis != null) {
                fis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (pfd != null) {
                pfd.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    protected void finalize() throws Throwable {
        try {
            // Note that guard could be null if the constructor threw.

            guard.warnIfOpen();

            close();

        } finally {
            super.finalize();
        }
    }
}
