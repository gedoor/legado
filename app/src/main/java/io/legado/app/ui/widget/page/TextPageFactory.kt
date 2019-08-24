package io.legado.app.ui.widget.page

class TextPageFactory private constructor(dataSource: DataSource) :
    PageFactory<TextPage>(dataSource) {

    companion object {
        fun create(dataSource: DataSource): TextPageFactory {
            return TextPageFactory(dataSource)
        }
    }

    var index: Int = 0

    override fun hasPrev(): Boolean {
        return true
    }

    override fun hasNext(): Boolean {
        return true
    }

    override fun pageAt(index: Int): TextPage {
        TODO("todo...")
    }

    override fun moveToFirst() {
        index = 0
    }

    override fun moveToLast() {
        index = dataSource.getCurrentChapter()?.let {
            if (it.pageSize() == 0) {
                0
            } else {
                it.pageSize() - 1
            }
        } ?: 0
    }

    override fun moveToNext(): Boolean {
        return if (hasNext()) {
            index = index.plus(1)
            true
        } else
            false
    }

    override fun moveToPrevious(): Boolean {
        return if (hasPrev()) {
            index = index.minus(1)
            true
        } else
            false
    }

    override fun currentPage(): TextPage? {
        return dataSource.getCurrentChapter()?.page(index)
            ?: TextPage(index, "index：$index", "index：$index")
    }

    override fun nextPage(): TextPage? {
        return dataSource.getCurrentChapter()?.page(index + 1)
            ?: TextPage(index + 1, "index：${index + 1}", "index：${index + 1}")
    }

    override fun previousPage(): TextPage? {
        return dataSource.getCurrentChapter()?.page(index - 1)
            ?: TextPage(index - 1, "index：${index - 1}", "index：${index - 1}")
    }


}
