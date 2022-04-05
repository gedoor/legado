/* me.ag2s.epublib.zip.DeflaterHuffman
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

/**
 * This is the DeflaterHuffman class.
 * <p>
 * This class is <i>not</i> thread safe.  This is inherent in the API, due
 * to the split of deflate and setInput.
 *
 * @author Jochen Hoenicke
 * @date Jan 6, 2000
 */
class DeflaterHuffman {
    private static final int BUFSIZE = 1 << (DeflaterConstants.DEFAULT_MEM_LEVEL + 6);
    private static final int LITERAL_NUM = 286;
    private static final int DIST_NUM = 30;
    private static final int BITLEN_NUM = 19;
    private static final int REP_3_6 = 16;
    private static final int REP_3_10 = 17;
    private static final int REP_11_138 = 18;
    private static final int EOF_SYMBOL = 256;
    private static final int[] BL_ORDER =
            {16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15};

    private final static String bit4Reverse =
            "\000\010\004\014\002\012\006\016\001\011\005\015\003\013\007\017";

    class Tree {
        short[] freqs;
        short[] codes;
        byte[] length;
        int[] bl_counts;
        int minNumCodes, numCodes;
        int maxLength;

        Tree(int elems, int minCodes, int maxLength) {
            this.minNumCodes = minCodes;
            this.maxLength = maxLength;
            freqs = new short[elems];
            bl_counts = new int[maxLength];
        }

        void reset() {
            for (int i = 0; i < freqs.length; i++)
                freqs[i] = 0;
            codes = null;
            length = null;
        }

        final void writeSymbol(int code) {
            if (DeflaterConstants.DEBUGGING) {
                freqs[code]--;
//  	  System.err.print("writeSymbol("+freqs.length+","+code+"): ");
            }
            pending.writeBits(codes[code] & 0xffff, length[code]);
        }

        final void checkEmpty() {
            boolean empty = true;
            for (int i = 0; i < freqs.length; i++)
                if (freqs[i] != 0) {
                    System.err.println("freqs[" + i + "] == " + freqs[i]);
                    empty = false;
                }
            if (!empty)
                throw new InternalError();
            System.err.println("checkEmpty suceeded!");
        }

        void setStaticCodes(short[] stCodes, byte[] stLength) {
            codes = stCodes;
            length = stLength;
        }

        public void buildCodes() {
            int[] nextCode = new int[maxLength];
            int code = 0;
            codes = new short[freqs.length];

            if (DeflaterConstants.DEBUGGING)
                System.err.println("buildCodes: " + freqs.length);
            for (int bits = 0; bits < maxLength; bits++) {
                nextCode[bits] = code;
                code += bl_counts[bits] << (15 - bits);
                if (DeflaterConstants.DEBUGGING)
                    System.err.println("bits: " + (bits + 1) + " count: " + bl_counts[bits]
                            + " nextCode: " + Integer.toHexString(code));
            }
            if (DeflaterConstants.DEBUGGING && code != 65536)
                throw new RuntimeException("Inconsistent bl_counts!");

            for (int i = 0; i < numCodes; i++) {
                int bits = length[i];
                if (bits > 0) {
                    if (DeflaterConstants.DEBUGGING)
                        System.err.println("codes[" + i + "] = rev("
                                + Integer.toHexString(nextCode[bits - 1]) + "),"
                                + bits);
                    codes[i] = bitReverse(nextCode[bits - 1]);
                    nextCode[bits - 1] += 1 << (16 - bits);
                }
            }
        }

