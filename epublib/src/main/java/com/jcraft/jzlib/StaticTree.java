/* -*-mode:java; c-basic-offset:2; -*- */
/*
Copyright (c) 2000,2001,2002,2003 ymnk, JCraft,Inc. All rights reserved.

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

final class StaticTree{
  static final private int MAX_BITS=15;

  static final private int BL_CODES=19;
  static final private int D_CODES=30;
  static final private int LITERALS=256;
  static final private int LENGTH_CODES=29;
  static final private int L_CODES=(LITERALS+1+LENGTH_CODES);

  // Bit length codes must not exceed MAX_BL_BITS bits
  static final int MAX_BL_BITS=7; 

  static final short[] static_ltree = {
    12,  8, 140,  8,  76,  8, 204,  8,  44,  8,
    172,  8, 108,  8, 236,  8,  28,  8, 156,  8,
    92,  8, 220,  8,  60,  8, 188,  8, 124,  8,
    252,  8,   2,  8, 130,  8,  66,  8, 194,  8,
    34,  8, 162,  8,  98,  8, 226,  8,  18,  8,
    146,  8,  82,  8, 210,  8,  50,  8, 178,  8,
    114,  8, 242,  8,  10,  8, 138,  8,  74,  8,
    202,  8,  42,  8, 170,  8, 106,  8, 234,  8,
    26,  8, 154,  8,  90,  8, 218,  8,  58,  8,
    186,  8, 122,  8, 250,  8,   6,  8, 134,  8,
    70,  8, 198,  8,  38,  8, 166,  8, 102,  8,
    230,  8,  22,  8, 150,  8,  86,  8, 214,  8,
    54,  8, 182,  8, 118,  8, 246,  8,  14,  8,
    142,  8,  78,  8, 206,  8,  46,  8, 174,  8,
    110,  8, 238,  8,  30,  8, 158,  8,  94,  8,
    222,  8,  62,  8, 190,  8, 126,  8, 254,  8,
    1,  8, 129,  8,  65,  8, 193,  8,  33,  8,
    161,  8,  97,  8, 225,  8,  17,  8, 145,  8,
    81,  8, 209,  8,  49,  8, 177,  8, 113,  8,
    241,  8,   9,  8, 137,  8,  73,  8, 201,  8,
    41,  8, 169,  8, 105,  8, 233,  8,  25,  8,
    153,  8,  89,  8, 217,  8,  57,  8, 185,  8,
    121,  8, 249,  8,   5,  8, 133,  8,  69,  8,
    197,  8,  37,  8, 165,  8, 101,  8, 229,  8,
    21,  8, 149,  8,  85,  8, 213,  8,  53,  8,
    181,  8, 117,  8, 245,  8,  13,  8, 141,  8,
    77,  8, 205,  8,  45,  8, 173,  8, 109,  8,
    237,  8,  29,  8, 157,  8,  93,  8, 221,  8,
    61,  8, 189,  8, 125,  8, 253,  8,  19,  9,
    275,  9, 147,  9, 403,  9,  83,  9, 339,  9,
    211,  9, 467,  9,  51,  9, 307,  9, 179,  9,
    435,  9, 115,  9, 371,  9, 243,  9, 499,  9,
    11,  9, 267,  9, 139,  9, 395,  9,  75,  9,
    331,  9, 203,  9, 459,  9,  43,  9, 299,  9,
    171,  9, 427,  9, 107,  9, 363,  9, 235,  9,
    491,  9,  27,  9, 283,  9, 155,  9, 411,  9,
    91,  9, 347,  9, 219,  9, 475,  9,  59,  9,
    315,  9, 187,  9, 443,  9, 123,  9, 379,  9,
    251,  9, 507,  9,   7,  9, 263,  9, 135,  9,
    391,  9,  71,  9, 327,  9, 199,  9, 455,  9,
    39,  9, 295,  9, 167,  9, 423,  9, 103,  9,
    359,  9, 231,  9, 487,  9,  23,  9, 279,  9,
    151,  9, 407,  9,  87,  9, 343,  9, 215,  9,
    471,  9,  55,  9, 311,  9, 183,  9, 439,  9,
    119,  9, 375,  9, 247,  9, 503,  9,  15,  9,
    271,  9, 143,  9, 399,  9,  79,  9, 335,  9,
    207,  9, 463,  9,  47,  9, 303,  9, 175,  9,
    431,  9, 111,  9, 367,  9, 239,  9, 495,  9,
    31,  9, 287,  9, 159,  9, 415,  9,  95,  9,
    351,  9, 223,  9, 479,  9,  63,  9, 319,  9,
    191,  9, 447,  9, 127,  9, 383,  9, 255,  9,
    511,  9,   0,  7,  64,  7,  32,  7,  96,  7,
    16,  7,  80,  7,  48,  7, 112,  7,   8,  7,
    72,  7,  40,  7, 104,  7,  24,  7,  88,  7,
    56,  7, 120,  7,   4,  7,  68,  7,  36,  7,
    100,  7,  20,  7,  84,  7,  52,  7, 116,  7,
    3,  8, 131,  8,  67,  8, 195,  8,  35,  8,
    163,  8,  99,  8, 227,  8
  };

  static final short[] static_dtree = {
    0, 5, 16, 5,  8, 5, 24, 5,  4, 5,
    20, 5, 12, 5, 28, 5,  2, 5, 18, 5,
    10, 5, 26, 5,  6, 5, 22, 5, 14, 5,
    30, 5,  1, 5, 17, 5,  9, 5, 25, 5,
    5, 5, 21, 5, 13, 5, 29, 5,  3, 5,
    19, 5, 11, 5, 27, 5,  7, 5, 23, 5
  };

  static StaticTree static_l_desc =
    new StaticTree(static_ltree, Tree.extra_lbits,
		   LITERALS+1, L_CODES, MAX_BITS);

  static StaticTree static_d_desc =
    new StaticTree(static_dtree, Tree.extra_dbits,
		   0,  D_CODES, MAX_BITS);

  static StaticTree static_bl_desc =
    new StaticTree(null, Tree.extra_blbits,
		   0, BL_CODES, MAX_BL_BITS);

  short[] static_tree;     // static tree or null
  int[] extra_bits;        // extra bits for each code or null
  int extra_base;          // base index for extra_bits
  int elems;               // max number of elements in the tree
  int max_length;          // max bit length for the codes

  private StaticTree(short[] static_tree,
                     int[] extra_bits,
                     int extra_base,
                     int elems,
                     int max_length){
    this.static_tree=static_tree;
    this.extra_bits=extra_bits;
    this.extra_base=extra_base;
    this.elems=elems;
    this.max_length=max_length;
  }
}
