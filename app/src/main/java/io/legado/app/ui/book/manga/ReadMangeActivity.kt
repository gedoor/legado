package io.legado.app.ui.book.manga

import android.os.Bundle
import android.os.Looper
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityMangeBinding
import io.legado.app.model.ReadMange
import io.legado.app.model.ReadMange.mFirstLoading
import io.legado.app.model.recyclerView.MangeContent
import io.legado.app.model.recyclerView.ReaderLoading
import io.legado.app.ui.book.manga.rv.MangeAdapter
import io.legado.app.utils.immersionFullScreen
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReadMangeActivity : VMBaseActivity<ActivityMangeBinding, MangaViewModel>(),
    ReadMange.Callback {

    private var mAdapter: MangeAdapter? = null
    override val binding by viewBinding(ActivityMangeBinding::inflate)
    override val viewModel by viewModels<MangaViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        immersionFullScreen(WindowInsetsControllerCompat(window, binding.root))
        mAdapter = MangeAdapter { nextIndex ->

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
            binding.loading.isVisible = true
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
            mAdapter?.submitList(list) {}
            if (!mFirstLoading) {
                if (list.size > 1) {
                    binding.infobar.isVisible = true
                    upText()
                }
                if (ReadMange.durChapterPos != 0) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            delay(200)
                        }
                        binding.mRecyclerMange.scrollToPosition(ReadMange.durChapterPos)
                    }
                }

                if (ReadMange.durChapterPos + 2 > mAdapter!!.getCurrentList().size - 3) {
                    val nextIndex =
                        (mAdapter!!.getCurrentList().last() as ReaderLoading).mNextChapterIndex
                    ReadMange.moveToNextChapter(nextIndex)
                }
            }
            mFirstLoading = true
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
        ReadMange.saveRead()
    }

    override fun loadComplete() {
        binding.flLoading.isGone = true
    }

    override fun loadFail() {
        if (!mFirstLoading) {
            binding.loading.isGone = true
            binding.retry.isVisible = true
            mFirstLoading = true
        }

    }

    override fun onDestroy() {
        ReadMange.unregister()
        super.onDestroy()
    }
}