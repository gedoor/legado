/* CheckedInputStream.java - Compute checksum of data being read
   Copyright (C) 1999, 2000 Free Software Foundation, Inc.

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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/* Written using on-line Java Platform 1.2 API Specification
 * and JCL book.
 * Believed complete and correct.
 */

/**
 * InputStream that computes a checksum of the data being read using a
 * supplied Checksum object.
 *
 * @author Tom Tromey
 * @date May 17, 1999
 * @see Checksum
 */
public class CheckedInputStream extends FilterInputStream {
  /**
   * Creates a new CheckInputStream on top of the supplied OutputStream
   * using the supplied Checksum.
   */
  public CheckedInputStream(InputStream in, Checksum sum) {
    super(in);
    this.sum = sum;
  }

  /**
   * Returns the Checksum object used. To get the data checksum computed so
   * far call <code>getChecksum.getValue()</code>.
   */
  public Checksum getChecksum() {
    return sum;
  }

  /**
   * Reads one byte, updates the checksum and returns the read byte
   * (or -1 when the end of file was reached).
   */
  public int read() throws IOException {
    int x = in.read();
    if (x != -1)
      sum.update(x);
    return x;
  }

  /**
   * Reads at most len bytes in the supplied buffer and updates the checksum
   * with it. Returns the number of bytes actually read or -1 when the end
   * of file was reached.
   */
  public int read(byte[] buf, int off, int len) throws IOException {
    int r = in.read(buf, off, len);
    if (r != -1)
      sum.update(buf, off, r);
    return r;
  }

  /**
   * Skips n bytes by reading them in a temporary buffer and updating the
   * the checksum with that buffer. Returns the actual number of bytes skiped
   * which can be less then requested when the end of file is reached.
   */
  public long skip(long n) throws IOException {
    if (n == 0)
      return 0;

    int min = (int) Math.min(n, 1024);
    byte[] buf = new byte[min];

    long s = 0;
    while (n > 0) {
      int r = in.read(buf, 0, min);
      if (r == -1)
        break;
      n -= r;
      s += r;
      min = (int) Math.min(n, 1024);
      sum.update(buf, 0, r);
    }

    return s;
  }

  /**
   * The checksum object.
   */
  private final Checksum sum;
}
