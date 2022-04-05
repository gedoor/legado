/* Deflater.java - Compress a data stream
   Copyright (C) 1999, 2000, 2001, 2004 Free Software Foundation, Inc.

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
 * This is the Deflater class.  The deflater class compresses input
 * with the deflate algorithm described in RFC 1951.  It has several
 * compression levels and three different strategies described below.
 * <p>
 * This class is <i>not</i> thread safe.  This is inherent in the API, due
 * to the split of deflate and setInput.
 *
 * @author Jochen Hoenicke
 * @author Tom Tromey
 */
public class Deflater {
    /**
     * The best and slowest compression level.  This tries to find very
     * long and distant string repetitions.
     */
    public static final int BEST_COMPRESSION = 9;
    /**
     * The worst but fastest compression level.
     */
    public static final int BEST_SPEED = 1;
    /**
     * The default compression level.
     */
    public static final int DEFAULT_COMPRESSION = -1;
    /**
     * This level won't compress at all but output uncompressed blocks.
     */
    public static final int NO_COMPRESSION = 0;

    /**
     * The default strategy.
     */
    public static final int DEFAULT_STRATEGY = 0;
    /**
     * This strategy will only allow longer string repetitions.  It is
     * useful for random data with a small character set.
     */
    public static final int FILTERED = 1;

    /**
     * This strategy will not look for string repetitions at all.  It
     * only encodes with Huffman trees (which means, that more common
     * characters get a smaller encoding.
     */
    public static final int HUFFMAN_ONLY = 2;

    /**
     * The compression method.  This is the only method supported so far.
     * There is no need to use this constant at all.
     */
    public static final int DEFLATED = 8;

    /*
     * The Deflater can do the following state transitions:
     *
     * (1) -> INIT_STATE   ----> INIT_FINISHING_STATE ---.
     *        /  | (2)      (5)                         |
     *       /   v          (5)                         |
     *   (3)| SETDICT_STATE ---> SETDICT_FINISHING_STATE |(3)
     *       \   | (3)                 |        ,-------'
     *        |  |                     | (3)   /
     *        v  v          (5)        v      v
     * (1) -> BUSY_STATE   ----> FINISHING_STATE
     *                                | (6)
     *                                v
     *                           FINISHED_STATE
     *    \_____________________________________/
     *          | (7)
     *          v
     *        CLOSED_STATE
     *
     * (1) If we should produce a header we start in INIT_STATE, otherwise
     *     we start in BUSY_STATE.
     * (2) A dictionary may be set only when we are in INIT_STATE, then
     *     we change the state as indicated.
     * (3) Whether a dictionary is set or not, on the first call of deflate
     *     we change to BUSY_STATE.
     * (4) -- intentionally left blank -- :)
     * (5) FINISHING_STATE is entered, when flush() is called to indicate that
     *     there is no more INPUT.  There are also states indicating, that
     *     the header wasn't written yet.
     * (6) FINISHED_STATE is entered, when everything has been flushed to the
     *     internal pending output buffer.
     * (7) At any time (7)
     *
     */

    private static final int IS_SETDICT = 0x01;
    private static final int IS_FLUSHING = 0x04;
    private static final int IS_FINISHING = 0x08;

    private static final int INIT_STATE = 0x00;
    private static final int SETDICT_STATE = 0x01;
    private static final int INIT_FINISHING_STATE = 0x08;
    private static final int SETDICT_FINISHING_STATE = 0x09;
    private static final int BUSY_STATE = 0x10;
    private static final int FLUSHING_STATE = 0x14;
    private static final int FINISHING_STATE = 0x1c;
    private static final int FINISHED_STATE = 0x1e;
    private static final int CLOSED_STATE = 0x7f;

    /**
     * Compression level.
     */
    private int level;

    /**
     * should we include a header.
     */
    private boolean noHeader;

    /**
     * The current state.
     */
    private int state;

    /**
     * The total bytes of output written.
     */
    private int totalOut;

    /**
     * The pending output.
     */
    private DeflaterPending pending;

    /**
     * The deflater engine.
     */
    private DeflaterEngine engine;

    /**
     * Creates a new deflater with default compression level.
     */
    public Deflater() {
        this(DEFAULT_COMPRESSION, false);
    }

    /**
     * Creates a new deflater with given compression level.
     *
     * @param lvl the compression level, a value between NO_COMPRESSION
     *            and BEST_COMPRESSION, or DEFAULT_COMPRESSION.
     * @throws IllegalArgumentException if lvl is out of range.
     */
    public Deflater(int lvl) {
        this(lvl, false);
    }

