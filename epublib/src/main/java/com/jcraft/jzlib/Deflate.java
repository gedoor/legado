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

public 
final class Deflate implements Cloneable {

  static final private int MAX_MEM_LEVEL=9;

  static final private int Z_DEFAULT_COMPRESSION=-1;

  static final private int MAX_WBITS=15;            // 32K LZ77 window
  static final private int DEF_MEM_LEVEL=8;

  static class Config{
    int good_length; // reduce lazy search above this match length
    int max_lazy;    // do not perform lazy search above this match length
    int nice_length; // quit search above this match length
    int max_chain;
    int func;
    Config(int good_length, int max_lazy, 
	   int nice_length, int max_chain, int func){
      this.good_length=good_length;
      this.max_lazy=max_lazy;
      this.nice_length=nice_length;
      this.max_chain=max_chain;
      this.func=func;
    }
  }
  
  static final private int STORED=0;
  static final private int FAST=1;
  static final private int SLOW=2;
  static final private Config[] config_table;    
  static{
    config_table=new Config[10];
    //                         good  lazy  nice  chain
    config_table[0]=new Config(0,    0,    0,    0, STORED);
    config_table[1]=new Config(4,    4,    8,    4, FAST);
    config_table[2]=new Config(4,    5,   16,    8, FAST);
    config_table[3]=new Config(4,    6,   32,   32, FAST);

    config_table[4]=new Config(4,    4,   16,   16, SLOW);
    config_table[5]=new Config(8,   16,   32,   32, SLOW);
    config_table[6]=new Config(8,   16,  128,  128, SLOW);
    config_table[7]=new Config(8,   32,  128,  256, SLOW);
    config_table[8]=new Config(32, 128,  258, 1024, SLOW);
    config_table[9]=new Config(32, 258,  258, 4096, SLOW);
  }

  static final private String[] z_errmsg = {
    "need dictionary",     // Z_NEED_DICT       2
    "stream end",          // Z_STREAM_END      1
    "",                    // Z_OK              0
    "file error",          // Z_ERRNO         (-1)
    "stream error",        // Z_STREAM_ERROR  (-2)
    "data error",          // Z_DATA_ERROR    (-3)
    "insufficient memory", // Z_MEM_ERROR     (-4)
    "buffer error",        // Z_BUF_ERROR     (-5)
    "incompatible version",// Z_VERSION_ERROR (-6)
    ""
  };

  // block not completed, need more input or more output
  static final private int NeedMore=0; 

  // block flush performed
  static final private int BlockDone=1; 

  // finish started, need only more output at next deflate
  static final private int FinishStarted=2;

  // finish done, accept no more input or output
  static final private int FinishDone=3;

  // preset dictionary flag in zlib header
  static final private int PRESET_DICT=0x20;

  static final private int Z_FILTERED=1;
  static final private int Z_HUFFMAN_ONLY=2;
  static final private int Z_DEFAULT_STRATEGY=0;

  static final private int Z_NO_FLUSH=0;
  static final private int Z_PARTIAL_FLUSH=1;
  static final private int Z_SYNC_FLUSH=2;
  static final private int Z_FULL_FLUSH=3;
  static final private int Z_FINISH=4;

  static final private int Z_OK=0;
  static final private int Z_STREAM_END=1;
  static final private int Z_NEED_DICT=2;
  static final private int Z_ERRNO=-1;
  static final private int Z_STREAM_ERROR=-2;
  static final private int Z_DATA_ERROR=-3;
  static final private int Z_MEM_ERROR=-4;
  static final private int Z_BUF_ERROR=-5;
  static final private int Z_VERSION_ERROR=-6;

  static final private int INIT_STATE=42;
  static final private int BUSY_STATE=113;
  static final private int FINISH_STATE=666;

  // The deflate compression method
  static final private int Z_DEFLATED=8;

  static final private int STORED_BLOCK=0;
  static final private int STATIC_TREES=1;
  static final private int DYN_TREES=2;

  // The three kinds of block type
  static final private int Z_BINARY=0;
  static final private int Z_ASCII=1;
  static final private int Z_UNKNOWN=2;

  static final private int Buf_size=8*2;

  // repeat previous bit length 3-6 times (2 bits of repeat count)
  static final private int REP_3_6=16; 

  // repeat a zero length 3-10 times  (3 bits of repeat count)
  static final private int REPZ_3_10=17; 

  // repeat a zero length 11-138 times  (7 bits of repeat count)
  static final private int REPZ_11_138=18; 

  static final private int MIN_MATCH=3;
  static final private int MAX_MATCH=258;
  static final private int MIN_LOOKAHEAD=(MAX_MATCH+MIN_MATCH+1);

  static final private int MAX_BITS=15;
  static final private int D_CODES=30;
  static final private int BL_CODES=19;
  static final private int LENGTH_CODES=29;
  static final private int LITERALS=256;
  static final private int L_CODES=(LITERALS+1+LENGTH_CODES);
  static final private int HEAP_SIZE=(2*L_CODES+1);

  static final private int END_BLOCK=256;

  ZStream strm;        // pointer back to this zlib stream
  int status;           // as the name implies
  byte[] pending_buf;   // output still pending
  int pending_buf_size; // size of pending_buf
  int pending_out;      // next pending byte to output to the stream
  int pending;          // nb of bytes in the pending buffer
  int wrap = 1;
  byte data_type;       // UNKNOWN, BINARY or ASCII
  byte method;          // STORED (for zip only) or DEFLATED
  int last_flush;       // value of flush param for previous deflate call

  int w_size;           // LZ77 window size (32K by default)
  int w_bits;           // log2(w_size)  (8..16)
  int w_mask;           // w_size - 1

  byte[] window;
  // Sliding window. Input bytes are read into the second half of the window,
  // and move to the first half later to keep a dictionary of at least wSize
  // bytes. With this organization, matches are limited to a distance of
  // wSize-MAX_MATCH bytes, but this ensures that IO is always
  // performed with a length multiple of the block size. Also, it limits
  // the window size to 64K, which is quite useful on MSDOS.
  // To do: use the user input buffer as sliding window.

  int window_size;
  // Actual size of window: 2*wSize, except when the user input buffer
  // is directly used as sliding window.

  short[] prev;
  // Link to older string with same hash index. To limit the size of this
  // array to 64K, this link is maintained only for the last 32K strings.
  // An index in this array is thus a window index modulo 32K.

  short[] head; // Heads of the hash chains or NIL.

  int ins_h;          // hash index of string to be inserted
  int hash_size;      // number of elements in hash table
  int hash_bits;      // log2(hash_size)
  int hash_mask;      // hash_size-1

  // Number of bits by which ins_h must be shifted at each input
  // step. It must be such that after MIN_MATCH steps, the oldest
  // byte no longer takes part in the hash key, that is:
  // hash_shift * MIN_MATCH >= hash_bits
  int hash_shift;

  // Window position at the beginning of the current output block. Gets
  // negative when the window is moved backwards.

  int block_start;

  int match_length;           // length of best match
  int prev_match;             // previous match
  int match_available;        // set if previous match exists
  int strstart;               // start of string to insert
  int match_start;            // start of matching string
  int lookahead;              // number of valid bytes ahead in window

  // Length of the best match at previous step. Matches not greater than this
  // are discarded. This is used in the lazy match evaluation.
  int prev_length;

  // To speed up deflation, hash chains are never searched beyond this
  // length.  A higher limit improves compression ratio but degrades the speed.
  int max_chain_length;

