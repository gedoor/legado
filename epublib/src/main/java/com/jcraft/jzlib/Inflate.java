/* -*-mode:java; c-basic-offset:2; -*- */
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

final class Inflate{
  
  static final private int MAX_WBITS=15; // 32K LZ77 window

  // preset dictionary flag in zlib header
  static final private int PRESET_DICT=0x20;

  static final int Z_NO_FLUSH=0;
  static final int Z_PARTIAL_FLUSH=1;
  static final int Z_SYNC_FLUSH=2;
  static final int Z_FULL_FLUSH=3;
  static final int Z_FINISH=4;

  static final private int Z_DEFLATED=8;

  static final private int Z_OK=0;
  static final private int Z_STREAM_END=1;
  static final private int Z_NEED_DICT=2;
  static final private int Z_ERRNO=-1;
  static final private int Z_STREAM_ERROR=-2;
  static final private int Z_DATA_ERROR=-3;
  static final private int Z_MEM_ERROR=-4;
  static final private int Z_BUF_ERROR=-5;
  static final private int Z_VERSION_ERROR=-6;

  static final private int METHOD=0;   // waiting for method byte
  static final private int FLAG=1;     // waiting for flag byte
  static final private int DICT4=2;    // four dictionary check bytes to go
  static final private int DICT3=3;    // three dictionary check bytes to go
  static final private int DICT2=4;    // two dictionary check bytes to go
  static final private int DICT1=5;    // one dictionary check byte to go
  static final private int DICT0=6;    // waiting for inflateSetDictionary
  static final private int BLOCKS=7;   // decompressing blocks
  static final private int CHECK4=8;   // four check bytes to go
  static final private int CHECK3=9;   // three check bytes to go
  static final private int CHECK2=10;  // two check bytes to go
  static final private int CHECK1=11;  // one check byte to go
  static final private int DONE=12;    // finished check, done
  static final private int BAD=13;     // got an error--stay here

  static final private int HEAD=14;
  static final private int LENGTH=15;
  static final private int TIME=16;
  static final private int OS=17;
  static final private int EXLEN=18;
  static final private int EXTRA=19;
  static final private int NAME=20;
  static final private int COMMENT=21;
  static final private int HCRC=22;
  static final private int FLAGS=23;

  static final int INFLATE_ANY=0x40000000;

  int mode;                            // current inflate mode

  // mode dependent information
  int method;        // if FLAGS, method byte

  // if CHECK, check values to compare
  long was = -1;           // computed check value
  long need;               // stream check value

  // if BAD, inflateSync's marker bytes count
  int marker;

  // mode independent information
  int  wrap;          // flag for no wrapper
                      // 0: no wrapper
                      // 1: zlib header
                      // 2: gzip header
                      // 4: auto detection

  int wbits;            // log2(window size)  (8..15, defaults to 15)

  InfBlocks blocks;     // current inflate_blocks state

  private final ZStream z;

  private int flags; 

  private int need_bytes = -1;
  private byte[] crcbuf=new byte[4];

  GZIPHeader gheader = null;

  int inflateReset(){
    if(z == null) return Z_STREAM_ERROR;
    
    z.total_in = z.total_out = 0;
    z.msg = null;
    this.mode = HEAD;
    this.need_bytes = -1;
    this.blocks.reset();
    return Z_OK;
  }

  int inflateEnd(){
    if(blocks != null){
      blocks.free();
    }
    return Z_OK;
  }

  Inflate(ZStream z){
    this.z=z;
  }

  int inflateInit(int w){
    z.msg = null;
    blocks = null;

    // handle undocumented wrap option (no zlib header or check)
    wrap = 0;
    if(w < 0){
      w = - w;
    }
    else if((w&INFLATE_ANY) != 0){
      wrap = 4;
      w &= ~INFLATE_ANY;
      if(w < 48)
        w &= 15;
    }
    else if((w & ~31) != 0) { // for example, DEF_WBITS + 32
      wrap = 4;               // zlib and gzip wrapped data should be accepted.
      w &= 15;
    }
    else {
      wrap = (w >> 4) + 1;
      if(w < 48)
        w &= 15;
    }

    if(w<8 ||w>15){
      inflateEnd();
      return Z_STREAM_ERROR;
    }
    if(blocks != null && wbits != w){
      blocks.free();
      blocks=null;
    }

    // set window size
    wbits=w;

    this.blocks=new InfBlocks(z, 1<<w);

    // reset state
    inflateReset();

    return Z_OK;
  }

