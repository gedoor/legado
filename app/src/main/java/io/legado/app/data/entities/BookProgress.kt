package io.legado.app.data.entities

data class BookProgress(
    val name: String,
    val author: String,
    val durChapterIndex: Int,
    val durChapterPos: Int,
    val durChapterTime: Long,
    val durChapterTitle: String?
)