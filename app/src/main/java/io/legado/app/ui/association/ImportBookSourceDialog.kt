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
import io.legado.app.data.entities.BookSource
import io.legado.app.help.SourceHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.customView
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.ui.widget.text.AutoCompleteTextView
import io.legado.app.utils.applyTint
import io.legado.app.utils.getSize
import io.legado.app.utils.getViewModelOfActivity
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.dialog_recycler_view.*
import kotlinx.android.synthetic.main.item_source_import.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class ImportBookSourceDialog : BaseDialogFragment(), Toolbar.OnMenuItemClickListener {

    val viewModel: ImportBookSourceViewModel
        get() =
            getViewModelOfActivity(ImportBookSourceViewModel::class.java)
    lateinit var adapter: SourcesAdapter
    private var _groupName: String? = null

    override fun onStart() {
        super.onStart()
        val dm = requireActivity().getSize()
        dialog?.window?.setLayout(
            (dm.widthPixels * 0.9).toInt(),
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
        tool_bar.setTitle(R.string.import_book_source)
        initMenu()
        adapter = SourcesAdapter(requireContext())
        val allSources = viewModel.allSources
        adapter.sourceCheckState = viewModel.sourceCheckState
        adapter.selectStatus = viewModel.selectStatus

        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.adapter = adapter
        adapter.setItems(allSources)
        tv_cancel.visible()
        tv_cancel.onClick {
            dismiss()
        }
        tv_ok.visible()
        tv_ok.onClick {
            importSelect()
            dismiss()
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
                            _groupName = group
                            item.title = getString(R.string.diy_edit_source_group_title, _groupName)
                        }
                    }
                    noButton { }
                }.show().applyTint()
            }
            R.id.menu_select_all -> {
                adapter.selectStatus.forEachIndexed { index, b ->
                    if (!b) {
                        adapter.selectStatus[index] = true
                    }
                }
                adapter.notifyDataSetChanged()
            }
            R.id.menu_un_select_all -> {
                adapter.selectStatus.forEachIndexed { index, b ->
                    if (b) {
                        adapter.selectStatus[index] = false
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

    private fun importSelect() {
        val selectSource = arrayListOf<BookSource>()
        adapter.selectStatus.forEachIndexed { index, b ->
            if (_groupName != null) {
                adapter.getItem(index)!!.bookSourceGroup = _groupName
            }
            if (b) {
                selectSource.add(adapter.getItem(index)!!)
            }
        }
        SourceHelp.insertBookSource(*selectSource.toTypedArray())
    }


    class SourcesAdapter(context: Context) :
        SimpleRecyclerAdapter<BookSource>(context, R.layout.item_source_import) {

        lateinit var sourceCheckState: ArrayList<Boolean>
        lateinit var selectStatus: ArrayList<Boolean>

        override fun convert(holder: ItemViewHolder, item: BookSource, payloads: MutableList<Any>) {
            holder.itemView.apply {
                cb_source_name.isChecked = selectStatus[holder.layoutPosition]
                cb_source_name.text = item.bookSourceName
                tv_source_state.text = if (sourceCheckState[holder.layoutPosition]) {
                    "已存在"
                } else {
                    "新书源"
                }

            }
        }

        override fun registerListener(holder: ItemViewHolder) {
            holder.itemView.apply {
                cb_source_name.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        selectStatus[holder.layoutPosition] = isChecked
                    }
                }
            }
        }

    }

}