  // Attempt to find a better match only when the current match is strictly
  // smaller than this value. This mechanism is used only for compression
  // levels >= 4.
  int max_lazy_match;

  // Insert new strings in the hash table only if the match length is not
  // greater than this length. This saves time but degrades compression.
  // max_insert_length is used only for compression levels <= 3.

  int level;    // compression level (1..9)
  int strategy; // favor or force Huffman coding

  // Use a faster search when the previous match is longer than this
  int good_match;

  // Stop searching when current match exceeds this
  int nice_match;

  short[] dyn_ltree;       // literal and length tree
  short[] dyn_dtree;       // distance tree
  short[] bl_tree;         // Huffman tree for bit lengths

  Tree l_desc=new Tree();  // desc for literal tree
  Tree d_desc=new Tree();  // desc for distance tree
  Tree bl_desc=new Tree(); // desc for bit length tree

  // number of codes at each bit length for an optimal tree
  short[] bl_count=new short[MAX_BITS+1];
  // working area to be used in Tree#gen_codes()
  short[] next_code=new short[MAX_BITS+1];

  // heap used to build the Huffman trees
  int[] heap=new int[2*L_CODES+1];

  int heap_len;               // number of elements in the heap
  int heap_max;               // element of largest frequency
  // The sons of heap[n] are heap[2*n] and heap[2*n+1]. heap[0] is not used.
  // The same heap array is used to build all trees.

  // Depth of each subtree used as tie breaker for trees of equal frequency
  byte[] depth=new byte[2*L_CODES+1];

  byte[] l_buf;               // index for literals or lengths */

  // Size of match buffer for literals/lengths.  There are 4 reasons for
  // limiting lit_bufsize to 64K:
  //   - frequencies can be kept in 16 bit counters
  //   - if compression is not successful for the first block, all input
  //     data is still in the window so we can still emit a stored block even
  //     when input comes from standard input.  (This can also be done for
  //     all blocks if lit_bufsize is not greater than 32K.)
  //   - if compression is not successful for a file smaller than 64K, we can
  //     even emit a stored file instead of a stored block (saving 5 bytes).
  //     This is applicable only for zip (not gzip or zlib).
  //   - creating new Huffman trees less frequently may not provide fast
  //     adaptation to changes in the input data statistics. (Take for
  //     example a binary file with poorly compressible code followed by
  //     a highly compressible string table.) Smaller buffer sizes give
  //     fast adaptation but have of course the overhead of transmitting
  //     trees more frequently.
  //   - I can't count above 4
  int lit_bufsize;

  int last_lit;      // running index in l_buf

  // Buffer for distances. To simplify the code, d_buf and l_buf have
  // the same number of elements. To use different lengths, an extra flag
  // array would be necessary.

  int d_buf;         // index of pendig_buf

  int opt_len;        // bit length of current block with optimal trees
  int static_len;     // bit length of current block with static trees
  int matches;        // number of string matches in current block
  int last_eob_len;   // bit length of EOB code for last block

  // Output buffer. bits are inserted starting at the bottom (least
  // significant bits).
  short bi_buf;

  // Number of valid bits in bi_buf.  All bits above the last valid bit
  // are always zero.
  int bi_valid;

  GZIPHeader gheader = null;

  Deflate(ZStream strm){
    this.strm=strm;
    dyn_ltree=new short[HEAP_SIZE*2];
    dyn_dtree=new short[(2*D_CODES+1)*2]; // distance tree
    bl_tree=new short[(2*BL_CODES+1)*2];  // Huffman tree for bit lengths
  }

  void lm_init() {
    window_size=2*w_size;

    head[hash_size-1]=0;
    for(int i=0; i<hash_size-1; i++){
      head[i]=0;
    }

    // Set the default configuration parameters:
    max_lazy_match   = Deflate.config_table[level].max_lazy;
    good_match       = Deflate.config_table[level].good_length;
    nice_match       = Deflate.config_table[level].nice_length;
    max_chain_length = Deflate.config_table[level].max_chain;

    strstart = 0;
    block_start = 0;
    lookahead = 0;
    match_length = prev_length = MIN_MATCH-1;
    match_available = 0;
    ins_h = 0;
  }

  // Initialize the tree data structures for a new zlib stream.
  void tr_init(){

    l_desc.dyn_tree = dyn_ltree;
    l_desc.stat_desc = StaticTree.static_l_desc;

    d_desc.dyn_tree = dyn_dtree;
    d_desc.stat_desc = StaticTree.static_d_desc;

    bl_desc.dyn_tree = bl_tree;
    bl_desc.stat_desc = StaticTree.static_bl_desc;

    bi_buf = 0;
    bi_valid = 0;
    last_eob_len = 8; // enough lookahead for inflate

    // Initialize the first block of the first file:
    init_block();
  }

  void init_block(){
    // Initialize the trees.
    for(int i = 0; i < L_CODES; i++) dyn_ltree[i*2] = 0;
    for(int i= 0; i < D_CODES; i++) dyn_dtree[i*2] = 0;
    for(int i= 0; i < BL_CODES; i++) bl_tree[i*2] = 0;

    dyn_ltree[END_BLOCK*2] = 1;
    opt_len = static_len = 0;
    last_lit = matches = 0;
  }

  // Restore the heap property by moving down the tree starting at node k,
  // exchanging a node with the smallest of its two sons if necessary, stopping
  // when the heap property is re-established (each father smaller than its
  // two sons).
  void pqdownheap(short[] tree,  // the tree to restore
		  int k          // node to move down
		  ){
    int v = heap[k];
    int j = k << 1;  // left son of k
    while (j <= heap_len) {
      // Set j to the smallest of the two sons:
      if (j < heap_len &&
	  smaller(tree, heap[j+1], heap[j], depth)){
	j++;
      }
      // Exit if v is smaller than both sons
      if(smaller(tree, v, heap[j], depth)) break;

      // Exchange v with the smallest son
      heap[k]=heap[j];  k = j;
      // And continue down the tree, setting j to the left son of k
      j <<= 1;
    }
    heap[k] = v;
  }

  static boolean smaller(short[] tree, int n, int m, byte[] depth){
    short tn2=tree[n*2];
    short tm2=tree[m*2];
    return (tn2<tm2 ||
	    (tn2==tm2 && depth[n] <= depth[m]));
  }

  // Scan a literal or distance tree to determine the frequencies of the codes
  // in the bit length tree.
  void scan_tree (short[] tree,// the tree to be scanned
		  int max_code // and its largest code of non zero frequency
		  ){
    int n;                     // iterates over all tree elements
    int prevlen = -1;          // last emitted length
    int curlen;                // length of current code
    int nextlen = tree[0*2+1]; // length of next code
    int count = 0;             // repeat count of the current code
    int max_count = 7;         // max repeat count
    int min_count = 4;         // min repeat count

    if (nextlen == 0){ max_count = 138; min_count = 3; }
    tree[(max_code+1)*2+1] = (short)0xffff; // guard

    for(n = 0; n <= max_code; n++) {
      curlen = nextlen; nextlen = tree[(n+1)*2+1];
      if(++count < max_count && curlen == nextlen) {
	continue;
      }
      else if(count < min_count) {
	bl_tree[curlen*2] += count;
      }
      else if(curlen != 0) {
	if(curlen != prevlen) bl_tree[curlen*2]++;
	bl_tree[REP_3_6*2]++;
      }
      else if(count <= 10) {
	bl_tree[REPZ_3_10*2]++;
      }
      else{
	bl_tree[REPZ_11_138*2]++;
      }
      count = 0; prevlen = curlen;
      if(nextlen == 0) {
	max_count = 138; min_count = 3;
      }
      else if(curlen == nextlen) {
	max_count = 6; min_count = 3;
      }
      else{
	max_count = 7; min_count = 4;
      }
    }
  }

