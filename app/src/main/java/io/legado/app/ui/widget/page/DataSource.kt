package io.legado.app.ui.widget.page

interface DataSource {
    val isScrollDelegate: Boolean

    val pageIndex: Int

    fun setPageIndex(pageIndex: Int)

    fun getChapterPosition(): Int

    fun getCurrentChapter(): TextChapter?

    fun getNextChapter(): TextChapter?

    fun getPreviousChapter(): TextChapter?

    fun hasNextChapter(): Boolean

    fun hasPrevChapter(): Boolean

}