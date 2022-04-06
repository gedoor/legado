/* me.ag2s.epublib.zip.InflaterDynHeader
   Copyright (C) 2001 Free Software Foundation, Inc.

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

class InflaterDynHeader {
    private static final int LNUM = 0;
    private static final int DNUM = 1;
    private static final int BLNUM = 2;
    private static final int BLLENS = 3;
    private static final int LENS = 4;
    private static final int REPS = 5;

    private static final int repMin[] = {3, 3, 11};
    private static final int repBits[] = {2, 3, 7};


    private byte[] blLens;
    private byte[] litdistLens;

    private InflaterHuffmanTree blTree;

    private int mode;
    private int lnum, dnum, blnum, num;
    private int repSymbol;
    private byte lastLen;
    private int ptr;

    private static final int[] BL_ORDER =
            {16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};

    public InflaterDynHeader() {
    }

    public boolean decode(StreamManipulator input) throws DataFormatException {
        decode_loop:
        for (; ; ) {
            switch (mode) {
                case LNUM:
                    lnum = input.peekBits(5);
                    if (lnum < 0)
                        return false;
                    lnum += 257;
                    input.dropBits(5);
//  	    System.err.println("LNUM: "+lnum);
                    mode = DNUM;
                    /* fall through */
                case DNUM:
                    dnum = input.peekBits(5);
                    if (dnum < 0)
                        return false;
                    dnum++;
                    input.dropBits(5);
//  	    System.err.println("DNUM: "+dnum);
                    num = lnum + dnum;
                    litdistLens = new byte[num];
                    mode = BLNUM;
                    /* fall through */
                case BLNUM:
                    blnum = input.peekBits(4);
                    if (blnum < 0)
                        return false;
                    blnum += 4;
                    input.dropBits(4);
                    blLens = new byte[19];
                    ptr = 0;
//  	    System.err.println("BLNUM: "+blnum);
                    mode = BLLENS;
                    /* fall through */
                case BLLENS:
                    while (ptr < blnum) {
                        int len = input.peekBits(3);
                        if (len < 0)
                            return false;
                        input.dropBits(3);
//  		System.err.println("blLens["+BL_ORDER[ptr]+"]: "+len);
                        blLens[BL_ORDER[ptr]] = (byte) len;
                        ptr++;
                    }
                    blTree = new InflaterHuffmanTree(blLens);
                    blLens = null;
                    ptr = 0;
                    mode = LENS;
                    /* fall through */
                case LENS: {
                    int symbol;
                    while (((symbol = blTree.getSymbol(input)) & ~15) == 0) {
                        /* Normal case: symbol in [0..15] */

//  		  System.err.println("litdistLens["+ptr+"]: "+symbol);
                        litdistLens[ptr++] = lastLen = (byte) symbol;

                        if (ptr == num) {
                            /* Finished */
                            return true;
                        }
                    }

                    /* need more input ? */
                    if (symbol < 0)
                        return false;

                    /* otherwise repeat code */
                    if (symbol >= 17) {
                        /* repeat zero */
//  		  System.err.println("repeating zero");
                        lastLen = 0;
                    } else {
                        if (ptr == 0)
                            throw new DataFormatException();
                    }
                    repSymbol = symbol - 16;
                    mode = REPS;
                }
                /* fall through */

                case REPS: {
                    int bits = repBits[repSymbol];
                    int count = input.peekBits(bits);
                    if (count < 0)
                        return false;
                    input.dropBits(bits);
                    count += repMin[repSymbol];
//  	      System.err.println("litdistLens repeated: "+count);

                    if (ptr + count > num)
                        throw new DataFormatException();
                    while (count-- > 0)
                        litdistLens[ptr++] = lastLen;

                    if (ptr == num) {
                        /* Finished */
                        return true;
                    }
                }
                mode = LENS;
                continue decode_loop;
            }
        }
    }

    public InflaterHuffmanTree buildLitLenTree() throws DataFormatException {
        byte[] litlenLens = new byte[lnum];
        System.arraycopy(litdistLens, 0, litlenLens, 0, lnum);
        return new InflaterHuffmanTree(litlenLens);
    }

    public InflaterHuffmanTree buildDistTree() throws DataFormatException {
        byte[] distLens = new byte[dnum];
        System.arraycopy(litdistLens, lnum, distLens, 0, dnum);
        return new InflaterHuffmanTree(distLens);
    }
}
