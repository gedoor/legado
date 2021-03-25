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

final public class JZlib{
  private static final String version="1.1.0";
  public static String version(){return version;}

  static final public int MAX_WBITS=15;        // 32K LZ77 window
  static final public int DEF_WBITS=MAX_WBITS;

  public enum WrapperType {
    NONE, ZLIB, GZIP, ANY
  }

  public static final WrapperType W_NONE = WrapperType.NONE;
  public static final WrapperType W_ZLIB = WrapperType.ZLIB;
  public static final WrapperType W_GZIP = WrapperType.GZIP;
  public static final WrapperType W_ANY = WrapperType.ANY;

  // compression levels
  static final public int Z_NO_COMPRESSION=0;
  static final public int Z_BEST_SPEED=1;
  static final public int Z_BEST_COMPRESSION=9;
  static final public int Z_DEFAULT_COMPRESSION=(-1);

  // compression strategy
  static final public int Z_FILTERED=1;
  static final public int Z_HUFFMAN_ONLY=2;
  static final public int Z_DEFAULT_STRATEGY=0;

  static final public int Z_NO_FLUSH=0;
  static final public int Z_PARTIAL_FLUSH=1;
  static final public int Z_SYNC_FLUSH=2;
  static final public int Z_FULL_FLUSH=3;
  static final public int Z_FINISH=4;

  static final public int Z_OK=0;
  static final public int Z_STREAM_END=1;
  static final public int Z_NEED_DICT=2;
  static final public int Z_ERRNO=-1;
  static final public int Z_STREAM_ERROR=-2;
  static final public int Z_DATA_ERROR=-3;
  static final public int Z_MEM_ERROR=-4;
  static final public int Z_BUF_ERROR=-5;
  static final public int Z_VERSION_ERROR=-6;

  // The three kinds of block type
  static final public byte Z_BINARY = 0;
  static final public byte Z_ASCII = 1;
  static final public byte Z_UNKNOWN = 2;

  public static long adler32_combine(long adler1, long adler2, long len2){
    return Adler32.combine(adler1, adler2, len2);
  }

  public static long crc32_combine(long crc1, long crc2, long len2){
    return CRC32.combine(crc1, crc2, len2);
  }
}
