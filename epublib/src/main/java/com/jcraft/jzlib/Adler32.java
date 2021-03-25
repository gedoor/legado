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

final public class Adler32 implements Checksum {

  // largest prime smaller than 65536
  static final private int BASE=65521; 
  // NMAX is the largest n such that 255n(n+1)/2 + (n+1)(BASE-1) <= 2^32-1
  static final private int NMAX=5552;

  private long s1=1L;
  private long s2=0L;

  public void reset(long init){
    s1=init&0xffff;
    s2=(init>>16)&0xffff;
  }

  public void reset(){
    s1=1L;
    s2=0L;
  }

  public long getValue(){
    return ((s2<<16)|s1);
  }

  public void update(byte[] buf, int index, int len){

    if(len==1){
      s1+=buf[index++]&0xff; s2+=s1;
      s1%=BASE;
      s2%=BASE;
      return;
    }

    int len1 = len/NMAX;
    int len2 = len%NMAX;
    while(len1-->0) {
      int k=NMAX;
      len-=k;
      while(k-->0){
	s1+=buf[index++]&0xff; s2+=s1;
      }
      s1%=BASE;
      s2%=BASE;
    }

    int k=len2;
    len-=k;
    while(k-->0){
      s1+=buf[index++]&0xff; s2+=s1;
    }
    s1%=BASE;
    s2%=BASE;
  }

  public Adler32 copy(){
    Adler32 foo = new Adler32();
    foo.s1 = this.s1;
    foo.s2 = this.s2;
    return foo;
  }

  // The following logic has come from zlib.1.2.
  static long combine(long adler1, long adler2, long len2){
    long BASEL = (long)BASE;
    long sum1;
    long sum2;
    long rem;  // unsigned int

    rem = len2 % BASEL;
    sum1 = adler1 & 0xffffL;
    sum2 = rem * sum1;
    sum2 %= BASEL; // MOD(sum2);
    sum1 += (adler2 & 0xffffL) + BASEL - 1;
    sum2 += ((adler1 >> 16) & 0xffffL) + ((adler2 >> 16) & 0xffffL) + BASEL - rem;
    if (sum1 >= BASEL) sum1 -= BASEL;
    if (sum1 >= BASEL) sum1 -= BASEL;
    if (sum2 >= (BASEL << 1)) sum2 -= (BASEL << 1);
    if (sum2 >= BASEL) sum2 -= BASEL;
    return sum1 | (sum2 << 16);
  }

/*
  private java.util.zip.Adler32 adler=new java.util.zip.Adler32();
  public void update(byte[] buf, int index, int len){
    if(buf==null) {adler.reset();}
    else{adler.update(buf, index, len);}
  }
  public void reset(){
    adler.reset();
  }
  public void reset(long init){
    if(init==1L){
      adler.reset();
    }
    else{
      System.err.println("unsupported operation");
    }
  }
  public long getValue(){
    return adler.getValue();
  }
*/
}
