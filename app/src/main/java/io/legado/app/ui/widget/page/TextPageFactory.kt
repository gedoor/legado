package io.legado.app.ui.widget.page

class TextPageFactory private constructor(dataSource: DataSource) :
    PageFactory<TextPage>(dataSource) {

    companion object {
        fun create(dataSource: DataSource): TextPageFactory {
            return TextPageFactory(dataSource)
        }
    }

    override fun hasPrev(): Boolean {
        return dataSource.hasPrevChapter() || dataSource.pageIndex() > 0
    }

    override fun hasNext(): Boolean {
        return dataSource.hasNextChapter()
                || dataSource.getCurrentChapter()?.isLastIndex(dataSource.pageIndex()) != true
    }

    override fun pageAt(index: Int): TextPage {
        return dataSource.getCurrentChapter()?.page(index)
            ?: TextPage(index = index, title = "index：$index")
    }

    override fun moveToFirst() {
        dataSource.setPageIndex(0)
    }

    override fun moveToLast() {
        dataSource.getCurrentChapter()?.let {
            if (it.pageSize() == 0) {
                dataSource.setPageIndex(0)
            } else {
                dataSource.setPageIndex(it.pageSize().minus(1))
            }
        } ?: dataSource.setPageIndex(0)
    }

    override fun moveToNext(): Boolean = dataSource.pageIndex().let { index ->
        return if (hasNext()) {
            if (dataSource.getCurrentChapter()?.isLastIndex(index) == true) {
                dataSource.moveToNextChapter()
            } else {
                dataSource.setPageIndex(index.plus(1))
            }
            true
        } else
            false
    }

    override fun moveToPrevious(): Boolean = dataSource.pageIndex().let { index ->
        return if (hasPrev()) {
            if (index > 0) {
                dataSource.setPageIndex(index.minus(1))
            } else {
                dataSource.moveToPrevChapter()
            }
            true
        } else
            false
    }

    override fun currentPage(): TextPage? = dataSource.pageIndex().let { index ->
        return if (dataSource.isScrollDelegate()) {
            dataSource.getCurrentChapter()?.scrollPage()
        } else {
            dataSource.getCurrentChapter()?.page(index)
        } ?: TextPage(index = index, title = "index：$index")
    }

    override fun nextPage(): TextPage? = dataSource.pageIndex().let { index ->
        if (dataSource.isScrollDelegate()) {
            return dataSource.getNextChapter()?.scrollPage()
                ?: TextPage(index = index + 1, title = "index：${index + 1}")
        }
        dataSource.getCurrentChapter()?.let {
            if (index < it.pageSize() - 1) {
                return dataSource.getCurrentChapter()?.page(index + 1)?.removePageAloudSpan()
                    ?: TextPage(index = index + 1, title = "index：${index + 1}")
            }
        }
        return dataSource.getNextChapter()?.page(0)?.removePageAloudSpan()
            ?: TextPage(index = index + 1, title = "index：${index + 1}")
    }

    override fun previousPage(): TextPage? = dataSource.pageIndex().let { index ->
        if (dataSource.isScrollDelegate()) {
            return dataSource.getPreviousChapter()?.scrollPage()
                ?: TextPage(index = index + 1, title = "index：${index + 1}")
        }
        if (index > 0) {
            return dataSource.getCurrentChapter()?.page(index - 1)?.removePageAloudSpan()
                ?: TextPage(index = index - 1, title = "index：${index - 1}")
        }
        return dataSource.getPreviousChapter()?.lastPage()?.removePageAloudSpan()
            ?: TextPage(index = index - 1, title = "index：${index - 1}")
    }


}
