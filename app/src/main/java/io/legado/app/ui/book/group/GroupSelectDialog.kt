package io.legado.app.ui.book.group

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookGroup
import io.legado.app.databinding.DialogBookGroupPickerBinding
import io.legado.app.databinding.ItemGroupSelectBinding
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.applyTint
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.windowSize
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


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

    private val binding by viewBinding(DialogBookGroupPickerBinding::bind)
    private var requestCode: Int = -1
    private val viewModel: GroupViewModel by viewModels()
    private lateinit var adapter: GroupAdapter
    private var callBack: CallBack? = null
    private var groupId: Long = 0

    override fun onStart() {
        super.onStart()
        val dm = requireActivity().windowSize
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_book_group_picker, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        callBack = activity as? CallBack
        arguments?.let {
            groupId = it.getLong("groupId")
            requestCode = it.getInt("requestCode", -1)
        }
        initView()
        initData()
    }

    private fun initView() {
        binding.toolBar.title = getString(R.string.group_select)
        binding.toolBar.inflateMenu(R.menu.book_group_manage)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
        adapter = GroupAdapter(requireContext())
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
        binding.tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.tvOk.setTextColor(requireContext().accentColor)
        binding.tvOk.setOnClickListener {
            callBack?.upGroup(requestCode, groupId)
            dismissAllowingStateLoss()
        }
    }

    private fun initData() {
        launch {
            appDb.bookGroupDao.flowSelect().collect {
                adapter.setItems(it)
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_add -> GroupEditDialog.start(childFragmentManager)
        }
        return true
    }

    private inner class GroupAdapter(context: Context) :
        RecyclerAdapter<BookGroup, ItemGroupSelectBinding>(context),
        ItemTouchCallback.Callback {

        private var isMoved: Boolean = false

        override fun getViewBinding(parent: ViewGroup): ItemGroupSelectBinding {
            return ItemGroupSelectBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemGroupSelectBinding,
            item: BookGroup,
            payloads: MutableList<Any>
        ) {
            binding.run {
                root.setBackgroundColor(context.backgroundColor)
                cbGroup.text = item.groupName
                cbGroup.isChecked = (groupId and item.groupId) > 0
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemGroupSelectBinding) {
            binding.run {
                cbGroup.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        getItem(holder.layoutPosition)?.let {
                            groupId = if (isChecked) {
                                groupId + it.groupId
                            } else {
                                groupId - it.groupId
                            }
                        }
                    }
                }
                tvEdit.setOnClickListener {
                    GroupEditDialog.start(childFragmentManager, getItem(holder.layoutPosition))
                }
            }
        }

        override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
            swapItem(srcPosition, targetPosition)
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