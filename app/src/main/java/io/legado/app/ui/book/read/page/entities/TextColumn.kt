package io.legado.app.ui.book.read.page.entities

/**
 * 字符信息
 */
data class TextColumn(
    val charData: String,
    var start: Float,
    var end: Float,
    val style: Int = 0, //0:文字,1:图片,2:按钮
    var selected: Boolean = false,
    var isSearchResult: Boolean = false
) {

    fun isTouch(x: Float): Boolean {
        return x > start && x < end
    }

}