package io.legado.app.ui.widget.page

abstract class PageFactory<DATA>(protected val dataSource: DataSource) {

    abstract fun moveToFirst()

    abstract fun moveToLast()

    abstract fun moveToNext():Boolean

    abstract fun moveToPrevious(): Boolean

    abstract fun nextPage(): DATA?

    abstract fun previousPage(): DATA?

    abstract fun currentPage(): DATA?

    abstract fun hasNext(): Boolean

    abstract fun hasPrev(): Boolean

}