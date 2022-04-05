/* Adler32.java - Computes Adler32 data checksum of a data stream
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

/*
 * Written using on-line Java Platform 1.2 API Specification, as well
 * as "The Java Class Libraries", 2nd edition (Addison-Wesley, 1998).
 * The actual Adler32 algorithm is taken from RFC 1950.
 * Status:  Believed complete and correct.
 */

/**
 * Computes Adler32 checksum for a stream of data. An Adler32
 * checksum is not as reliable as a CRC32 checksum, but a lot faster to
 * compute.
 * <p>
 * The specification for Adler32 may be found in RFC 1950.
 * (ZLIB Compressed Data Format Specification version 3.3)
 * <p>
 * <p>
 * From that document:
 * <p>
 * "ADLER32 (Adler-32 checksum)
 * This contains a checksum value of the uncompressed data
 * (excluding any dictionary data) computed according to Adler-32
 * algorithm. This algorithm is a 32-bit extension and improvement
 * of the Fletcher algorithm, used in the ITU-T X.224 / ISO 8073
 * standard.
 * <p>
 * Adler-32 is composed of two sums accumulated per byte: s1 is
 * the sum of all bytes, s2 is the sum of all s1 values. Both sums
 * are done modulo 65521. s1 is initialized to 1, s2 to zero.  The
 * Adler-32 checksum is stored as s2*65536 + s1 in most-
 * significant-byte first (network) order."
 * <p>
 * "8.2. The Adler-32 algorithm
 * <p>
 * The Adler-32 algorithm is much faster than the CRC32 algorithm yet
 * still provides an extremely low probability of undetected errors.
 * <p>
 * The modulo on unsigned long accumulators can be delayed for 5552
 * bytes, so the modulo operation time is negligible.  If the bytes
 * are a, b, c, the second sum is 3a + 2b + c + 3, and so is position
 * and order sensitive, unlike the first sum, which is just a
 * checksum.  That 65521 is prime is important to avoid a possible
 * large class of two-byte errors that leave the check unchanged.
 * (The Fletcher checksum uses 255, which is not prime and which also
 * makes the Fletcher check insensitive to single byte changes 0 <->
 * 255.)
 * <p>
 * The sum s1 is initialized to 1 instead of zero to make the length
 * of the sequence part of s2, so that the length does not have to be
 * checked separately. (Any sequence of zeroes has a Fletcher
 * checksum of zero.)"
 *
 * @author John Leuner, Per Bothner
 * @see InflaterInputStream
 * @see DeflaterOutputStream
 * @since JDK 1.1
 */
public class Adler32 implements Checksum {

    /**
     * largest prime smaller than 65536
     */
    private static final int BASE = 65521;

    private int checksum; //we do all in int.

    //Note that java doesn't have unsigned integers,
    //so we have to be careful with what arithmetic
    //we do. We return the checksum as a long to
    //avoid sign confusion.

    /**
     * Creates a new instance of the <code>Adler32</code> class.
     * The checksum starts off with a value of 1.
     */
    public Adler32() {
        reset();
    }

    /**
     * Resets the Adler32 checksum to the initial value.
     */
    public void reset() {
        checksum = 1; //Initialize to 1
    }

    /**
     * Updates the checksum with the byte b.
     *
     * @param bval the data value to add. The high byte of the int is ignored.
     */
    public void update(int bval) {
        //We could make a length 1 byte array and call update again, but I
        //would rather not have that overhead
        int s1 = checksum & 0xffff;
        int s2 = checksum >>> 16;

        s1 = (s1 + (bval & 0xFF)) % BASE;
        s2 = (s1 + s2) % BASE;

        checksum = (s2 << 16) + s1;
    }

    /**
     * Updates the checksum with the bytes taken from the array.
     *
     * @param buffer an array of bytes
     */
    public void update(byte[] buffer) {
        update(buffer, 0, buffer.length);
    }

    /**
     * Updates the checksum with the bytes taken from the array.
     *
     * @param buf an array of bytes
     * @param off the start of the data used for this update
     * @param len the number of bytes to use for this update
     */
    public void update(byte[] buf, int off, int len) {
        //(By Per Bothner)
        int s1 = checksum & 0xffff;
        int s2 = checksum >>> 16;

        while (len > 0) {
            // We can defer the modulo operation:
            // s1 maximally grows from 65521 to 65521 + 255 * 3800
            // s2 maximally grows by 3800 * median(s1) = 2090079800 < 2^31
            int n = 3800;
            if (n > len)
                n = len;
            len -= n;
            while (--n >= 0) {
                s1 = s1 + (buf[off++] & 0xFF);
                s2 = s2 + s1;
            }
            s1 %= BASE;
            s2 %= BASE;
        }

    /*Old implementation, borrowed from somewhere:
    int n;
    
    while (len-- > 0) {

      s1 = (s1 + (bs[offset++] & 0xff)) % BASE; 
      s2 = (s2 + s1) % BASE;
    }*/

        checksum = (s2 << 16) | s1;
    }

    /**
     * Returns the Adler32 data checksum computed so far.
     */
    public long getValue() {
        return (long) checksum & 0xffffffffL;
    }
}