        private void buildLength(int childs[]) {
            this.length = new byte[freqs.length];
            int numNodes = childs.length / 2;
            int numLeafs = (numNodes + 1) / 2;
            int overflow = 0;

            for (int i = 0; i < maxLength; i++)
                bl_counts[i] = 0;

            /* First calculate optimal bit lengths */
            int lengths[] = new int[numNodes];
            lengths[numNodes - 1] = 0;
            for (int i = numNodes - 1; i >= 0; i--) {
                if (childs[2 * i + 1] != -1) {
                    int bitLength = lengths[i] + 1;
                    if (bitLength > maxLength) {
                        bitLength = maxLength;
                        overflow++;
                    }
                    lengths[childs[2 * i]] = lengths[childs[2 * i + 1]] = bitLength;
                } else {
                    /* A leaf node */
                    int bitLength = lengths[i];
                    bl_counts[bitLength - 1]++;
                    this.length[childs[2 * i]] = (byte) lengths[i];
                }
            }

            if (DeflaterConstants.DEBUGGING) {
                System.err.println("Tree " + freqs.length + " lengths:");
                for (int i = 0; i < numLeafs; i++)
                    System.err.println("Node " + childs[2 * i] + " freq: " + freqs[childs[2 * i]]
                            + " len: " + length[childs[2 * i]]);
            }

            if (overflow == 0)
                return;

            int incrBitLen = maxLength - 1;
            do {
                /* Find the first bit length which could increase: */
                while (bl_counts[--incrBitLen] == 0)
                    ;

                /* Move this node one down and remove a corresponding
                 * amount of overflow nodes.
                 */
                do {
                    bl_counts[incrBitLen]--;
                    bl_counts[++incrBitLen]++;
                    overflow -= 1 << (maxLength - 1 - incrBitLen);
                }
                while (overflow > 0 && incrBitLen < maxLength - 1);
            }
            while (overflow > 0);

            /* We may have overshot above.  Move some nodes from maxLength to
             * maxLength-1 in that case.
             */
            bl_counts[maxLength - 1] += overflow;
            bl_counts[maxLength - 2] -= overflow;

            /* Now recompute all bit lengths, scanning in increasing
             * frequency.  It is simpler to reconstruct all lengths instead of
             * fixing only the wrong ones. This idea is taken from 'ar'
             * written by Haruhiko Okumura.
             *
             * The nodes were inserted with decreasing frequency into the childs
             * array.
             */
            int nodePtr = 2 * numLeafs;
            for (int bits = maxLength; bits != 0; bits--) {
                int n = bl_counts[bits - 1];
                while (n > 0) {
                    int childPtr = 2 * childs[nodePtr++];
                    if (childs[childPtr + 1] == -1) {
                        /* We found another leaf */
                        length[childs[childPtr]] = (byte) bits;
                        n--;
                    }
                }
            }
            if (DeflaterConstants.DEBUGGING) {
                System.err.println("*** After overflow elimination. ***");
                for (int i = 0; i < numLeafs; i++)
                    System.err.println("Node " + childs[2 * i] + " freq: " + freqs[childs[2 * i]]
                            + " len: " + length[childs[2 * i]]);
            }
        }

        void buildTree() {
            int numSymbols = freqs.length;

            /* heap is a priority queue, sorted by frequency, least frequent
             * nodes first.  The heap is a binary tree, with the property, that
             * the parent node is smaller than both child nodes.  This assures
             * that the smallest node is the first parent.
             *
             * The binary tree is encoded in an array:  0 is root node and
             * the nodes 2*n+1, 2*n+2 are the child nodes of node n.
             */
            int[] heap = new int[numSymbols];
            int heapLen = 0;
            int maxCode = 0;
            for (int n = 0; n < numSymbols; n++) {
                int freq = freqs[n];
                if (freq != 0) {
                    /* Insert n into heap */
                    int pos = heapLen++;
                    int ppos;
                    while (pos > 0 &&
                            freqs[heap[ppos = (pos - 1) / 2]] > freq) {
                        heap[pos] = heap[ppos];
                        pos = ppos;
                    }
                    heap[pos] = n;
                    maxCode = n;
                }
            }

            /* We could encode a single literal with 0 bits but then we
             * don't see the literals.  Therefore we force at least two
             * literals to avoid this case.  We don't care about order in
             * this case, both literals get a 1 bit code.
             */
            while (heapLen < 2) {
                int node = maxCode < 2 ? ++maxCode : 0;
                heap[heapLen++] = node;
            }

            numCodes = Math.max(maxCode + 1, minNumCodes);

            int numLeafs = heapLen;
            int[] childs = new int[4 * heapLen - 2];
            int[] values = new int[2 * heapLen - 1];
            int numNodes = numLeafs;
            for (int i = 0; i < heapLen; i++) {
                int node = heap[i];
                childs[2 * i] = node;
                childs[2 * i + 1] = -1;
                values[i] = freqs[node] << 8;
                heap[i] = i;
            }

            /* Construct the Huffman tree by repeatedly combining the least two
             * frequent nodes.
             */
            do {
                int first = heap[0];
                int last = heap[--heapLen];

                /* Propagate the hole to the leafs of the heap */
                int ppos = 0;
                int path = 1;
                while (path < heapLen) {
                    if (path + 1 < heapLen
                            && values[heap[path]] > values[heap[path + 1]])
                        path++;

                    heap[ppos] = heap[path];
                    ppos = path;
                    path = path * 2 + 1;
                }

                /* Now propagate the last element down along path.  Normally
                 * it shouldn't go too deep.
                 */
                int lastVal = values[last];
                while ((path = ppos) > 0
                        && values[heap[ppos = (path - 1) / 2]] > lastVal)
                    heap[path] = heap[ppos];
                heap[path] = last;


                int second = heap[0];

                /* Create a new node father of first and second */
                last = numNodes++;
                childs[2 * last] = first;
                childs[2 * last + 1] = second;
                int mindepth = Math.min(values[first] & 0xff, values[second] & 0xff);
                values[last] = lastVal = values[first] + values[second] - mindepth + 1;

                /* Again, propagate the hole to the leafs */
                ppos = 0;
                path = 1;
                while (path < heapLen) {
                    if (path + 1 < heapLen
                            && values[heap[path]] > values[heap[path + 1]])
                        path++;

                    heap[ppos] = heap[path];
                    ppos = path;
                    path = ppos * 2 + 1;
                }

                /* Now propagate the new element down along path */
                while ((path = ppos) > 0
                        && values[heap[ppos = (path - 1) / 2]] > lastVal)
                    heap[path] = heap[ppos];
                heap[path] = last;
            }
            while (heapLen > 1);

            if (heap[0] != childs.length / 2 - 1)
                throw new RuntimeException("Weird!");

            buildLength(childs);
        }

