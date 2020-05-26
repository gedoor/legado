package io.legado.app.ui.book.group

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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.constant.Theme
import io.legado.app.data.entities.BookGroup
import io.legado.app.help.AppConfig
import io.legado.app.help.ItemTouchCallback
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.customView
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.yesButton
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.applyTint
import io.legado.app.utils.getViewModel
import io.legado.app.utils.requestInputMethod
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.dialog_recycler_view.*
import kotlinx.android.synthetic.main.item_group_manage.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.*
import kotlin.collections.ArrayList

class GroupManageDialog : DialogFragment(), Toolbar.OnMenuItemClickListener {
    private lateinit var viewModel: GroupViewModel
    private lateinit var adapter: GroupAdapter
    private val callBack: CallBack? get() = parentFragment as? CallBack

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
        viewModel = getViewModel(GroupViewModel::class.java)
        return inflater.inflate(R.layout.dialog_recycler_view, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tool_bar.title = getString(R.string.group_manage)
        initData()
        initMenu()
    }

    private fun initData() {
        adapter = GroupAdapter(requireContext())
        recycler_view.isEnableScroll = !AppConfig.isEInkMode
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(VerticalDivider(requireContext()))
        recycler_view.adapter = adapter
        tv_ok.setTextColor(requireContext().accentColor)
        tv_ok.visible()
        tv_ok.onClick { dismiss() }
        App.db.bookGroupDao().liveDataAll().observe(viewLifecycleOwner, Observer {
            val diffResult =
                DiffUtil.calculateDiff(GroupDiffCallBack(ArrayList(adapter.getItems()), it))
            adapter.setItems(it, diffResult)
        })
        val itemTouchCallback = ItemTouchCallback()
        itemTouchCallback.onItemTouchCallbackListener = adapter
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recycler_view)
    }

    private fun initMenu() {
        tool_bar.setOnMenuItemClickListener(this)
        tool_bar.inflateMenu(R.menu.book_group_manage)
        tool_bar.menu.let {
            it.applyTint(requireContext(), Theme.getTheme())
            it.findItem(R.id.menu_group_all)
                .isChecked = AppConfig.bookGroupAllShow
            it.findItem(R.id.menu_group_local)
                .isChecked = AppConfig.bookGroupLocalShow
            it.findItem(R.id.menu_group_audio)
                .isChecked = AppConfig.bookGroupAudioShow
            it.findItem(R.id.menu_group_none)
                .isChecked = AppConfig.bookGroupNoneShow
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_add -> addGroup()
            R.id.menu_group_all -> {
                item.isChecked = !item.isChecked
                AppConfig.bookGroupAllShow = item.isChecked
                callBack?.upGroup()
            }
            R.id.menu_group_local -> {
                item.isChecked = !item.isChecked
                AppConfig.bookGroupLocalShow = item.isChecked
                callBack?.upGroup()
            }
            R.id.menu_group_audio -> {
                item.isChecked = !item.isChecked
                AppConfig.bookGroupAudioShow = item.isChecked
                callBack?.upGroup()
            }
            R.id.menu_group_none -> {
                item.isChecked = !item.isChecked
                AppConfig.bookGroupNoneShow = item.isChecked
            }
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
            return true
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            return oldItem.groupName == newItem.groupName
        }

    }

    private inner class GroupAdapter(context: Context) :
        SimpleRecyclerAdapter<BookGroup>(context, R.layout.item_group_manage),
        ItemTouchCallback.OnItemTouchCallbackListener {

        private var isMoved = false

        override fun convert(holder: ItemViewHolder, item: BookGroup, payloads: MutableList<Any>) {
            holder.itemView.apply {
                tv_group.text = item.groupName

            }
        }

        override fun registerListener(holder: ItemViewHolder) {
            holder.itemView.apply {
                tv_edit.onClick { getItem(holder.layoutPosition)?.let { editGroup(it) } }
                tv_del.onClick { getItem(holder.layoutPosition)?.let { viewModel.delGroup(it) } }
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
        fun upGroup()
    }
}