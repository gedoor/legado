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
            index = if (dataSource.getCurrentChapter()?.isLastIndex(index) == true) {
                dataSource.moveToNextChapter()
                0
            } else {
                index.plus(1)
            }
            true
        } else
            false
    }

    override fun moveToPrevious(): Boolean {
        return if (hasPrev()) {
            index = if (index > 0) {
                index.minus(1)
            } else {
                dataSource.moveToPrevChapter()
                dataSource.getPreviousChapter()?.lastIndex() ?: 0
            }
            true
        } else
            false
    }

    override fun currentPage(): TextPage? {
        return dataSource.getCurrentChapter()?.page(index)
            ?: TextPage(index, "index：$index", "index：$index")
    }

    override fun nextPage(): TextPage? {
        dataSource.getCurrentChapter()?.let {
            if (index < it.pageSize() - 1) {
                return dataSource.getCurrentChapter()?.page(index + 1)
                    ?: TextPage(index + 1, "index：${index + 1}", "index：${index + 1}")
            }
        }
        return dataSource.getNextChapter()?.page(0)
            ?: TextPage(index + 1, "index：${index + 1}", "index：${index + 1}")
    }

    override fun previousPage(): TextPage? {
        if (index > 0) {
            return dataSource.getCurrentChapter()?.page(index - 1)
                ?: TextPage(index - 1, "index：${index - 1}", "index：${index - 1}")
        }

        return dataSource.getPreviousChapter()?.lastPage()
            ?: TextPage(index - 1, "index：${index - 1}", "index：${index - 1}")
    }


}
