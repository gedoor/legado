/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.ag2s.epublib.util.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * This class is used to wrap a stream that includes an encoded
 * {@link ByteOrderMark} as its first bytes.
 *
 * This class detects these bytes and, if required, can automatically skip them
 * and return the subsequent byte as the first byte in the stream.
 *
 * The {@link ByteOrderMark} implementation has the following pre-defined BOMs:
 * <ul>
 *   <li>UTF-8 - {@link ByteOrderMark#UTF_8}</li>
 *   <li>UTF-16BE - {@link ByteOrderMark#UTF_16LE}</li>
 *   <li>UTF-16LE - {@link ByteOrderMark#UTF_16BE}</li>
 * </ul>
 *
 *
 * <h3>Example 1 - Detect and exclude a UTF-8 BOM</h3>
 * <pre>
 *      BOMInputStream bomIn = new BOMInputStream(in);
 *      if (bomIn.hasBOM()) {
 *          // has a UTF-8 BOM
 *      }
 * </pre>
 *
 * <h3>Example 2 - Detect a UTF-8 BOM (but don't exclude it)</h3>
 * <pre>
 *      boolean include = true;
 *      BOMInputStream bomIn = new BOMInputStream(in, include);
 *      if (bomIn.hasBOM()) {
 *          // has a UTF-8 BOM
 *      }
 * </pre>
 *
 * <h3>Example 3 - Detect Multiple BOMs</h3>
 * <pre>
 *      BOMInputStream bomIn = new BOMInputStream(in, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE);
 *      if (bomIn.hasBOM() == false) {
 *          // No BOM found
 *      } else if (bomIn.hasBOM(ByteOrderMark.UTF_16LE)) {
 *          // has a UTF-16LE BOM
 *      } else if (bomIn.hasBOM(ByteOrderMark.UTF_16BE)) {
 *          // has a UTF-16BE BOM
 *      }
 * </pre>
 *
 * @see ByteOrderMark
 * @see <a href="http://en.wikipedia.org/wiki/Byte_order_mark">Wikipedia - Byte Order Mark</a>
 * @version $Revision: 1052095 $ $Date: 2010-12-22 23:03:20 +0000 (Wed, 22 Dec 2010) $
 * @since Commons IO 2.0
 */
public class BOMInputStream extends ProxyInputStream {
    private final boolean include;
    private final List<ByteOrderMark> boms;
    private ByteOrderMark byteOrderMark;
    private int[] firstBytes;
    private int fbLength;
    private int fbIndex;
    private int markFbIndex;
    private boolean markedAtStart;

    /**
     * Constructs a new BOM InputStream that excludes
     * a {@link ByteOrderMark#UTF_8} BOM.
     * @param delegate the InputStream to delegate to
     */
    @SuppressWarnings("unused")
    public BOMInputStream(InputStream delegate) {
        this(delegate, false, ByteOrderMark.UTF_8);
    }

    /**
     * Constructs a new BOM InputStream that detects a
     * a {@link ByteOrderMark#UTF_8} and optionally includes it.
     * @param delegate the InputStream to delegate to
     * @param include true to include the UTF-8 BOM or
     * false to exclude it
     */
    @SuppressWarnings("unused")
    public BOMInputStream(InputStream delegate, boolean include) {
        this(delegate, include, ByteOrderMark.UTF_8);
    }

    /**
     * Constructs a new BOM InputStream that excludes
     * the specified BOMs.
     * @param delegate the InputStream to delegate to
     * @param boms The BOMs to detect and exclude
     */
    @SuppressWarnings("unused")
    public BOMInputStream(InputStream delegate, ByteOrderMark... boms) {
        this(delegate, false, boms);
    }

    /**
     * Constructs a new BOM InputStream that detects the
     * specified BOMs and optionally includes them.
     * @param delegate the InputStream to delegate to
     * @param include true to include the specified BOMs or
     * false to exclude them
     * @param boms The BOMs to detect and optionally exclude
     */
    @SuppressWarnings("unused")
    public BOMInputStream(InputStream delegate, boolean include, ByteOrderMark... boms) {
        super(delegate);
        if (boms == null || boms.length == 0) {
            throw new IllegalArgumentException("No BOMs specified");
        }
        this.include = include;
        this.boms = Arrays.asList(boms);
    }

    /**
     * Indicates whether the stream contains one of the specified BOMs.
     *
     * @return true if the stream has one of the specified BOMs, otherwise false
     * if it does not
     * @throws IOException if an error reading the first bytes of the stream occurs
     */
    @SuppressWarnings("unused")
    public boolean hasBOM() throws IOException {
        return (getBOM() != null);
    }

    /**
     * Indicates whether the stream contains the specified BOM.
     *
     * @param bom The BOM to check for
     * @return true if the stream has the specified BOM, otherwise false
     * if it does not
     * @throws IllegalArgumentException if the BOM is not one the stream
     * is configured to detect
     * @throws IOException if an error reading the first bytes of the stream occurs
     */
    @SuppressWarnings("unused")
    public boolean hasBOM(ByteOrderMark bom) throws IOException {
        if (!boms.contains(bom)) {
            throw new IllegalArgumentException("Stream not configure to detect " + bom);
        }
        return (byteOrderMark != null && getBOM().equals(bom));
    }

    /**
     * Return the BOM (Byte Order Mark).
     *
     * @return The BOM or null if none
     * @throws IOException if an error reading the first bytes of the stream occurs
     */
    public ByteOrderMark getBOM() throws IOException {
        if (firstBytes == null) {
            int max = 0;
            for (ByteOrderMark bom : boms) {
                max = Math.max(max, bom.length());
            }
            firstBytes = new int[max];
            for (int i = 0; i < firstBytes.length; i++) {
                firstBytes[i] = in.read();
                fbLength++;
                if (firstBytes[i] < 0) {
                    break;
                }

                byteOrderMark = find();
                if (byteOrderMark != null) {
                    if (!include) {
                        fbLength = 0;
                    }
                    break;
                }
            }
        }
        return byteOrderMark;
    }

    /**
     * Return the BOM charset Name - {@link ByteOrderMark#getCharsetName()}.
     *
     * @return The BOM charset Name or null if no BOM found
     * @throws IOException if an error reading the first bytes of the stream occurs
     * 
     */
    public String getBOMCharsetName() throws IOException {
        getBOM();
        return (byteOrderMark == null ? null : byteOrderMark.getCharsetName());
    }

    /**
     * This method reads and either preserves or skips the first bytes in the
     * stream. It behaves like the single-byte <code>read()</code> method,
     * either returning a valid byte or -1 to indicate that the initial bytes
     * have been processed already.
     * @return the byte read (excluding BOM) or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     */
    private int readFirstBytes() throws IOException {
        getBOM();
        return (fbIndex < fbLength) ? firstBytes[fbIndex++] : -1;
    }

    /**
     * Find a BOM with the specified bytes.
     *
     * @return The matched BOM or null if none matched
     */
    private ByteOrderMark find() {
        for (ByteOrderMark bom : boms) {
            if (matches(bom)) {
                return bom;
            }
        }
        return null;
    }

    /**
     * Check if the bytes match a BOM.
     *
     * @param bom The BOM
     * @return true if the bytes match the bom, otherwise false
     */
    private boolean matches(ByteOrderMark bom) {
        if (bom.length() != fbLength) {
            return false;
        }
        for (int i = 0; i < bom.length(); i++) {
            if (bom.get(i) != firstBytes[i]) {
                return false;
            }
        }
        return true;
    }

    //----------------------------------------------------------------------------
    //  Implementation of InputStream
    //----------------------------------------------------------------------------

    /**
     * Invokes the delegate's <code>read()</code> method, detecting and
     * optionally skipping BOM.
     * @return the byte read (excluding BOM) or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        int b = readFirstBytes();
        return (b >= 0) ? b : in.read();
    }

    /**
     * Invokes the delegate's <code>read(byte[], int, int)</code> method, detecting
     * and optionally skipping BOM.
     * @param buf the buffer to read the bytes into
     * @param off The start offset
     * @param len The number of bytes to read (excluding BOM)
     * @return the number of bytes read or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        int firstCount = 0;
        int b = 0;
        while ((len > 0) && (b >= 0)) {
            b = readFirstBytes();
            if (b >= 0) {
                buf[off++] = (byte) (b & 0xFF);
                len--;
                firstCount++;
            }
        }
        int secondCount = in.read(buf, off, len);
        return (secondCount < 0) ? (firstCount > 0 ? firstCount : -1) : firstCount + secondCount;
    }

    /**
     * Invokes the delegate's <code>read(byte[])</code> method, detecting and
     * optionally skipping BOM.
     * @param buf the buffer to read the bytes into
     * @return the number of bytes read (excluding BOM)
     * or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    /**
     * Invokes the delegate's <code>mark(int)</code> method.
     * @param readlimit read ahead limit
     */
    @Override
    public synchronized void mark(int readlimit) {
        markFbIndex = fbIndex;
        markedAtStart = (firstBytes == null);
        in.mark(readlimit);
    }

    /**
     * Invokes the delegate's <code>reset()</code> method.
     * @throws IOException if an I/O error occurs
     */
    @Override
    public synchronized void reset() throws IOException {
        fbIndex = markFbIndex;
        if (markedAtStart) {
            firstBytes = null;
        }

        in.reset();
    }

    /**
     * Invokes the delegate's <code>skip(long)</code> method, detecting
     * and optionallyskipping BOM.
     * @param n the number of bytes to skip
     * @return the number of bytes to skipped or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long skip(long n) throws IOException {
        while ((n > 0) && (readFirstBytes() >= 0)) {
            n--;
        }
        return in.skip(n);
    }
}
