package io.legado.app.ui.association

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.RssSource
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.customView
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.ui.widget.text.AutoCompleteTextView
import io.legado.app.utils.applyTint
import io.legado.app.utils.getViewModelOfActivity
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.dialog_recycler_view.*
import kotlinx.android.synthetic.main.item_source_import.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

/**
 * 导入rss源弹出窗口
 */
class ImportRssSourcesDialog : BaseDialogFragment(), Toolbar.OnMenuItemClickListener {

    val viewModel: ImportRssSourceViewModel
        get() =
            getViewModelOfActivity(ImportRssSourceViewModel::class.java)
    lateinit var adapter: SourcesAdapter

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
        tool_bar.title = getString(R.string.import_rss_source)
        initMenu()
        adapter = SourcesAdapter(requireContext())
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.adapter = adapter
        adapter.setItems(viewModel.allSources)
        tv_cancel.visible()
        tv_cancel.onClick {
            dismiss()
        }
        tv_ok.visible()
        tv_ok.onClick {
            viewModel.importSelect {
                dismiss()
            }
        }
    }

    private fun initMenu() {
        tool_bar.setOnMenuItemClickListener(this)
        tool_bar.inflateMenu(R.menu.import_source)
    }

    @SuppressLint("InflateParams")
    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_new_group -> {
                alert(R.string.diy_edit_source_group) {
                    var editText: AutoCompleteTextView? = null
                    customView {
                        layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                            editText = edit_view
                        }
                    }
                    okButton {
                        editText?.text?.toString()?.let { group ->
                            viewModel.groupName = group
                            item.title = getString(R.string.diy_edit_source_group_title, group)
                        }
                    }
                    noButton { }
                }.show().applyTint()
            }
            R.id.menu_select_all -> {
                viewModel.selectStatus.forEachIndexed { index, b ->
                    if (!b) {
                        viewModel.selectStatus[index] = true
                    }
                }
                adapter.notifyDataSetChanged()
            }
            R.id.menu_un_select_all -> {
                viewModel.selectStatus.forEachIndexed { index, b ->
                    if (b) {
                        viewModel.selectStatus[index] = false
                    }
                }
                adapter.notifyDataSetChanged()
            }
        }
        return false
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.finish()
    }

    inner class SourcesAdapter(context: Context) :
        SimpleRecyclerAdapter<RssSource>(context, R.layout.item_source_import) {

        override fun convert(holder: ItemViewHolder, item: RssSource, payloads: MutableList<Any>) {
            holder.itemView.apply {
                cb_source_name.isChecked = viewModel.selectStatus[holder.layoutPosition]
                cb_source_name.text = item.sourceName
                tv_source_state.text = if (viewModel.sourceCheckState[holder.layoutPosition]) {
                    "已存在"
                } else {
                    "新订阅源"
                }

            }
        }

        override fun registerListener(holder: ItemViewHolder) {
            holder.itemView.apply {
                cb_source_name.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        viewModel.selectStatus[holder.layoutPosition] = isChecked
                    }
                }
            }
        }
    }


}
