

package me.ag2s.epublib.util.zip;

import static me.ag2s.base.PfdHelper.seek;

import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipOutputStream;

import me.ag2s.base.PfdHelper;

/**
 * This class represents a Zip archive.  You can ask for the contained
 * entries, or get an input stream for a file entry.  The entry is
 * automatically decompressed.
 * <p>
 * This class is thread safe:  You can open input streams for arbitrary
 * entries in different threads.
 *
 * @author Jochen Hoenicke
 * @author Artur Biesiadowski
 */
public class AndroidZipFile implements ZipConstants {

    /**
     * Mode flag to open a zip file for reading.
     */
    public static final int OPEN_READ = 0x1;

    /**
     * Mode flag to delete a zip file after reading.
     */
    public static final int OPEN_DELETE = 0x4;

    // Name of this zip file.
    private final String name;

    // File from which zip entries are read.
    //private final RandomAccessFile raf;

    private final ParcelFileDescriptor pfd;

    // The entries of this zip file when initialized and not yet closed.
    private HashMap<String, AndroidZipEntry> entries;

    private boolean closed = false;

    /**
     * Opens a Zip file with the given name for reading.
     *
     * @throws IOException  if a i/o error occured.
     * @throws ZipException if the file doesn't contain a valid zip
     *                      archive.
     */
    public AndroidZipFile(@NonNull ParcelFileDescriptor pfd, String name) throws ZipException, IOException {
        this.pfd = pfd;
        this.name = name;
    }

    /**
     * Opens a Zip file reading the given File.
     *
     * @throws IOException  if a i/o error occured.
     * @throws ZipException if the file doesn't contain a valid zip
     *                      archive.
     */
    public AndroidZipFile(File file) throws ZipException, IOException {
        this.pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        this.name = file.getPath();
    }

    /**
     * Opens a Zip file reading the given File in the given mode.
     * <p>
     * If the OPEN_DELETE mode is specified, the zip file will be deleted at
     * some time moment after it is opened. It will be deleted before the zip
     * file is closed or the Virtual Machine exits.
     * <p>
     * The contents of the zip file will be accessible until it is closed.
     * <p>
     * The OPEN_DELETE mode is currently unimplemented in this library
     *
     * @param mode Must be one of OPEN_READ or OPEN_READ | OPEN_DELETE
     * @throws IOException  if a i/o error occured.
     * @throws ZipException if the file doesn't contain a valid zip
     *                      archive.
     * @since JDK1.3
     */
//    public AndroidZipFile(File file, int mode) throws ZipException, IOException {
//        if ((mode & OPEN_DELETE) != 0) {
//            throw new IllegalArgumentException
//                    ("OPEN_DELETE mode not supported yet in net.sf.jazzlib.AndroidZipFile");
//        }
//        this.raf = new RandomAccessFile(file, "r");
//        this.name = file.getPath();
//    }

    /**
     * Read an unsigned short in little endian byte order from the given
     * DataInput stream using the given byte buffer.
     *
     * @param di DataInput stream to read from.
     * @param b  the byte buffer to read in (must be at least 2 bytes long).
     * @return The value read.
     * @throws IOException  if a i/o error occured.
     * @throws EOFException if the file ends prematurely
     */
    private final int readLeShort(DataInput di, byte[] b) throws IOException {
        di.readFully(b, 0, 2);
        return (b[0] & 0xff) | (b[1] & 0xff) << 8;
    }

    private final int readLeShort(ParcelFileDescriptor pfd, byte[] b) throws IOException {
        PfdHelper.readFully(pfd, b, 0, 2);//di.readFully(b, 0, 2);
        return (b[0] & 0xff) | (b[1] & 0xff) << 8;
    }

    /**
     * Read an int in little endian byte order from the given
     * DataInput stream using the given byte buffer.
     *
     * @param di DataInput stream to read from.
     * @param b  the byte buffer to read in (must be at least 4 bytes long).
     * @return The value read.
     * @throws IOException  if a i/o error occured.
     * @throws EOFException if the file ends prematurely
     */
    private final int readLeInt(DataInput di, byte[] b) throws IOException {
        di.readFully(b, 0, 4);
        return ((b[0] & 0xff) | (b[1] & 0xff) << 8)
                | ((b[2] & 0xff) | (b[3] & 0xff) << 8) << 16;
    }

    private final int readLeInt(ParcelFileDescriptor pfd, byte[] b) throws IOException {
        PfdHelper.readFully(pfd, b, 0, 4);//di.readFully(b, 0, 4);
        return ((b[0] & 0xff) | (b[1] & 0xff) << 8)
                | ((b[2] & 0xff) | (b[3] & 0xff) << 8) << 16;
    }


    /**
     * Read an unsigned short in little endian byte order from the given
     * byte buffer at the given offset.
     *
     * @param b   the byte array to read from.
     * @param off the offset to read from.
     * @return The value read.
     */
    private final int readLeShort(byte[] b, int off) {
        return (b[off] & 0xff) | (b[off + 1] & 0xff) << 8;
    }