  // Construct the Huffman tree for the bit lengths and return the index in
  // bl_order of the last bit length code to send.
  int build_bl_tree(){
    int max_blindex;  // index of last bit length code of non zero freq

    // Determine the bit length frequencies for literal and distance trees
    scan_tree(dyn_ltree, l_desc.max_code);
    scan_tree(dyn_dtree, d_desc.max_code);

    // Build the bit length tree:
    bl_desc.build_tree(this);
    // opt_len now includes the length of the tree representations, except
    // the lengths of the bit lengths codes and the 5+5+4 bits for the counts.

    // Determine the number of bit length codes to send. The pkzip format
    // requires that at least 4 bit length codes be sent. (appnote.txt says
    // 3 but the actual value used is 4.)
    for (max_blindex = BL_CODES-1; max_blindex >= 3; max_blindex--) {
      if (bl_tree[Tree.bl_order[max_blindex]*2+1] != 0) break;
    }
    // Update opt_len to include the bit length tree and counts
    opt_len += 3*(max_blindex+1) + 5+5+4;

    return max_blindex;
  }


  // Send the header for a block using dynamic Huffman trees: the counts, the
  // lengths of the bit length codes, the literal tree and the distance tree.
  // IN assertion: lcodes >= 257, dcodes >= 1, blcodes >= 4.
  void send_all_trees(int lcodes, int dcodes, int blcodes){
    int rank;                    // index in bl_order

    send_bits(lcodes-257, 5); // not +255 as stated in appnote.txt
    send_bits(dcodes-1,   5);
    send_bits(blcodes-4,  4); // not -3 as stated in appnote.txt
    for (rank = 0; rank < blcodes; rank++) {
      send_bits(bl_tree[Tree.bl_order[rank]*2+1], 3);
    }
    send_tree(dyn_ltree, lcodes-1); // literal tree
    send_tree(dyn_dtree, dcodes-1); // distance tree
  }

  // Send a literal or distance tree in compressed form, using the codes in
  // bl_tree.
  void send_tree (short[] tree,// the tree to be sent
		  int max_code // and its largest code of non zero frequency
		  ){
    int n;                     // iterates over all tree elements
    int prevlen = -1;          // last emitted length
    int curlen;                // length of current code
    int nextlen = tree[0*2+1]; // length of next code
    int count = 0;             // repeat count of the current code
    int max_count = 7;         // max repeat count
    int min_count = 4;         // min repeat count

    if (nextlen == 0){ max_count = 138; min_count = 3; }

    for (n = 0; n <= max_code; n++) {
      curlen = nextlen; nextlen = tree[(n+1)*2+1];
      if(++count < max_count && curlen == nextlen) {
	continue;
      }
      else if(count < min_count) {
	do { send_code(curlen, bl_tree); } while (--count != 0);
      }
      else if(curlen != 0){
	if(curlen != prevlen){
	  send_code(curlen, bl_tree); count--;
	}
	send_code(REP_3_6, bl_tree); 
	send_bits(count-3, 2);
      }
      else if(count <= 10){
	send_code(REPZ_3_10, bl_tree); 
	send_bits(count-3, 3);
      }
      else{
	send_code(REPZ_11_138, bl_tree);
	send_bits(count-11, 7);
      }
      count = 0; prevlen = curlen;
      if(nextlen == 0){
	max_count = 138; min_count = 3;
      }
      else if(curlen == nextlen){
	max_count = 6; min_count = 3;
      }
      else{
	max_count = 7; min_count = 4;
      }
    }
  }

  // Output a byte on the stream.
  // IN assertion: there is enough room in pending_buf.
  final void put_byte(byte[] p, int start, int len){
    System.arraycopy(p, start, pending_buf, pending, len);
    pending+=len;
  }

  final void put_byte(byte c){
    pending_buf[pending++]=c;
  }
  final void put_short(int w) {
    put_byte((byte)(w/*&0xff*/));
    put_byte((byte)(w>>>8));
  }
  final void putShortMSB(int b){
    put_byte((byte)(b>>8));
    put_byte((byte)(b/*&0xff*/));
  }   

  final void send_code(int c, short[] tree){
    int c2=c*2;
    send_bits((tree[c2]&0xffff), (tree[c2+1]&0xffff));
  }

  void send_bits(int value, int length){
    int len = length;
    if (bi_valid > (int)Buf_size - len) {
      int val = value;
//      bi_buf |= (val << bi_valid);
      bi_buf |= ((val << bi_valid)&0xffff);
      put_short(bi_buf);
      bi_buf = (short)(val >>> (Buf_size - bi_valid));
      bi_valid += len - Buf_size;
    } else {
//      bi_buf |= (value) << bi_valid;
      bi_buf |= (((value) << bi_valid)&0xffff);
      bi_valid += len;
    }
  }

  // Send one empty static block to give enough lookahead for inflate.
  // This takes 10 bits, of which 7 may remain in the bit buffer.
  // The current inflate code requires 9 bits of lookahead. If the
  // last two codes for the previous block (real code plus EOB) were coded
  // on 5 bits or less, inflate may have only 5+3 bits of lookahead to decode
  // the last real code. In this case we send two empty static blocks instead
  // of one. (There are no problems if the previous block is stored or fixed.)
  // To simplify the code, we assume the worst case of last real code encoded
  // on one bit only.
  void _tr_align(){
    send_bits(STATIC_TREES<<1, 3);
    send_code(END_BLOCK, StaticTree.static_ltree);

    bi_flush();

    // Of the 10 bits for the empty block, we have already sent
    // (10 - bi_valid) bits. The lookahead for the last real code (before
    // the EOB of the previous block) was thus at least one plus the length
    // of the EOB plus what we have just sent of the empty static block.
    if (1 + last_eob_len + 10 - bi_valid < 9) {
      send_bits(STATIC_TREES<<1, 3);
      send_code(END_BLOCK, StaticTree.static_ltree);
      bi_flush();
    }
    last_eob_len = 7;
  }


  // Save the match info and tally the frequency counts. Return true if
  // the current block must be flushed.
  boolean _tr_tally (int dist, // distance of matched string
		     int lc // match length-MIN_MATCH or unmatched char (if dist==0)
		     ){

    pending_buf[d_buf+last_lit*2] = (byte)(dist>>>8);
    pending_buf[d_buf+last_lit*2+1] = (byte)dist;

    l_buf[last_lit] = (byte)lc; last_lit++;

    if (dist == 0) {
      // lc is the unmatched char
      dyn_ltree[lc*2]++;
    } 
    else {
      matches++;
      // Here, lc is the match length - MIN_MATCH
      dist--;             // dist = match distance - 1
      dyn_ltree[(Tree._length_code[lc]+LITERALS+1)*2]++;
      dyn_dtree[Tree.d_code(dist)*2]++;
    }

    if ((last_lit & 0x1fff) == 0 && level > 2) {
      // Compute an upper bound for the compressed length
      int out_length = last_lit*8;
      int in_length = strstart - block_start;
      int dcode;
      for (dcode = 0; dcode < D_CODES; dcode++) {
	out_length += (int)dyn_dtree[dcode*2] *
	  (5L+Tree.extra_dbits[dcode]);
      }
      out_length >>>= 3;
      if ((matches < (last_lit/2)) && out_length < in_length/2) return true;
    }

    return (last_lit == lit_bufsize-1);
    // We avoid equality with lit_bufsize because of wraparound at 64K
    // on 16 bit machines and because stored blocks are restricted to
    // 64K-1 bytes.
  }

