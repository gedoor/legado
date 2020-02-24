package io.legado.app.ui.book.read.page

import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.entities.TextChapter

interface DataSource {

    val pageIndex: Int get() = ReadBook.durChapterPos()

    fun getCurrentChapter(): TextChapter?

    fun getNextChapter(): TextChapter?

    fun getPreviousChapter(): TextChapter?

    fun hasNextChapter(): Boolean

    fun hasPrevChapter(): Boolean
}