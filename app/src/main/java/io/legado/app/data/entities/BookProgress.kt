package io.legado.app.data.entities

data class BookProgress(
    val bookUrl: String,
    val durChapterIndex: Int,
    var durChapterPos: Int
)