  // Send the block data compressed using the given Huffman trees
  void compress_block(short[] ltree, short[] dtree){
    int  dist;      // distance of matched string
    int lc;         // match length or unmatched char (if dist == 0)
    int lx = 0;     // running index in l_buf
    int code;       // the code to send
    int extra;      // number of extra bits to send

    if (last_lit != 0){
      do{
	dist=((pending_buf[d_buf+lx*2]<<8)&0xff00)|
	  (pending_buf[d_buf+lx*2+1]&0xff);
	lc=(l_buf[lx])&0xff; lx++;

	if(dist == 0){
	  send_code(lc, ltree); // send a literal byte
	} 
	else{
	  // Here, lc is the match length - MIN_MATCH
	  code = Tree._length_code[lc];

	  send_code(code+LITERALS+1, ltree); // send the length code
	  extra = Tree.extra_lbits[code];
	  if(extra != 0){
	    lc -= Tree.base_length[code];
	    send_bits(lc, extra);       // send the extra length bits
	  }
	  dist--; // dist is now the match distance - 1
	  code = Tree.d_code(dist);

	  send_code(code, dtree);       // send the distance code
	  extra = Tree.extra_dbits[code];
	  if (extra != 0) {
	    dist -= Tree.base_dist[code];
	    send_bits(dist, extra);   // send the extra distance bits
	  }
	} // literal or match pair ?

	// Check that the overlay between pending_buf and d_buf+l_buf is ok:
      }
      while (lx < last_lit);
    }

    send_code(END_BLOCK, ltree);
    last_eob_len = ltree[END_BLOCK*2+1];
  }

  // Set the data type to ASCII or BINARY, using a crude approximation:
  // binary if more than 20% of the bytes are <= 6 or >= 128, ascii otherwise.
  // IN assertion: the fields freq of dyn_ltree are set and the total of all
  // frequencies does not exceed 64K (to fit in an int on 16 bit machines).
  void set_data_type(){
    int n = 0;
    int  ascii_freq = 0;
    int  bin_freq = 0;
    while(n<7){ bin_freq += dyn_ltree[n*2]; n++;}
    while(n<128){ ascii_freq += dyn_ltree[n*2]; n++;}
    while(n<LITERALS){ bin_freq += dyn_ltree[n*2]; n++;}
    data_type=(byte)(bin_freq > (ascii_freq >>> 2) ? Z_BINARY : Z_ASCII);
  }

  // Flush the bit buffer, keeping at most 7 bits in it.
  void bi_flush(){
    if (bi_valid == 16) {
      put_short(bi_buf);
      bi_buf=0;
      bi_valid=0;
    }
    else if (bi_valid >= 8) {
      put_byte((byte)bi_buf);
      bi_buf>>>=8;
      bi_valid-=8;
    }
  }

  // Flush the bit buffer and align the output on a byte boundary
  void bi_windup(){
    if (bi_valid > 8) {
      put_short(bi_buf);
    } else if (bi_valid > 0) {
      put_byte((byte)bi_buf);
    }
    bi_buf = 0;
    bi_valid = 0;
  }

  // Copy a stored block, storing first the length and its
  // one's complement if requested.
  void copy_block(int buf,         // the input data
		  int len,         // its length
		  boolean header   // true if block header must be written
		  ){
    int index=0;
    bi_windup();      // align on byte boundary
    last_eob_len = 8; // enough lookahead for inflate

    if (header) {
      put_short((short)len);   
      put_short((short)~len);
    }

    //  while(len--!=0) {
    //    put_byte(window[buf+index]);
    //    index++;
    //  }
    put_byte(window, buf, len);
  }

  void flush_block_only(boolean eof){
    _tr_flush_block(block_start>=0 ? block_start : -1,
		    strstart-block_start,
		    eof);
    block_start=strstart;
    strm.flush_pending();
  }

  // Copy without compression as much as possible from the input stream, return
  // the current block state.
  // This function does not insert new strings in the dictionary since
  // uncompressible data is probably not useful. This function is used
  // only for the level=0 compression option.
  // NOTE: this function should be optimized to avoid extra copying from
  // window to pending_buf.
  int deflate_stored(int flush){
    // Stored blocks are limited to 0xffff bytes, pending_buf is limited
    // to pending_buf_size, and each stored block has a 5 byte header:

    int max_block_size = 0xffff;
    int max_start;

    if(max_block_size > pending_buf_size - 5) {
      max_block_size = pending_buf_size - 5;
    }

    // Copy as much as possible from input to output:
    while(true){
      // Fill the window as much as possible:
      if(lookahead<=1){
	fill_window();
	if(lookahead==0 && flush==Z_NO_FLUSH) return NeedMore;
	if(lookahead==0) break; // flush the current block
      }

      strstart+=lookahead;
      lookahead=0;

      // Emit a stored block if pending_buf will be full:
      max_start=block_start+max_block_size;
      if(strstart==0|| strstart>=max_start) {
	// strstart == 0 is possible when wraparound on 16-bit machine
	lookahead = (int)(strstart-max_start);
	strstart = (int)max_start;
      
	flush_block_only(false);
	if(strm.avail_out==0) return NeedMore;

      }

      // Flush if we may have to slide, otherwise block_start may become
      // negative and the data will be gone:
      if(strstart-block_start >= w_size-MIN_LOOKAHEAD) {
	flush_block_only(false);
	if(strm.avail_out==0) return NeedMore;
      }
    }

    flush_block_only(flush == Z_FINISH);
    if(strm.avail_out==0)
      return (flush == Z_FINISH) ? FinishStarted : NeedMore;

    return flush == Z_FINISH ? FinishDone : BlockDone;
  }

  // Send a stored block
  void _tr_stored_block(int buf,        // input block
			int stored_len, // length of input block
			boolean eof     // true if this is the last block for a file
			){
    send_bits((STORED_BLOCK<<1)+(eof?1:0), 3);  // send block type
    copy_block(buf, stored_len, true);          // with header
  }

