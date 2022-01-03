package io.legado.app.ui.widget.recycler.scroller

interface FastScrollStateChangeListener {

    /**
     * Called when fast scrolling begins
     */
    fun onFastScrollStart(fastScroller: FastScroller)

    /**
     * Called when fast scrolling ends
     */
    fun onFastScrollStop(fastScroller: FastScroller)
}