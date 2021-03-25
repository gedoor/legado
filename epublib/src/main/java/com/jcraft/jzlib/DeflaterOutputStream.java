/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2011 ymnk, JCraft,Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright 
     notice, this list of conditions and the following disclaimer in 
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jcraft.jzlib;
import java.io.*;

public class DeflaterOutputStream extends FilterOutputStream {

  protected final Deflater deflater;

  protected byte[] buffer;

  private boolean closed = false;

  private boolean syncFlush = false;

  private final byte[] buf1 = new byte[1];

  protected boolean mydeflater = false;

  private boolean close_out = true;

  protected static final int DEFAULT_BUFSIZE = 512;

  public DeflaterOutputStream(OutputStream out) throws IOException {
    this(out, 
         new Deflater(JZlib.Z_DEFAULT_COMPRESSION),
         DEFAULT_BUFSIZE, true);
    mydeflater = true;
  }

  public DeflaterOutputStream(OutputStream out, Deflater def) throws IOException {
    this(out, def, DEFAULT_BUFSIZE, true);
  }

  public DeflaterOutputStream(OutputStream out,
                              Deflater deflater,
                              int size) throws IOException {
    this(out, deflater, size, true);
  }
  public DeflaterOutputStream(OutputStream out,
                              Deflater deflater,
                              int size,
                              boolean close_out)  throws IOException {
    super(out);
    if (out == null || deflater == null) {
      throw new NullPointerException();
    }
    else if (size <= 0) {
      throw new IllegalArgumentException("buffer size must be greater than 0");
    }
    this.deflater = deflater;
    buffer = new byte[size];
    this.close_out = close_out;
  }

  public void write(int b) throws IOException {
    buf1[0] = (byte)(b & 0xff);
    write(buf1, 0, 1);
  }

  public void write(byte[] b, int off, int len) throws IOException {
    if (deflater.finished()) {
      throw new IOException("finished");
    }
    else if (off<0 | len<0 | off+len>b.length) {
      throw new IndexOutOfBoundsException();
    }
    else if (len == 0) {
      return;
    }
    else {
      int flush = syncFlush ? JZlib.Z_SYNC_FLUSH : JZlib.Z_NO_FLUSH;
      deflater.setInput(b, off, len, true);
      while (deflater.avail_in>0) {
        int err = deflate(flush);
        if (err == JZlib.Z_STREAM_END)
          break;
      }
    }
  }

  public void finish() throws IOException {
    while (!deflater.finished()) {
      deflate(JZlib.Z_FINISH);
    }
  }

  public void close() throws IOException {
    if (!closed) {
      finish();
      if (mydeflater){
        deflater.end();
      }
      if(close_out)
        out.close();
      closed = true;
    }
  }

  protected int deflate(int flush) throws IOException {
    deflater.setOutput(buffer, 0, buffer.length);
    int err = deflater.deflate(flush);
    switch(err) {
      case JZlib.Z_OK:
      case JZlib.Z_STREAM_END:
        break;
      case JZlib.Z_BUF_ERROR:
        if(deflater.avail_in<=0 && flush!=JZlib.Z_FINISH){
          // flush() without any data
          break;
        }
      default:
        throw new IOException("failed to deflate");
    }
    int len = deflater.next_out_index;
    if (len > 0) {
      out.write(buffer, 0, len);
    }
    return err;
  }

  public void flush() throws IOException {
    if (syncFlush && !deflater.finished()) {
      while (true) {
        int err = deflate(JZlib.Z_SYNC_FLUSH);
        if (deflater.next_out_index < buffer.length)
          break;
        if (err == JZlib.Z_STREAM_END)
          break;
      }
    }
    out.flush();
  }

  public long getTotalIn() {
    return deflater.getTotalIn();
  }

  public long getTotalOut() {
    return deflater.getTotalOut();
  }

  public void setSyncFlush(boolean syncFlush){
    this.syncFlush = syncFlush;
  }

  public boolean getSyncFlush(){
    return this.syncFlush;
  }

  public Deflater getDeflater(){
    return deflater;
  }
}
