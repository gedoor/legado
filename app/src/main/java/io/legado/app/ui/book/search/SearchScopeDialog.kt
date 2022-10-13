package io.legado.app.ui.book.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.DialogSearchScopeBinding
import io.legado.app.databinding.ItemCheckBoxBinding
import io.legado.app.databinding.ItemRadioButtonBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchScopeDialog : BaseDialogFragment(R.layout.dialog_search_scope) {

    private val binding by viewBinding(DialogSearchScopeBinding::bind)
    val callback: Callback get() = parentFragment as? Callback ?: activity as Callback
    var groups: List<String>? = null
    var sources: List<BookSource>? = null

    val adapter by lazy {
        RecyclerAdapter()
    }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.8f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.recyclerView.adapter = adapter
        initOtherView()
        initData()
        upData()
    }


    private fun initOtherView() {
        binding.rgScope.setOnCheckedChangeListener { group, checkedId ->
            upData()
        }
        binding.tvCancel.setOnClickListener {
            dismiss()
        }
        binding.tvAllSource.setOnClickListener {
            callback.onSearchScopeOk(SearchScope(""))
            dismiss()
        }
        binding.tvOk.setOnClickListener {
            if (binding.rbGroup.isChecked) {
                callback.onSearchScopeOk(SearchScope(adapter.selectGroups))
            } else {
                val selectSource = adapter.selectSource
                if (selectSource != null) {
                    callback.onSearchScopeOk(SearchScope(selectSource))
                } else {
                    callback.onSearchScopeOk(SearchScope(""))
                }
            }
            dismiss()
        }
    }

    private fun initData() {
        launch {
            withContext(IO) {
                groups = appDb.bookSourceDao.allGroups
                sources = appDb.bookSourceDao.allEnabled
            }
            upData()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun upData() {
        launch {
            adapter.notifyDataSetChanged()
        }
    }

    inner class RecyclerAdapter : RecyclerView.Adapter<ItemViewHolder>() {

        val selectGroups = arrayListOf<String>()
        var selectSource: BookSource? = null

        override fun getItemViewType(position: Int): Int {
            return if (binding.rbSource.isChecked) {
                1
            } else {
                0
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            return if (viewType == 1) {
                ItemViewHolder(ItemRadioButtonBinding.inflate(layoutInflater, parent, false))
            } else {
                ItemViewHolder(ItemCheckBoxBinding.inflate(layoutInflater, parent, false))
            }
        }

        override fun onBindViewHolder(
            holder: ItemViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                when (holder.binding) {
                    is ItemCheckBoxBinding -> {
                        groups?.get(position)?.let {
                            holder.binding.checkBox.isChecked = selectGroups.contains(it)
                            holder.binding.checkBox.text = it
                        }
                    }
                    is ItemRadioButtonBinding -> {
                        sources?.get(position)?.let {
                            holder.binding.radioButton.isChecked = selectSource == it
                            holder.binding.radioButton.text = it.bookSourceName
                        }
                    }
                }
            }
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            when (holder.binding) {
                is ItemCheckBoxBinding -> {
                    groups?.get(position)?.let {
                        holder.binding.checkBox.isChecked = selectGroups.contains(it)
                        holder.binding.checkBox.text = it
                        holder.binding.checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                            if (buttonView.isPressed) {
                                if (isChecked) {
                                    selectGroups.add(it)
                                } else {
                                    selectGroups.remove(it)
                                }
                                notifyItemRangeChanged(0, itemCount, "up")
                            }
                        }
                    }
                }
                is ItemRadioButtonBinding -> {
                    sources?.get(position)?.let {
                        holder.binding.radioButton.isChecked = selectSource == it
                        holder.binding.radioButton.text = it.bookSourceName
                        holder.binding.radioButton.setOnCheckedChangeListener { buttonView, isChecked ->
                            if (buttonView.isPressed) {
                                if (isChecked) {
                                    selectSource = it
                                }
                                notifyItemRangeChanged(0, itemCount, "up")
                            }
                        }
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return if (binding.rbSource.isChecked) {
                sources?.size ?: 0
            } else {
                groups?.size ?: 0
            }
        }

    }

    interface Callback {

        /**
         * 搜索范围确认
         */
        fun onSearchScopeOk(searchScope: SearchScope)

    }

}