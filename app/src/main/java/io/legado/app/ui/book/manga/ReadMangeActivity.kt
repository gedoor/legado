package io.legado.app.ui.book.manga

import android.os.Bundle
import android.os.Looper
import androidx.activity.viewModels
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityMangeBinding
import io.legado.app.model.ReadMange
import io.legado.app.ui.book.manga.rv.ComicStriptRvAdapter
import io.legado.app.utils.viewbindingdelegate.viewBinding

class ReadMangeActivity : VMBaseActivity<ActivityMangeBinding, MangaViewModel>(),
    ReadMange.Callback {

    private var mAdapter: ComicStriptRvAdapter? = null
    override val binding by viewBinding(ActivityMangeBinding::inflate)
    override val viewModel by viewModels<MangaViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        mAdapter = ComicStriptRvAdapter { nextIndex, isNext ->

        }
        binding.mRecyclerMange.adapter = mAdapter
        ReadMange.register(this)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        Looper.myQueue().addIdleHandler {
            viewModel.initData(intent)
            false
        }
    }

    override fun loadContentFinish(list: MutableList<Any>) {
        mAdapter?.submitList(list) {

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ReadMange.unregister(this)
    }
}