  int inflate(int f){
    int hold = 0;

    int r;
    int b;

    if(z == null || z.next_in == null){
      if(f == Z_FINISH && this.mode==HEAD)
        return Z_OK; 
      return Z_STREAM_ERROR;
    }

    f = f == Z_FINISH ? Z_BUF_ERROR : Z_OK;
    r = Z_BUF_ERROR;
    while (true){

      switch (this.mode){
      case HEAD:
        if(wrap==0){
	  this.mode = BLOCKS;
          break;
        } 

        try { r=readBytes(2, r, f); }
        catch(Return e){ return e.r; }

        if((wrap == 4 || (wrap&2)!=0) &&
           this.need == 0x8b1fL) {   // gzip header
          if(wrap == 4){
            wrap = 2;
          }
	  z.adler=new CRC32();
          checksum(2, this.need);

          if(gheader==null) 
            gheader=new GZIPHeader();

          this.mode = FLAGS;
          break;
        }

        if((wrap&2) != 0){
          this.mode = BAD;
          z.msg = "incorrect header check";
          break;
        }

        flags = 0;

        this.method = ((int)this.need)&0xff;
        b=((int)(this.need>>8))&0xff;

        if(((wrap&1)==0 ||  // check if zlib header allowed
            (((this.method << 8)+b) % 31)!=0) &&
           (this.method&0xf)!=Z_DEFLATED){
          if(wrap == 4){
            z.next_in_index -= 2;
            z.avail_in += 2;
            z.total_in -= 2;
            wrap = 0;
            this.mode = BLOCKS;
            break;
          }  
          this.mode = BAD;
          z.msg = "incorrect header check";
          // since zlib 1.2, it is allowted to inflateSync for this case.
          /*
          this.marker = 5;       // can't try inflateSync
          */
          break;
        }

        if((this.method&0xf)!=Z_DEFLATED){
          this.mode = BAD;
          z.msg="unknown compression method";
          // since zlib 1.2, it is allowted to inflateSync for this case.
	  /*
          this.marker = 5;       // can't try inflateSync
	  */
          break;
        }
  
        if(wrap == 4){
          wrap = 1;
        }  

        if((this.method>>4)+8>this.wbits){
          this.mode = BAD;
          z.msg="invalid window size";
          // since zlib 1.2, it is allowted to inflateSync for this case.
	  /*
          this.marker = 5;       // can't try inflateSync
	  */
          break;
        }

        z.adler=new Adler32();

        if((b&PRESET_DICT)==0){
          this.mode = BLOCKS;
          break;
        }
        this.mode = DICT4;
      case DICT4:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        this.need=((z.next_in[z.next_in_index++]&0xff)<<24)&0xff000000L;
        this.mode=DICT3;
      case DICT3:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        this.need+=((z.next_in[z.next_in_index++]&0xff)<<16)&0xff0000L;
        this.mode=DICT2;
      case DICT2:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        this.need+=((z.next_in[z.next_in_index++]&0xff)<<8)&0xff00L;
        this.mode=DICT1;
      case DICT1:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        this.need += (z.next_in[z.next_in_index++]&0xffL);
        z.adler.reset(this.need);
        this.mode = DICT0;
        return Z_NEED_DICT;
      case DICT0:
        this.mode = BAD;
        z.msg = "need dictionary";
        this.marker = 0;       // can try inflateSync
        return Z_STREAM_ERROR;
      case BLOCKS:
        r = this.blocks.proc(r);
        if(r == Z_DATA_ERROR){
          this.mode = BAD;
          this.marker = 0;     // can try inflateSync
          break;
        }
        if(r == Z_OK){
          r = f;
        }
        if(r != Z_STREAM_END){
          return r;
        }
        r = f;
        this.was=z.adler.getValue();
        this.blocks.reset();
        if(this.wrap==0){
          this.mode=DONE;
          break;
        }
        this.mode=CHECK4;
      case CHECK4:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        this.need=((z.next_in[z.next_in_index++]&0xff)<<24)&0xff000000L;
        this.mode=CHECK3;
      case CHECK3:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        this.need+=((z.next_in[z.next_in_index++]&0xff)<<16)&0xff0000L;
        this.mode = CHECK2;
      case CHECK2:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        this.need+=((z.next_in[z.next_in_index++]&0xff)<<8)&0xff00L;
        this.mode = CHECK1;
      case CHECK1:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        this.need+=(z.next_in[z.next_in_index++]&0xffL);

        if(flags!=0){  // gzip
          this.need = ((this.need&0xff000000)>>24 | 
                          (this.need&0x00ff0000)>>8 | 
                          (this.need&0x0000ff00)<<8 | 
                          (this.need&0x0000ffff)<<24)&0xffffffffL;
        }

        if(((int)(this.was)) != ((int)(this.need))){
          z.msg = "incorrect data check";
          // chack is delayed
          /*
          this.mode = BAD;
          this.marker = 5;       // can't try inflateSync
          break;
	  */
        }
        else if(flags!=0 && gheader!=null){
          gheader.crc = this.need; 
        }

        this.mode = LENGTH;
      case LENGTH:
        if (wrap!=0 && flags!=0) {

          try { r=readBytes(4, r, f); }
          catch(Return e){ return e.r; }

          if(z.msg!=null && z.msg.equals("incorrect data check")){
            this.mode = BAD;
            this.marker = 5;       // can't try inflateSync
            break;
          }

          if (this.need != (z.total_out & 0xffffffffL)) {
            z.msg = "incorrect length check";
            this.mode = BAD;
            break;
          }
          z.msg = null;
        }
        else {
          if(z.msg!=null && z.msg.equals("incorrect data check")){
            this.mode = BAD;
            this.marker = 5;       // can't try inflateSync
            break;
          }
        }

        this.mode = DONE;
      case DONE:
        return Z_STREAM_END;
      case BAD:
        return Z_DATA_ERROR;

      case FLAGS:

        try { r=readBytes(2, r, f); }
        catch(Return e){ return e.r; }

        flags = ((int)this.need)&0xffff;

        if ((flags & 0xff) != Z_DEFLATED) {
          z.msg = "unknown compression method";
          this.mode = BAD; 
          break;
        }
        if ((flags & 0xe000)!=0) {
          z.msg = "unknown header flags set";
          this.mode = BAD; 
          break;
        }

        if ((flags & 0x0200)!=0){
          checksum(2, this.need);
        } 

        this.mode = TIME;

      case TIME:
        try { r=readBytes(4, r, f); }
        catch(Return e){ return e.r; }
        if(gheader!=null)
          gheader.time = this.need;
        if ((flags & 0x0200)!=0){
          checksum(4, this.need);
        }
        this.mode = OS;
      case OS:
        try { r=readBytes(2, r, f); }
        catch(Return e){ return e.r; }
        if(gheader!=null){
          gheader.xflags = ((int)this.need)&0xff;
          gheader.os = (((int)this.need)>>8)&0xff;
        }
        if ((flags & 0x0200)!=0){
          checksum(2, this.need);
        }
        this.mode = EXLEN;
      case EXLEN:
        if ((flags & 0x0400)!=0) {
          try { r=readBytes(2, r, f); }
          catch(Return e){ return e.r; }
          if(gheader!=null){
            gheader.extra = new byte[((int)this.need)&0xffff];
          }
          if ((flags & 0x0200)!=0){
            checksum(2, this.need);
          }
        }
        else if(gheader!=null){
          gheader.extra=null;
        }
        this.mode = EXTRA;

      case EXTRA:
        if ((flags & 0x0400)!=0) {
          try { 
            r=readBytes(r, f);
            if(gheader!=null){
              byte[] foo = tmp_string.toByteArray();
              tmp_string=null;
              if(foo.length == gheader.extra.length){
                System.arraycopy(foo, 0, gheader.extra, 0, foo.length);
	      }
              else{
                z.msg = "bad extra field length";
                this.mode = BAD; 
                break;
	      }
            }
          }
          catch(Return e){ return e.r; }
        }
        else if(gheader!=null){
          gheader.extra=null;
	}
	this.mode = NAME;
      case NAME:
	if ((flags & 0x0800)!=0) {
          try { 
            r=readString(r, f);
            if(gheader!=null){
              gheader.name=tmp_string.toByteArray();
            }
            tmp_string=null;
          }
          catch(Return e){ return e.r; }
        }
        else if(gheader!=null){
          gheader.name=null;
	}
        this.mode = COMMENT;
      case COMMENT:
        if ((flags & 0x1000)!=0) {
          try { 
            r=readString(r, f);
            if(gheader!=null){
              gheader.comment=tmp_string.toByteArray();
            }
            tmp_string=null;
          }
          catch(Return e){ return e.r; }
        }
        else if(gheader!=null){
          gheader.comment=null;
	}
        this.mode = HCRC;
      case HCRC:
	if ((flags & 0x0200)!=0) {
          try { r=readBytes(2, r, f); }
          catch(Return e){ return e.r; }
          if(gheader!=null){
            gheader.hcrc=(int)(this.need&0xffff);
          }
          if(this.need != (z.adler.getValue()&0xffffL)){
            this.mode = BAD;
            z.msg = "header crc mismatch";
            this.marker = 5;       // can't try inflateSync
            break;
          }
        }
        z.adler = new CRC32();

        this.mode = BLOCKS;
        break;
      default:
        return Z_STREAM_ERROR;
      }
    }
  }

