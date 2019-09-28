package io.legado.app.ui.main.bookshelf

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookGroup
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.customView
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.yesButton
import io.legado.app.utils.applyTint
import io.legado.app.utils.getViewModel
import io.legado.app.utils.requestInputMethod
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.dialog_recycler_view.*
import kotlinx.android.synthetic.main.item_group_manage.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class GroupManageDialog : DialogFragment(), Toolbar.OnMenuItemClickListener {
    private lateinit var viewModel: BookshelfViewModel
    private lateinit var adapter: GroupAdapter

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = getViewModel(BookshelfViewModel::class.java)
        return inflater.inflate(R.layout.dialog_recycler_view, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    private fun initData() {
        tool_bar.title = getString(R.string.group_manage)
        tool_bar.inflateMenu(R.menu.book_group_manage)
        tool_bar.menu.applyTint(requireContext(), false)
        tool_bar.setOnMenuItemClickListener(this)
        adapter = GroupAdapter(requireContext())
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(
            DividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
        )
        recycler_view.adapter = adapter
        App.db.bookGroupDao().liveDataAll().observe(viewLifecycleOwner, Observer {
            adapter.setItems(it)
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
                        hint = "分组名称"
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
    private fun editGroup(bookGroup: BookGroup) {
        alert(title = getString(R.string.group_edit)) {
            var editText: EditText? = null
            customView {
                layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                    editText = edit_view.apply {
                        hint = "分组名称"
                        setText(bookGroup.groupName)
                    }
                }
            }
            yesButton {
                viewModel.upGroup(bookGroup.copy(groupName = editText?.text?.toString() ?: ""))
            }
            noButton()
        }.show().applyTint().requestInputMethod()
    }

    private inner class GroupAdapter(context: Context) :
        SimpleRecyclerAdapter<BookGroup>(context, R.layout.item_group_manage) {

        override fun convert(holder: ItemViewHolder, item: BookGroup, payloads: MutableList<Any>) {
            with(holder.itemView) {
                tv_group.text = item.groupName
                tv_edit.onClick { editGroup(item) }
                tv_del.onClick { viewModel.delGroup(item) }
            }
        }
    }

}