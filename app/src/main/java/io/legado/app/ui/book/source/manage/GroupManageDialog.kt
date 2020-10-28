package io.legado.app.ui.book.source.manage

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.constant.AppPattern
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.customView
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.yesButton
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.dialog_recycler_view.*
import kotlinx.android.synthetic.main.item_group_manage.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class GroupManageDialog : DialogFragment(), Toolbar.OnMenuItemClickListener {
    private lateinit var viewModel: BookSourceViewModel
    private lateinit var adapter: GroupAdapter

    override fun onStart() {
        super.onStart()
        val dm = requireActivity().getSize()
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = getViewModelOfActivity(BookSourceViewModel::class.java)
        return inflater.inflate(R.layout.dialog_recycler_view, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(backgroundColor)
        tool_bar.setBackgroundColor(primaryColor)
        tool_bar.title = getString(R.string.group_manage)
        tool_bar.inflateMenu(R.menu.group_manage)
        tool_bar.menu.applyTint(requireContext())
        tool_bar.setOnMenuItemClickListener(this)
        adapter = GroupAdapter(requireContext())
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(VerticalDivider(requireContext()))
        recycler_view.adapter = adapter
        initData()
    }

    private fun initData() {
        App.db.bookSourceDao().liveGroup().observe(viewLifecycleOwner, {
            val groups = linkedSetOf<String>()
            it.map { group ->
                groups.addAll(group.splitNotBlank(AppPattern.splitGroupRegex))
            }
            adapter.setItems(groups.toList())
        })
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_add -> addGroup()
        }
        return true
    }

    @SuppressLint("InflateParams")
    private fun addGroup() {
        alert(title = getString(R.string.add_group)) {
            var editText: EditText? = null
            customView {
                layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                    editText = edit_view.apply {
                        setHint(R.string.group_name)
                    }
                }
            }
            yesButton {
                editText?.text?.toString()?.let {
                    if (it.isNotBlank()) {
                        viewModel.addGroup(it)
                    }
                }
            }
            noButton()
        }.show().applyTint().requestInputMethod()
    }

    @SuppressLint("InflateParams")
    private fun editGroup(group: String) {
        alert(title = getString(R.string.group_edit)) {
            var editText: EditText? = null
            customView {
                layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                    editText = edit_view.apply {
                        setHint(R.string.group_name)
                        setText(group)
                    }
                }
            }
            yesButton {
                viewModel.upGroup(group, editText?.text?.toString())
            }
            noButton()
        }.show().applyTint().requestInputMethod()
    }

    private inner class GroupAdapter(context: Context) :
        SimpleRecyclerAdapter<String>(context, R.layout.item_group_manage) {

        override fun convert(holder: ItemViewHolder, item: String, payloads: MutableList<Any>) {
            with(holder.itemView) {
                setBackgroundColor(context.backgroundColor)
                tv_group.text = item
            }
        }

        override fun registerListener(holder: ItemViewHolder) {
            holder.itemView.apply {
                tv_edit.onClick {
                    getItem(holder.layoutPosition)?.let {
                        editGroup(it)
                    }
                }
                tv_del.onClick {
                    getItem(holder.layoutPosition)?.let { viewModel.delGroup(it) }
                }
            }
        }
    }

}