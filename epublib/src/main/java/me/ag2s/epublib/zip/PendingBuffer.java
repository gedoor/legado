/* me.ag2s.epublib.zip.PendingBuffer
   Copyright (C) 2001 Free Software Foundation, Inc.

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

/**
 * This class is general purpose class for writing data to a buffer.
 * <p>
 * It allows you to write bits as well as bytes
 * <p>
 * Based on DeflaterPending.java
 *
 * @author Jochen Hoenicke
 * @date Jan 5, 2000
 */

class PendingBuffer {
    protected byte[] buf;
    int start;
    int end;

    int bits;
    int bitCount;

    public PendingBuffer() {
        this(4096);
    }

    public PendingBuffer(int bufsize) {
        buf = new byte[bufsize];
    }

    public final void reset() {
        start = end = bitCount = 0;
    }

    public final void writeByte(int b) {
        if (DeflaterConstants.DEBUGGING && start != 0)
            throw new IllegalStateException();
        buf[end++] = (byte) b;
    }

    public final void writeShort(int s) {
        if (DeflaterConstants.DEBUGGING && start != 0)
            throw new IllegalStateException();
        buf[end++] = (byte) s;
        buf[end++] = (byte) (s >> 8);
    }

    public final void writeInt(int s) {
        if (DeflaterConstants.DEBUGGING && start != 0)
            throw new IllegalStateException();
        buf[end++] = (byte) s;
        buf[end++] = (byte) (s >> 8);
        buf[end++] = (byte) (s >> 16);
        buf[end++] = (byte) (s >> 24);
    }

    public final void writeBlock(byte[] block, int offset, int len) {
        if (DeflaterConstants.DEBUGGING && start != 0)
            throw new IllegalStateException();
        System.arraycopy(block, offset, buf, end, len);
        end += len;
    }

    public final int getBitCount() {
        return bitCount;
    }

    public final void alignToByte() {
        if (DeflaterConstants.DEBUGGING && start != 0)
            throw new IllegalStateException();
        if (bitCount > 0) {
            buf[end++] = (byte) bits;
            if (bitCount > 8)
                buf[end++] = (byte) (bits >>> 8);
        }
        bits = 0;
        bitCount = 0;
    }

    public final void writeBits(int b, int count) {
        if (DeflaterConstants.DEBUGGING && start != 0)
            throw new IllegalStateException();
        if (DeflaterConstants.DEBUGGING)
            System.err.println("writeBits(" + Integer.toHexString(b) + "," + count + ")");
        bits |= b << bitCount;
        bitCount += count;
        if (bitCount >= 16) {
            buf[end++] = (byte) bits;
            buf[end++] = (byte) (bits >>> 8);
            bits >>>= 16;
            bitCount -= 16;
        }
    }

    public final void writeShortMSB(int s) {
        if (DeflaterConstants.DEBUGGING && start != 0)
            throw new IllegalStateException();
        buf[end++] = (byte) (s >> 8);
        buf[end++] = (byte) s;
    }

    public final boolean isFlushed() {
        return end == 0;
    }

    /**
     * Flushes the pending buffer into the given output array.  If the
     * output array is to small, only a partial flush is done.
     *
     * @param output the output array;
     * @param offset the offset into output array;
     * @param length the maximum number of bytes to store;
     * @throws IndexOutOfBoundsException if offset or length are
     *                                   invalid.
     */
    public final int flush(byte[] output, int offset, int length) {
        if (bitCount >= 8) {
            buf[end++] = (byte) bits;
            bits >>>= 8;
            bitCount -= 8;
        }
        if (length > end - start) {
            length = end - start;
            System.arraycopy(buf, start, output, offset, length);
            start = 0;
            end = 0;
        } else {
            System.arraycopy(buf, start, output, offset, length);
            start += length;
        }
        return length;
    }


    public final byte[] toByteArray() {
        byte[] ret = new byte[end - start];
        System.arraycopy(buf, start, ret, 0, ret.length);
        start = 0;
        end = 0;
        return ret;
    }


}

