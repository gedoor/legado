/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2000-2011 ymnk, JCraft,Inc. All rights reserved.

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
/*
 * This program is based on zlib-1.1.3, so all credit should go authors
 * Jean-loup Gailly(jloup@gzip.org) and Mark Adler(madler@alumni.caltech.edu)
 * and contributors of zlib.
 */

package com.jcraft.jzlib;

/**
 * ZStream
 *
 * @deprecated  Not for public use in the future.
 */
@Deprecated
public class ZStream{

  static final private int MAX_WBITS=15;        // 32K LZ77 window
  static final private int DEF_WBITS=MAX_WBITS;

  static final private int Z_NO_FLUSH=0;
  static final private int Z_PARTIAL_FLUSH=1;
  static final private int Z_SYNC_FLUSH=2;
  static final private int Z_FULL_FLUSH=3;
  static final private int Z_FINISH=4;

  static final private int MAX_MEM_LEVEL=9;

  static final private int Z_OK=0;
  static final private int Z_STREAM_END=1;
  static final private int Z_NEED_DICT=2;
  static final private int Z_ERRNO=-1;
  static final private int Z_STREAM_ERROR=-2;
  static final private int Z_DATA_ERROR=-3;
  static final private int Z_MEM_ERROR=-4;
  static final private int Z_BUF_ERROR=-5;
  static final private int Z_VERSION_ERROR=-6;

  public byte[] next_in;     // next input byte
  public int next_in_index;
  public int avail_in;       // number of bytes available at next_in
  public long total_in;      // total nb of input bytes read so far

  public byte[] next_out;    // next output byte should be put there
  public int next_out_index;
  public int avail_out;      // remaining free space at next_out
  public long total_out;     // total nb of bytes output so far

  public String msg;

  Deflate dstate; 
  Inflate istate; 

  int data_type; // best guess about the data type: ascii or binary

  Checksum adler;

  public ZStream(){
    this(new Adler32());
  }

  public ZStream(Checksum adler){
    this.adler=adler;
  }

  public int inflateInit(){
    return inflateInit(DEF_WBITS);
  }
  public int inflateInit(boolean nowrap){
    return inflateInit(DEF_WBITS, nowrap);
  }
  public int inflateInit(int w){
    return inflateInit(w, false);
  }
  public int inflateInit(JZlib.WrapperType wrapperType) {
    return inflateInit(DEF_WBITS, wrapperType);
  }
  public int inflateInit(int w, JZlib.WrapperType wrapperType) {
    boolean nowrap = false;
    if(wrapperType == JZlib.W_NONE){
      nowrap = true;
    }
    else if(wrapperType == JZlib.W_GZIP) {
      w += 16;
    }
    else if(wrapperType == JZlib.W_ANY) {
      w |= Inflate.INFLATE_ANY;
    }
    else if(wrapperType == JZlib.W_ZLIB) {
    }
    return inflateInit(w, nowrap);
  }
  public int inflateInit(int w, boolean nowrap){
    istate=new Inflate(this);
    return istate.inflateInit(nowrap?-w:w);
  }

  public int inflate(int f){
    if(istate==null) return Z_STREAM_ERROR;
    return istate.inflate(f);
  }
  public int inflateEnd(){
    if(istate==null) return Z_STREAM_ERROR;
    int ret=istate.inflateEnd();
//    istate = null;
    return ret;
  }
  public int inflateSync(){
    if(istate == null)
      return Z_STREAM_ERROR;
    return istate.inflateSync();
  }
  public int inflateSyncPoint(){
    if(istate == null)
      return Z_STREAM_ERROR;
    return istate.inflateSyncPoint();
  }
  public int inflateSetDictionary(byte[] dictionary, int dictLength){
    if(istate == null)
      return Z_STREAM_ERROR;
    return istate.inflateSetDictionary(dictionary, dictLength);
  }
  public boolean inflateFinished(){
    return istate.mode==12 /*DONE*/;
  }

  public int deflateInit(int level){
    return deflateInit(level, MAX_WBITS);
  }
  public int deflateInit(int level, boolean nowrap){
    return deflateInit(level, MAX_WBITS, nowrap);
  }
  public int deflateInit(int level, int bits){
    return deflateInit(level, bits, false);
  }
  public int deflateInit(int level, int bits, int memlevel, JZlib.WrapperType wrapperType){
    if(bits < 9 || bits > 15){
      return Z_STREAM_ERROR;
    }
    if(wrapperType == JZlib.W_NONE) {
      bits *= -1;
    }
    else if(wrapperType == JZlib.W_GZIP) {
        bits += 16;
    }
    else if(wrapperType == JZlib.W_ANY) {
        return Z_STREAM_ERROR;
    }
    else if(wrapperType == JZlib.W_ZLIB) {
    }
    return this.deflateInit(level, bits, memlevel);
  }
  public int deflateInit(int level, int bits, int memlevel){
    dstate=new Deflate(this);
    return dstate.deflateInit(level, bits, memlevel);
  }
  public int deflateInit(int level, int bits, boolean nowrap){
    dstate=new Deflate(this);
    return dstate.deflateInit(level, nowrap?-bits:bits);
  }
  public int deflate(int flush){
    if(dstate==null){
      return Z_STREAM_ERROR;
    }
    return dstate.deflate(flush);
  }
  public int deflateEnd(){
    if(dstate==null) return Z_STREAM_ERROR;
    int ret=dstate.deflateEnd();
    dstate=null;
    return ret;
  }
  public int deflateParams(int level, int strategy){
    if(dstate==null) return Z_STREAM_ERROR;
    return dstate.deflateParams(level, strategy);
  }
  public int deflateSetDictionary (byte[] dictionary, int dictLength){
    if(dstate == null)
      return Z_STREAM_ERROR;
    return dstate.deflateSetDictionary(dictionary, dictLength);
  }

