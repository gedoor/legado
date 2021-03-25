/* -*-mode:java; c-basic-offset:2; -*- */
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
/*
 * This program is based on zlib-1.1.3, so all credit should go authors
 * Jean-loup Gailly(jloup@gzip.org) and Mark Adler(madler@alumni.caltech.edu)
 * and contributors of zlib.
 */

package com.jcraft.jzlib;

final class InfBlocks{
  static final private int MANY=1440;

  // And'ing with mask[n] masks the lower n bits
  static final private int[] inflate_mask = {
    0x00000000, 0x00000001, 0x00000003, 0x00000007, 0x0000000f,
    0x0000001f, 0x0000003f, 0x0000007f, 0x000000ff, 0x000001ff,
    0x000003ff, 0x000007ff, 0x00000fff, 0x00001fff, 0x00003fff,
    0x00007fff, 0x0000ffff
  };

  // Table for deflate from PKZIP's appnote.txt.
  static final int[] border = { // Order of the bit length code lengths
    16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15
  };

  static final private int Z_OK=0;
  static final private int Z_STREAM_END=1;
  static final private int Z_NEED_DICT=2;
  static final private int Z_ERRNO=-1;
  static final private int Z_STREAM_ERROR=-2;
  static final private int Z_DATA_ERROR=-3;
  static final private int Z_MEM_ERROR=-4;
  static final private int Z_BUF_ERROR=-5;
  static final private int Z_VERSION_ERROR=-6;

  static final private int TYPE=0;  // get type bits (3, including end bit)
  static final private int LENS=1;  // get lengths for stored
  static final private int STORED=2;// processing stored block
  static final private int TABLE=3; // get table lengths
  static final private int BTREE=4; // get bit lengths tree for a dynamic block
  static final private int DTREE=5; // get length, distance trees for a dynamic block
  static final private int CODES=6; // processing fixed or dynamic block
  static final private int DRY=7;   // output remaining window bytes
  static final private int DONE=8;  // finished last block, done
  static final private int BAD=9;   // ot a data error--stuck here

  int mode;            // current inflate_block mode 

  int left;            // if STORED, bytes left to copy 

  int table;           // table lengths (14 bits) 
  int index;           // index into blens (or border) 
  int[] blens;         // bit lengths of codes 
  int[] bb=new int[1]; // bit length tree depth 
  int[] tb=new int[1]; // bit length decoding tree 

  int[] bl=new int[1];
  int[] bd=new int[1];

  int[][] tl=new int[1][];
  int[][] td=new int[1][];
  int[] tli=new int[1]; // tl_index
  int[] tdi=new int[1]; // td_index

  private final InfCodes codes;      // if CODES, current state 

  int last;            // true if this block is the last block 

  // mode independent information 
  int bitk;            // bits in bit buffer 
  int bitb;            // bit buffer 
  int[] hufts;         // single malloc for tree space 
  byte[] window;       // sliding window 
  int end;             // one byte after sliding window 
  int read;            // window read pointer 
  int write;           // window write pointer 
  private boolean check;

  private final InfTree inftree=new InfTree();

  private final ZStream z; 

  InfBlocks(ZStream z, int w){
    this.z=z;
    this.codes=new InfCodes(this.z, this);
    hufts=new int[MANY*3];
    window=new byte[w];
    end=w;
    this.check = (z.istate.wrap==0) ? false : true;
    mode = TYPE;
    reset();
  }

  void reset(){
    if(mode==BTREE || mode==DTREE){
    }
    if(mode==CODES){
      codes.free(z);
    }
    mode=TYPE;
    bitk=0;
    bitb=0;
    read=write=0;
    if(check){
      z.adler.reset();
    }
  }

