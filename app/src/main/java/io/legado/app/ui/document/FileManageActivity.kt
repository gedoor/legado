package io.legado.app.ui.document

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.databinding.ActivityFileManageBinding
import io.legado.app.databinding.ItemFileBinding
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.document.adapter.PathAdapter
import io.legado.app.ui.document.utils.FilePickerIcon
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.ConvertUtils
import io.legado.app.utils.FileDoc
import io.legado.app.utils.applyTint
import io.legado.app.utils.list
import io.legado.app.utils.viewbindingdelegate.viewBinding

class FileManageActivity : VMBaseActivity<ActivityFileManageBinding, FileManageViewModel>(),
    PathAdapter.CallBack {

    override val binding by viewBinding(ActivityFileManageBinding::inflate)
    override val viewModel by viewModels<FileManageViewModel>()
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private val pathAdapter by lazy {
        PathAdapter(this, this)
    }
    private val fileAdapter by lazy {
        FileAdapter()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initSearchView()
        initData()
    }

    private fun initView() {
        binding.rvPath.layoutManager = LinearLayoutManager(this)
        binding.rvPath.addItemDecoration(VerticalDivider(this))
        binding.rvPath.adapter = pathAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = fileAdapter
    }

    private fun initSearchView() {
        searchView.applyTint(primaryTextColor)
        searchView.queryHint = getString(R.string.screen) + " • " + getString(R.string.file_manage)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                return false
            }
        })
    }

    private fun initData() {
        viewModel.rootDoc?.list()?.let {
            fileAdapter.setItems(it)
        }
    }

    override fun onPathClick(position: Int) {

    }

    inner class FileAdapter : RecyclerAdapter<FileDoc, ItemFileBinding>(this@FileManageActivity) {
        private val upIcon = ConvertUtils.toDrawable(FilePickerIcon.getUpDir())!!
        private val folderIcon = ConvertUtils.toDrawable(FilePickerIcon.getFolder())!!
        private val fileIcon = ConvertUtils.toDrawable(FilePickerIcon.getFile())!!

        override fun getViewBinding(parent: ViewGroup): ItemFileBinding {
            return ItemFileBinding.inflate(inflater, parent, false)
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemFileBinding) {

        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemFileBinding,
            item: FileDoc,
            payloads: MutableList<Any>
        ) {
            if (!item.isDir) {
                binding.imageView.setImageDrawable(fileIcon)
            } else if (holder.layoutPosition == 0 && viewModel.subDocs.isNotEmpty()) {
                binding.imageView.setImageDrawable(upIcon)
            } else {
                binding.imageView.setImageDrawable(folderIcon)
            }
            binding.textView.text = item.name
        }


    }

}