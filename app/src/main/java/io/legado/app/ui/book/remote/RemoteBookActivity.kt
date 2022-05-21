package io.legado.app.ui.book.remote

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity


import io.legado.app.databinding.ActivityRemoteBookBinding
import io.legado.app.utils.toastOnUi

import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

/**
 * 展示远程书籍
 * @author qianfanguojin
 * @time 2022/05/12
 */
class RemoteBookActivity : VMBaseActivity<ActivityRemoteBookBinding,RemoteBookViewModel>(),
    RemoteBookAdapter.CallBack {
    override val binding by viewBinding(ActivityRemoteBookBinding::inflate)
    override val viewModel by viewModels<RemoteBookViewModel>()
    private val adapter by lazy { RemoteBookAdapter(this, this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    private fun initData() {
        binding.refreshProgressBar.isAutoLoading = true
        launch {
            viewModel.dataFlow.conflate().collect { remoteBooks ->
                binding.refreshProgressBar.isAutoLoading = false
                binding.tvEmptyMsg.isGone = remoteBooks.isNotEmpty()
                adapter.setItems(remoteBooks)
            }
        }
        viewModel.loadRemoteBookList()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun addToBookshelf(remoteBook: RemoteBook) {
        viewModel.addToBookshelf(remoteBook){
            toastOnUi(getString(R.string.download_book_fail))
            adapter.notifyDataSetChanged()
        }
    }
}