  int inflateSetDictionary(byte[] dictionary, int dictLength){
    if(z==null || (this.mode != DICT0 && this.wrap != 0)){
      return Z_STREAM_ERROR;
    }

    int index=0;
    int length = dictLength;

    if(this.mode==DICT0){
      long adler_need=z.adler.getValue();
      z.adler.reset();
      z.adler.update(dictionary, 0, dictLength);
      if(z.adler.getValue()!=adler_need){
        return Z_DATA_ERROR;
      }
    }

    z.adler.reset();

    if(length >= (1<<this.wbits)){
      length = (1<<this.wbits)-1;
      index=dictLength - length;
    }
    this.blocks.set_dictionary(dictionary, index, length);
    this.mode = BLOCKS;
    return Z_OK;
  }

  static private byte[] mark = {(byte)0, (byte)0, (byte)0xff, (byte)0xff};

  int inflateSync(){
    int n;       // number of bytes to look at
    int p;       // pointer to bytes
    int m;       // number of marker bytes found in a row
    long r, w;   // temporaries to save total_in and total_out

    // set up
    if(z == null)
      return Z_STREAM_ERROR;
    if(this.mode != BAD){
      this.mode = BAD;
      this.marker = 0;
    }
    if((n=z.avail_in)==0)
      return Z_BUF_ERROR;

    p=z.next_in_index;
    m=this.marker;
    // search
    while (n!=0 && m < 4){
      if(z.next_in[p] == mark[m]){
        m++;
      }
      else if(z.next_in[p]!=0){
        m = 0;
      }
      else{
        m = 4 - m;
      }
      p++; n--;
    }

    // restore
    z.total_in += p-z.next_in_index;
    z.next_in_index = p;
    z.avail_in = n;
    this.marker = m;

    // return no joy or set up to restart on a new block
    if(m != 4){
      return Z_DATA_ERROR;
    }
    r=z.total_in;  w=z.total_out;
    inflateReset();
    z.total_in=r;  z.total_out = w;
    this.mode = BLOCKS;

    return Z_OK;
  }

