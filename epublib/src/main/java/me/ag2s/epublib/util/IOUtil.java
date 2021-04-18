package me.ag2s.epublib.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Most of the functions herein are re-implementations of the ones in
 * apache io IOUtils.
 * <p>
 * The reason for re-implementing this is that the functions are fairly simple
 * and using my own implementation saves the inclusion of a 200Kb jar file.
 */
public class IOUtil {

    /**
     * Represents the end-of-file (or stream).
     * @since 2.5 (made public)
     */
    public static final int EOF = -1;

    public static final int IO_COPY_BUFFER_SIZE = 1024 * 8;
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * Gets the contents of the Reader as a byte[], with the given character encoding.
     *
     * @param in       g
     * @param encoding g
     * @return the contents of the Reader as a byte[], with the given character encoding.
     * @throws IOException g
     */
    public static byte[] toByteArray(Reader in, String encoding)
            throws IOException {
        StringWriter out = new StringWriter();
        copy(in, out);
        out.flush();
        return out.toString().getBytes(encoding);
    }

    /**
     * Returns the contents of the InputStream as a byte[]
     *
     * @param in f
     * @return the contents of the InputStream as a byte[]
     * @throws IOException f
     */
    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        copy(in, result);
        result.flush();
        return result.toByteArray();
    }

    /**
     * Reads data from the InputStream, using the specified buffer size.
     * <p>
     * This is meant for situations where memory is tight, since
     * it prevents buffer expansion.
     *
     * @param in   the stream to read data from
     * @param size the size of the array to create
     * @return the array, or null
     * @throws IOException f
     */
    public static byte[] toByteArray(InputStream in, int size)
            throws IOException {

        try {
            ByteArrayOutputStream result;

            if (size > 0) {
                result = new ByteArrayOutputStream(size);
            } else {
                result = new ByteArrayOutputStream();
            }

            copy(in, result);
            result.flush();
            return result.toByteArray();
        } catch (OutOfMemoryError error) {
            //Return null so it gets loaded lazily.
            return null;
        }

    }


    /**
     * if totalNrRead &lt; 0 then totalNrRead is returned, if
     * (nrRead + totalNrRead) &lt; Integer.MAX_VALUE then nrRead + totalNrRead
     * is returned, -1 otherwise.
     *
     * @param nrRead       f
     * @param totalNrNread f
     * @return if totalNrRead &lt; 0 then totalNrRead is returned, if
     * (nrRead + totalNrRead) &lt; Integer.MAX_VALUE then nrRead + totalNrRead
     * is returned, -1 otherwise.
     */
    protected static int calcNewNrReadSize(int nrRead, int totalNrNread) {
        if (totalNrNread < 0) {
            return totalNrNread;
        }
        if (totalNrNread > (Integer.MAX_VALUE - nrRead)) {
            return -1;
        } else {
            return (totalNrNread + nrRead);
        }
    }

    /**
     * Copies the contents of the InputStream to the OutputStream.
     *
     * @param in  f
     * @param out f
     * @return the nr of bytes read, or -1 if the amount &gt; Integer.MAX_VALUE
     * @throws IOException f
     */
    public static int copy(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[IO_COPY_BUFFER_SIZE];
        int readSize;
        int result = 0;
        while ((readSize = in.read(buffer)) >= 0) {
            out.write(buffer, 0, readSize);
            result = calcNewNrReadSize(readSize, result);
        }
        out.flush();
        return result;
    }

    /**
     * Copies the contents of the Reader to the Writer.
     *
     * @param in  f
     * @param out f
     * @return the nr of characters read, or -1 if the amount &gt; Integer.MAX_VALUE
     * @throws IOException f
     */
    public static int copy(Reader in, Writer out) throws IOException {
        char[] buffer = new char[IO_COPY_BUFFER_SIZE];
        int readSize;
        int result = 0;
        while ((readSize = in.read(buffer)) >= 0) {
            out.write(buffer, 0, readSize);
            result = calcNewNrReadSize(readSize, result);
        }
        out.flush();
        return result;
    }
    /**
     * Returns the length of the given array in a null-safe manner.
     *
     * @param array an array or null
     * @return the array length -- or 0 if the given array is null.
     * @since 2.7
     */
    public static int length(final byte[] array) {
        return array == null ? 0 : array.length;
    }

    /**
     * Returns the length of the given array in a null-safe manner.
     *
     * @param array an array or null
     * @return the array length -- or 0 if the given array is null.
     * @since 2.7
     */
    public static int length(final char[] array) {
        return array == null ? 0 : array.length;
    }

    /**
     * Returns the length of the given CharSequence in a null-safe manner.
     *
     * @param csq a CharSequence or null
     * @return the CharSequence length -- or 0 if the given CharSequence is null.
     * @since 2.7
     */
    public static int length(final CharSequence csq) {
        return csq == null ? 0 : csq.length();
    }

    /**
     * Returns the length of the given array in a null-safe manner.
     *
     * @param array an array or null
     * @return the array length -- or 0 if the given array is null.
     * @since 2.7
     */
    public static int length(final Object[] array) {
        return array == null ? 0 : array.length;
    }

    @SuppressWarnings("unused")
    public static String Stream2String(InputStream inputStream) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString();
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }

    }
}
