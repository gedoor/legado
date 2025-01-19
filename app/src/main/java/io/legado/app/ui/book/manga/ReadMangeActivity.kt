package io.legado.app.ui.book.manga

import android.os.Bundle
import android.os.Looper
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.core.view.isVisible
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityMangeBinding
import io.legado.app.model.ReadMange
import io.legado.app.model.recyclerView.ReaderLoading
import io.legado.app.ui.book.manga.rv.MangeAdapter
import io.legado.app.utils.DebugLog
import io.legado.app.utils.viewbindingdelegate.viewBinding

class ReadMangeActivity : VMBaseActivity<ActivityMangeBinding, MangaViewModel>(),
    ReadMange.Callback {

    private var mAdapter: MangeAdapter? = null
    override val binding by viewBinding(ActivityMangeBinding::inflate)
    override val viewModel by viewModels<MangaViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        mAdapter = MangeAdapter { nextIndex, isNext ->

        }
        binding.mRecyclerMange.adapter = mAdapter
        ReadMange.register(this)
        binding.mRecyclerMange.setPreScrollListener { _, dy, position ->
            ReadMange.durChapterPos = position
            if (dy > 0 && position + 2 > mAdapter!!.getCurrentList().size - 3) {
                if (mAdapter?.getCurrentList()?.last() is ReaderLoading) {
                    val nextIndex = (mAdapter!!.getCurrentList().last() as ReaderLoading).mNextIndex
                    ReadMange.moveToNextChapter(nextIndex)
                }
            }
        }
        binding.retry.setOnClickListener {
            binding.loading.isVisible = true
            binding.retry.isGone = true
            ReadMange.mFirstLoading = false
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
            mAdapter?.submitList(list)
            if (ReadMange.durChapterPos != 0) {
                binding.mRecyclerMange.scrollToPosition(ReadMange.durChapterPos)
            }

            if (!ReadMange.mFirstLoading && ReadMange.durChapterPos + 2 > mAdapter!!.getCurrentList().size - 3) {
                val nextIndex = (mAdapter!!.getCurrentList().last() as ReaderLoading).mNextIndex
                ReadMange.moveToNextChapter(nextIndex)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        ReadMange.saveRead()
    }

    override fun loadComplete() {
        binding.flLoading.isGone = true
    }

    override fun loadFail() {
        DebugLog.e("tag", "执行一次")
        binding.loading.isGone = true
        binding.retry.isVisible = true
    }

    override fun onDestroy() {
        ReadMange.unregister(this)
        super.onDestroy()
    }
}