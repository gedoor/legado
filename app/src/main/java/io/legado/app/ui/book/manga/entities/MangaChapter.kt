package io.legado.app.ui.book.manga.entities

import io.legado.app.data.entities.BookChapter

data class MangaChapter(
    val chapter: BookChapter,
    val contents: List<Any>,
    val imageCount: Int
)