    /**
     * Read an int in little endian byte order from the given
     * byte buffer at the given offset.
     *
     * @param b   the byte array to read from.
     * @param off the offset to read from.
     * @return The value read.
     */
    private final int readLeInt(byte[] b, int off) {
        return ((b[off] & 0xff) | (b[off + 1] & 0xff) << 8)
                | ((b[off + 2] & 0xff) | (b[off + 3] & 0xff) << 8) << 16;
    }


    /**
     * Read the central directory of a zip file and fill the entries
     * array.  This is called exactly once when first needed. It is called
     * while holding the lock on <code>raf</code>.
     *
     * @throws IOException  if a i/o error occured.
     * @throws ZipException if the central directory is malformed
     */
    private void readEntries() throws ZipException, IOException {
        /* Search for the End Of Central Directory.  When a zip comment is
         * present the directory may start earlier.
         * FIXME: This searches the whole file in a very slow manner if the
         * file isn't a zip file.
         */
        //long pos = raf.length() - ENDHDR;
        long pos = PfdHelper.length(pfd) - ENDHDR;
        byte[] ebs = new byte[CENHDR];

        do {
            if (pos < 0)
                throw new ZipException
                        ("central directory not found, probably not a zip file: " + name);
            //raf.seek(pos--);
            seek(pfd, pos--);
        }
        //while (readLeInt(raf, ebs) != ENDSIG);
        while (readLeInt(pfd, ebs) != ENDSIG);

        if (PfdHelper.skipBytes(pfd, ENDTOT - ENDNRD) != ENDTOT - ENDNRD)
            throw new EOFException(name);
        //int count = readLeShort(raf, ebs);
        int count = readLeShort(pfd, ebs);
        if (PfdHelper.skipBytes(pfd, ENDOFF - ENDSIZ) != ENDOFF - ENDSIZ)
            throw new EOFException(name);
        int centralOffset = readLeInt(pfd, ebs);

        entries = new HashMap<>(count + count / 2);
        //raf.seek(centralOffset);
        seek(pfd, centralOffset);

        byte[] buffer = new byte[16];
        for (int i = 0; i < count; i++) {
            //raf.readFully(ebs);
            PfdHelper.readFully(pfd, ebs);
            if (readLeInt(ebs, 0) != CENSIG)
                throw new ZipException("Wrong Central Directory signature: " + name);

            int method = readLeShort(ebs, CENHOW);
            int dostime = readLeInt(ebs, CENTIM);
            int crc = readLeInt(ebs, CENCRC);
            int csize = readLeInt(ebs, CENSIZ);
            int size = readLeInt(ebs, CENLEN);
            int nameLen = readLeShort(ebs, CENNAM);
            int extraLen = readLeShort(ebs, CENEXT);
            int commentLen = readLeShort(ebs, CENCOM);

            int offset = readLeInt(ebs, CENOFF);

            int needBuffer = Math.max(nameLen, commentLen);
            if (buffer.length < needBuffer)
                buffer = new byte[needBuffer];

            PfdHelper.readFully(pfd, buffer, 0, nameLen);
            String name = new String(buffer, 0, nameLen);

            AndroidZipEntry entry = new AndroidZipEntry(name, nameLen);
            entry.setMethod(method);
            entry.setCrc(crc & 0xffffffffL);
            entry.setSize(size & 0xffffffffL);
            entry.setCompressedSize(csize & 0xffffffffL);
            entry.setTime(dostime);
            if (extraLen > 0) {
                byte[] extra = new byte[extraLen];
                PfdHelper.readFully(pfd, extra);
                entry.setExtra(extra);
            }
            if (commentLen > 0) {
                PfdHelper.readFully(pfd, buffer, 0, commentLen);
                entry.setComment(new String(buffer, 0, commentLen));
            }
            entry.offset = offset;
            //ZipEntryHelper.setOffset(entry,offset);

            //entry. = offset;
            entries.put(name, entry);
        }
    }

    /**
     * Closes the AndroidZipFile.  This also closes all input streams given by
     * this class.  After this is called, no further method should be
     * called.
     *
     * @throws IOException if a i/o error occured.
     */
    public void close() throws IOException {
        synchronized (pfd) {
            closed = true;
            entries = null;
            pfd.close();
        }
    }

    /**
     * Calls the <code>close()</code> method when this AndroidZipFile has not yet
     * been explicitly closed.
     */
    protected void finalize() throws IOException {
        if (!closed && pfd != null) close();
    }

    /**
     * Returns an enumeration of all Zip entries in this Zip file.
     */
    public Enumeration<AndroidZipEntry> entries() {
        try {
            return new ZipEntryEnumeration(getEntries().values().iterator());
        } catch (IOException ioe) {
            return null;
        }
    }

    /**
     * Checks that the AndroidZipFile is still open and reads entries when necessary.
     *
     * @throws IllegalStateException when the AndroidZipFile has already been closed.
     * @throws java,                 IOEexception          when the entries could not be read.
     */
    private HashMap<String, AndroidZipEntry> getEntries() throws IOException {
        synchronized (pfd) {
            if (closed)
                throw new IllegalStateException("AndroidZipFile has closed: " + name);

            if (entries == null)
                readEntries();

            return entries;
        }
    }

