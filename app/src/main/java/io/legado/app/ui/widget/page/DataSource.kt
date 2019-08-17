package io.legado.app.ui.widget.page

interface DataSource {

    fun getChapterPosition()

    fun getChapter(position: Int): TextChapter?

    fun getNextChapter(): TextChapter?

    fun getPreviousChapter(): TextChapter?

    fun hasNextChapter(): Boolean

    fun hasPrevChapter(): Boolean
}