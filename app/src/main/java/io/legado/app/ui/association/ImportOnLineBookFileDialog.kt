package io.legado.app.ui.association

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.net.Uri
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.databinding.DialogRecyclerViewBinding
import io.legado.app.databinding.ItemBookFileImportBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import splitties.views.onClick


/**
 * 导入在线书籍文件弹出窗口
 */
class ImportOnLineBookFileDialog() : BaseDialogFragment(R.layout.dialog_recycler_view) {


    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel by viewModels<ImportOnLineBookFileViewModel>()
    private val adapter by lazy { BookFileAdapter(requireContext()) }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val bookUrl = arguments?.getString("bookUrl")
        val infoHtml = arguments?.getString("infoHtml")
        viewModel.initData(bookUrl, infoHtml)
        binding.toolBar.setBackgroundColor(primaryColor)
        //标题
        binding.toolBar.setTitle("导入在线书籍文件")
        binding.rotateLoading.show()
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
        binding.tvFooterLeft.visible()
        binding.tvFooterLeft.setOnClickListener {
            val selectAll = viewModel.isSelectAll
            viewModel.selectStatus.forEachIndexed { index, b ->
                if (b != !selectAll) {
                    viewModel.selectStatus[index] = !selectAll
                }
            }
            adapter.notifyDataSetChanged()
            upSelectText()
        }
        viewModel.errorLiveData.observe(this) {
            binding.rotateLoading.hide()
            binding.tvMsg.apply {
                text = it
                visible()
            }
        }
        viewModel.successLiveData.observe(this) {
            binding.rotateLoading.hide()
            if (it > 0) {
                adapter.setItems(viewModel.allBookFiles)
                upSelectText()
            }
        }
    }

    private fun upSelectText() {
        if (viewModel.isSelectAll) {
            binding.tvFooterLeft.text = getString(
                R.string.select_cancel_count,
                viewModel.selectCount,
                viewModel.allBookFiles.size
            )
        } else {
            binding.tvFooterLeft.text = getString(
                R.string.select_all_count,
                viewModel.selectCount,
                viewModel.allBookFiles.size
            )
        }
    }

    inner class BookFileAdapter(context: Context) :
        RecyclerAdapter<Triple<String, String, Boolean>
, ItemBookFileImportBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemBookFileImportBinding {
            return ItemBookFileImportBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemBookFileImportBinding,
            item: Triple<String, String, Boolean>,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                cbFileName.isChecked = viewModel.selectStatus[holder.layoutPosition] 
                cbFileName.text = item.second
                if (item.third) {
                    tvOpen.invisible()
                } else {
                    tvOpen.visible()
                }
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemBookFileImportBinding) {
            binding.apply {
                cbFileName.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        val selectFile = viewModel.allBookFiles[holder.layoutPosition]
                        if (selectFile.third) {
                            viewModel.selectStatus[holder.layoutPosition] = isChecked
                        } else {
                            toastOnUi("不支持直接导入")
                        }
                        upSelectText()
                    }
                }
                root.onClick {
                    cbFileName.isChecked = !cbFileName.isChecked
                    viewModel.selectStatus[holder.layoutPosition] = cbFileName.isChecked
                    upSelectText()
                }
                tvOpen.setOnClickListener {
                    val bookFile = viewModel.allBookFiles[holder.layoutPosition]
                    //intent解压
                    viewModel.downloadUrl(bookFile.first, bookFile.second) {
                        //openFileUri(it)
                    }
                }
            }
        }

    }

}