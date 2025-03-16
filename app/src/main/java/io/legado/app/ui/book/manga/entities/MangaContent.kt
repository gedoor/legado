package io.legado.app.ui.book.manga.entities

data class MangaContent(
    val pos: Int,
    val items: List<Any>,
    val curFinish: Boolean,
    val nextFinish: Boolean
)
