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
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.utils.FileDoc
import io.legado.app.utils.applyTint
import io.legado.app.utils.viewbindingdelegate.viewBinding

class FileManageActivity : VMBaseActivity<ActivityFileManageBinding, FileManageViewModel>() {


    override val binding by viewBinding(ActivityFileManageBinding::inflate)
    override val viewModel by viewModels<FileManageViewModel>()
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private val adapter by lazy {
        FileAdapter()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initSearchView()
    }

    private fun initView() {
        binding.layTop.setBackgroundColor(backgroundColor)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
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


    inner class FileAdapter : RecyclerAdapter<FileDoc, ItemFileBinding>(this@FileManageActivity) {


        override fun getViewBinding(parent: ViewGroup): ItemFileBinding {
            TODO("Not yet implemented")
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemFileBinding) {
            TODO("Not yet implemented")
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemFileBinding,
            item: FileDoc,
            payloads: MutableList<Any>
        ) {
            TODO("Not yet implemented")
        }


    }

}