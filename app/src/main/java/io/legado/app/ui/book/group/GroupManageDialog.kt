package io.legado.app.ui.book.group

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isGone
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookGroup
import io.legado.app.lib.dialogs.*
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.dialog_recycler_view.*
import kotlinx.android.synthetic.main.item_group_manage.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.*
import kotlin.collections.ArrayList

class GroupManageDialog : BaseDialogFragment(), Toolbar.OnMenuItemClickListener {
    private lateinit var viewModel: GroupViewModel
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
        viewModel = getViewModel(GroupViewModel::class.java)
        return inflater.inflate(R.layout.dialog_recycler_view, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        tool_bar.setBackgroundColor(primaryColor)
        tool_bar.title = getString(R.string.group_manage)
        initView()
        initData()
        initMenu()
    }

    private fun initView() {
        adapter = GroupAdapter(requireContext())
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(VerticalDivider(requireContext()))
        recycler_view.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recycler_view)
        tv_ok.setTextColor(requireContext().accentColor)
        tv_ok.visible()
        tv_ok.onClick { dismiss() }
    }

    private fun initData() {
        App.db.bookGroupDao().liveDataAll().observe(viewLifecycleOwner, {
            val diffResult =
                DiffUtil.calculateDiff(GroupDiffCallBack(ArrayList(adapter.getItems()), it))
            adapter.setItems(it, diffResult)
        })
    }

    private fun initMenu() {
        tool_bar.setOnMenuItemClickListener(this)
        tool_bar.inflateMenu(R.menu.book_group_manage)
        tool_bar.menu.applyTint(requireContext())
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
    private fun editGroup(bookGroup: BookGroup) {
        alert(title = getString(R.string.group_edit)) {
            var editText: EditText? = null
            customView {
                layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                    editText = edit_view.apply {
                        setHint(R.string.group_name)
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

    private fun deleteGroup(bookGroup: BookGroup) {
        alert(R.string.delete, R.string.sure_del) {
            okButton {
                viewModel.delGroup(bookGroup)
            }
            noButton()
        }.show().applyTint()
    }

    private class GroupDiffCallBack(
        private val oldItems: List<BookGroup>,
        private val newItems: List<BookGroup>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldItems.size
        }

        override fun getNewListSize(): Int {
            return newItems.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            return oldItem.groupId == newItem.groupId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            return oldItem.groupName == newItem.groupName
                    && oldItem.show == newItem.show
        }

    }

    private inner class GroupAdapter(context: Context) :
        SimpleRecyclerAdapter<BookGroup>(context, R.layout.item_group_manage),
        ItemTouchCallback.Callback {

        private var isMoved = false

        override fun convert(holder: ItemViewHolder, item: BookGroup, payloads: MutableList<Any>) {
            holder.itemView.apply {
                setBackgroundColor(context.backgroundColor)
                tv_group.text = item.getManageName(context)
                sw_show.isChecked = item.show
                tv_del.isGone = item.groupId < 0
                sw_show.isGone = item.groupId >= 0
            }
        }

        override fun registerListener(holder: ItemViewHolder) {
            holder.itemView.apply {
                tv_edit.onClick { getItem(holder.layoutPosition)?.let { editGroup(it) } }
                tv_del.onClick { getItem(holder.layoutPosition)?.let { deleteGroup(it) } }
                sw_show.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        getItem(holder.layoutPosition)?.let {
                            viewModel.upGroup(it.copy(show = isChecked))
                        }
                    }
                }
            }
        }

        override fun onMove(srcPosition: Int, targetPosition: Int): Boolean {
            Collections.swap(getItems(), srcPosition, targetPosition)
            notifyItemMoved(srcPosition, targetPosition)
            isMoved = true
            return true
        }

        override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            if (isMoved) {
                for ((index, item) in getItems().withIndex()) {
                    item.order = index + 1
                }
                viewModel.upGroup(*getItems().toTypedArray())
            }
            isMoved = false
        }
    }

}