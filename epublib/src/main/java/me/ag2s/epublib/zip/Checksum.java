/* Checksum.java - Interface to compute a data checksum
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
 * Status:  Believed complete and correct.
 */

/**
 * Interface to compute a data checksum used by checked input/output streams.
 * A data checksum can be updated by one byte or with a byte array. After each
 * update the value of the current checksum can be returned by calling
 * <code>getValue</code>. The complete checksum object can also be reset
 * so it can be used again with new data.
 *
 * @author Per Bothner
 * @author Jochen Hoenicke
 * @see CheckedInputStream
 * @see CheckedOutputStream
 */
public interface Checksum {
    /**
     * Returns the data checksum computed so far.
     */
    long getValue();

    /**
     * Resets the data checksum as if no update was ever called.
     */
    void reset();

    /**
     * Adds one byte to the data checksum.
     *
     * @param bval the data value to add. The high byte of the int is ignored.
     */
    void update(int bval);

    /**
     * Adds the byte array to the data checksum.
     *
     * @param buf the buffer which contains the data
     * @param off the offset in the buffer where the data starts
     * @param len the length of the data
     */
    void update(byte[] buf, int off, int len);
}
