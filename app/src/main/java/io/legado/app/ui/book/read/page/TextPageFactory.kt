package io.legado.app.ui.book.read.page

import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.entities.TextPage

class TextPageFactory(dataSource: DataSource) : PageFactory<TextPage>(dataSource) {

    override fun hasPrev(): Boolean = with(dataSource) {
        return hasPrevChapter() || pageIndex > 0
    }

    override fun hasNext(): Boolean = with(dataSource) {
        return hasNextChapter() || getCurrentChapter()?.isLastIndex(pageIndex) != true
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
            if (getCurrentChapter()?.isLastIndex(pageIndex) == true) {
                ReadBook.moveToNextChapter(false)
            } else {
                setPageIndex(pageIndex.plus(1))
            }
            true
        } else
            false
    }

    override fun moveToPrev(): Boolean = with(dataSource) {
        return if (hasPrev()) {
            if (pageIndex <= 0) {
                ReadBook.moveToPrevChapter(false)
            } else {
                setPageIndex(pageIndex.minus(1))
            }
            true
        } else
            false
    }

    override val currentPage: TextPage?
        get() = with(dataSource) {
            return getCurrentChapter()?.page(pageIndex)
        }

    override val nextPage: TextPage?
        get() = with(dataSource) {
            getCurrentChapter()?.let {
                if (pageIndex < it.pageSize() - 1) {
                    return getCurrentChapter()?.page(pageIndex + 1)?.removePageAloudSpan()
                }
            }
            return getNextChapter()?.page(0)?.removePageAloudSpan()
        }

    override val prevPage: TextPage?
        get() = with(dataSource) {
            if (pageIndex > 0) {
                return getCurrentChapter()?.page(pageIndex - 1)?.removePageAloudSpan()
            }
            return getPreviousChapter()?.lastPage()?.removePageAloudSpan()
        }


}