  // Determine the best encoding for the current block: dynamic trees, static
  // trees or store, and output the encoded block to the zip file.
  void _tr_flush_block(int buf,        // input block, or NULL if too old
		       int stored_len, // length of input block
		       boolean eof     // true if this is the last block for a file
		       ) {
    int opt_lenb, static_lenb;// opt_len and static_len in bytes
    int max_blindex = 0;      // index of last bit length code of non zero freq

    // Build the Huffman trees unless a stored block is forced
    if(level > 0) {
      // Check if the file is ascii or binary
      if(data_type == Z_UNKNOWN) set_data_type();

      // Construct the literal and distance trees
      l_desc.build_tree(this);

      d_desc.build_tree(this);

      // At this point, opt_len and static_len are the total bit lengths of
      // the compressed block data, excluding the tree representations.

      // Build the bit length tree for the above two trees, and get the index
      // in bl_order of the last bit length code to send.
      max_blindex=build_bl_tree();

      // Determine the best encoding. Compute first the block length in bytes
      opt_lenb=(opt_len+3+7)>>>3;
      static_lenb=(static_len+3+7)>>>3;

      if(static_lenb<=opt_lenb) opt_lenb=static_lenb;
    }
    else {
      opt_lenb=static_lenb=stored_len+5; // force a stored block
    }

    if(stored_len+4<=opt_lenb && buf != -1){
      // 4: two words for the lengths
      // The test buf != NULL is only necessary if LIT_BUFSIZE > WSIZE.
      // Otherwise we can't have processed more than WSIZE input bytes since
      // the last block flush, because compression would have been
      // successful. If LIT_BUFSIZE <= WSIZE, it is never too late to
      // transform a block into a stored block.
      _tr_stored_block(buf, stored_len, eof);
    }
    else if(static_lenb == opt_lenb){
      send_bits((STATIC_TREES<<1)+(eof?1:0), 3);
      compress_block(StaticTree.static_ltree, StaticTree.static_dtree);
    }
    else{
      send_bits((DYN_TREES<<1)+(eof?1:0), 3);
      send_all_trees(l_desc.max_code+1, d_desc.max_code+1, max_blindex+1);
      compress_block(dyn_ltree, dyn_dtree);
    }

    // The above check is made mod 2^32, for files larger than 512 MB
    // and uLong implemented on 32 bits.

    init_block();

    if(eof){
      bi_windup();
    }
  }

  // Fill the window when the lookahead becomes insufficient.
  // Updates strstart and lookahead.
  //
  // IN assertion: lookahead < MIN_LOOKAHEAD
  // OUT assertions: strstart <= window_size-MIN_LOOKAHEAD
  //    At least one byte has been read, or avail_in == 0; reads are
  //    performed for at least two bytes (required for the zip translate_eol
  //    option -- not supported here).
  void fill_window(){
    int n, m;
    int p;
    int more;    // Amount of free space at the end of the window.

    do{
      more = (window_size-lookahead-strstart);

      // Deal with !@#$% 64K limit:
      if(more==0 && strstart==0 && lookahead==0){
	more = w_size;
      } 
      else if(more==-1) {
	// Very unlikely, but possible on 16 bit machine if strstart == 0
	// and lookahead == 1 (input done one byte at time)
	more--;

	// If the window is almost full and there is insufficient lookahead,
	// move the upper half to the lower one to make room in the upper half.
      }
      else if(strstart >= w_size+ w_size-MIN_LOOKAHEAD) {
	System.arraycopy(window, w_size, window, 0, w_size);
	match_start-=w_size;
	strstart-=w_size; // we now have strstart >= MAX_DIST
	block_start-=w_size;

	// Slide the hash table (could be avoided with 32 bit values
	// at the expense of memory usage). We slide even when level == 0
	// to keep the hash table consistent if we switch back to level > 0
	// later. (Using level 0 permanently is not an optimal usage of
	// zlib, so we don't care about this pathological case.)

	n = hash_size;
	p=n;
	do {
	  m = (head[--p]&0xffff);
	  head[p]=(m>=w_size ? (short)(m-w_size) : 0);
	}
	while (--n != 0);

	n = w_size;
	p = n;
	do {
	  m = (prev[--p]&0xffff);
	  prev[p] = (m >= w_size ? (short)(m-w_size) : 0);
	  // If n is not on any hash chain, prev[n] is garbage but
	  // its value will never be used.
	}
	while (--n!=0);
	more += w_size;
      }

      if (strm.avail_in == 0) return;

      // If there was no sliding:
      //    strstart <= WSIZE+MAX_DIST-1 && lookahead <= MIN_LOOKAHEAD - 1 &&
      //    more == window_size - lookahead - strstart
      // => more >= window_size - (MIN_LOOKAHEAD-1 + WSIZE + MAX_DIST-1)
      // => more >= window_size - 2*WSIZE + 2
      // In the BIG_MEM or MMAP case (not yet supported),
      //   window_size == input_size + MIN_LOOKAHEAD  &&
      //   strstart + s->lookahead <= input_size => more >= MIN_LOOKAHEAD.
      // Otherwise, window_size == 2*WSIZE so more >= 2.
      // If there was sliding, more >= WSIZE. So in all cases, more >= 2.

      n = strm.read_buf(window, strstart + lookahead, more);
      lookahead += n;

      // Initialize the hash value now that we have some input:
      if(lookahead >= MIN_MATCH) {
	ins_h = window[strstart]&0xff;
	ins_h=(((ins_h)<<hash_shift)^(window[strstart+1]&0xff))&hash_mask;
      }
      // If the whole input has less than MIN_MATCH bytes, ins_h is garbage,
      // but this is not important since only literal bytes will be emitted.
    }
    while (lookahead < MIN_LOOKAHEAD && strm.avail_in != 0);
  }

  // Compress as much as possible from the input stream, return the current
  // block state.
  // This function does not perform lazy evaluation of matches and inserts
  // new strings in the dictionary only for unmatched strings or for short
  // matches. It is used only for the fast compression options.
  int deflate_fast(int flush){
//    short hash_head = 0; // head of the hash chain
    int hash_head = 0; // head of the hash chain
    boolean bflush;      // set if current block must be flushed

    while(true){
      // Make sure that we always have enough lookahead, except
      // at the end of the input file. We need MAX_MATCH bytes
      // for the next match, plus MIN_MATCH bytes to insert the
      // string following the next match.
      if(lookahead < MIN_LOOKAHEAD){
	fill_window();
	if(lookahead < MIN_LOOKAHEAD && flush == Z_NO_FLUSH){
	  return NeedMore;
	}
	if(lookahead == 0) break; // flush the current block
      }

      // Insert the string window[strstart .. strstart+2] in the
      // dictionary, and set hash_head to the head of the hash chain:
      if(lookahead >= MIN_MATCH){
	ins_h=(((ins_h)<<hash_shift)^(window[(strstart)+(MIN_MATCH-1)]&0xff))&hash_mask;

//	prev[strstart&w_mask]=hash_head=head[ins_h];
        hash_head=(head[ins_h]&0xffff);
	prev[strstart&w_mask]=head[ins_h];
	head[ins_h]=(short)strstart;
      }

      // Find the longest match, discarding those <= prev_length.
      // At this point we have always match_length < MIN_MATCH

      if(hash_head!=0L && 
	 ((strstart-hash_head)&0xffff) <= w_size-MIN_LOOKAHEAD
	 ){
	// To simplify the code, we prevent matches with the string
	// of window index 0 (in particular we have to avoid a match
	// of the string with itself at the start of the input file).
	if(strategy != Z_HUFFMAN_ONLY){
	  match_length=longest_match (hash_head);
	}
	// longest_match() sets match_start
      }
      if(match_length>=MIN_MATCH){
	//        check_match(strstart, match_start, match_length);

	bflush=_tr_tally(strstart-match_start, match_length-MIN_MATCH);

	lookahead -= match_length;

	// Insert new strings in the hash table only if the match length
	// is not too large. This saves time but degrades compression.
	if(match_length <= max_lazy_match &&
	   lookahead >= MIN_MATCH) {
	  match_length--; // string at strstart already in hash table
	  do{
	    strstart++;

	    ins_h=((ins_h<<hash_shift)^(window[(strstart)+(MIN_MATCH-1)]&0xff))&hash_mask;
//	    prev[strstart&w_mask]=hash_head=head[ins_h];
	    hash_head=(head[ins_h]&0xffff);
	    prev[strstart&w_mask]=head[ins_h];
	    head[ins_h]=(short)strstart;

	    // strstart never exceeds WSIZE-MAX_MATCH, so there are
	    // always MIN_MATCH bytes ahead.
	  }
	  while (--match_length != 0);
	  strstart++; 
	}
	else{
	  strstart += match_length;
	  match_length = 0;
	  ins_h = window[strstart]&0xff;

	  ins_h=(((ins_h)<<hash_shift)^(window[strstart+1]&0xff))&hash_mask;
	  // If lookahead < MIN_MATCH, ins_h is garbage, but it does not
	  // matter since it will be recomputed at next deflate call.
	}
      }
      else {
	// No match, output a literal byte

	bflush=_tr_tally(0, window[strstart]&0xff);
	lookahead--;
	strstart++; 
      }
      if (bflush){

	flush_block_only(false);
	if(strm.avail_out==0) return NeedMore;
      }
    }

    flush_block_only(flush == Z_FINISH);
    if(strm.avail_out==0){
      if(flush == Z_FINISH) return FinishStarted;
      else return NeedMore;
    }
    return flush==Z_FINISH ? FinishDone : BlockDone;
  }

