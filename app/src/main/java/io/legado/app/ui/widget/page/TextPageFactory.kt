package io.legado.app.ui.widget.page

class TextPageFactory private constructor(dataSource: DataSource) :
    PageFactory<TextPage>(dataSource) {

    companion object {
        fun create(dataSource: DataSource): TextPageFactory {
            return TextPageFactory(dataSource)
        }
    }

    override fun hasPrev(): Boolean = with(dataSource) {
        return if (isScrollDelegate()) {
            hasPrevChapter()
        } else {
            hasPrevChapter() || pageIndex() > 0
        }
    }

    override fun hasNext(): Boolean = with(dataSource) {
        return if (isScrollDelegate()) {
            hasNextChapter()
        } else {
            hasNextChapter()
                    || getCurrentChapter()?.isLastIndex(pageIndex()) != true
        }
    }

    override fun pageAt(index: Int): TextPage {
        return dataSource.getCurrentChapter()?.page(index)
            ?: TextPage(index = index, title = "index：$index")
    }

    override fun moveToFirst() {
        dataSource.setPageIndex(0)
    }

    override fun moveToLast() = with(dataSource) {
        getCurrentChapter()?.let {
            if (it.pageSize() == 0) {
                setPageIndex(0)
            } else {
                setPageIndex(it.pageSize().minus(1))
            }
        } ?: setPageIndex(0)
    }

    override fun moveToNext(): Boolean = with(dataSource) {
        return if (hasNext()) {
            if (getCurrentChapter()?.isLastIndex(pageIndex()) == true
                || isScrollDelegate()
            ) {
                moveToNextChapter()
            } else {
                setPageIndex(pageIndex().plus(1))
            }
            true
        } else
            false
    }

    override fun moveToPrevious(): Boolean = with(dataSource) {
        return if (hasPrev()) {
            if (pageIndex() <= 0 || isScrollDelegate()) {
                moveToPrevChapter()
            } else {
                setPageIndex(pageIndex().minus(1))
            }
            true
        } else
            false
    }

    override fun currentPage(): TextPage? = with(dataSource) {
        return if (isScrollDelegate()) {
            getCurrentChapter()?.scrollPage()
        } else {
            getCurrentChapter()?.page(pageIndex())
        } ?: TextPage(index = pageIndex(), title = "index：${pageIndex()}")
    }

    override fun nextPage(): TextPage? = with(dataSource) {
        if (isScrollDelegate()) {
            return getNextChapter()?.scrollPage()
                ?: TextPage(index = pageIndex() + 1, title = "index：${pageIndex() + 1}")
        }
        getCurrentChapter()?.let {
            if (pageIndex() < it.pageSize() - 1) {
                return getCurrentChapter()?.page(pageIndex() + 1)?.removePageAloudSpan()
                    ?: TextPage(index = pageIndex() + 1, title = "index：${pageIndex() + 1}")
            }
        }
        return getNextChapter()?.page(0)?.removePageAloudSpan()
            ?: TextPage(index = pageIndex() + 1, title = "index：${pageIndex() + 1}")
    }

    override fun previousPage(): TextPage? = with(dataSource) {
        if (isScrollDelegate()) {
            return getPreviousChapter()?.scrollPage()
                ?: TextPage(index = pageIndex() + 1, title = "index：${pageIndex() + 1}")
        }
        if (pageIndex() > 0) {
            return getCurrentChapter()?.page(pageIndex() - 1)?.removePageAloudSpan()
                ?: TextPage(index = pageIndex() - 1, title = "index：${pageIndex() - 1}")
        }
        return getPreviousChapter()?.lastPage()?.removePageAloudSpan()
            ?: TextPage(index = pageIndex() - 1, title = "index：${pageIndex() - 1}")
    }


}
