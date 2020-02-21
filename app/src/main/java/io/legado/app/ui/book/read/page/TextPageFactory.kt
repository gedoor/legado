package io.legado.app.ui.book.read.page

import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.entities.TextPage

class TextPageFactory(dataSource: DataSource) : PageFactory<TextPage>(dataSource) {

    override fun hasPrev(): Boolean = with(dataSource) {
        return if (isScrollDelegate) {
            hasPrevChapter()
        } else {
            hasPrevChapter() || pageIndex > 0
        }
    }

    override fun hasNext(): Boolean = with(dataSource) {
        return if (isScrollDelegate) {
            hasNextChapter()
        } else {
            hasNextChapter()
                    || getCurrentChapter()?.isLastIndex(pageIndex) != true
        }
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
            if (getCurrentChapter()?.isLastIndex(pageIndex) == true
                || isScrollDelegate
            ) {
                ReadBook.moveToNextChapter(false)
            } else {
                setPageIndex(pageIndex.plus(1))
            }
            true
        } else
            false
    }

    override fun moveToPrevious(): Boolean = with(dataSource) {
        return if (hasPrev()) {
            if (pageIndex <= 0 || isScrollDelegate) {
                ReadBook.moveToPrevChapter(false)
            } else {
                setPageIndex(pageIndex.minus(1))
            }
            true
        } else
            false
    }

    override fun currentPage(): TextPage? = with(dataSource) {
        return if (isScrollDelegate) {
            getCurrentChapter()?.scrollPage()
        } else {
            getCurrentChapter()?.page(pageIndex)
        }
    }

    override fun nextPage(): TextPage? = with(dataSource) {
        if (isScrollDelegate) {
            return getNextChapter()?.scrollPage()
        }
        getCurrentChapter()?.let {
            if (pageIndex < it.pageSize() - 1) {
                return getCurrentChapter()?.page(pageIndex + 1)?.removePageAloudSpan()
            }
        }
        return getNextChapter()?.page(0)?.removePageAloudSpan()
    }

    override fun previousPage(): TextPage? = with(dataSource) {
        if (isScrollDelegate) {
            return getPreviousChapter()?.scrollPage()
        }
        if (pageIndex > 0) {
            return getCurrentChapter()?.page(pageIndex - 1)?.removePageAloudSpan()
        }
        return getPreviousChapter()?.lastPage()?.removePageAloudSpan()
    }


}