  // Same as above, but achieves better compression. We use a lazy
  // evaluation for matches: a match is finally adopted only if there is
  // no better match at the next window position.
  int deflate_slow(int flush){
//    short hash_head = 0;    // head of hash chain
    int hash_head = 0;    // head of hash chain
    boolean bflush;         // set if current block must be flushed

    // Process the input block.
    while(true){
      // Make sure that we always have enough lookahead, except
      // at the end of the input file. We need MAX_MATCH bytes
      // for the next match, plus MIN_MATCH bytes to insert the
      // string following the next match.

      if (lookahead < MIN_LOOKAHEAD) {
	fill_window();
	if(lookahead < MIN_LOOKAHEAD && flush == Z_NO_FLUSH) {
	  return NeedMore;
	}
	if(lookahead == 0) break; // flush the current block
      }

      // Insert the string window[strstart .. strstart+2] in the
      // dictionary, and set hash_head to the head of the hash chain:

      if(lookahead >= MIN_MATCH) {
	ins_h=(((ins_h)<<hash_shift)^(window[(strstart)+(MIN_MATCH-1)]&0xff)) & hash_mask;
//	prev[strstart&w_mask]=hash_head=head[ins_h];
	hash_head=(head[ins_h]&0xffff);
	prev[strstart&w_mask]=head[ins_h];
	head[ins_h]=(short)strstart;
      }

      // Find the longest match, discarding those <= prev_length.
      prev_length = match_length; prev_match = match_start;
      match_length = MIN_MATCH-1;

      if (hash_head != 0 && prev_length < max_lazy_match &&
	  ((strstart-hash_head)&0xffff) <= w_size-MIN_LOOKAHEAD
	  ){
	// To simplify the code, we prevent matches with the string
	// of window index 0 (in particular we have to avoid a match
	// of the string with itself at the start of the input file).

	if(strategy != Z_HUFFMAN_ONLY) {
	  match_length = longest_match(hash_head);
	}
	// longest_match() sets match_start

	if (match_length <= 5 && (strategy == Z_FILTERED ||
				  (match_length == MIN_MATCH &&
				   strstart - match_start > 4096))) {

	  // If prev_match is also MIN_MATCH, match_start is garbage
	  // but we will ignore the current match anyway.
	  match_length = MIN_MATCH-1;
	}
      }

      // If there was a match at the previous step and the current
      // match is not better, output the previous match:
      if(prev_length >= MIN_MATCH && match_length <= prev_length) {
	int max_insert = strstart + lookahead - MIN_MATCH;
	// Do not insert strings in hash table beyond this.

	//          check_match(strstart-1, prev_match, prev_length);

	bflush=_tr_tally(strstart-1-prev_match, prev_length - MIN_MATCH);

	// Insert in hash table all strings up to the end of the match.
	// strstart-1 and strstart are already inserted. If there is not
	// enough lookahead, the last two strings are not inserted in
	// the hash table.
	lookahead -= prev_length-1;
	prev_length -= 2;
	do{
	  if(++strstart <= max_insert) {
	    ins_h=(((ins_h)<<hash_shift)^(window[(strstart)+(MIN_MATCH-1)]&0xff))&hash_mask;
	    //prev[strstart&w_mask]=hash_head=head[ins_h];
	    hash_head=(head[ins_h]&0xffff);
	    prev[strstart&w_mask]=head[ins_h];
	    head[ins_h]=(short)strstart;
	  }
	}
	while(--prev_length != 0);
	match_available = 0;
	match_length = MIN_MATCH-1;
	strstart++;

	if (bflush){
	  flush_block_only(false);
	  if(strm.avail_out==0) return NeedMore;
	}
      } else if (match_available!=0) {

	// If there was no match at the previous position, output a
	// single literal. If there was a match but the current match
	// is longer, truncate the previous match to a single literal.

	bflush=_tr_tally(0, window[strstart-1]&0xff);

	if (bflush) {
	  flush_block_only(false);
	}
	strstart++;
	lookahead--;
	if(strm.avail_out == 0) return NeedMore;
      } else {
	// There is no previous match to compare with, wait for
	// the next step to decide.

	match_available = 1;
	strstart++;
	lookahead--;
      }
    }

    if(match_available!=0) {
      bflush=_tr_tally(0, window[strstart-1]&0xff);
      match_available = 0;
    }
    flush_block_only(flush == Z_FINISH);

    if(strm.avail_out==0){
      if(flush == Z_FINISH) return FinishStarted;
      else return NeedMore;
    }

    return flush == Z_FINISH ? FinishDone : BlockDone;
  }

  int longest_match(int cur_match){
    int chain_length = max_chain_length; // max hash chain length
    int scan = strstart;                 // current string
    int match;                           // matched string
    int len;                             // length of current match
    int best_len = prev_length;          // best match length so far
    int limit = strstart>(w_size-MIN_LOOKAHEAD) ?
      strstart-(w_size-MIN_LOOKAHEAD) : 0;
    int nice_match=this.nice_match;

    // Stop when cur_match becomes <= limit. To simplify the code,
    // we prevent matches with the string of window index 0.

    int wmask = w_mask;

    int strend = strstart + MAX_MATCH;
    byte scan_end1 = window[scan+best_len-1];
    byte scan_end = window[scan+best_len];

    // The code is optimized for HASH_BITS >= 8 and MAX_MATCH-2 multiple of 16.
    // It is easy to get rid of this optimization if necessary.

    // Do not waste too much time if we already have a good match:
    if (prev_length >= good_match) {
      chain_length >>= 2;
    }

    // Do not look for matches beyond the end of the input. This is necessary
    // to make deflate deterministic.
    if (nice_match > lookahead) nice_match = lookahead;

    do {
      match = cur_match;

      // Skip to next match if the match length cannot increase
      // or if the match length is less than 2:
      if (window[match+best_len]   != scan_end  ||
	  window[match+best_len-1] != scan_end1 ||
	  window[match]       != window[scan]     ||
	  window[++match]     != window[scan+1])      continue;

      // The check at best_len-1 can be removed because it will be made
      // again later. (This heuristic is not always a win.)
      // It is not necessary to compare scan[2] and match[2] since they
      // are always equal when the other bytes match, given that
      // the hash keys are equal and that HASH_BITS >= 8.
      scan += 2; match++;

      // We check for insufficient lookahead only every 8th comparison;
      // the 256th check will be made at strstart+258.
      do {
      } while (window[++scan] == window[++match] &&
	       window[++scan] == window[++match] &&
	       window[++scan] == window[++match] &&
	       window[++scan] == window[++match] &&
	       window[++scan] == window[++match] &&
	       window[++scan] == window[++match] &&
	       window[++scan] == window[++match] &&
	       window[++scan] == window[++match] &&
	       scan < strend);

      len = MAX_MATCH - (int)(strend - scan);
      scan = strend - MAX_MATCH;

      if(len>best_len) {
	match_start = cur_match;
	best_len = len;
	if (len >= nice_match) break;
	scan_end1  = window[scan+best_len-1];
	scan_end   = window[scan+best_len];
      }

    } while ((cur_match = (prev[cur_match & wmask]&0xffff)) > limit
	     && --chain_length != 0);

    if (best_len <= lookahead) return best_len;
    return lookahead;
  }

