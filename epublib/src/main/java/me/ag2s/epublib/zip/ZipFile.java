/* me.ag2s.epublib.zip.ZipFile
   Copyright (C) 2001, 2002, 2003 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */

package me.ag2s.epublib.zip;

import android.content.Context;
import android.net.Uri;
import android.system.ErrnoException;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
public class ZipFile implements ZipConstants, Closeable {

    /**
     * Mode flag to open a zip file for reading.
     */
    public static final int OPEN_READ = 0x1;

    /**
     * Mode flag to delete a zip file after reading.
     */
    public static final int OPEN_DELETE = 0x4;

    @NonNull
    public Context getContext() {
        return context;
    }

    public Uri getUri() {
        return Uri.parse(name);
    }


    @NonNull
    private final Context context;

    // Name of this zip file.
    @NonNull
    private final String name;

    // File from which zip entries are read.
    @NonNull
    private final  /*RandomAccessFile*/ AndroidRandomReadableFile araf;


    // The entries of this zip file when initialized and not yet closed.
    private HashMap<String, ZipEntry> entries;

    private boolean closed = false;

    /**
     * Opens a Zip file with the given name for reading.
     *
     * @throws IOException  if a i/o error occured.
     * @throws ZipException if the file doesn't contain a valid zip
     *                      archive.
     */
    public ZipFile(@NonNull Context context, @NonNull Uri uri) throws ZipException, IOException {
        this.context = context;
        this.araf = new AndroidRandomReadableFile(context, uri);
        this.name = uri.toString();
    }


    /**
     * Opens a Zip file reading the given File in the given mode.
     *
     * If the OPEN_DELETE mode is specified, the zip file will be deleted at
     * some time moment after it is opened. It will be deleted before the zip
     * file is closed or the Virtual Machine exits.
     *
     * The contents of the zip file will be accessible until it is closed.
     *
     * The OPEN_DELETE mode is currently unimplemented in this library
     *
     * @since JDK1.3
     * @param mode Must be one of OPEN_READ or OPEN_READ | OPEN_DELETE
     *
     * @exception IOException if a i/o error occured.
     * @exception ZipException if the file doesn't contain a valid zip
     * archive.
     */
//  public ZipFile(File file, int mode) throws ZipException, IOException
//  {
//    if ((mode & OPEN_DELETE) != 0)
//      {
//	throw new IllegalArgumentException
//	  ("OPEN_DELETE mode not supported yet in me.ag2s.epublib.zip.ZipFile");
//      }
//    this.raf = new RandomAccessFile(file, "r");
//    this.name = file.getPath();
//  }

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
    private void readEntries() throws ZipException, IOException, ErrnoException {
        /* Search for the End Of Central Directory.  When a zip comment is
         * present the directory may start earlier.
         * FIXME: This searches the whole file in a very slow manner if the
         * file isn't a zip file.
         */
        long pos = araf.length() - ENDHDR;
        byte[] ebs = new byte[CENHDR];

        do {
            if (pos < 0)
                throw new ZipException
                        ("central directory not found, probably not a zip file: " + name);
            araf.seek(pos--);
        }
        while (readLeInt(araf, ebs) != ENDSIG);

        if (araf.skipBytes(ENDTOT - ENDNRD) != ENDTOT - ENDNRD)
            throw new EOFException(name);
        int count = readLeShort(araf, ebs);
        if (araf.skipBytes(ENDOFF - ENDSIZ) != ENDOFF - ENDSIZ)
            throw new EOFException(name);
        int centralOffset = readLeInt(araf, ebs);

        entries = new HashMap<>(count + count / 2);
        araf.seek(centralOffset);

        byte[] buffer = new byte[16];
        for (int i = 0; i < count; i++) {
            araf.readFully(ebs);
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

            araf.readFully(buffer, 0, nameLen);
            String name = new String(buffer, 0, 0, nameLen);

            ZipEntry entry = new ZipEntry(name);
            entry.setMethod(method);
            entry.setCrc(crc & 0xffffffffL);
            entry.setSize(size & 0xffffffffL);
            entry.setCompressedSize(csize & 0xffffffffL);
            entry.setDOSTime(dostime);
            if (extraLen > 0) {
                byte[] extra = new byte[extraLen];
                araf.readFully(extra);
                entry.setExtra(extra);
            }
            if (commentLen > 0) {
                araf.readFully(buffer, 0, commentLen);
                entry.setComment(new String(buffer, 0, commentLen));
            }
            entry.offset = offset;
            entries.put(name, entry);
        }
    }

