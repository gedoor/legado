package io.legado.app.ui.association

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
import io.legado.app.help.SourceHelp
import io.legado.app.utils.getSize
import io.legado.app.utils.getViewModelOfActivity
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.dialog_recycler_view.*
import kotlinx.android.synthetic.main.item_source_import.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class ImportRssSourcesDialog : BaseDialogFragment(), Toolbar.OnMenuItemClickListener {

    val viewModel: ImportRssSourceViewModel
        get() =
            getViewModelOfActivity(ImportRssSourceViewModel::class.java)
    lateinit var adapter: SourcesAdapter

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
        tool_bar.title = getString(R.string.import_rss_source)
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

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
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
        val selectSource = arrayListOf<RssSource>()
        adapter.selectStatus.forEachIndexed { index, b ->
            if (b) {
                selectSource.add(adapter.getItem(index)!!)
            }
        }
        SourceHelp.insertRssSource(*selectSource.toTypedArray())
    }


    class SourcesAdapter(context: Context) :
        SimpleRecyclerAdapter<RssSource>(context, R.layout.item_source_import) {

        lateinit var sourceCheckState: ArrayList<Boolean>
        lateinit var selectStatus: ArrayList<Boolean>

        override fun convert(holder: ItemViewHolder, item: RssSource, payloads: MutableList<Any>) {
            holder.itemView.apply {
                cb_source_name.isChecked = selectStatus[holder.layoutPosition]
                cb_source_name.text = item.sourceName
                tv_source_state.text = if (sourceCheckState[holder.layoutPosition]) {
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
                        selectStatus[holder.layoutPosition] = isChecked
                    }
                }
            }
        }
    }


}