  int deflateInit(int level, int bits, int memlevel){
    return deflateInit(level, Z_DEFLATED, bits, memlevel,
			Z_DEFAULT_STRATEGY);
  }
    
  int deflateInit(int level, int bits){
    return deflateInit(level, Z_DEFLATED, bits, DEF_MEM_LEVEL,
			Z_DEFAULT_STRATEGY);
  }
  int deflateInit(int level){
    return deflateInit(level, MAX_WBITS);
  }
  private int deflateInit(int level, int method,  int windowBits,
			  int memLevel, int strategy){
    int wrap = 1;
    //    byte[] my_version=ZLIB_VERSION;

    //
    //  if (version == null || version[0] != my_version[0]
    //  || stream_size != sizeof(z_stream)) {
    //  return Z_VERSION_ERROR;
    //  }

    strm.msg = null;

    if (level == Z_DEFAULT_COMPRESSION) level = 6;

    if (windowBits < 0) { // undocumented feature: suppress zlib header
      wrap = 0;
      windowBits = -windowBits;
    }
    else if(windowBits > 15){
      wrap = 2;
      windowBits -= 16;
      strm.adler=new CRC32();
    }

    if (memLevel < 1 || memLevel > MAX_MEM_LEVEL || 
	method != Z_DEFLATED ||
	windowBits < 9 || windowBits > 15 || level < 0 || level > 9 ||
        strategy < 0 || strategy > Z_HUFFMAN_ONLY) {
      return Z_STREAM_ERROR;
    }

    strm.dstate = (Deflate)this;

    this.wrap = wrap;
    w_bits = windowBits;
    w_size = 1 << w_bits;
    w_mask = w_size - 1;

    hash_bits = memLevel + 7;
    hash_size = 1 << hash_bits;
    hash_mask = hash_size - 1;
    hash_shift = ((hash_bits+MIN_MATCH-1)/MIN_MATCH);

    window = new byte[w_size*2];
    prev = new short[w_size];
    head = new short[hash_size];

    lit_bufsize = 1 << (memLevel + 6); // 16K elements by default

    // We overlay pending_buf and d_buf+l_buf. This works since the average
    // output size for (length,distance) codes is <= 24 bits.
    pending_buf = new byte[lit_bufsize*3];
    pending_buf_size = lit_bufsize*3;

    d_buf = lit_bufsize;
    l_buf = new byte[lit_bufsize];

    this.level = level;

    this.strategy = strategy;
    this.method = (byte)method;

    return deflateReset();
  }

  int deflateReset(){
    strm.total_in = strm.total_out = 0;
    strm.msg = null; //
    strm.data_type = Z_UNKNOWN;

    pending = 0;
    pending_out = 0;

    if(wrap < 0){
      wrap = -wrap;
    }
    status = (wrap==0) ? BUSY_STATE : INIT_STATE;
    strm.adler.reset();

    last_flush = Z_NO_FLUSH;

    tr_init();
    lm_init();
    return Z_OK;
  }

  int deflateEnd(){
    if(status!=INIT_STATE && status!=BUSY_STATE && status!=FINISH_STATE){
      return Z_STREAM_ERROR;
    }
    // Deallocate in reverse order of allocations:
    pending_buf=null;
    l_buf=null;
    head=null;
    prev=null;
    window=null;
    // free
    // dstate=null;
    return status == BUSY_STATE ? Z_DATA_ERROR : Z_OK;
  }

  int deflateParams(int _level, int _strategy){
    int err=Z_OK;

    if(_level == Z_DEFAULT_COMPRESSION){
      _level = 6;
    }
    if(_level < 0 || _level > 9 || 
       _strategy < 0 || _strategy > Z_HUFFMAN_ONLY) {
      return Z_STREAM_ERROR;
    }

    if(config_table[level].func!=config_table[_level].func &&
       strm.total_in != 0) {
      // Flush the last buffer:
      err = strm.deflate(Z_PARTIAL_FLUSH);
    }

    if(level != _level) {
      level = _level;
      max_lazy_match   = config_table[level].max_lazy;
      good_match       = config_table[level].good_length;
      nice_match       = config_table[level].nice_length;
      max_chain_length = config_table[level].max_chain;
    }
    strategy = _strategy;
    return err;
  }

  int deflateSetDictionary (byte[] dictionary, int dictLength){
    int length = dictLength;
    int index=0;

    if(dictionary == null || status != INIT_STATE)
      return Z_STREAM_ERROR;

    strm.adler.update(dictionary, 0, dictLength);

    if(length < MIN_MATCH) return Z_OK;
    if(length > w_size-MIN_LOOKAHEAD){
      length = w_size-MIN_LOOKAHEAD;
      index=dictLength-length; // use the tail of the dictionary
    }
    System.arraycopy(dictionary, index, window, 0, length);
    strstart = length;
    block_start = length;

    // Insert all strings in the hash table (except for the last two bytes).
    // s->lookahead stays null, so s->ins_h will be recomputed at the next
    // call of fill_window.

    ins_h = window[0]&0xff;
    ins_h=(((ins_h)<<hash_shift)^(window[1]&0xff))&hash_mask;

    for(int n=0; n<=length-MIN_MATCH; n++){
      ins_h=(((ins_h)<<hash_shift)^(window[(n)+(MIN_MATCH-1)]&0xff))&hash_mask;
      prev[n&w_mask]=head[ins_h];
      head[ins_h]=(short)n;
    }
    return Z_OK;
  }

