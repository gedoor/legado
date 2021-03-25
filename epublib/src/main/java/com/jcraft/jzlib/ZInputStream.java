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

/**
 * ZInputStream
 *
 * @deprecated  use DeflaterOutputStream or InflaterInputStream
 */
@Deprecated
public class ZInputStream extends FilterInputStream {

  protected int flush=JZlib.Z_NO_FLUSH;
  protected boolean compress;
  protected InputStream in=null;

  protected Deflater deflater;
  protected InflaterInputStream iis;

  public ZInputStream(InputStream in) throws IOException {
    this(in, false);
  }
  public ZInputStream(InputStream in, boolean nowrap) throws IOException {
    super(in);
    iis = new InflaterInputStream(in, nowrap);
    compress=false;
  }

  public ZInputStream(InputStream in, int level) throws IOException {
    super(in);
    this.in=in;
    deflater = new Deflater();
    deflater.init(level);
    compress=true;
  }

  private byte[] buf1 = new byte[1];
  public int read() throws IOException {
    if(read(buf1, 0, 1)==-1) return -1;
    return(buf1[0]&0xFF);
  }

  private byte[] buf = new byte[512];

  public int read(byte[] b, int off, int len) throws IOException {
    if(compress){
      deflater.setOutput(b, off, len);
      while(true){
        int datalen = in.read(buf, 0, buf.length);
        if(datalen == -1) return -1;
        deflater.setInput(buf, 0, datalen, true);
        int err = deflater.deflate(flush);
        if(deflater.next_out_index>0)
          return deflater.next_out_index;
        if(err == JZlib.Z_STREAM_END)
          return 0;
        if(err == JZlib.Z_STREAM_ERROR ||
           err == JZlib.Z_DATA_ERROR){
          throw new ZStreamException("deflating: "+deflater.msg);
        }
      }
    }
    else{
      return iis.read(b, off, len); 
    }
  }

  public long skip(long n) throws IOException {
    int len=512;
    if(n<len)
      len=(int)n;
    byte[] tmp=new byte[len];
    return((long)read(tmp));
  }

  public int getFlushMode() {
    return flush;
  }

  public void setFlushMode(int flush) {
    this.flush=flush;
  }

  public long getTotalIn() {
    if(compress) return deflater.total_in;
    else return iis.getTotalIn();
  }

  public long getTotalOut() {
    if(compress) return deflater.total_out;
    else return iis.getTotalOut();
  }

  public void close() throws IOException{
    if(compress) deflater.end();
    else iis.close();
  }
}
