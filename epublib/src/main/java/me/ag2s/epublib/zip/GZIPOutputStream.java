/* GZIPOutputStream.java - Create a file in gzip format
   Copyright (C) 1999, 2000, 2001 Free Software Foundation, Inc.

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

import java.io.IOException;
import java.io.OutputStream;

/**
 * This filter stream is used to compress a stream into a "GZIP" stream.
 * The "GZIP" format is described in RFC 1952.
 *
 * @author John Leuner
 * @author Tom Tromey
 * @since JDK 1.1
 */

/* Written using on-line Java Platform 1.2 API Specification
 * and JCL book.
 * Believed complete and correct.
 */

public class GZIPOutputStream extends DeflaterOutputStream {
    /**
     * CRC-32 value for uncompressed data
     */
    protected CRC32 crc;

    /* Creates a GZIPOutputStream with the default buffer size
     *
     *
     * @param out The stream to read data (to be compressed) from
     *
     */
    public GZIPOutputStream(OutputStream out) throws IOException {
        this(out, 4096);
    }

    /**
     * Creates a GZIPOutputStream with the specified buffer size
     *
     * @param out  The stream to read compressed data from
     * @param size Size of the buffer to use
     */
    public GZIPOutputStream(OutputStream out, int size) throws IOException {
        super(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true), size);

        crc = new CRC32();
        int mod_time = (int) (System.currentTimeMillis() / 1000L);
        byte[] gzipHeader =
                {
                        /* The two magic bytes */
                        (byte) (GZIPInputStream.GZIP_MAGIC >> 8),
                        (byte) GZIPInputStream.GZIP_MAGIC,

                        /* The compression type */
                        (byte) Deflater.DEFLATED,

                        /* The flags (not set) */
                        0,

                        /* The modification time */
                        (byte) mod_time, (byte) (mod_time >> 8),
                        (byte) (mod_time >> 16), (byte) (mod_time >> 24),

                        /* The extra flags */
                        0,

                        /* The OS type (unknown) */
                        (byte) 255
                };

        out.write(gzipHeader);
        //    System.err.println("wrote GZIP header (" + gzipHeader.length + " bytes )");
    }

    public synchronized void write(byte[] buf, int off, int len)
            throws IOException {
        super.write(buf, off, len);
        crc.update(buf, off, len);
    }

    /**
     * Writes remaining compressed output data to the output stream
     * and closes it.
     */
    public void close() throws IOException {
        finish();
        out.close();
    }

    public void finish() throws IOException {
        super.finish();

        int totalin = def.getTotalIn();
        int crcval = (int) (crc.getValue() & 0xffffffff);

        //    System.err.println("CRC val is " + Integer.toHexString( crcval ) 		       + " and length " + Integer.toHexString(totalin));

        byte[] gzipFooter =
                {
                        (byte) crcval, (byte) (crcval >> 8),
                        (byte) (crcval >> 16), (byte) (crcval >> 24),

                        (byte) totalin, (byte) (totalin >> 8),
                        (byte) (totalin >> 16), (byte) (totalin >> 24)
                };

        out.write(gzipFooter);
        //    System.err.println("wrote GZIP trailer (" + gzipFooter.length + " bytes )");
    }
}
