package io.legado.app.ui.widget.page

data class TextPage(
    val index: Int,
    val text: CharSequence,
    val title: String,
    var pageSize: Int = 0,
    var chapterSize: Int = 0,
    var chapterIndex: Int = 0
)