    /**
     * Creates a new deflater with given compression level.
     *
     * @param lvl    the compression level, a value between NO_COMPRESSION
     *               and BEST_COMPRESSION.
     * @param nowrap true, iff we should suppress the deflate header at the
     *               beginning and the adler checksum at the end of the output.  This is
     *               useful for the GZIP format.
     * @throws IllegalArgumentException if lvl is out of range.
     */
    public Deflater(int lvl, boolean nowrap) {
        if (lvl == DEFAULT_COMPRESSION)
            lvl = 6;
        else if (lvl < NO_COMPRESSION || lvl > BEST_COMPRESSION)
            throw new IllegalArgumentException();

        pending = new DeflaterPending();
        engine = new DeflaterEngine(pending);
        this.noHeader = nowrap;
        setStrategy(DEFAULT_STRATEGY);
        setLevel(lvl);
        reset();
    }

    /**
     * Resets the deflater.  The deflater acts afterwards as if it was
     * just created with the same compression level and strategy as it
     * had before.
     */
    public void reset() {
        state = (noHeader ? BUSY_STATE : INIT_STATE);
        totalOut = 0;
        pending.reset();
        engine.reset();
    }

    /**
     * Frees all objects allocated by the compressor.  There's no
     * reason to call this, since you can just rely on garbage
     * collection.  Exists only for compatibility against Sun's JDK,
     * where the compressor allocates native memory.
     * If you call any method (even reset) afterwards the behaviour is
     * <i>undefined</i>.
     *
     * @deprecated Just clear all references to deflater instead.
     */
    public void end() {
        engine = null;
        pending = null;
        state = CLOSED_STATE;
    }

    /**
     * Gets the current adler checksum of the data that was processed so
     * far.
     */
    public int getAdler() {
        return engine.getAdler();
    }

    /**
     * Gets the number of input bytes processed so far.
     */
    public int getTotalIn() {
        return engine.getTotalIn();
    }

    /**
     * Gets the number of output bytes so far.
     */
    public int getTotalOut() {
        return totalOut;
    }

    /**
     * Finalizes this object.
     */
    protected void finalize() {
        /* Exists solely for compatibility.  We don't have any native state. */
    }

    /**
     * Flushes the current input block.  Further calls to deflate() will
     * produce enough output to inflate everything in the current input
     * block.  This is not part of Sun's JDK so I have made it package
     * private.  It is used by DeflaterOutputStream to implement
     * flush().
     */
    void flush() {
        state |= IS_FLUSHING;
    }

    /**
     * Finishes the deflater with the current input block.  It is an error
     * to give more input after this method was called.  This method must
     * be called to force all bytes to be flushed.
     */
    public void finish() {
        state |= IS_FLUSHING | IS_FINISHING;
    }

    /**
     * Returns true iff the stream was finished and no more output bytes
     * are available.
     */
    public boolean finished() {
        return state == FINISHED_STATE && pending.isFlushed();
    }

    /**
     * Returns true, if the input buffer is empty.
     * You should then call setInput(). <br>
     *
     * <em>NOTE</em>: This method can also return true when the stream
     * was finished.
     */
    public boolean needsInput() {
        return engine.needsInput();
    }

    /**
     * Sets the data which should be compressed next.  This should be only
     * called when needsInput indicates that more input is needed.
     * If you call setInput when needsInput() returns false, the
     * previous input that is still pending will be thrown away.
     * The given byte array should not be changed, before needsInput() returns
     * true again.
     * This call is equivalent to <code>setInput(input, 0, input.length)</code>.
     *
     * @param input the buffer containing the input data.
     * @throws IllegalStateException if the buffer was finished() or ended().
     */
    public void setInput(byte[] input) {
        setInput(input, 0, input.length);
    }

    /**
     * Sets the data which should be compressed next.  This should be
     * only called when needsInput indicates that more input is needed.
     * The given byte array should not be changed, before needsInput() returns
     * true again.
     *
     * @param input the buffer containing the input data.
     * @param off   the start of the data.
     * @param len   the length of the data.
     * @throws IllegalStateException if the buffer was finished() or ended()
     *                               or if previous input is still pending.
     */
    public void setInput(byte[] input, int off, int len) {
        if ((state & IS_FINISHING) != 0)
            throw new IllegalStateException("finish()/end() already called");
        engine.setInput(input, off, len);
    }

    /**
     * Sets the compression level.  There is no guarantee of the exact
     * position of the change, but if you call this when needsInput is
     * true the change of compression level will occur somewhere near
     * before the end of the so far given input.
     *
     * @param lvl the new compression level.
     */
    public void setLevel(int lvl) {
        if (lvl == DEFAULT_COMPRESSION)
            lvl = 6;
        else if (lvl < NO_COMPRESSION || lvl > BEST_COMPRESSION)
            throw new IllegalArgumentException();


        if (level != lvl) {
            level = lvl;
            engine.setLevel(lvl);
        }
    }

