package io.legado.app.ui.book.read.page.provider

import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.api.DataSource
import io.legado.app.ui.book.read.page.api.PageFactory
import io.legado.app.ui.book.read.page.entities.TextPage

class TextPageFactory(dataSource: DataSource) : PageFactory<TextPage>(dataSource) {

    override fun hasPrev(): Boolean = with(dataSource) {
        return hasPrevChapter() || pageIndex > 0
    }

    override fun hasNext(): Boolean = with(dataSource) {
        return hasNextChapter() || currentChapter?.isLastIndex(pageIndex) != true
    }

    override fun hasNextPlus(): Boolean = with(dataSource) {
        return hasNextChapter() || pageIndex < (currentChapter?.pageSize ?: 1) - 2
    }

    override fun moveToFirst() {
        ReadBook.setPageIndex(0)
    }

    override fun moveToLast() = with(dataSource) {
        currentChapter?.let {
            if (it.pageSize == 0) {
                ReadBook.setPageIndex(0)
            } else {
                ReadBook.setPageIndex(it.pageSize.minus(1))
            }
        } ?: ReadBook.setPageIndex(0)
    }

    override fun moveToNext(upContent: Boolean): Boolean = with(dataSource) {
        return if (hasNext()) {
            if (currentChapter?.isLastIndex(pageIndex) == true) {
                ReadBook.moveToNextChapter(upContent)
            } else {
                ReadBook.setPageIndex(pageIndex.plus(1))
            }
            if (upContent) upContent(resetPageOffset = false)
            true
        } else
            false
    }

    override fun moveToPrev(upContent: Boolean): Boolean = with(dataSource) {
        return if (hasPrev()) {
            if (pageIndex <= 0) {
                ReadBook.moveToPrevChapter(upContent)
            } else {
                ReadBook.setPageIndex(pageIndex.minus(1))
            }
            if (upContent) upContent(resetPageOffset = false)
            true
        } else
            false
    }

    override val curPage: TextPage
        get() = with(dataSource) {
            ReadBook.msg?.let {
                return@with TextPage(text = it).format()
            }
            currentChapter?.let {
                return@with it.page(pageIndex) ?: TextPage(title = it.title).format()
            }
            return TextPage().format()
        }

    override val nextPage: TextPage
        get() = with(dataSource) {
            ReadBook.msg?.let {
                return@with TextPage(text = it).format()
            }
            currentChapter?.let {
                if (pageIndex < it.pageSize - 1) {
                    return@with it.page(pageIndex + 1)?.removePageAloudSpan()
                        ?: TextPage(title = it.title).format()
                }
            }
            if (!hasNextChapter()) {
                return@with TextPage(text = "")
            }
            nextChapter?.let {
                return@with it.page(0)?.removePageAloudSpan()
                    ?: TextPage(title = it.title).format()
            }
            return TextPage().format()
        }

    override val prevPage: TextPage
        get() = with(dataSource) {
            ReadBook.msg?.let {
                return@with TextPage(text = it).format()
            }
            if (pageIndex > 0) {
                currentChapter?.let {
                    return@with it.page(pageIndex - 1)?.removePageAloudSpan()
                        ?: TextPage(title = it.title).format()
                }
            }
            prevChapter?.let {
                return@with it.lastPage?.removePageAloudSpan()
                    ?: TextPage(title = it.title).format()
            }
            return TextPage().format()
        }

    override val nextPlusPage: TextPage
        get() = with(dataSource) {
            currentChapter?.let {
                if (pageIndex < it.pageSize - 2) {
                    return@with it.page(pageIndex + 2)?.removePageAloudSpan()
                        ?: TextPage(title = it.title).format()
                }
                nextChapter?.let { nc ->
                    if (pageIndex < it.pageSize - 1) {
                        return@with nc.page(0)?.removePageAloudSpan()
                            ?: TextPage(title = nc.title).format()
                    }
                    return@with nc.page(1)?.removePageAloudSpan()
                        ?: TextPage(title = nc.title).format()
                }

            }
            return TextPage().format()
        }
}
