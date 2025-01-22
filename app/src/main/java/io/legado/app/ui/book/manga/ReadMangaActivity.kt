package io.legado.app.ui.book.manga

import android.os.Bundle
import android.os.Looper
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityMangeBinding
import io.legado.app.model.ReadMange
import io.legado.app.model.ReadMange.mFirstLoading
import io.legado.app.model.recyclerView.MangeContent
import io.legado.app.model.recyclerView.ReaderLoading
import io.legado.app.ui.book.manga.rv.MangaAdapter
import io.legado.app.utils.immersionFullScreen
import io.legado.app.utils.viewbindingdelegate.viewBinding

class ReadMangaActivity : VMBaseActivity<ActivityMangeBinding, MangaViewModel>(),
    ReadMange.Callback {

    private var mAdapter: MangaAdapter? = null
    override val binding by viewBinding(ActivityMangeBinding::inflate)
    override val viewModel by viewModels<MangaViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        immersionFullScreen(WindowInsetsControllerCompat(window, binding.root))
        mAdapter = MangaAdapter { nextIndex ->

        }
        binding.mRecyclerMange.adapter = mAdapter
        binding.mRecyclerMange.itemAnimator = null
        ReadMange.register(this)
        binding.mRecyclerMange.setPreScrollListener { _, dy, position ->
            val content = mAdapter!!.getCurrentList()[position]
            if (content is MangeContent) {
                ReadMange.durChapterPos = content.mDurChapterPos.minus(1)
            }
            upText()
            if (dy > 0 && position + 2 > mAdapter!!.getCurrentList().size - 3) {
                if (mAdapter?.getCurrentList()?.last() is ReaderLoading) {
                    val nextIndex =
                        (mAdapter!!.getCurrentList().last() as ReaderLoading).mNextChapterIndex
                    ReadMange.moveToNextChapter(nextIndex)
                }
            }
        }
        binding.retry.setOnClickListener {
            binding.llLoading.isVisible = true
            binding.retry.isGone = true
            mFirstLoading = false
            ReadMange.loadContent()
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
            mAdapter?.submitList(list) {
                if (!mFirstLoading) {
                    if (list.size > 1) {
                        binding.infobar.isVisible = true
                        upText()
                    }

                    if (ReadMange.durChapterPos + 2 > mAdapter!!.getCurrentList().size - 3) {
                        val nextIndex =
                            (mAdapter!!.getCurrentList().last() as ReaderLoading).mNextChapterIndex
                        ReadMange.moveToNextChapter(nextIndex)
                    } else {
                        if (ReadMange.durChapterPos != 0) {
                            binding.mRecyclerMange.scrollToPosition(ReadMange.durChapterPos)
                        }
                    }
                }
                mFirstLoading = true
            }
        }
    }

    private fun upText() {
        binding.infobar.update(
            ReadMange.durChapterIndex,
            ReadMange.chapterSize,
            ReadMange.durChapterIndex.minus(1f).div(ReadMange.chapterSize.minus(1f)),
            ReadMange.durChapterPos,
            ReadMange.durChapterSize
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
        }

    }

    override val chapterList: MutableList<Any>
        get() = mAdapter!!.getCurrentList()

    override fun onDestroy() {
        ReadMange.unregister()
        super.onDestroy()
    }
}