    /**
     * Closes the ZipFile.  This also closes all input streams given by
     * this class.  After this is called, no further method should be
     * called.
     *
     * @throws IOException if a i/o error occured.
     */
    @Override
    public void close() throws IOException {
        synchronized (araf) {
            closed = true;
            entries = null;
            araf.close();
        }
    }

    /**
     * Calls the <code>close()</code> method when this ZipFile has not yet
     * been explicitly closed.
     */
    protected void finalize() throws IOException {
        if (!closed && araf != null) close();
    }

    /**
     * Returns an enumeration of all Zip entries in this Zip file.
     */
    public Enumeration<? extends ZipEntry> entries() {
        try {
            return new ZipEntryEnumeration(getEntries().values().iterator());
        } catch (IOException ioe) {
            return null;
        }
    }

    /**
     * Checks that the ZipFile is still open and reads entries when necessary.
     *
     * @throws IllegalStateException when the ZipFile has already been closed.
     * @throws IOException,          ErrnoException when the entries could not be read.
     */
    private HashMap<String, ZipEntry> getEntries() throws IOException {
        synchronized (araf) {
            if (closed)
                throw new IllegalStateException("ZipFile has closed: " + name);

            if (entries == null) {
                try {
                    readEntries();
                } catch (ErrnoException e) {
                    e.printStackTrace();
                }
            }

            return entries;
        }
    }

    /**
     * Searches for a zip entry in this archive with the given name.
     *
     * @param name May contain directory components separated by
     *             slashes ('/').
     * @return the zip entry, or null if no entry with that name exists.
     */
    public ZipEntry getEntry(String name) {
        try {
            HashMap<String, ZipEntry> entries = getEntries();
            ZipEntry entry = entries.get(name);
            return entry != null ? (ZipEntry) entry.clone() : null;
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
    private long checkLocalHeader(ZipEntry entry) throws IOException {
        synchronized (araf) {
            try {
                araf.seek(entry.offset);
            } catch (IOException e) {
                e.printStackTrace();
            }
            araf.readFully(locBuf);

            if (readLeInt(locBuf, 0) != LOCSIG)
                throw new ZipException("Wrong Local header signature: " + name);

            if (entry.getMethod() != readLeShort(locBuf, LOCHOW))
                throw new ZipException("Compression method mismatch: " + name);

            if (entry.getName().length() != readLeShort(locBuf, LOCNAM))
                throw new ZipException("file name length mismatch: " + name);

            int extraLen = entry.getName().length() + readLeShort(locBuf, LOCEXT);
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
    public InputStream getInputStream(ZipEntry entry) throws IOException {
        HashMap<String, ZipEntry> entries = getEntries();
        String name = entry.getName();
        ZipEntry zipEntry = entries.get(name);
        if (zipEntry == null)
            throw new NoSuchElementException(name);

        long start = checkLocalHeader(zipEntry);
        int method = zipEntry.getMethod();
        InputStream is = new BufferedInputStream(new PartialInputStream
                (araf, start, zipEntry.getCompressedSize()));
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
    @NonNull
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

    private static class ZipEntryEnumeration implements Enumeration<ZipEntry> {
        private final Iterator<ZipEntry> elements;

        public ZipEntryEnumeration(Iterator<ZipEntry> elements) {
            this.elements = elements;
        }

        public boolean hasMoreElements() {
            return elements.hasNext();
        }

        public ZipEntry nextElement() {
            /* We return a clone, just to be safe that the user doesn't
             * change the entry.
             */
            return (ZipEntry) elements.next().clone();
        }
    }

    private static class PartialInputStream extends InputStream {
        private final /*RandomAccessFile*/ AndroidRandomReadableFile raf;
        long filepos, end;

        public PartialInputStream(AndroidRandomReadableFile raf, long start, long len) {
            this.raf = raf;
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
            synchronized (raf) {
                try {
                    raf.seek(filepos++);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return raf.read();
            }
        }

        public int read(byte[] b, int off, int len) throws IOException {
            if (len > end - filepos) {
                len = (int) (end - filepos);
                if (len == 0)
                    return -1;
            }
            synchronized (raf) {
                try {
                    raf.seek(filepos);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int count = raf.read(b, off, len);
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