        int getEncodedLength() {
            int len = 0;
            for (int i = 0; i < freqs.length; i++)
                len += freqs[i] * length[i];
            return len;
        }

        void calcBLFreq(Tree blTree) {
            int max_count;               /* max repeat count */
            int min_count;               /* min repeat count */
            int count;                   /* repeat count of the current code */
            int curlen = -1;             /* length of current code */

            int i = 0;
            while (i < numCodes) {
                count = 1;
                int nextlen = length[i];
                if (nextlen == 0) {
                    max_count = 138;
                    min_count = 3;
                } else {
                    max_count = 6;
                    min_count = 3;
                    if (curlen != nextlen) {
                        blTree.freqs[nextlen]++;
                        count = 0;
                    }
                }
                curlen = nextlen;
                i++;

                while (i < numCodes && curlen == length[i]) {
                    i++;
                    if (++count >= max_count)
                        break;
                }

                if (count < min_count)
                    blTree.freqs[curlen] += count;
                else if (curlen != 0)
                    blTree.freqs[REP_3_6]++;
                else if (count <= 10)
                    blTree.freqs[REP_3_10]++;
                else
                    blTree.freqs[REP_11_138]++;
            }
        }

        void writeTree(Tree blTree) {
            int max_count;               /* max repeat count */
            int min_count;               /* min repeat count */
            int count;                   /* repeat count of the current code */
            int curlen = -1;             /* length of current code */

            int i = 0;
            while (i < numCodes) {
                count = 1;
                int nextlen = length[i];
                if (nextlen == 0) {
                    max_count = 138;
                    min_count = 3;
                } else {
                    max_count = 6;
                    min_count = 3;
                    if (curlen != nextlen) {
                        blTree.writeSymbol(nextlen);
                        count = 0;
                    }
                }
                curlen = nextlen;
                i++;

                while (i < numCodes && curlen == length[i]) {
                    i++;
                    if (++count >= max_count)
                        break;
                }

                if (count < min_count) {
                    while (count-- > 0)
                        blTree.writeSymbol(curlen);
                } else if (curlen != 0) {
                    blTree.writeSymbol(REP_3_6);
                    pending.writeBits(count - 3, 2);
                } else if (count <= 10) {
                    blTree.writeSymbol(REP_3_10);
                    pending.writeBits(count - 3, 3);
                } else {
                    blTree.writeSymbol(REP_11_138);
                    pending.writeBits(count - 11, 7);
                }
            }
        }
    }


    DeflaterPending pending;
    private Tree literalTree, distTree, blTree;

