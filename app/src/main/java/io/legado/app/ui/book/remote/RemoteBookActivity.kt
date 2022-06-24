package io.legado.app.ui.book.remote

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityImportBookBinding
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.book.remote.manager.RemoteBookWebDav
import io.legado.app.ui.widget.SelectActionBar
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import java.io.File

/**
 * 展示远程书籍
 * @author qianfanguojin
 * @time 2022/05/12
 */
class RemoteBookActivity : VMBaseActivity<ActivityImportBookBinding, RemoteBookViewModel>(),
    RemoteBookAdapter.CallBack,
    SelectActionBar.CallBack {
    override val binding by viewBinding(ActivityImportBookBinding::inflate)
    override val viewModel by viewModels<RemoteBookViewModel>()
    private val adapter by lazy { RemoteBookAdapter(this, this) }
    private val waitDialog by lazy { WaitDialog(this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.setTitle(R.string.remote_book)
        initView()
        initData()
        initEvent()
    }

    private fun initView() {
        binding.layTop.setBackgroundColor(backgroundColor)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.selectActionBar.setMainActionText(R.string.add_to_shelf)
        binding.selectActionBar.setCallBack(this)
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
        upPath()
    }

    private fun initEvent() {
        binding.tvGoBack.setOnClickListener {
           goBackDir()
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_remote, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> upPath()
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun revertSelection() {
        adapter.revertSelection()
    }

    override fun selectAll(selectAll: Boolean) {
        adapter.selectAll(selectAll)
    }

    override fun onClickSelectBarMainAction() {
        waitDialog.show()
        viewModel.addToBookshelf(adapter.selected) {
            adapter.selected.clear()
            adapter.notifyDataSetChanged()
            waitDialog.dismiss()
        }
    }

    override fun onBackPressed() {
        if (!goBackDir()) {
            super.onBackPressed()
        }
    }

    private fun goBackDir(): Boolean {
        if (viewModel.dirList.isEmpty()) {
            return false
        }
        viewModel.dirList.removeLastOrNull()
        upPath()
        return true
    }

    private fun upPath() {
        binding.tvGoBack.isEnabled = viewModel.dirList.isNotEmpty()
        var path = "books" + File.separator
        viewModel.dirList.forEach {
            path = path + it.filename + File.separator
        }
        binding.tvPath.text = path
        viewModel.dataCallback?.clear()
        adapter.selected.clear()
        viewModel.loadRemoteBookList(
            viewModel.dirList.lastOrNull()?.path ?: RemoteBookWebDav.rootBookUrl
        ) {
            if (it) {
                waitDialog.show()
            } else {
                waitDialog.dismiss()
            }
        }
    }

    override fun openDir(remoteBook: RemoteBook) {
        viewModel.dirList.add(remoteBook)
        upPath()
    }

    override fun upCountView() {
        binding.selectActionBar.upCountView(adapter.selected.size, adapter.checkableCount)
    }
}