  // Returns true if inflate is currently at the end of a block generated
  // by Z_SYNC_FLUSH or Z_FULL_FLUSH. This function is used by one PPP
  // implementation to provide an additional safety check. PPP uses Z_SYNC_FLUSH
  // but removes the length bytes of the resulting empty stored block. When
  // decompressing, PPP checks that at the end of input packet, inflate is
  // waiting for these length bytes.
  int inflateSyncPoint(){
    if(z == null || this.blocks == null)
      return Z_STREAM_ERROR;
    return this.blocks.sync_point();
  }

  private int readBytes(int n, int r, int f) throws Return{
    if(need_bytes == -1){
      need_bytes=n;
      this.need=0;
    }
    while(need_bytes>0){
      if(z.avail_in==0){ throw new Return(r); }; r=f;
      z.avail_in--; z.total_in++;
      this.need = this.need | 
	((z.next_in[z.next_in_index++]&0xff)<<((n-need_bytes)*8));
      need_bytes--;
    }
    if(n==2){
      this.need&=0xffffL;
    }
    else if(n==4) {
      this.need&=0xffffffffL;
    }
    need_bytes=-1;
    return r;
  }
  class Return extends Exception{
    int r;
    Return(int r){this.r=r; }
  }

  private java.io.ByteArrayOutputStream tmp_string = null;
  private int readString(int r, int f) throws Return{
    if(tmp_string == null){
      tmp_string=new java.io.ByteArrayOutputStream();
    }
    int b=0; 
    do {
      if(z.avail_in==0){ throw new Return(r); }; r=f;
      z.avail_in--; z.total_in++;
      b = z.next_in[z.next_in_index];
      if(b!=0) tmp_string.write(z.next_in, z.next_in_index, 1);
      z.adler.update(z.next_in, z.next_in_index, 1);
      z.next_in_index++;
    }while(b!=0);
    return r;
  }

  private int readBytes(int r, int f) throws Return{
    if(tmp_string == null){
      tmp_string=new java.io.ByteArrayOutputStream();
    }
    int b=0; 
    while(this.need>0){
      if(z.avail_in==0){ throw new Return(r); }; r=f;
      z.avail_in--; z.total_in++;
      b = z.next_in[z.next_in_index];
      tmp_string.write(z.next_in, z.next_in_index, 1);
      z.adler.update(z.next_in, z.next_in_index, 1);
      z.next_in_index++;
      this.need--;
    }
    return r;
  }

  private void checksum(int n, long v){
    for(int i=0; i<n; i++){
      crcbuf[i]=(byte)(v&0xff);
      v>>=8;
    }
    z.adler.update(crcbuf, 0, n);
  }

  public GZIPHeader getGZIPHeader(){
    return gheader;
  }

  boolean inParsingHeader(){
    switch(mode){
      case HEAD:
      case DICT4:
      case DICT3:
      case DICT2:
      case DICT1:
      case FLAGS:
      case TIME:
      case OS:
      case EXLEN:
      case EXTRA:
      case NAME:
      case COMMENT:
      case HCRC:
	return true;
      default:
	return false;
    }
  }
}