    private short d_buf[];
    private byte l_buf[];
    private int last_lit;
    private int extra_bits;

    private static short staticLCodes[];
    private static byte staticLLength[];
    private static short staticDCodes[];
    private static byte staticDLength[];

    /**
     * Reverse the bits of a 16 bit value.
     */
    static short bitReverse(int value) {
        return (short) (bit4Reverse.charAt(value & 0xf) << 12
                | bit4Reverse.charAt((value >> 4) & 0xf) << 8
                | bit4Reverse.charAt((value >> 8) & 0xf) << 4
                | bit4Reverse.charAt(value >> 12));
    }

    static {
        /* See RFC 1951 3.2.6 */
        /* Literal codes */
        staticLCodes = new short[LITERAL_NUM];
        staticLLength = new byte[LITERAL_NUM];
        int i = 0;
        while (i < 144) {
            staticLCodes[i] = bitReverse((0x030 + i) << 8);
            staticLLength[i++] = 8;
        }
        while (i < 256) {
            staticLCodes[i] = bitReverse((0x190 - 144 + i) << 7);
            staticLLength[i++] = 9;
        }
        while (i < 280) {
            staticLCodes[i] = bitReverse((0x000 - 256 + i) << 9);
            staticLLength[i++] = 7;
        }
        while (i < LITERAL_NUM) {
            staticLCodes[i] = bitReverse((0x0c0 - 280 + i) << 8);
            staticLLength[i++] = 8;
        }

        /* Distant codes */
        staticDCodes = new short[DIST_NUM];
        staticDLength = new byte[DIST_NUM];
        for (i = 0; i < DIST_NUM; i++) {
            staticDCodes[i] = bitReverse(i << 11);
            staticDLength[i] = 5;
        }
    }

    public DeflaterHuffman(DeflaterPending pending) {
        this.pending = pending;

        literalTree = new Tree(LITERAL_NUM, 257, 15);
        distTree = new Tree(DIST_NUM, 1, 15);
        blTree = new Tree(BITLEN_NUM, 4, 7);

        d_buf = new short[BUFSIZE];
        l_buf = new byte[BUFSIZE];
    }

    public final void reset() {
        last_lit = 0;
        extra_bits = 0;
        literalTree.reset();
        distTree.reset();
        blTree.reset();
    }

    private final int l_code(int len) {
        if (len == 255)
            return 285;

        int code = 257;
        while (len >= 8) {
            code += 4;
            len >>= 1;
        }
        return code + len;
    }

    private final int d_code(int distance) {
        int code = 0;
        while (distance >= 4) {
            code += 2;
            distance >>= 1;
        }
        return code + distance;
    }

    public void sendAllTrees(int blTreeCodes) {
        blTree.buildCodes();
        literalTree.buildCodes();
        distTree.buildCodes();
        pending.writeBits(literalTree.numCodes - 257, 5);
        pending.writeBits(distTree.numCodes - 1, 5);
        pending.writeBits(blTreeCodes - 4, 4);
        for (int rank = 0; rank < blTreeCodes; rank++)
            pending.writeBits(blTree.length[BL_ORDER[rank]], 3);
        literalTree.writeTree(blTree);
        distTree.writeTree(blTree);
        if (DeflaterConstants.DEBUGGING)
            blTree.checkEmpty();
    }

    public void compressBlock() {
        for (int i = 0; i < last_lit; i++) {
            int litlen = l_buf[i] & 0xff;
            int dist = d_buf[i];
            if (dist-- != 0) {
                if (DeflaterConstants.DEBUGGING)
                    System.err.print("[" + (dist + 1) + "," + (litlen + 3) + "]: ");

                int lc = l_code(litlen);
                literalTree.writeSymbol(lc);

                int bits = (lc - 261) / 4;
                if (bits > 0 && bits <= 5)
                    pending.writeBits(litlen & ((1 << bits) - 1), bits);

                int dc = d_code(dist);
                distTree.writeSymbol(dc);

                bits = dc / 2 - 1;
                if (bits > 0)
                    pending.writeBits(dist & ((1 << bits) - 1), bits);
            } else {
                if (DeflaterConstants.DEBUGGING) {
                    if (litlen > 32 && litlen < 127)
                        System.err.print("(" + (char) litlen + "): ");
                    else
                        System.err.print("{" + litlen + "}: ");
                }
                literalTree.writeSymbol(litlen);
            }
        }
        if (DeflaterConstants.DEBUGGING)
            System.err.print("EOF: ");
        literalTree.writeSymbol(EOF_SYMBOL);
        if (DeflaterConstants.DEBUGGING) {
            literalTree.checkEmpty();
            distTree.checkEmpty();
        }
    }

