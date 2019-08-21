package io.legado.app.ui.widget.page

import io.legado.app.data.entities.BookChapter

interface DataSource {

    fun isPrepared(): Boolean

    fun getChapterPosition(): Int

    fun getChapter(position: Int): TextChapter?

    fun getCurrentChapter(): TextChapter?

    fun getNextChapter(): TextChapter?

    fun getPreviousChapter(): TextChapter?

    fun hasNextChapter(): Boolean

    fun hasPrevChapter(): Boolean

    interface CallBack {
        fun onLoadFinish(bookChapter: BookChapter, content: String)
    }
}