  int deflate(int flush){
    int old_flush;

    if(flush>Z_FINISH || flush<0){
      return Z_STREAM_ERROR;
    }

    if(strm.next_out == null ||
       (strm.next_in == null && strm.avail_in != 0) ||
       (status == FINISH_STATE && flush != Z_FINISH)) {
      strm.msg=z_errmsg[Z_NEED_DICT-(Z_STREAM_ERROR)];
      return Z_STREAM_ERROR;
    }
    if(strm.avail_out == 0){
      strm.msg=z_errmsg[Z_NEED_DICT-(Z_BUF_ERROR)];
      return Z_BUF_ERROR;
    }

    old_flush = last_flush;
    last_flush = flush;

    // Write the zlib header
    if(status == INIT_STATE) {
      if(wrap == 2){
        getGZIPHeader().put(this);
        status=BUSY_STATE;
        strm.adler.reset();
      }
      else{
      int header = (Z_DEFLATED+((w_bits-8)<<4))<<8;
      int level_flags=((level-1)&0xff)>>1;

      if(level_flags>3) level_flags=3;
      header |= (level_flags<<6);
      if(strstart!=0) header |= PRESET_DICT;
      header+=31-(header % 31);

      status=BUSY_STATE;
      putShortMSB(header);


      // Save the adler32 of the preset dictionary:
      if(strstart!=0){
        long adler=strm.adler.getValue();
        putShortMSB((int)(adler>>>16));
        putShortMSB((int)(adler&0xffff));
      }
      strm.adler.reset();
      }
    }

    // Flush as much pending output as possible
    if(pending != 0) {
      strm.flush_pending();
      if(strm.avail_out == 0) {
	// Since avail_out is 0, deflate will be called again with
	// more output space, but possibly with both pending and
	// avail_in equal to zero. There won't be anything to do,
	// but this is not an error situation so make sure we
	// return OK instead of BUF_ERROR at next call of deflate:
	last_flush = -1;
	return Z_OK;
      }

      // Make sure there is something to do and avoid duplicate consecutive
      // flushes. For repeated and useless calls with Z_FINISH, we keep
      // returning Z_STREAM_END instead of Z_BUFF_ERROR.
    }
    else if(strm.avail_in==0 && flush <= old_flush &&
	    flush != Z_FINISH) {
      strm.msg=z_errmsg[Z_NEED_DICT-(Z_BUF_ERROR)];
      return Z_BUF_ERROR;
    }

    // User must not provide more input after the first FINISH:
    if(status == FINISH_STATE && strm.avail_in != 0) {
      strm.msg=z_errmsg[Z_NEED_DICT-(Z_BUF_ERROR)];
      return Z_BUF_ERROR;
    }

    // Start a new block or continue the current one.
    if(strm.avail_in!=0 || lookahead!=0 ||
       (flush != Z_NO_FLUSH && status != FINISH_STATE)) {
      int bstate=-1;
      switch(config_table[level].func){
      case STORED: 
	bstate = deflate_stored(flush);
	break;
      case FAST: 
	bstate = deflate_fast(flush);
	break;
      case SLOW: 
	bstate = deflate_slow(flush);
	break;
      default:
      }

      if (bstate==FinishStarted || bstate==FinishDone) {
	status = FINISH_STATE;
      }
      if (bstate==NeedMore || bstate==FinishStarted) {
	if(strm.avail_out == 0) {
	  last_flush = -1; // avoid BUF_ERROR next call, see above
	}
	return Z_OK;
	// If flush != Z_NO_FLUSH && avail_out == 0, the next call
	// of deflate should use the same flush parameter to make sure
	// that the flush is complete. So we don't have to output an
	// empty block here, this will be done at next call. This also
	// ensures that for a very small output buffer, we emit at most
	// one empty block.
      }

      if (bstate==BlockDone) {
	if(flush == Z_PARTIAL_FLUSH) {
	  _tr_align();
	} 
	else { // FULL_FLUSH or SYNC_FLUSH
	  _tr_stored_block(0, 0, false);
	  // For a full flush, this empty block will be recognized
	  // as a special marker by inflate_sync().
	  if(flush == Z_FULL_FLUSH) {
	    //state.head[s.hash_size-1]=0;
	    for(int i=0; i<hash_size/*-1*/; i++)  // forget history
	      head[i]=0;
	  }
	}
	strm.flush_pending();
	if(strm.avail_out == 0) {
	  last_flush = -1; // avoid BUF_ERROR at next call, see above
	  return Z_OK;
	}
      }
    }

    if(flush!=Z_FINISH) return Z_OK;
    if(wrap<=0) return Z_STREAM_END;

    if(wrap==2){
      long adler=strm.adler.getValue();
      put_byte((byte)(adler&0xff));
      put_byte((byte)((adler>>8)&0xff));
      put_byte((byte)((adler>>16)&0xff));
      put_byte((byte)((adler>>24)&0xff));
      put_byte((byte)(strm.total_in&0xff));
      put_byte((byte)((strm.total_in>>8)&0xff));
      put_byte((byte)((strm.total_in>>16)&0xff));
      put_byte((byte)((strm.total_in>>24)&0xff));

      getGZIPHeader().setCRC(adler);
    } 
    else{
      // Write the zlib trailer (adler32)
      long adler=strm.adler.getValue();
      putShortMSB((int)(adler>>>16));
      putShortMSB((int)(adler&0xffff));
    }

    strm.flush_pending();

    // If avail_out is zero, the application will call deflate again
    // to flush the rest.

    if(wrap > 0) wrap = -wrap; // write the trailer only once!
    return pending != 0 ? Z_OK : Z_STREAM_END;
  }

  static int deflateCopy(ZStream dest, ZStream src){

    if(src.dstate == null){
      return Z_STREAM_ERROR;
    }

    if(src.next_in!=null){
      dest.next_in = new byte[src.next_in.length];
      System.arraycopy(src.next_in, 0, dest.next_in, 0, src.next_in.length);
    }
    dest.next_in_index = src.next_in_index;
    dest.avail_in = src.avail_in;
    dest.total_in = src.total_in;

    if(src.next_out!=null){
      dest.next_out = new byte[src.next_out.length];
      System.arraycopy(src.next_out, 0, dest.next_out ,0 , src.next_out.length);
    }

    dest.next_out_index = src.next_out_index;
    dest.avail_out = src.avail_out;
    dest.total_out = src.total_out;

    dest.msg = src.msg;
    dest.data_type = src.data_type;
    dest.adler = src.adler.copy();

    try{
      dest.dstate = (Deflate)src.dstate.clone();
      dest.dstate.strm = dest;
    }
    catch(CloneNotSupportedException e){
      //
    }
    return Z_OK;
  }

  public Object clone() throws CloneNotSupportedException {
    Deflate dest = (Deflate)super.clone();

    dest.pending_buf = dup(dest.pending_buf);
    dest.d_buf = dest.d_buf;
    dest.l_buf = dup(dest.l_buf);
    dest.window = dup(dest.window);

    dest.prev = dup(dest.prev);
    dest.head = dup(dest.head);
    dest.dyn_ltree = dup(dest.dyn_ltree);
    dest.dyn_dtree = dup(dest.dyn_dtree);
    dest.bl_tree = dup(dest.bl_tree);

    dest.bl_count = dup(dest.bl_count);
    dest.next_code = dup(dest.next_code);
    dest.heap = dup(dest.heap);
    dest.depth = dup(dest.depth);

    dest.l_desc.dyn_tree = dest.dyn_ltree;
    dest.d_desc.dyn_tree = dest.dyn_dtree;
    dest.bl_desc.dyn_tree = dest.bl_tree;

    /*
    dest.l_desc.stat_desc = StaticTree.static_l_desc;
    dest.d_desc.stat_desc = StaticTree.static_d_desc;
    dest.bl_desc.stat_desc = StaticTree.static_bl_desc;
    */

    if(dest.gheader!=null){
      dest.gheader = (GZIPHeader)dest.gheader.clone();
    }

    return dest;
  }

  private byte[] dup(byte[] buf){
    byte[] foo = new byte[buf.length];
    System.arraycopy(buf, 0, foo, 0, foo.length);
    return foo;
  }
  private short[] dup(short[] buf){
    short[] foo = new short[buf.length];
    System.arraycopy(buf, 0, foo, 0, foo.length);
    return foo;
  }
  private int[] dup(int[] buf){
    int[] foo = new int[buf.length];
    System.arraycopy(buf, 0, foo, 0, foo.length);
    return foo;
  }

  synchronized GZIPHeader getGZIPHeader(){
    if(gheader==null){
      gheader = new GZIPHeader();
    }
    return gheader;
  }
}
