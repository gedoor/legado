package io.legado.app.ui.book.remote

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.base.VMBaseActivity


import io.legado.app.databinding.ActivityRemoteBookBinding

import io.legado.app.utils.viewbindingdelegate.viewBinding
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
//        initEvent()
        initData()
//        toastOnUi("远程书籍")
    }



    private fun initView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
//        binding.layTop.setBackgroundColor(backgroundColor)
//        binding.recyclerView.layoutManager = LinearLayoutManager(this)
//        binding.recyclerView.adapter = adapter
//        binding.selectActionBar.setMainActionText(R.string.add_to_shelf)
//        binding.selectActionBar.inflateMenu(R.menu.import_book_sel)
//        binding.selectActionBar.setOnMenuItemClickListener(this)
//        binding.selectActionBar.setCallBack(this)
    }
    private fun initData() {
//        viewModel.getRemoteBooks().observe(this, {
//            adapter.submitList(it)
//        })
        binding.refreshProgressBar.isAutoLoading = true
        viewModel.loadRemoteBookList()
        launch {
            viewModel.dataFlow.collect { remoteBooks ->
                adapter.setItems(remoteBooks)
            }
        }


//        toastOnUi("1")

    }


    @SuppressLint("NotifyDataSetChanged")
    override fun addToBookshelf(remoteBook: RemoteBook) {
        viewModel.addToBookshelf(remoteBook){
            adapter.notifyDataSetChanged()
        }
    }
}