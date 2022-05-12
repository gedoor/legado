package io.legado.app.ui.book.remote

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.base.VMBaseActivity


import io.legado.app.databinding.ActivityRemoteBookBinding
import io.legado.app.utils.toastOnUi

import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 展示远程书籍
 * @author qianfanguojin
 * @time 2022/05/12
 */
class RemoteBookActivity : VMBaseActivity<ActivityRemoteBookBinding,RemoteBookViewModel>() {
    override val binding by viewBinding(ActivityRemoteBookBinding::inflate)
    override val viewModel by viewModels<RemoteBookViewModel>()
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        toastOnUi("远程书籍")
    }

    private fun initView() {
//        binding.layTop.setBackgroundColor(backgroundColor)
//        binding.recyclerView.layoutManager = LinearLayoutManager(this)
//        binding.recyclerView.adapter = adapter
//        binding.selectActionBar.setMainActionText(R.string.add_to_shelf)
//        binding.selectActionBar.inflateMenu(R.menu.import_book_sel)
//        binding.selectActionBar.setOnMenuItemClickListener(this)
//        binding.selectActionBar.setCallBack(this)
    }
}