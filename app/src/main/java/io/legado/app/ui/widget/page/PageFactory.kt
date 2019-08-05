package io.legado.app.ui.widget.page

abstract class PageFactory<DATA>(protected val dataSource: DataSource) {

    abstract fun pageAt(index: Int): DATA

    abstract fun nextPage(): DATA

    abstract fun previousPage(): DATA

}