    public void flushStoredBlock(byte[] stored,
                                 int stored_offset, int stored_len,
                                 boolean lastBlock) {
        if (DeflaterConstants.DEBUGGING)
            System.err.println("Flushing stored block " + stored_len);
        pending.writeBits((DeflaterConstants.STORED_BLOCK << 1)
                + (lastBlock ? 1 : 0), 3);
        pending.alignToByte();
        pending.writeShort(stored_len);
        pending.writeShort(~stored_len);
        pending.writeBlock(stored, stored_offset, stored_len);
        reset();
    }

    public void flushBlock(byte[] stored, int stored_offset, int stored_len,
                           boolean lastBlock) {
        literalTree.freqs[EOF_SYMBOL]++;

        /* Build trees */
        literalTree.buildTree();
        distTree.buildTree();

        /* Calculate bitlen frequency */
        literalTree.calcBLFreq(blTree);
        distTree.calcBLFreq(blTree);

        /* Build bitlen tree */
        blTree.buildTree();

        int blTreeCodes = 4;
        for (int i = 18; i > blTreeCodes; i--) {
            if (blTree.length[BL_ORDER[i]] > 0)
                blTreeCodes = i + 1;
        }
        int opt_len = 14 + blTreeCodes * 3 + blTree.getEncodedLength()
                + literalTree.getEncodedLength() + distTree.getEncodedLength()
                + extra_bits;

        int static_len = extra_bits;
        for (int i = 0; i < LITERAL_NUM; i++)
            static_len += literalTree.freqs[i] * staticLLength[i];
        for (int i = 0; i < DIST_NUM; i++)
            static_len += distTree.freqs[i] * staticDLength[i];
        if (opt_len >= static_len) {
            /* Force static trees */
            opt_len = static_len;
        }

        if (stored_offset >= 0 && stored_len + 4 < opt_len >> 3) {
            /* Store Block */
            if (DeflaterConstants.DEBUGGING)
                System.err.println("Storing, since " + stored_len + " < " + opt_len
                        + " <= " + static_len);
            flushStoredBlock(stored, stored_offset, stored_len, lastBlock);
        } else if (opt_len == static_len) {
            /* Encode with static tree */
            pending.writeBits((DeflaterConstants.STATIC_TREES << 1)
                    + (lastBlock ? 1 : 0), 3);
            literalTree.setStaticCodes(staticLCodes, staticLLength);
            distTree.setStaticCodes(staticDCodes, staticDLength);
            compressBlock();
            reset();
        } else {
            /* Encode with dynamic tree */
            pending.writeBits((DeflaterConstants.DYN_TREES << 1)
                    + (lastBlock ? 1 : 0), 3);
            sendAllTrees(blTreeCodes);
            compressBlock();
            reset();
        }
    }

    public final boolean isFull() {
        return last_lit == BUFSIZE;
    }

    public final boolean tallyLit(int lit) {
        if (DeflaterConstants.DEBUGGING) {
            if (lit > 32 && lit < 127)
                System.err.println("(" + (char) lit + ")");
            else
                System.err.println("{" + lit + "}");
        }
        d_buf[last_lit] = 0;
        l_buf[last_lit++] = (byte) lit;
        literalTree.freqs[lit]++;
        return last_lit == BUFSIZE;
    }

    public final boolean tallyDist(int dist, int len) {
        if (DeflaterConstants.DEBUGGING)
            System.err.println("[" + dist + "," + len + "]");

        d_buf[last_lit] = (short) dist;
        l_buf[last_lit++] = (byte) (len - 3);

        int lc = l_code(len - 3);
        literalTree.freqs[lc]++;
        if (lc >= 265 && lc < 285)
            extra_bits += (lc - 261) / 4;

        int dc = d_code(dist - 1);
        distTree.freqs[dc]++;
        if (dc >= 4)
            extra_bits += dc / 2 - 1;
        return last_lit == BUFSIZE;
    }
}
