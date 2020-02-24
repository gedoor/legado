package io.legado.app.ui.book.read.page

abstract class PageFactory<DATA>(protected val dataSource: DataSource) {

    abstract fun moveToFirst()

    abstract fun moveToLast()

    abstract fun moveToNext():Boolean

    abstract fun moveToPrevious(): Boolean

    abstract val nextPage: DATA?

    abstract val prevPage: DATA?

    abstract val currentPage: DATA?

    abstract fun hasNext(): Boolean

    abstract fun hasPrev(): Boolean

}