    /**
     * Searches for a zip entry in this archive with the given name.
     *
     * @param name name. May contain directory components separated by
     *             slashes ('/').
     * @return the zip entry, or null if no entry with that name exists.
     */
    public AndroidZipEntry getEntry(String name) {
        try {
            HashMap<String, AndroidZipEntry> entries = getEntries();
            AndroidZipEntry entry = entries.get(name);
            return entry != null ? (AndroidZipEntry) entry.clone() : null;
        } catch (IOException ioe) {
            return null;
        }
    }


    //access should be protected by synchronized(raf)
    private final byte[] locBuf = new byte[LOCHDR];

    /**
     * Checks, if the local header of the entry at index i matches the
     * central directory, and returns the offset to the data.
     *
     * @param entry to check.
     * @return the start offset of the (compressed) data.
     * @throws IOException  if a i/o error occured.
     * @throws ZipException if the local header doesn't match the
     *                      central directory header
     */
    private long checkLocalHeader(AndroidZipEntry entry) throws IOException {
        synchronized (pfd) {
            seek(pfd, entry.offset);
            PfdHelper.readFully(pfd, locBuf);

            if (readLeInt(locBuf, 0) != LOCSIG)
                throw new ZipException("Wrong Local header signature: " + name);

            if (entry.getMethod() != readLeShort(locBuf, LOCHOW))
                throw new ZipException("Compression method mismatch: " + name);

            if (entry.getNameLen() != readLeShort(locBuf, LOCNAM))
                throw new ZipException("file name length mismatch: " + name);

            int extraLen = entry.getNameLen() + readLeShort(locBuf, LOCEXT);
            return entry.offset + LOCHDR + extraLen;
        }
    }

    /**
     * Creates an input stream reading the given zip entry as
     * uncompressed data.  Normally zip entry should be an entry
     * returned by getEntry() or entries().
     *
     * @param entry the entry to create an InputStream for.
     * @return the input stream.
     * @throws IOException  if a i/o error occured.
     * @throws ZipException if the Zip archive is malformed.
     */
    public InputStream getInputStream(AndroidZipEntry entry) throws IOException {
        HashMap<String, AndroidZipEntry> entries = getEntries();
        String name = entry.getName();
        AndroidZipEntry zipEntry = (AndroidZipEntry) entries.get(name);
        if (zipEntry == null)
            throw new NoSuchElementException(name);

        long start = checkLocalHeader((AndroidZipEntry) zipEntry);
        int method = zipEntry.getMethod();
        InputStream is = new BufferedInputStream(new PartialInputStream
                (pfd, start, zipEntry.getCompressedSize()));
        switch (method) {
            case ZipOutputStream.STORED:
                return is;
            case ZipOutputStream.DEFLATED:
                return new InflaterInputStream(is, new Inflater(true));
            default:
                throw new ZipException("Unknown compression method " + method);
        }
    }

    /**
     * Returns the (path) name of this zip file.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the number of entries in this zip file.
     */
    public int size() {
        try {
            return getEntries().size();
        } catch (IOException ioe) {
            return 0;
        }
    }

    private static class ZipEntryEnumeration implements Enumeration<AndroidZipEntry> {
        private final Iterator<AndroidZipEntry> elements;

        public ZipEntryEnumeration(Iterator<AndroidZipEntry> elements) {
            this.elements = elements;
        }

        public boolean hasMoreElements() {
            return elements.hasNext();
        }

        public AndroidZipEntry nextElement() {
            /* We return a clone, just to be safe that the user doesn't
             * change the entry.
             */
            return (AndroidZipEntry) (elements.next()).clone();
        }
    }

    private static class PartialInputStream extends InputStream {
        private final ParcelFileDescriptor pfd;
        long filepos, end;

        public PartialInputStream(ParcelFileDescriptor pfd, long start, long len) {
            this.pfd = pfd;
            filepos = start;
            end = start + len;
        }

        public int available() {
            long amount = end - filepos;
            if (amount > Integer.MAX_VALUE)
                return Integer.MAX_VALUE;
            return (int) amount;
        }

        public int read() throws IOException {
            if (filepos == end)
                return -1;
            synchronized (pfd) {
                seek(pfd, filepos++);
                return PfdHelper.read(pfd);
            }
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (len > end - filepos) {
                len = (int) (end - filepos);
                if (len == 0)
                    return -1;
            }
            synchronized (pfd) {
                seek(pfd, filepos);
                int count = PfdHelper.read(pfd, b, off, len);
                if (count > 0)
                    filepos += len;
                return count;
            }
        }

        public long skip(long amount) {
            if (amount < 0)
                throw new IllegalArgumentException();
            if (amount > end - filepos)
                amount = end - filepos;
            filepos += amount;
            return amount;
        }
    }
}
