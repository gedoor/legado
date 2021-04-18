package me.ag2s.epublib.util.commons.io;

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

import java.io.Serializable;
import java.util.Locale;

/**
 * Byte Order Mark (BOM) representation - see {@link BOMInputStream}.
 *
 * @see BOMInputStream
 * @see <a href="http://en.wikipedia.org/wiki/Byte_order_mark">Wikipedia: Byte Order Mark</a>
 * @see <a href="http://www.w3.org/TR/2006/REC-xml-20060816/#sec-guessing">W3C: Autodetection of Character Encodings
 *      (Non-Normative)</a>
 * @since 2.0
 */
public class ByteOrderMark implements Serializable {

    private static final long serialVersionUID = 1L;

    /** UTF-8 BOM */
    public static final ByteOrderMark UTF_8    = new ByteOrderMark("UTF-8",    0xEF, 0xBB, 0xBF);

    /** UTF-16BE BOM (Big-Endian) */
    public static final ByteOrderMark UTF_16BE = new ByteOrderMark("UTF-16BE", 0xFE, 0xFF);

    /** UTF-16LE BOM (Little-Endian) */
    public static final ByteOrderMark UTF_16LE = new ByteOrderMark("UTF-16LE", 0xFF, 0xFE);

    /**
     * UTF-32BE BOM (Big-Endian)
     * @since 2.2
     */
    public static final ByteOrderMark UTF_32BE = new ByteOrderMark("UTF-32BE", 0x00, 0x00, 0xFE, 0xFF);

    /**
     * UTF-32LE BOM (Little-Endian)
     * @since 2.2
     */
    public static final ByteOrderMark UTF_32LE = new ByteOrderMark("UTF-32LE", 0xFF, 0xFE, 0x00, 0x00);

    /**
     * Unicode BOM character; external form depends on the encoding.
     * @see <a href="http://unicode.org/faq/utf_bom.html#BOM">Byte Order Mark (BOM) FAQ</a>
     * @since 2.5
     */
    @SuppressWarnings("unused")
    public static final char UTF_BOM = '\uFEFF';

    private final String charsetName;
    private final int[] bytes;

    /**
     * Construct a new BOM.
     *
     * @param charsetName The name of the charset the BOM represents
     * @param bytes The BOM's bytes
     * @throws IllegalArgumentException if the charsetName is null or
     * zero length
     * @throws IllegalArgumentException if the bytes are null or zero
     * length
     */
    public ByteOrderMark(final String charsetName, final int... bytes) {
        if (charsetName == null || charsetName.isEmpty()) {
            throw new IllegalArgumentException("No charsetName specified");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("No bytes specified");
        }
        this.charsetName = charsetName;
        this.bytes = new int[bytes.length];
        System.arraycopy(bytes, 0, this.bytes, 0, bytes.length);
    }

    /**
     * Return the name of the {@link java.nio.charset.Charset} the BOM represents.
     *
     * @return the character set name
     */
    public String getCharsetName() {
        return charsetName;
    }

    /**
     * Return the length of the BOM's bytes.
     *
     * @return the length of the BOM's bytes
     */
    public int length() {
        return bytes.length;
    }

    /**
     * The byte at the specified position.
     *
     * @param pos The position
     * @return The specified byte
     */
    public int get(final int pos) {
        return bytes[pos];
    }

    /**
     * Return a copy of the BOM's bytes.
     *
     * @return a copy of the BOM's bytes
     */
    public byte[] getBytes() {
        final byte[] copy = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            copy[i] = (byte)bytes[i];
        }
        return copy;
    }

    /**
     * Indicates if this BOM's bytes equals another.
     *
     * @param obj The object to compare to
     * @return true if the bom's bytes are equal, otherwise
     * false
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ByteOrderMark)) {
            return false;
        }
        final ByteOrderMark bom = (ByteOrderMark)obj;
        if (bytes.length != bom.length()) {
            return false;
        }
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != bom.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return the hashcode for this BOM.
     *
     * @return the hashcode for this BOM.
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int hashCode = getClass().hashCode();
        for (final int b : bytes) {
            hashCode += b;
        }
        return hashCode;
    }

    /**
     * Provide a String representation of the BOM.
     *
     * @return the length of the BOM's bytes
     */
    @Override
    @SuppressWarnings("NullableProblems")
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append('[');
        builder.append(charsetName);
        builder.append(": ");
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append("0x");
            builder.append(Integer.toHexString(0xFF & bytes[i]).toUpperCase(Locale.ROOT));
        }
        builder.append(']');
        return builder.toString();
    }

}