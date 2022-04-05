/* me.ag2s.epublib.zip.DeflaterEngine
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

class DeflaterEngine implements DeflaterConstants {
    private final static int TOO_FAR = 4096;

    private int ins_h;

    /**
     * Hashtable, hashing three characters to an index for window, so
     * that window[index]..window[index+2] have this hash code.
     * Note that the array should really be unsigned short, so you need
     * to and the values with 0xffff.
     */
    private short[] head;

    /**
     * prev[index & WMASK] points to the previous index that has the
     * same hash code as the string starting at index.  This way
     * entries with the same hash code are in a linked list.
     * Note that the array should really be unsigned short, so you need
     * to and the values with 0xffff.
     */
    private short[] prev;

    private int matchStart, matchLen;
    private boolean prevAvailable;
    private int blockStart;

    /**
     * strstart points to the current character in window.
     */
    private int strstart;

    /**
     * lookahead is the number of characters starting at strstart in
     * window that are valid.
     * So window[strstart] until window[strstart+lookahead-1] are valid
     * characters.
     */
    private int lookahead;

    /**
     * This array contains the part of the uncompressed stream that
     * is of relevance.  The current character is indexed by strstart.
     */
    private byte[] window;

    private int strategy, max_chain, max_lazy, niceLength, goodLength;

    /**
     * The current compression function.
     */
    private int comprFunc;

    /**
     * The input data for compression.
     */
    private byte[] inputBuf;

    /**
     * The total bytes of input read.
     */
    private int totalIn;

    /**
     * The offset into inputBuf, where input data starts.
     */
    private int inputOff;

    /**
     * The end offset of the input data.
     */
    private int inputEnd;

    private DeflaterPending pending;
    private DeflaterHuffman huffman;

    /**
     * The adler checksum
     */
    private Adler32 adler;

    /* DEFLATE ALGORITHM:
     *
     * The uncompressed stream is inserted into the window array.  When
     * the window array is full the first half is thrown away and the
     * second half is copied to the beginning.
     *
     * The head array is a hash table.  Three characters build a hash value
     * and they the value points to the corresponding index in window of
     * the last string with this hash.  The prev array implements a
     * linked list of matches with the same hash: prev[index & WMASK] points
     * to the previous index with the same hash.
     *
     *
     */


    DeflaterEngine(DeflaterPending pending) {
        this.pending = pending;
        huffman = new DeflaterHuffman(pending);
        adler = new Adler32();

        window = new byte[2 * WSIZE];
        head = new short[HASH_SIZE];
        prev = new short[WSIZE];

        /* We start at index 1, to avoid a implementation deficiency, that
         * we cannot build a repeat pattern at index 0.
         */
        blockStart = strstart = 1;
    }

    public void reset() {
        huffman.reset();
        adler.reset();
        blockStart = strstart = 1;
        lookahead = 0;
        totalIn = 0;
        prevAvailable = false;
        matchLen = MIN_MATCH - 1;
        for (int i = 0; i < HASH_SIZE; i++)
            head[i] = 0;
        for (int i = 0; i < WSIZE; i++)
            prev[i] = 0;
    }

    public final void resetAdler() {
        adler.reset();
    }

    public final int getAdler() {
        int chksum = (int) adler.getValue();
        return chksum;
    }

    public final int getTotalIn() {
        return totalIn;
    }

    public final void setStrategy(int strat) {
        strategy = strat;
    }

    public void setLevel(int lvl) {
        goodLength = DeflaterConstants.GOOD_LENGTH[lvl];
        max_lazy = DeflaterConstants.MAX_LAZY[lvl];
        niceLength = DeflaterConstants.NICE_LENGTH[lvl];
        max_chain = DeflaterConstants.MAX_CHAIN[lvl];

        if (DeflaterConstants.COMPR_FUNC[lvl] != comprFunc) {
            if (DeflaterConstants.DEBUGGING)
                System.err.println("Change from " + comprFunc + " to "
                        + DeflaterConstants.COMPR_FUNC[lvl]);
            switch (comprFunc) {
                case DEFLATE_STORED:
                    if (strstart > blockStart) {
                        huffman.flushStoredBlock(window, blockStart,
                                strstart - blockStart, false);
                        blockStart = strstart;
                    }
                    updateHash();
                    break;
                case DEFLATE_FAST:
                    if (strstart > blockStart) {
                        huffman.flushBlock(window, blockStart, strstart - blockStart,
                                false);
                        blockStart = strstart;
                    }
                    break;
                case DEFLATE_SLOW:
                    if (prevAvailable)
                        huffman.tallyLit(window[strstart - 1] & 0xff);
                    if (strstart > blockStart) {
                        huffman.flushBlock(window, blockStart, strstart - blockStart,
                                false);
                        blockStart = strstart;
                    }
                    prevAvailable = false;
                    matchLen = MIN_MATCH - 1;
                    break;
            }
            comprFunc = COMPR_FUNC[lvl];
        }
    }

    private final void updateHash() {
        if (DEBUGGING)
            System.err.println("updateHash: " + strstart);
        ins_h = (window[strstart] << HASH_SHIFT) ^ window[strstart + 1];
    }

    /**
     * Inserts the current string in the head hash and returns the previous
     * value for this hash.
     */
    private final int insertString() {
        short match;
        int hash = ((ins_h << HASH_SHIFT) ^ window[strstart + (MIN_MATCH - 1)])
                & HASH_MASK;

        if (DEBUGGING) {
            if (hash != (((window[strstart] << (2 * HASH_SHIFT))
                    ^ (window[strstart + 1] << HASH_SHIFT)
                    ^ (window[strstart + 2])) & HASH_MASK))
                throw new InternalError("hash inconsistent: " + hash + "/"
                        + window[strstart] + ","
                        + window[strstart + 1] + ","
                        + window[strstart + 2] + "," + HASH_SHIFT);
        }

        prev[strstart & WMASK] = match = head[hash];
        head[hash] = (short) strstart;
        ins_h = hash;
        return match & 0xffff;
    }

    private void slideWindow() {
        System.arraycopy(window, WSIZE, window, 0, WSIZE);
        matchStart -= WSIZE;
        strstart -= WSIZE;
        blockStart -= WSIZE;

        /* Slide the hash table (could be avoided with 32 bit values
         * at the expense of memory usage).
         */
        for (int i = 0; i < HASH_SIZE; i++) {
            int m = head[i] & 0xffff;
            head[i] = m >= WSIZE ? (short) (m - WSIZE) : 0;
        }

        /* Slide the prev table.
         */
        for (int i = 0; i < WSIZE; i++) {
            int m = prev[i] & 0xffff;
            prev[i] = m >= WSIZE ? (short) (m - WSIZE) : 0;
        }
    }

    /**
     * Fill the window when the lookahead becomes insufficient.
     * Updates strstart and lookahead.
     * <p>
     * OUT assertions: strstart + lookahead <= 2*WSIZE
     * lookahead >= MIN_LOOKAHEAD or inputOff == inputEnd
     */
    private void fillWindow() {
        /* If the window is almost full and there is insufficient lookahead,
         * move the upper half to the lower one to make room in the upper half.
         */
        if (strstart >= WSIZE + MAX_DIST)
            slideWindow();

        /* If there is not enough lookahead, but still some input left,
         * read in the input
         */
        while (lookahead < DeflaterConstants.MIN_LOOKAHEAD && inputOff < inputEnd) {
            int more = 2 * WSIZE - lookahead - strstart;

            if (more > inputEnd - inputOff)
                more = inputEnd - inputOff;

            System.arraycopy(inputBuf, inputOff,
                    window, strstart + lookahead, more);
            adler.update(inputBuf, inputOff, more);
            inputOff += more;
            totalIn += more;
            lookahead += more;
        }

        if (lookahead >= MIN_MATCH)
            updateHash();
    }

    /**
     * Find the best (longest) string in the window matching the
     * string starting at strstart.
     * <p>
     * Preconditions:
     * strstart + MAX_MATCH <= window.length.
     *
     * @param curMatch
     */
    private boolean findLongestMatch(int curMatch) {
        int chainLength = this.max_chain;
        int niceLength = this.niceLength;
        short[] prev = this.prev;
        int scan = this.strstart;
        int match;
        int best_end = this.strstart + matchLen;
        int best_len = Math.max(matchLen, MIN_MATCH - 1);

        int limit = Math.max(strstart - MAX_DIST, 0);

        int strend = scan + MAX_MATCH - 1;
        byte scan_end1 = window[best_end - 1];
        byte scan_end = window[best_end];

        /* Do not waste too much time if we already have a good match: */
        if (best_len >= this.goodLength)
            chainLength >>= 2;

        /* Do not look for matches beyond the end of the input. This is necessary
         * to make deflate deterministic.
         */
        if (niceLength > lookahead)
            niceLength = lookahead;

        if (DeflaterConstants.DEBUGGING
                && strstart > 2 * WSIZE - MIN_LOOKAHEAD)
            throw new InternalError("need lookahead");

        do {
            if (DeflaterConstants.DEBUGGING && curMatch >= strstart)
                throw new InternalError("future match");
            if (window[curMatch + best_len] != scan_end
                    || window[curMatch + best_len - 1] != scan_end1
                    || window[curMatch] != window[scan]
                    || window[curMatch + 1] != window[scan + 1])
                continue;

            match = curMatch + 2;
            scan += 2;

            /* We check for insufficient lookahead only every 8th comparison;
             * the 256th check will be made at strstart+258.
             */
            while (window[++scan] == window[++match]
                    && window[++scan] == window[++match]
                    && window[++scan] == window[++match]
                    && window[++scan] == window[++match]
                    && window[++scan] == window[++match]
                    && window[++scan] == window[++match]
                    && window[++scan] == window[++match]
                    && window[++scan] == window[++match]
                    && scan < strend) ;

            if (scan > best_end) {
//  	if (DeflaterConstants.DEBUGGING && ins_h == 0)
//  	  System.err.println("Found match: "+curMatch+"-"+(scan-strstart));
                matchStart = curMatch;
                best_end = scan;
                best_len = scan - strstart;
                if (best_len >= niceLength)
                    break;

                scan_end1 = window[best_end - 1];
                scan_end = window[best_end];
            }
            scan = strstart;
        } while ((curMatch = (prev[curMatch & WMASK] & 0xffff)) > limit
                && --chainLength != 0);

        matchLen = Math.min(best_len, lookahead);
        return matchLen >= MIN_MATCH;
    }

    void setDictionary(byte[] buffer, int offset, int length) {
        if (DeflaterConstants.DEBUGGING && strstart != 1)
            throw new IllegalStateException("strstart not 1");
        adler.update(buffer, offset, length);
        if (length < MIN_MATCH)
            return;
        if (length > MAX_DIST) {
            offset += length - MAX_DIST;
            length = MAX_DIST;
        }

        System.arraycopy(buffer, offset, window, strstart, length);

        updateHash();
        length--;
        while (--length > 0) {
            insertString();
            strstart++;
        }
        strstart += 2;
        blockStart = strstart;
    }

    private boolean deflateStored(boolean flush, boolean finish) {
        if (!flush && lookahead == 0)
            return false;

        strstart += lookahead;
        lookahead = 0;

        int storedLen = strstart - blockStart;

        if ((storedLen >= DeflaterConstants.MAX_BLOCK_SIZE)
                /* Block is full */
                || (blockStart < WSIZE && storedLen >= MAX_DIST)
                /* Block may move out of window */
                || flush) {
            boolean lastBlock = finish;
            if (storedLen > DeflaterConstants.MAX_BLOCK_SIZE) {
                storedLen = DeflaterConstants.MAX_BLOCK_SIZE;
                lastBlock = false;
            }

            if (DeflaterConstants.DEBUGGING)
                System.err.println("storedBlock[" + storedLen + "," + lastBlock + "]");

            huffman.flushStoredBlock(window, blockStart, storedLen, lastBlock);
            blockStart += storedLen;
            return !lastBlock;
        }
        return true;
    }

    private boolean deflateFast(boolean flush, boolean finish) {
        if (lookahead < MIN_LOOKAHEAD && !flush)
            return false;

        while (lookahead >= MIN_LOOKAHEAD || flush) {
            if (lookahead == 0) {
                /* We are flushing everything */
                huffman.flushBlock(window, blockStart, strstart - blockStart,
                        finish);
                blockStart = strstart;
                return false;
            }

            if (strstart > 2 * WSIZE - MIN_LOOKAHEAD) {
                /* slide window, as findLongestMatch need this.
                 * This should only happen when flushing and the window
                 * is almost full.
                 */
                slideWindow();
            }

            int hashHead;
            if (lookahead >= MIN_MATCH
                    && (hashHead = insertString()) != 0
                    && strategy != Deflater.HUFFMAN_ONLY
                    && strstart - hashHead <= MAX_DIST
                    && findLongestMatch(hashHead)) {
                /* longestMatch sets matchStart and matchLen */
                if (DeflaterConstants.DEBUGGING) {
                    for (int i = 0; i < matchLen; i++) {
                        if (window[strstart + i] != window[matchStart + i])
                            throw new InternalError();
                    }
                }
                huffman.tallyDist(strstart - matchStart, matchLen);

                lookahead -= matchLen;
                if (matchLen <= max_lazy && lookahead >= MIN_MATCH) {
                    while (--matchLen > 0) {
                        strstart++;
                        insertString();
                    }
                    strstart++;
                } else {
                    strstart += matchLen;
                    if (lookahead >= MIN_MATCH - 1)
                        updateHash();
                }
                matchLen = MIN_MATCH - 1;
                continue;
            } else {
                /* No match found */
                huffman.tallyLit(window[strstart] & 0xff);
                strstart++;
                lookahead--;
            }

            if (huffman.isFull()) {
                boolean lastBlock = finish && lookahead == 0;
                huffman.flushBlock(window, blockStart, strstart - blockStart,
                        lastBlock);
                blockStart = strstart;
                return !lastBlock;
            }
        }
        return true;
    }

    private boolean deflateSlow(boolean flush, boolean finish) {
        if (lookahead < MIN_LOOKAHEAD && !flush)
            return false;

        while (lookahead >= MIN_LOOKAHEAD || flush) {
            if (lookahead == 0) {
                if (prevAvailable)
                    huffman.tallyLit(window[strstart - 1] & 0xff);
                prevAvailable = false;

                /* We are flushing everything */
                if (DeflaterConstants.DEBUGGING && !flush)
                    throw new InternalError("Not flushing, but no lookahead");
                huffman.flushBlock(window, blockStart, strstart - blockStart,
                        finish);
                blockStart = strstart;
                return false;
            }

            if (strstart >= 2 * WSIZE - MIN_LOOKAHEAD) {
                /* slide window, as findLongestMatch need this.
                 * This should only happen when flushing and the window
                 * is almost full.
                 */
                slideWindow();
            }

            int prevMatch = matchStart;
            int prevLen = matchLen;
            if (lookahead >= MIN_MATCH) {
                int hashHead = insertString();
                if (strategy != Deflater.HUFFMAN_ONLY
                        && hashHead != 0 && strstart - hashHead <= MAX_DIST
                        && findLongestMatch(hashHead)) {
                    /* longestMatch sets matchStart and matchLen */

                    /* Discard match if too small and too far away */
                    if (matchLen <= 5
                            && (strategy == Deflater.FILTERED
                            || (matchLen == MIN_MATCH
                            && strstart - matchStart > TOO_FAR))) {
                        matchLen = MIN_MATCH - 1;
                    }
                }
            }

            /* previous match was better */
            if (prevLen >= MIN_MATCH && matchLen <= prevLen) {
                if (DeflaterConstants.DEBUGGING) {
                    for (int i = 0; i < matchLen; i++) {
                        if (window[strstart - 1 + i] != window[prevMatch + i])
                            throw new InternalError();
                    }
                }
                huffman.tallyDist(strstart - 1 - prevMatch, prevLen);
                prevLen -= 2;
                do {
                    strstart++;
                    lookahead--;
                    if (lookahead >= MIN_MATCH)
                        insertString();
                }
                while (--prevLen > 0);
                strstart++;
                lookahead--;
                prevAvailable = false;
                matchLen = MIN_MATCH - 1;
            } else {
                if (prevAvailable)
                    huffman.tallyLit(window[strstart - 1] & 0xff);
                prevAvailable = true;
                strstart++;
                lookahead--;
            }

            if (huffman.isFull()) {
                int len = strstart - blockStart;
                if (prevAvailable)
                    len--;
                boolean lastBlock = (finish && lookahead == 0 && !prevAvailable);
                huffman.flushBlock(window, blockStart, len, lastBlock);
                blockStart += len;
                return !lastBlock;
            }
        }
        return true;
    }

    public boolean deflate(boolean flush, boolean finish) {
        boolean progress;
        do {
            fillWindow();
            boolean canFlush = flush && inputOff == inputEnd;
            if (DeflaterConstants.DEBUGGING)
                System.err.println("window: [" + blockStart + "," + strstart + ","
                        + lookahead + "], " + comprFunc + "," + canFlush);
            switch (comprFunc) {
                case DEFLATE_STORED:
                    progress = deflateStored(canFlush, finish);
                    break;
                case DEFLATE_FAST:
                    progress = deflateFast(canFlush, finish);
                    break;
                case DEFLATE_SLOW:
                    progress = deflateSlow(canFlush, finish);
                    break;
                default:
                    throw new InternalError();
            }
        }
        while (pending.isFlushed()  /* repeat while we have no pending output */
                && progress);        /* and progress was made */

        return progress;
    }

    public void setInput(byte[] buf, int off, int len) {
        if (inputOff < inputEnd)
            throw new IllegalStateException
                    ("Old input was not completely processed");

        int end = off + len;

        /* We want to throw an ArrayIndexOutOfBoundsException early.  The
         * check is very tricky: it also handles integer wrap around.
         */
        if (0 > off || off > end || end > buf.length)
            throw new ArrayIndexOutOfBoundsException();

        inputBuf = buf;
        inputOff = off;
        inputEnd = end;
    }

    public final boolean needsInput() {
        return inputEnd == inputOff;
    }
}
