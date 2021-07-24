package io.legado.app.ui.association

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.constant.AppPattern
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.DialogRecyclerViewBinding
import io.legado.app.databinding.ItemSourceImportBinding
import io.legado.app.help.AppConfig
import io.legado.app.help.IntentDataHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding


/**
 * 导入书源弹出窗口
 */
class ImportBookSourceDialog : BaseDialogFragment(), Toolbar.OnMenuItemClickListener {

    companion object {

        fun start(fragmentManager: FragmentManager, source: String) {
            ImportBookSourceDialog().apply {
                arguments = Bundle().apply {
                    putString("source", source)
                }
            }.show(fragmentManager, "importBookSource")
        }

    }

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel by viewModels<ImportBookSourceViewModel>()
    private lateinit var adapter: SourcesAdapter

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_recycler_view, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setTitle(R.string.import_book_source)
        binding.rotateLoading.show()
        initMenu()
        adapter = SourcesAdapter(requireContext())
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.tvCancel.visible()
        binding.tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.tvOk.visible()
        binding.tvOk.setOnClickListener {
            val waitDialog = WaitDialog(requireContext())
            waitDialog.show()
            viewModel.importSelect {
                waitDialog.dismiss()
                dismissAllowingStateLoss()
            }
        }
        upSelectText()
        binding.tvFooterLeft.visible()
        binding.tvFooterLeft.setOnClickListener {
            val selectAll = viewModel.isSelectAll()
            viewModel.selectStatus.forEachIndexed { index, b ->
                if (b != !selectAll) {
                    viewModel.selectStatus[index] = !selectAll
                }
            }
            adapter.notifyDataSetChanged()
            upSelectText()
        }
        viewModel.errorLiveData.observe(this, {
            binding.rotateLoading.hide()
            binding.tvMsg.apply {
                text = it
                visible()
            }
        })
        viewModel.successLiveData.observe(this, {
            binding.rotateLoading.hide()
            if (it > 0) {
                adapter.setItems(viewModel.allSources)
            } else {
                binding.tvMsg.apply {
                    setText(R.string.wrong_format)
                    visible()
                }
            }
        })
        val source = arguments?.getString("source")
        if (source.isNullOrEmpty()) {
            dismiss()
            return
        }
        if (source.isAbsUrl()) {
            viewModel.importSource(source)
        } else {
            IntentDataHelp.getData<String>(source)?.let {
                viewModel.importSource(it)
                return
            }
        }
    }

    private fun upSelectText() {
        if (viewModel.isSelectAll()) {
            binding.tvFooterLeft.text = getString(
                R.string.select_cancel_count,
                viewModel.selectCount(),
                viewModel.allSources.size
            )
        } else {
            binding.tvFooterLeft.text = getString(
                R.string.select_all_count,
                viewModel.selectCount(),
                viewModel.allSources.size
            )
        }
    }

    private fun initMenu() {
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.toolBar.inflateMenu(R.menu.import_source)
        binding.toolBar.menu.findItem(R.id.menu_Keep_original_name)
            ?.isChecked = AppConfig.importKeepName
    }

    @SuppressLint("InflateParams")
    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_new_group -> {
                alert(R.string.diy_edit_source_group) {
                    val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                        val groups = linkedSetOf<String>()
                        appDb.bookSourceDao.allGroup.forEach { group ->
                            groups.addAll(group.splitNotBlank(AppPattern.splitGroupRegex))
                        }
                        editView.setFilterValues(groups.toList())
                        editView.dropDownHeight = 180.dp
                    }
                    customView {
                        alertBinding.root
                    }
                    okButton {
                        alertBinding.editView.text?.toString()?.let { group ->
                            viewModel.groupName = group
                            item.title = getString(R.string.diy_edit_source_group_title, group)
                        }
                    }
                    noButton()
                }.show()
            }
            R.id.menu_Keep_original_name -> {
                item.isChecked = !item.isChecked
                putPrefBoolean(PreferKey.importKeepName, item.isChecked)
            }
        }
        return false
    }

    inner class SourcesAdapter(context: Context) :
        RecyclerAdapter<BookSource, ItemSourceImportBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemSourceImportBinding {
            return ItemSourceImportBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemSourceImportBinding,
            item: BookSource,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                cbSourceName.isChecked = viewModel.selectStatus[holder.layoutPosition]
                cbSourceName.text = item.bookSourceName
                val localSource = viewModel.checkSources[holder.layoutPosition]
                tvSourceState.text = when {
                    localSource == null -> "新书源"
                    item.lastUpdateTime > localSource.lastUpdateTime -> "更新"
                    else -> "已存在"
                }
            }

        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemSourceImportBinding) {
            binding.apply {
                cbSourceName.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        viewModel.selectStatus[holder.layoutPosition] = isChecked
                        upSelectText()
                    }
                }
            }
        }

    }

}