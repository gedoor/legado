/* DeflaterOutputStream.java - Output filter for compressing.
   Copyright (C) 1999, 2000, 2001, 2004 Free Software Foundation, Inc.

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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/* Written using on-line Java Platform 1.2 API Specification
 * and JCL book.
 * Believed complete and correct.
 */

/**
 * This is a special FilterOutputStream deflating the bytes that are
 * written through it.  It uses the Deflater for deflating.
 * <p>
 * A special thing to be noted is that flush() doesn't flush
 * everything in Sun's JDK, but it does so in jazzlib. This is because
 * Sun's Deflater doesn't have a way to flush() everything, without
 * finishing the stream.
 *
 * @author Tom Tromey, Jochen Hoenicke
 * @date Jan 11, 2001
 */
public class DeflaterOutputStream extends FilterOutputStream {
  /**
   * This buffer is used temporarily to retrieve the bytes from the
   * deflater and write them to the underlying output stream.
   */
  protected byte[] buf;

  /**
   * The deflater which is used to deflate the stream.
   */
  protected Deflater def;

  /**
   * Deflates everything in the def's input buffers.  This will call
   * <code>def.deflate()</code> until all bytes from the input buffers
   * are processed.
   */
  protected void deflate() throws IOException {
    while (!def.needsInput()) {
      int len = def.deflate(buf, 0, buf.length);

      //	System.err.println("DOS deflated " + len + " out of " + buf.length);
      if (len <= 0)
        break;
      out.write(buf, 0, len);
    }

    if (!def.needsInput())
      throw new InternalError("Can't deflate all input?");
  }

  /**
   * Creates a new DeflaterOutputStream with a default Deflater and
   * default buffer size.
   *
   * @param out the output stream where deflated output should be written.
   */
  public DeflaterOutputStream(OutputStream out) {
    this(out, new Deflater(), 512);
  }

  /**
   * Creates a new DeflaterOutputStream with the given Deflater and
   * default buffer size.
   *
   * @param out  the output stream where deflated output should be written.
   * @param defl the underlying deflater.
   */
  public DeflaterOutputStream(OutputStream out, Deflater defl) {
    this(out, defl, 512);
  }

  /**
   * Creates a new DeflaterOutputStream with the given Deflater and
   * buffer size.
   *
   * @param out     the output stream where deflated output should be written.
   * @param defl    the underlying deflater.
   * @param bufsize the buffer size.
   * @throws IllegalArgumentException if bufsize isn't positive.
   */
  public DeflaterOutputStream(OutputStream out, Deflater defl, int bufsize) {
    super(out);
    if (bufsize <= 0)
      throw new IllegalArgumentException("bufsize <= 0");
    buf = new byte[bufsize];
    def = defl;
  }

  /**
   * Flushes the stream by calling flush() on the deflater and then
   * on the underlying stream.  This ensures that all bytes are
   * flushed.  This function doesn't work in Sun's JDK, but only in
   * jazzlib.
   */
  public void flush() throws IOException {
    def.flush();
    deflate();
    out.flush();
  }

  /**
   * Finishes the stream by calling finish() on the deflater.  This
   * was the only way to ensure that all bytes are flushed in Sun's
   * JDK.
   */
  public void finish() throws IOException {
    def.finish();
    while (!def.finished()) {
      int len = def.deflate(buf, 0, buf.length);
      if (len <= 0)
        break;
      out.write(buf, 0, len);
    }
    if (!def.finished())
      throw new InternalError("Can't deflate all input?");
    out.flush();
  }

  /**
   * Calls finish () and closes the stream.
   */
  public void close() throws IOException {
    finish();
    out.close();
  }

  /**
   * Writes a single byte to the compressed output stream.
   *
   * @param bval the byte value.
   */
  public void write(int bval) throws IOException {
    byte[] b = new byte[1];
    b[0] = (byte) bval;
    write(b, 0, 1);
  }

  /**
   * Writes a len bytes from an array to the compressed stream.
   *
   * @param buf the byte array.
   * @param off the offset into the byte array where to start.
   * @param len the number of bytes to write.
   */
  public void write(byte[] buf, int off, int len) throws IOException {
    def.setInput(buf, off, len);
    deflate();
  }
}
