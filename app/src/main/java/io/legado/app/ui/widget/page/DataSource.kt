package io.legado.app.ui.widget.page

interface DataSource {
    fun isScroll(): Boolean

    fun pageIndex(): Int

    fun setPageIndex(pageIndex: Int)

    fun isPrepared(): Boolean

    fun getChapterPosition(): Int

    fun getChapter(position: Int): TextChapter?

    fun getCurrentChapter(): TextChapter?

    fun getNextChapter(): TextChapter?

    fun getPreviousChapter(): TextChapter?

    fun hasNextChapter(): Boolean

    fun hasPrevChapter(): Boolean

    fun moveToNextChapter()

    fun moveToPrevChapter()

}