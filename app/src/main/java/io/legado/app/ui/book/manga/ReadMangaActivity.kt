package io.legado.app.ui.book.manga

import android.os.Bundle
import android.os.Looper
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.ActivityMangeBinding
import io.legado.app.databinding.ViewLoadMoreBinding
import io.legado.app.help.book.isImage
import io.legado.app.model.ReadMange
import io.legado.app.model.ReadMange.mFirstLoading
import io.legado.app.model.recyclerView.MangeContent
import io.legado.app.model.recyclerView.ReaderLoading
import io.legado.app.ui.book.changesource.ChangeBookSourceDialog
import io.legado.app.ui.book.manga.rv.MangaAdapter
import io.legado.app.ui.book.toc.TocActivityResult
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.gone
import io.legado.app.utils.immersionFullScreen
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible

class ReadMangaActivity : VMBaseActivity<ActivityMangeBinding, MangaViewModel>(),
    ReadMange.Callback, ChangeBookSourceDialog.CallBack {

    private val mAdapter: MangaAdapter by lazy {
        MangaAdapter()
    }
    private val loadMoreView by lazy {
        LoadMoreView(this).apply {
            setBackgroundColor(getCompatColor(R.color.book_ant_10))
            getLoading().loadingColor = getCompatColor(R.color.white)
            getLoadingText().setTextColor(getCompatColor(R.color.white))
        }
    }

    //打开目录返回选择章节返回结果
    private val tocActivity =
        registerForActivityResult(TocActivityResult()) {
            it?.let {
                viewModel.openChapter(it.first, it.second)
            }
        }
    override val binding by viewBinding(ActivityMangeBinding::inflate)
    override val viewModel by viewModels<MangaViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        immersionFullScreen(WindowInsetsControllerCompat(window, binding.root))
        binding.mRecyclerMange.adapter = mAdapter
        binding.mRecyclerMange.itemAnimator = null
        ReadMange.register(this)
        binding.mRecyclerMange.setPreScrollListener { _, _, dy, position ->
            if (dy > 0 && position + 2 > mAdapter.getCurrentList().size - 3) {
                if (mAdapter.getCurrentList().last() is ReaderLoading) {
                    val nextIndex =
                        (mAdapter.getCurrentList().last() as ReaderLoading).mNextChapterIndex
                    if (nextIndex != -1) {
                        scrollToBottom(false, nextIndex)
                    }
                }
            }
        }
        binding.mRecyclerMange.setNestedPreScrollListener { _, _, _, position ->
            if (mAdapter.getCurrentList().isNotEmpty()) {
                val content = mAdapter.getCurrentList()[position]
                if (content is MangeContent) {
                    ReadMange.durChapterPos = content.mDurChapterPos.minus(1)
                    upText(
                        content.mChapterPagePos,
                        content.mChapterPageCount,
                        content.mDurChapterPos,
                        content.mDurChapterCount
                    )
                }
            }
        }
        binding.retry.setOnClickListener {
            binding.llLoading.isVisible = true
            binding.retry.isGone = true
            mFirstLoading = false
            ReadMange.loadContent()
        }

        mAdapter.addFooterView {
            ViewLoadMoreBinding.bind(loadMoreView)
        }
        loadMoreView.setOnClickListener {
            if (!loadMoreView.isLoading && !ReadMange.gameOver) {
                scrollToBottom(true, ReadMange.durChapterPagePos)
            }
        }
        loadMoreView.gone()

        //打开换源界面
        /*ReadMange.book?.let {
            showDialogFragment(ChangeBookSourceDialog(it.name, it.author))
        }*/

        //打开目录
        /*ReadMange.book?.let {
            tocActivity.launch(it.bookUrl)
        }*/
    }

    private fun scrollToBottom(forceLoad: Boolean = false, index: Int) {
        if ((loadMoreView.hasMore && !loadMoreView.isLoading) && !ReadMange.gameOver || forceLoad) {
            loadMoreView.hasMore()
            ReadMange.moveToNextChapter(index)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        Looper.myQueue().addIdleHandler {
            viewModel.initData(intent)
            false
        }
    }

    override fun loadContentFinish(list: MutableList<Any>) {
        if (!this.isDestroyed) {
            mAdapter.submitList(list) {
                if (!mFirstLoading) {
                    if (list.size > 1) {
                        binding.infobar.isVisible = true
                        upText(
                            ReadMange.durChapterPagePos,
                            ReadMange.durChapterPageCount,
                            ReadMange.durChapterPos,
                            ReadMange.durChapterCount
                        )
                    }

                    if (ReadMange.durChapterPos + 2 > mAdapter.getCurrentList().size - 3) {
                        val nextIndex =
                            (mAdapter.getCurrentList().last() as ReaderLoading).mNextChapterIndex
                        scrollToBottom(index = nextIndex)
                    } else {
                        if (ReadMange.durChapterPos != 0) {
                            binding.mRecyclerMange.scrollToPosition(ReadMange.durChapterPos)
                        }
                    }
                }
                loadMoreView.visible()
                mFirstLoading = true
                loadMoreView.stopLoad()
            }
        }
    }

    private fun upText(
        chapterPagePos: Int, chapterPageCount: Int, chapterPos: Int, chapterCount: Int
    ) {
        binding.infobar.update(
            chapterPagePos,
            chapterPageCount,
            chapterPagePos.minus(1f).div(chapterPageCount.minus(1f)),
            chapterPos,
            chapterCount
        )
    }

    override fun onPause() {
        super.onPause()
        if (ReadMange.inBookshelf) {
            ReadMange.saveRead()
        }
    }

    override fun loadComplete() {
        binding.flLoading.isGone = true
    }

    override fun loadFail() {
        if (!mFirstLoading) {
            binding.llLoading.isGone = true
            binding.retry.isVisible = true
        } else {
            loadMoreView.error(null, "加载失败，点击重试")
        }
    }

    override fun noData() {
        loadMoreView.noMore("暂无章节了！")
    }

    override val chapterList: MutableList<Any>
        get() = mAdapter.getCurrentList()

    override fun onDestroy() {
        ReadMange.unregister()
        super.onDestroy()
    }

    override val oldBook: Book?
        get() = ReadMange.book

    override fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        if (book.isImage) {
            viewModel.changeTo(book, toc)
        } else {
            toastOnUi("所选择的源不是漫画源")
        }
    }
}