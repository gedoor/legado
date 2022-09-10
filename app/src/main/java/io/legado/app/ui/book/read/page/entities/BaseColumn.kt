package io.legado.app.ui.book.read.page.entities

/**
 * 列基类
 */
interface BaseColumn {
    var start: Float
    var end: Float

    fun isTouch(x: Float): Boolean {
        return x > start && x < end
    }

}