  // Flush as much pending output as possible. All deflate() output goes
  // through this function so some applications may wish to modify it
  // to avoid allocating a large strm->next_out buffer and copying into it.
  // (See also read_buf()).
  void flush_pending(){
    int len=dstate.pending;

    if(len>avail_out) len=avail_out;
    if(len==0) return;

    if(dstate.pending_buf.length<=dstate.pending_out ||
       next_out.length<=next_out_index ||
       dstate.pending_buf.length<(dstate.pending_out+len) ||
       next_out.length<(next_out_index+len)){
      //System.out.println(dstate.pending_buf.length+", "+dstate.pending_out+
      //		 ", "+next_out.length+", "+next_out_index+", "+len);
      //System.out.println("avail_out="+avail_out);
    }

    System.arraycopy(dstate.pending_buf, dstate.pending_out,
		     next_out, next_out_index, len);

    next_out_index+=len;
    dstate.pending_out+=len;
    total_out+=len;
    avail_out-=len;
    dstate.pending-=len;
    if(dstate.pending==0){
      dstate.pending_out=0;
    }
  }

  // Read a new buffer from the current input stream, update the adler32
  // and total number of bytes read.  All deflate() input goes through
  // this function so some applications may wish to modify it to avoid
  // allocating a large strm->next_in buffer and copying from it.
  // (See also flush_pending()).
  int read_buf(byte[] buf, int start, int size) {
    int len=avail_in;

    if(len>size) len=size;
    if(len==0) return 0;

    avail_in-=len;

    if(dstate.wrap!=0) {
      adler.update(next_in, next_in_index, len);
    }
    System.arraycopy(next_in, next_in_index, buf, start, len);
    next_in_index  += len;
    total_in += len;
    return len;
  }

  public long getAdler(){
    return adler.getValue();
  }

  public void free(){
    next_in=null;
    next_out=null;
    msg=null;
  }

  public void setOutput(byte[] buf){
    setOutput(buf, 0, buf.length); 
  }

  public void setOutput(byte[] buf, int off, int len){
    next_out = buf;
    next_out_index = off;
    avail_out = len;
  }

  public void setInput(byte[] buf){
    setInput(buf, 0, buf.length, false); 
  }

  public void setInput(byte[] buf, boolean append){
    setInput(buf, 0, buf.length, append); 
  }

  public void setInput(byte[] buf, int off, int len, boolean append){
    if(len<=0 && append && next_in!=null) return;

    if(avail_in>0 && append){  
      byte[] tmp = new byte[avail_in+len];
      System.arraycopy(next_in, next_in_index, tmp, 0, avail_in);
      System.arraycopy(buf, off, tmp, avail_in, len);
      next_in=tmp;
      next_in_index=0;
      avail_in+=len;
    }
    else{
      next_in=buf;
      next_in_index=off;
      avail_in=len;
    }
  }

  public byte[] getNextIn(){
    return next_in;
  }

  public void setNextIn(byte[] next_in){
    this.next_in = next_in;
  }

  public int getNextInIndex(){
    return next_in_index;
  }

  public void setNextInIndex(int next_in_index){
    this.next_in_index = next_in_index;
  }

  public int getAvailIn(){
    return avail_in;
  }

  public void setAvailIn(int avail_in){
    this.avail_in = avail_in;
  }

  public byte[] getNextOut(){
    return next_out;
  }

  public void setNextOut(byte[] next_out){
    this.next_out = next_out;
  }

  public int getNextOutIndex(){
    return next_out_index;
  }

  public void setNextOutIndex(int next_out_index){
    this.next_out_index = next_out_index;
  }

  public int getAvailOut(){
    return avail_out;

  }

  public void setAvailOut(int avail_out){
    this.avail_out = avail_out;
  }

  public long getTotalOut(){
    return total_out;
  }

  public long getTotalIn(){
    return total_in;
  }

  public String getMessage(){
    return msg;
  }

  /**
   * Those methods are expected to be override by Inflater and Deflater.
   * In the future, they will become abstract methods.
   */ 
  public int end(){ return Z_OK; }
  public boolean finished(){ return false; }
}
