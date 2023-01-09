package me.ag2s.base;

import static me.ag2s.base.ThrowableUtils.rethrowAsIOException;

import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.OsConstants;

import java.io.EOFException;
import java.io.IOException;

/**
 * 读取ParcelFileDescriptor的工具类
 */
@SuppressWarnings("unused")
public final class PfdHelper {

    /**
     * 读取基本类型的buffer
     */
    private static final byte[] readBuffer = new byte[8];

    public static void seek(ParcelFileDescriptor pfd, long pos) throws IOException {
        try {
            android.system.Os.lseek(pfd.getFileDescriptor(), pos, OsConstants.SEEK_SET);
        } catch (ErrnoException e) {
            throw rethrowAsIOException(e);
        }

    }

    public static long getFilePointer(ParcelFileDescriptor pfd) throws IOException {
        try {
            return android.system.Os.lseek(pfd.getFileDescriptor(), 0, OsConstants.SEEK_CUR);
        } catch (ErrnoException e) {
            throw rethrowAsIOException(e);
        }
    }

    public static long length(ParcelFileDescriptor pfd) throws IOException {
        try {
            return android.system.Os.fstat(pfd.getFileDescriptor()).st_size; //android.system.Os.lseek(pfd.getFileDescriptor(), 0, OsConstants.SEEK_END);
        } catch (ErrnoException e) {
            throw rethrowAsIOException(e);
        }
    }

    private static int readBytes(ParcelFileDescriptor pfd, byte[] b, int off, int len) throws IOException {
        try {
            return android.system.Os.read(pfd.getFileDescriptor(), b, off, len);
        } catch (ErrnoException e) {
            throw rethrowAsIOException(e);
        }
    }

    public static int read(ParcelFileDescriptor pfd) throws IOException {
        return (read(pfd, readBuffer, 0, 1) != -1) ? readBuffer[0] & 0xff : -1;
    }

    public static int read(ParcelFileDescriptor pfd, byte[] b, int off, int len) throws IOException {
        return readBytes(pfd, b, off, len);
    }

    public static int read(ParcelFileDescriptor pfd, byte[] b) throws IOException {
        return readBytes(pfd, b, 0, b.length);
    }

    public static void readFully(ParcelFileDescriptor pfd, byte[] b) throws IOException {
        readFully(pfd, b, 0, b.length);
    }

    public static void readFully(ParcelFileDescriptor pfd, byte[] b, int off, int len) throws IOException {
        int n = 0;
        do {
            int count = read(pfd, b, off + n, len - n);
            if (count < 0)
                throw new EOFException();
            n += count;
        } while (n < len);
    }


    public static int skipBytes(ParcelFileDescriptor pfd, int n) throws IOException {
        long pos;
        long len;
        long newpos;

        if (n <= 0) {
            return 0;
        }
        pos = getFilePointer(pfd);
        len = length(pfd);
        newpos = pos + n;
        if (newpos > len) {
            newpos = len;
        }
        seek(pfd, newpos);

        /* return the actual number of bytes skipped */
        return (int) (newpos - pos);
    }
}
