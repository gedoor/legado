/* me.ag2s.epublib.zip.OutputWindow
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

/*
 * Contains the output from the Inflation process.
 *
 * We need to have a window so that we can refer backwards into the output stream
 * to repeat stuff.
 *
 * @author John Leuner
 * @since JDK 1.1
 */

class OutputWindow {
    private final int WINDOW_SIZE = 1 << 15;
    private final int WINDOW_MASK = WINDOW_SIZE - 1;

    private byte[] window = new byte[WINDOW_SIZE]; //The window is 2^15 bytes
    private int window_end = 0;
    private int window_filled = 0;

    public void write(int abyte) {
        if (window_filled++ == WINDOW_SIZE)
            throw new IllegalStateException("Window full");
        window[window_end++] = (byte) abyte;
        window_end &= WINDOW_MASK;
    }


    private final void slowRepeat(int rep_start, int len, int dist) {
        while (len-- > 0) {
            window[window_end++] = window[rep_start++];
            window_end &= WINDOW_MASK;
            rep_start &= WINDOW_MASK;
        }
    }

    public void repeat(int len, int dist) {
        if ((window_filled += len) > WINDOW_SIZE)
            throw new IllegalStateException("Window full");

        int rep_start = (window_end - dist) & WINDOW_MASK;
        int border = WINDOW_SIZE - len;
        if (rep_start <= border && window_end < border) {
            if (len <= dist) {
                System.arraycopy(window, rep_start, window, window_end, len);
                window_end += len;
            } else {
                /* We have to copy manually, since the repeat pattern overlaps.
                 */
                while (len-- > 0)
                    window[window_end++] = window[rep_start++];
            }
        } else
            slowRepeat(rep_start, len, dist);
    }

    public int copyStored(StreamManipulator input, int len) {
        len = Math.min(Math.min(len, WINDOW_SIZE - window_filled),
                input.getAvailableBytes());
        int copied;

        int tailLen = WINDOW_SIZE - window_end;
        if (len > tailLen) {
            copied = input.copyBytes(window, window_end, tailLen);
            if (copied == tailLen)
                copied += input.copyBytes(window, 0, len - tailLen);
        } else
            copied = input.copyBytes(window, window_end, len);

        window_end = (window_end + copied) & WINDOW_MASK;
        window_filled += copied;
        return copied;
    }

    public void copyDict(byte[] dict, int offset, int len) {
        if (window_filled > 0)
            throw new IllegalStateException();

        if (len > WINDOW_SIZE) {
            offset += len - WINDOW_SIZE;
            len = WINDOW_SIZE;
        }
        System.arraycopy(dict, offset, window, 0, len);
        window_end = len & WINDOW_MASK;
    }

    public int getFreeSpace() {
        return WINDOW_SIZE - window_filled;
    }

    public int getAvailable() {
        return window_filled;
    }

    public int copyOutput(byte[] output, int offset, int len) {
        int copy_end = window_end;
        if (len > window_filled)
            len = window_filled;
        else
            copy_end = (window_end - window_filled + len) & WINDOW_MASK;

        int copied = len;
        int tailLen = len - copy_end;

        if (tailLen > 0) {
            System.arraycopy(window, WINDOW_SIZE - tailLen,
                    output, offset, tailLen);
            offset += tailLen;
            len = copy_end;
        }
        System.arraycopy(window, copy_end - len, output, offset, len);
        window_filled -= copied;
        if (window_filled < 0)
            throw new IllegalStateException();
        return copied;
    }

    public void reset() {
        window_filled = window_end = 0;
    }
}