  int proc(int r){
    int t;              // temporary storage
    int b;              // bit buffer
    int k;              // bits in bit buffer
    int p;              // input data pointer
    int n;              // bytes available there
    int q;              // output window write pointer
    int m;              // bytes to end of window or read pointer

    // copy input/output information to locals (UPDATE macro restores)
    {p=z.next_in_index;n=z.avail_in;b=bitb;k=bitk;}
    {q=write;m=(int)(q<read?read-q-1:end-q);}

    // process input based on current state
    while(true){
      switch (mode){
      case TYPE:

	while(k<(3)){
	  if(n!=0){
	    r=Z_OK;
	  }
	  else{
	    bitb=b; bitk=k; 
	    z.avail_in=n;
	    z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    write=q;
	    return inflate_flush(r);
	  }
	  n--;
	  b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}
	t = (int)(b & 7);
	last = t & 1;

	switch (t >>> 1){
        case 0:                         // stored 
          {b>>>=(3);k-=(3);}
          t = k & 7;                    // go to byte boundary

          {b>>>=(t);k-=(t);}
          mode = LENS;                  // get length of stored block
          break;
        case 1:                         // fixed
          InfTree.inflate_trees_fixed(bl, bd, tl, td, z);
          codes.init(bl[0], bd[0], tl[0], 0, td[0], 0);

          {b>>>=(3);k-=(3);}

          mode = CODES;
          break;
        case 2:                         // dynamic

          {b>>>=(3);k-=(3);}

          mode = TABLE;
          break;
        case 3:                         // illegal

          {b>>>=(3);k-=(3);}
          mode = BAD;
          z.msg = "invalid block type";
          r = Z_DATA_ERROR;

	  bitb=b; bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  write=q;
	  return inflate_flush(r);
	}
	break;
      case LENS:

	while(k<(32)){
	  if(n!=0){
	    r=Z_OK;
	  }
	  else{
	    bitb=b; bitk=k; 
	    z.avail_in=n;
	    z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    write=q;
	    return inflate_flush(r);
	  }
	  n--;
	  b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}

	if ((((~b) >>> 16) & 0xffff) != (b & 0xffff)){
	  mode = BAD;
	  z.msg = "invalid stored block lengths";
	  r = Z_DATA_ERROR;

	  bitb=b; bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  write=q;
	  return inflate_flush(r);
	}
	left = (b & 0xffff);
	b = k = 0;                       // dump bits
	mode = left!=0 ? STORED : (last!=0 ? DRY : TYPE);
	break;
      case STORED:
	if (n == 0){
	  bitb=b; bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  write=q;
	  return inflate_flush(r);
	}

	if(m==0){
	  if(q==end&&read!=0){
	    q=0; m=(int)(q<read?read-q-1:end-q);
	  }
	  if(m==0){
	    write=q; 
	    r=inflate_flush(r);
	    q=write;m=(int)(q<read?read-q-1:end-q);
	    if(q==end&&read!=0){
	      q=0; m=(int)(q<read?read-q-1:end-q);
	    }
	    if(m==0){
	      bitb=b; bitk=k; 
	      z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      write=q;
	      return inflate_flush(r);
	    }
	  }
	}
	r=Z_OK;

	t = left;
	if(t>n) t = n;
	if(t>m) t = m;
	System.arraycopy(z.next_in, p, window, q, t);
	p += t;  n -= t;
	q += t;  m -= t;
	if ((left -= t) != 0)
	  break;
	mode = last!=0 ? DRY : TYPE;
	break;
      case TABLE:

	while(k<(14)){
	  if(n!=0){
	    r=Z_OK;
	  }
	  else{
	    bitb=b; bitk=k; 
	    z.avail_in=n;
	    z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    write=q;
	    return inflate_flush(r);
	  }
	  n--;
	  b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}

	table = t = (b & 0x3fff);
	if ((t & 0x1f) > 29 || ((t >> 5) & 0x1f) > 29)
	  {
	    mode = BAD;
	    z.msg = "too many length or distance symbols";
	    r = Z_DATA_ERROR;

	    bitb=b; bitk=k; 
	    z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    write=q;
	    return inflate_flush(r);
	  }
	t = 258 + (t & 0x1f) + ((t >> 5) & 0x1f);
	if(blens==null || blens.length<t){
	  blens=new int[t];
	}
	else{
	  for(int i=0; i<t; i++){blens[i]=0;}
	}

	{b>>>=(14);k-=(14);}

	index = 0;
	mode = BTREE;
      case BTREE:
	while (index < 4 + (table >>> 10)){
	  while(k<(3)){
	    if(n!=0){
	      r=Z_OK;
	    }
	    else{
	      bitb=b; bitk=k; 
	      z.avail_in=n;
	      z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      write=q;
	      return inflate_flush(r);
	    }
	    n--;
	    b|=(z.next_in[p++]&0xff)<<k;
	    k+=8;
	  }

	  blens[border[index++]] = b&7;

	  {b>>>=(3);k-=(3);}
	}

	while(index < 19){
	  blens[border[index++]] = 0;
	}

	bb[0] = 7;
	t = inftree.inflate_trees_bits(blens, bb, tb, hufts, z);
	if (t != Z_OK){
	  r = t;
	  if (r == Z_DATA_ERROR){
	    blens=null;
	    mode = BAD;
	  }

	  bitb=b; bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  write=q;
	  return inflate_flush(r);
	}

	index = 0;
	mode = DTREE;
      case DTREE:
	while (true){
	  t = table;
	  if(!(index < 258 + (t & 0x1f) + ((t >> 5) & 0x1f))){
	    break;
	  }

	  int[] h;
	  int i, j, c;

	  t = bb[0];

	  while(k<(t)){
	    if(n!=0){
	      r=Z_OK;
	    }
	    else{
	      bitb=b; bitk=k; 
	      z.avail_in=n;
	      z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      write=q;
	      return inflate_flush(r);
	    }
	    n--;
	    b|=(z.next_in[p++]&0xff)<<k;
	    k+=8;
	  }

	  if(tb[0]==-1){
            //System.err.println("null...");
	  }

	  t=hufts[(tb[0]+(b&inflate_mask[t]))*3+1];
	  c=hufts[(tb[0]+(b&inflate_mask[t]))*3+2];

	  if (c < 16){
	    b>>>=(t);k-=(t);
	    blens[index++] = c;
	  }
	  else { // c == 16..18
	    i = c == 18 ? 7 : c - 14;
	    j = c == 18 ? 11 : 3;

	    while(k<(t+i)){
	      if(n!=0){
		r=Z_OK;
	      }
	      else{
		bitb=b; bitk=k; 
		z.avail_in=n;
		z.total_in+=p-z.next_in_index;z.next_in_index=p;
		write=q;
		return inflate_flush(r);
	      }
	      n--;
	      b|=(z.next_in[p++]&0xff)<<k;
	      k+=8;
	    }

	    b>>>=(t);k-=(t);

	    j += (b & inflate_mask[i]);

	    b>>>=(i);k-=(i);

	    i = index;
	    t = table;
	    if (i + j > 258 + (t & 0x1f) + ((t >> 5) & 0x1f) ||
		(c == 16 && i < 1)){
	      blens=null;
	      mode = BAD;
	      z.msg = "invalid bit length repeat";
	      r = Z_DATA_ERROR;

	      bitb=b; bitk=k; 
	      z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      write=q;
	      return inflate_flush(r);
	    }

	    c = c == 16 ? blens[i-1] : 0;
	    do{
	      blens[i++] = c;
	    }
	    while (--j!=0);
	    index = i;
	  }
	}

	tb[0]=-1;
	{
	  bl[0] = 9;         // must be <= 9 for lookahead assumptions
	  bd[0] = 6;         // must be <= 9 for lookahead assumptions
	  t = table;
	  t = inftree.inflate_trees_dynamic(257 + (t & 0x1f), 
					    1 + ((t >> 5) & 0x1f),
					    blens, bl, bd, tli, tdi, hufts, z);

	  if (t != Z_OK){
	    if (t == Z_DATA_ERROR){
	      blens=null;
	      mode = BAD;
	    }
	    r = t;

	    bitb=b; bitk=k; 
	    z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    write=q;
	    return inflate_flush(r);
	  }
	  codes.init(bl[0], bd[0], hufts, tli[0], hufts, tdi[0]);
	}
	mode = CODES;
      case CODES:
	bitb=b; bitk=k;
	z.avail_in=n; z.total_in+=p-z.next_in_index;z.next_in_index=p;
	write=q;

	if ((r = codes.proc(r)) != Z_STREAM_END){
	  return inflate_flush(r);
	}
	r = Z_OK;
	codes.free(z);

	p=z.next_in_index; n=z.avail_in;b=bitb;k=bitk;
	q=write;m=(int)(q<read?read-q-1:end-q);

	if (last==0){
	  mode = TYPE;
	  break;
	}
	mode = DRY;
      case DRY:
	write=q; 
	r=inflate_flush(r); 
	q=write; m=(int)(q<read?read-q-1:end-q);
	if (read != write){
	  bitb=b; bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  write=q;
	  return inflate_flush(r);
	}
	mode = DONE;
      case DONE:
	r = Z_STREAM_END;

	bitb=b; bitk=k; 
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	write=q;
	return inflate_flush(r);
      case BAD:
	r = Z_DATA_ERROR;

	bitb=b; bitk=k; 
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	write=q;
	return inflate_flush(r);

      default:
	r = Z_STREAM_ERROR;

	bitb=b; bitk=k; 
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	write=q;
	return inflate_flush(r);
      }
    }
  }

