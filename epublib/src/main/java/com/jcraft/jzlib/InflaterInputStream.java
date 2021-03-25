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

public class InflaterInputStream extends FilterInputStream {
  protected final Inflater inflater;
  protected byte[] buf;

  private boolean closed = false;

  private boolean eof = false;

  private boolean close_in = true;

  protected static final int DEFAULT_BUFSIZE = 512;

  public InflaterInputStream(InputStream in) throws IOException {
    this(in, false);
  }

  public InflaterInputStream(InputStream in, boolean nowrap) throws IOException {
    this(in, new Inflater(nowrap));
    myinflater = true;
  }

  public InflaterInputStream(InputStream in, Inflater inflater) throws IOException {
    this(in, inflater, DEFAULT_BUFSIZE);
  }

  public InflaterInputStream(InputStream in,
                             Inflater inflater, int size) throws IOException {
    this(in, inflater, size, true);
  }

  public InflaterInputStream(InputStream in,
                             Inflater inflater,
                             int size, boolean close_in) throws IOException {
    super(in);
    if (in == null || inflater == null) {
      throw new NullPointerException();
    }
    else if (size <= 0) {
      throw new IllegalArgumentException("buffer size must be greater than 0");
    }
    this.inflater = inflater;
    buf = new byte[size];
    this.close_in = close_in;
  }

  protected boolean myinflater = false;

  private byte[] byte1 = new byte[1];

  public int read() throws IOException {
    if (closed) { throw new IOException("Stream closed"); }
    return read(byte1, 0, 1) == -1 ? -1 : byte1[0] & 0xff;
  }

  public int read(byte[] b, int off, int len) throws IOException {
    if (closed) { throw new IOException("Stream closed"); }
    if (b == null) {
      throw new NullPointerException();
    }
    else if (off < 0 || len < 0 || len > b.length - off) {
      throw new IndexOutOfBoundsException();
    }
    else if (len == 0) {
      return 0;
    }
    else if (eof) {
      return -1;
    }

    int n = 0;
    inflater.setOutput(b, off, len);
    while(!eof) {
      if(inflater.avail_in==0)
        fill();
      int err = inflater.inflate(JZlib.Z_NO_FLUSH);
      n += inflater.next_out_index - off;
      off = inflater.next_out_index;
      switch(err) {
        case JZlib.Z_DATA_ERROR:
          throw new IOException(inflater.msg);
        case JZlib.Z_STREAM_END:
        case JZlib.Z_NEED_DICT:
          eof = true;
          if(err == JZlib.Z_NEED_DICT)
            return -1;
          break;
        default:
      } 
      if(inflater.avail_out==0)
        break;
    }
    return n;
  }

  public int available() throws IOException {
    if (closed) { throw new IOException("Stream closed"); }
    if (eof) {
      return 0;
    }
    else {
      return 1;
    }
  }

  private byte[] b = new byte[512];

  public long skip(long n) throws IOException {
    if (n < 0) {
      throw new IllegalArgumentException("negative skip length");
    }

    if (closed) { throw new IOException("Stream closed"); }

    int max = (int)Math.min(n, Integer.MAX_VALUE);
    int total = 0;
    while (total < max) {
      int len = max - total;
      if (len > b.length) {
        len = b.length;
      }
      len = read(b, 0, len);
      if (len == -1) {
        eof = true;
        break;
      }
      total += len;
    }
    return total;
  }

  public void close() throws IOException {
    if (!closed) {
      if (myinflater)
        inflater.end();
      if(close_in)
        in.close();
      closed = true;
    }
  }

  protected void fill() throws IOException {
    if (closed) { throw new IOException("Stream closed"); }
    int len = in.read(buf, 0, buf.length);
    if (len == -1) {
      if(inflater.istate.wrap == 0 &&
         !inflater.finished()){
        buf[0]=0;
        len=1;
      }
      else if(inflater.istate.was != -1){  // in reading trailer
        throw new IOException("footer is not found");
      }
      else{
        throw new EOFException("Unexpected end of ZLIB input stream");
      }
    }
    inflater.setInput(buf, 0, len, true);
  }

  public boolean markSupported() {
    return false;
  }

  public synchronized void mark(int readlimit) {
  }

  public synchronized void reset() throws IOException {
    throw new IOException("mark/reset not supported");
  }

  public long getTotalIn() {
    return inflater.getTotalIn();
  }

  public long getTotalOut() {
    return inflater.getTotalOut();
  }

  public byte[] getAvailIn() {
    if(inflater.avail_in<=0)
      return null;
    byte[] tmp = new byte[inflater.avail_in];
    System.arraycopy(inflater.next_in, inflater.next_in_index,
                     tmp, 0, inflater.avail_in);
    return tmp;
  }

  public void readHeader() throws IOException {

    byte[] empty = "".getBytes();
    inflater.setInput(empty, 0, 0, false);
    inflater.setOutput(empty, 0, 0);

    int err = inflater.inflate(JZlib.Z_NO_FLUSH);
    if(!inflater.istate.inParsingHeader()){
      return;
    }

    byte[] b1 = new byte[1];
    do{
      int i = in.read(b1);
      if(i<=0)
        throw new IOException("no input");
      inflater.setInput(b1);
      err = inflater.inflate(JZlib.Z_NO_FLUSH);
      if(err!=0/*Z_OK*/)
        throw new IOException(inflater.msg);
    }
    while(inflater.istate.inParsingHeader());
  }

  public Inflater getInflater(){
    return inflater;
  }
}