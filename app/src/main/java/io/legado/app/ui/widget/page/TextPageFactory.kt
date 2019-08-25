package io.legado.app.ui.widget.page

import io.legado.app.App
import io.legado.app.R

class TextPageFactory private constructor(dataSource: DataSource) :
    PageFactory<TextPage>(dataSource) {

    companion object {
        fun create(dataSource: DataSource): TextPageFactory {
            return TextPageFactory(dataSource)
        }
    }

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
        return dataSource.getCurrentChapter()?.page(index)
            ?: TextPage(index, App.INSTANCE.getString(R.string.data_loading), "index：$index")
    }

    override fun nextPage(): TextPage? = dataSource.pageIndex().let { index ->
        dataSource.getCurrentChapter()?.let {
            if (index < it.pageSize() - 1) {
                return dataSource.getCurrentChapter()?.page(index + 1)
                    ?: TextPage(
                        index + 1,
                        App.INSTANCE.getString(R.string.data_loading),
                        "index：${index + 1}"
                    )
            }
        }
        return dataSource.getNextChapter()?.page(0)
            ?: TextPage(
                index + 1,
                App.INSTANCE.getString(R.string.data_loading),
                "index：${index + 1}"
            )
    }

    override fun previousPage(): TextPage? = dataSource.pageIndex().let { index ->
        if (index > 0) {
            return dataSource.getCurrentChapter()?.page(index - 1)
                ?: TextPage(
                    index - 1,
                    App.INSTANCE.getString(R.string.data_loading),
                    "index：${index - 1}"
                )
        }
        return dataSource.getPreviousChapter()?.lastPage()
            ?: TextPage(
                index - 1,
                App.INSTANCE.getString(R.string.data_loading),
                "index：${index - 1}"
            )
    }


}