  void free(){
    reset();
    window=null;
    hufts=null;
    //ZFREE(z, s);
  }

  void set_dictionary(byte[] d, int start, int n){
    System.arraycopy(d, start, window, 0, n);
    read = write = n;
  }

  // Returns true if inflate is currently at the end of a block generated
  // by Z_SYNC_FLUSH or Z_FULL_FLUSH. 
  int sync_point(){
    return mode == LENS ? 1 : 0;
  }

  // copy as much as possible from the sliding window to the output area
  int inflate_flush(int r){
    int n;
    int p;
    int q;

    // local copies of source and destination pointers
    p = z.next_out_index;
    q = read;

    // compute number of bytes to copy as far as end of window
    n = (int)((q <= write ? write : end) - q);
    if(n > z.avail_out) n = z.avail_out;
    if(n!=0 && r == Z_BUF_ERROR) r = Z_OK;

    // update counters
    z.avail_out -= n;
    z.total_out += n;

    // update check information
    if(check && n>0){
      z.adler.update(window, q, n);
    }

    // copy as far as end of window
    System.arraycopy(window, q, z.next_out, p, n);
    p += n;
    q += n;

    // see if more to copy at beginning of window
    if (q == end){
      // wrap pointers
      q = 0;
      if (write == end)
        write = 0;

      // compute bytes to copy
      n = write - q;
      if (n > z.avail_out) n = z.avail_out;
      if (n!=0 && r == Z_BUF_ERROR) r = Z_OK;

      // update counters
      z.avail_out -= n;
      z.total_out += n;

      // update check information
      if(check && n>0){
	z.adler.update(window, q, n);
      }

      // copy
      System.arraycopy(window, q, z.next_out, p, n);
      p += n;
      q += n;
    }

    // update pointers
    z.next_out_index = p;
    read = q;

    // done
    return r;
  }
}
