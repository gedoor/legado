package io.legado.app.ui.book.read.page.provider

import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.api.DataSource
import io.legado.app.ui.book.read.page.api.PageFactory
import io.legado.app.ui.book.read.page.entities.PageData
import io.legado.app.ui.book.read.page.entities.TextPage

class TextPageFactory(dataSource: DataSource) : PageFactory<PageData>(dataSource) {

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

    override val curData: PageData
        get() = with(dataSource) {
            ReadBook.msg?.let {
                return@with PageData(TextPage(text = it).format())
            }
            currentChapter?.let {
                val page = it.page(pageIndex) ?: TextPage(title = it.title).format()
                return@with PageData(page, it)
            }
            return PageData(TextPage().format())
        }

    override val nextData: PageData
        get() = with(dataSource) {
            ReadBook.msg?.let {
                return@with PageData(TextPage(text = it).format())
            }
            currentChapter?.let {
                if (pageIndex < it.pageSize - 1) {
                    val page = it.page(pageIndex + 1)?.removePageAloudSpan()
                        ?: TextPage(title = it.title).format()
                    return@with PageData(page, it)
                }
            }
            if (!hasNextChapter()) {
                return@with PageData(TextPage(text = ""))
            }
            nextChapter?.let {
                val page = it.page(0)?.removePageAloudSpan()
                    ?: TextPage(title = it.title).format()
                return@with PageData(page, it)
            }
            return PageData(TextPage().format())
        }

    override val prevData: PageData
        get() = with(dataSource) {
            ReadBook.msg?.let {
                return@with PageData(TextPage(text = it).format())
            }
            if (pageIndex > 0) {
                currentChapter?.let {
                    val page = it.page(pageIndex - 1)?.removePageAloudSpan()
                        ?: TextPage(title = it.title).format()
                    return@with PageData(page, it)
                }
            }
            prevChapter?.let {
                val page = it.lastPage?.removePageAloudSpan()
                    ?: TextPage(title = it.title).format()
                return@with PageData(page, it)
            }
            return PageData(TextPage().format())
        }

    override val nextPlusData: PageData
        get() = with(dataSource) {
            currentChapter?.let {
                if (pageIndex < it.pageSize - 2) {
                    val page = it.page(pageIndex + 2)?.removePageAloudSpan()
                        ?: TextPage(title = it.title).format()
                    return@with PageData(page, it)
                }
                nextChapter?.let { nc ->
                    if (pageIndex < it.pageSize - 1) {
                        val page = nc.page(0)?.removePageAloudSpan()
                            ?: TextPage(title = nc.title).format()
                        return@with PageData(page, nc)
                    }
                    val page = nc.page(1)?.removePageAloudSpan()
                        ?: TextPage(title = nc.title).format()
                    return@with PageData(page, nc)
                }

            }
            return PageData(TextPage().format())
        }
}
