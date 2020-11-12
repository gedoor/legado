package io.legado.app.ui.book.read.page

abstract class PageFactory<DATA>(protected val dataSource: DataSource) {

    abstract fun moveToFirst()

    abstract fun moveToLast()

    abstract fun moveToNext(upContent: Boolean): Boolean

    abstract fun moveToPrev(upContent: Boolean): Boolean

    abstract val nextData: DATA

    abstract val prevData: DATA

    abstract val curData: DATA

    abstract val nextPlusData: DATA

    abstract fun hasNext(): Boolean

    abstract fun hasPrev(): Boolean

    abstract fun hasNextPlus(): Boolean
}