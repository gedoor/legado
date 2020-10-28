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
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.data.entities.BookGroup
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.customView
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.yesButton
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.applyTint
import io.legado.app.utils.getSize
import io.legado.app.utils.getViewModel
import io.legado.app.utils.requestInputMethod
import kotlinx.android.synthetic.main.dialog_book_group_picker.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.dialog_recycler_view.tool_bar
import kotlinx.android.synthetic.main.item_group_select.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.*

class GroupSelectDialog : BaseDialogFragment(), Toolbar.OnMenuItemClickListener {

    companion object {
        const val tag = "groupSelectDialog"

        fun show(manager: FragmentManager, groupId: Long, requestCode: Int = -1) {
            val fragment = GroupSelectDialog().apply {
                val bundle = Bundle()
                bundle.putLong("groupId", groupId)
                bundle.putInt("requestCode", requestCode)
                arguments = bundle
            }
            fragment.show(manager, tag)
        }
    }

    private var requestCode: Int = -1
    private lateinit var viewModel: GroupViewModel
    private lateinit var adapter: GroupAdapter
    private var callBack: CallBack? = null
    private var groupId: Long = 0

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
        return inflater.inflate(R.layout.dialog_book_group_picker, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        tool_bar.setBackgroundColor(primaryColor)
        callBack = activity as? CallBack
        arguments?.let {
            groupId = it.getLong("groupId")
            requestCode = it.getInt("requestCode", -1)
        }
        initView()
        initData()
    }

    private fun initView() {
        tool_bar.title = getString(R.string.group_select)
        tool_bar.inflateMenu(R.menu.book_group_manage)
        tool_bar.menu.applyTint(requireContext())
        tool_bar.setOnMenuItemClickListener(this)
        adapter = GroupAdapter(requireContext())
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(VerticalDivider(requireContext()))
        recycler_view.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recycler_view)
        tv_cancel.onClick { dismiss() }
        tv_ok.setTextColor(requireContext().accentColor)
        tv_ok.onClick {
            callBack?.upGroup(requestCode, groupId)
            dismiss()
        }
    }

    private fun initData() {
        App.db.bookGroupDao().liveDataSelect().observe(viewLifecycleOwner, {
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

    private inner class GroupAdapter(context: Context) :
        SimpleRecyclerAdapter<BookGroup>(context, R.layout.item_group_select),
        ItemTouchCallback.Callback {

        private var isMoved: Boolean = false

        override fun convert(holder: ItemViewHolder, item: BookGroup, payloads: MutableList<Any>) {
            holder.itemView.apply {
                setBackgroundColor(context.backgroundColor)
                cb_group.text = item.groupName
                cb_group.isChecked = (groupId and item.groupId) > 0
            }
        }

        override fun registerListener(holder: ItemViewHolder) {
            holder.itemView.apply {
                cb_group.setOnCheckedChangeListener { buttonView, isChecked ->
                    getItem(holder.layoutPosition)?.let {
                        if (buttonView.isPressed) {
                            groupId = if (isChecked) {
                                groupId + it.groupId
                            } else {
                                groupId - it.groupId
                            }
                        }
                    }
                }
                tv_edit.onClick { getItem(holder.layoutPosition)?.let { editGroup(it) } }
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

    interface CallBack {
        fun upGroup(requestCode: Int, groupId: Long)
    }
}