    /**
     * Sets the compression strategy. Strategy is one of
     * DEFAULT_STRATEGY, HUFFMAN_ONLY and FILTERED.  For the exact
     * position where the strategy is changed, the same as for
     * setLevel() applies.
     *
     * @param stgy the new compression strategy.
     */
    public void setStrategy(int stgy) {
        if (stgy != DEFAULT_STRATEGY && stgy != FILTERED
                && stgy != HUFFMAN_ONLY)
            throw new IllegalArgumentException();
        engine.setStrategy(stgy);
    }

    /**
     * Deflates the current input block to the given array.  It returns
     * the number of bytes compressed, or 0 if either
     * needsInput() or finished() returns true or length is zero.
     *
     * @param output the buffer where to write the compressed data.
     */
    public int deflate(byte[] output) {
        return deflate(output, 0, output.length);
    }

    /**
     * Deflates the current input block to the given array.  It returns
     * the number of bytes compressed, or 0 if either
     * needsInput() or finished() returns true or length is zero.
     *
     * @param output the buffer where to write the compressed data.
     * @param offset the offset into the output array.
     * @param length the maximum number of bytes that may be written.
     * @throws IllegalStateException     if end() was called.
     * @throws IndexOutOfBoundsException if offset and/or length
     *                                   don't match the array length.
     */
    public int deflate(byte[] output, int offset, int length) {
        int origLength = length;

        if (state == CLOSED_STATE)
            throw new IllegalStateException("Deflater closed");

        if (state < BUSY_STATE) {
            /* output header */
            int header = (DEFLATED +
                    ((DeflaterConstants.MAX_WBITS - 8) << 4)) << 8;
            int level_flags = (level - 1) >> 1;
            if (level_flags < 0 || level_flags > 3)
                level_flags = 3;
            header |= level_flags << 6;
            if ((state & IS_SETDICT) != 0)
                /* Dictionary was set */
                header |= DeflaterConstants.PRESET_DICT;
            header += 31 - (header % 31);

            pending.writeShortMSB(header);
            if ((state & IS_SETDICT) != 0) {
                int chksum = engine.getAdler();
                engine.resetAdler();
                pending.writeShortMSB(chksum >> 16);
                pending.writeShortMSB(chksum & 0xffff);
            }

            state = BUSY_STATE | (state & (IS_FLUSHING | IS_FINISHING));
        }

        for (; ; ) {
            int count = pending.flush(output, offset, length);
            offset += count;
            totalOut += count;
            length -= count;
            if (length == 0 || state == FINISHED_STATE)
                break;

            if (!engine.deflate((state & IS_FLUSHING) != 0,
                    (state & IS_FINISHING) != 0)) {
                if (state == BUSY_STATE)
                    /* We need more input now */
                    return origLength - length;
                else if (state == FLUSHING_STATE) {
                    if (level != NO_COMPRESSION) {
                        /* We have to supply some lookahead.  8 bit lookahead
                         * are needed by the zlib inflater, and we must fill
                         * the next byte, so that all bits are flushed.
                         */
                        int neededbits = 8 + ((-pending.getBitCount()) & 7);
                        while (neededbits > 0) {
                            /* write a static tree block consisting solely of
                             * an EOF:
                             */
                            pending.writeBits(2, 10);
                            neededbits -= 10;
                        }
                    }
                    state = BUSY_STATE;
                } else if (state == FINISHING_STATE) {
                    pending.alignToByte();
                    /* We have completed the stream */
                    if (!noHeader) {
                        int adler = engine.getAdler();
                        pending.writeShortMSB(adler >> 16);
                        pending.writeShortMSB(adler & 0xffff);
                    }
                    state = FINISHED_STATE;
                }
            }
        }

        return origLength - length;
    }

    /**
     * Sets the dictionary which should be used in the deflate process.
     * This call is equivalent to <code>setDictionary(dict, 0,
     * dict.length)</code>.
     *
     * @param dict the dictionary.
     * @throws IllegalStateException if setInput () or deflate ()
     *                               were already called or another dictionary was already set.
     */
    public void setDictionary(byte[] dict) {
        setDictionary(dict, 0, dict.length);
    }

    /**
     * Sets the dictionary which should be used in the deflate process.
     * The dictionary should be a byte array containing strings that are
     * likely to occur in the data which should be compressed.  The
     * dictionary is not stored in the compressed output, only a
     * checksum.  To decompress the output you need to supply the same
     * dictionary again.
     *
     * @param dict   the dictionary.
     * @param offset an offset into the dictionary.
     * @param length the length of the dictionary.
     * @throws IllegalStateException if setInput () or deflate () were
     *                               already called or another dictionary was already set.
     */
    public void setDictionary(byte[] dict, int offset, int length) {
        if (state != INIT_STATE)
            throw new IllegalStateException();

        state = SETDICT_STATE;
        engine.setDictionary(dict, offset, length);
    }
}
