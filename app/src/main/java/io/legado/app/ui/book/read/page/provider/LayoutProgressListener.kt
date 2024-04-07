package io.legado.app.ui.book.read.page.provider

import io.legado.app.ui.book.read.page.entities.TextPage

interface LayoutProgressListener {

    /**
     * 单页排版完成
     */
    fun onLayoutPageCompleted(index: Int, page: TextPage) {}

    /**
     * 全部排版完成
     */
    fun onLayoutCompleted() {}

    /**
     * 排版出现异常
     */
    fun onLayoutException(e: Throwable) {}

}
