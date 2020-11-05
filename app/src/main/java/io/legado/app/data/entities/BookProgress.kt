package io.legado.app.data.entities

data class BookProgress(
    val bookUrl: String,
    val tocUrl: String = "",
    var origin: String = "",
    var originName: String = "",
    val durChapterIndex: Int,
    val durChapterPos: Int,
    val durChapterTime: Long,
    val durChapterTitle: String?
)