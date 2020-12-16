package io.legado.app.ui.book.read.config

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.DialogTocRegexBinding
import io.legado.app.databinding.DialogTocRegexEditBinding
import io.legado.app.databinding.ItemTocRegexBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.*


class TocRegexDialog : BaseDialogFragment(), Toolbar.OnMenuItemClickListener {
    private val importTocRuleKey = "tocRuleUrl"
    private lateinit var adapter: TocRegexAdapter
    private var tocRegexLiveData: LiveData<List<TxtTocRule>>? = null
    var selectedName: String? = null
    private var durRegex: String? = null
    lateinit var viewModel: TocRegexViewModel
    private val binding by viewBinding(DialogTocRegexBinding::bind)

    override fun onStart() {
        super.onStart()
        val dm = requireActivity().getSize()
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.8).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = getViewModel(TocRegexViewModel::class.java)
        return inflater.inflate(R.layout.dialog_toc_regex, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        durRegex = arguments?.getString("tocRegex")
        binding.toolBar.setTitle(R.string.txt_toc_regex)
        binding.toolBar.inflateMenu(R.menu.txt_toc_regex)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
        initView()
        initData()
    }

    private fun initView() = with(binding) {
        adapter = TocRegexAdapter(requireContext())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recyclerView)
        tvCancel.onClick {
            dismiss()
        }
        tvOk.onClick {
            adapter.getItems().forEach { tocRule ->
                if (selectedName == tocRule.name) {
                    val callBack = activity as? CallBack
                    callBack?.onTocRegexDialogResult(tocRule.rule)
                    dismiss()
                    return@onClick
                }
            }
        }
    }

    private fun initData() {
        tocRegexLiveData?.removeObservers(viewLifecycleOwner)
        tocRegexLiveData = App.db.txtTocRule.observeAll()
        tocRegexLiveData?.observe(viewLifecycleOwner, { tocRules ->
            initSelectedName(tocRules)
            adapter.setItems(tocRules)
        })
    }

    private fun initSelectedName(tocRules: List<TxtTocRule>) {
        if (selectedName == null && durRegex != null) {
            tocRules.forEach {
                if (durRegex == it.rule) {
                    selectedName = it.name
                    return@forEach
                }
            }
            if (selectedName == null) {
                selectedName = ""
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_add -> editRule()
            R.id.menu_default -> viewModel.importDefault()
            R.id.menu_import -> showImportDialog()
        }
        return false
    }

    @SuppressLint("InflateParams")
    private fun showImportDialog() {
        val aCache = ACache.get(requireContext(), cacheDir = false)
        val defaultUrl = "https://gitee.com/fisher52/YueDuJson/raw/master/myTxtChapterRule.json"
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importTocRuleKey)
            ?.splitNotBlank(",")
            ?.toMutableList()
            ?: mutableListOf()
        if (!cacheUrls.contains(defaultUrl)) {
            cacheUrls.add(0, defaultUrl)
        }
        requireContext().alert(titleResource = R.string.import_book_source_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater)
            alertBinding.apply {
                editView.setFilterValues(cacheUrls)
                editView.delCallBack = {
                    cacheUrls.remove(it)
                    aCache.put(importTocRuleKey, cacheUrls.joinToString(","))
                }
            }
            customView = alertBinding.root
            okButton {
                val text = alertBinding.editView.text?.toString()
                text?.let {
                    if (!cacheUrls.contains(it)) {
                        cacheUrls.add(0, it)
                        aCache.put(importTocRuleKey, cacheUrls.joinToString(","))
                    }
                    Snackbar.make(binding.toolBar, R.string.importing, Snackbar.LENGTH_INDEFINITE)
                        .show()
                    viewModel.importOnLine(it) { msg ->
                        binding.toolBar.snackbar(msg)
                    }
                }
            }
            cancelButton()
        }.show()
    }

    @SuppressLint("InflateParams")
    private fun editRule(rule: TxtTocRule? = null) {
        val tocRule = rule?.copy() ?: TxtTocRule()
        requireContext().alert(titleResource = R.string.txt_toc_regex) {
            val alertBinding = DialogTocRegexEditBinding.inflate(layoutInflater)
            alertBinding.apply {
                tvRuleName.setText(tocRule.name)
                tvRuleRegex.setText(tocRule.rule)
            }
            customView = alertBinding.root
            okButton {
                alertBinding.apply {
                    tocRule.name = tvRuleName.text.toString()
                    tocRule.rule = tvRuleRegex.text.toString()
                    viewModel.saveRule(tocRule)
                }
            }
            cancelButton()
        }.show()
    }

    inner class TocRegexAdapter(context: Context) :
        RecyclerAdapter<TxtTocRule, ItemTocRegexBinding>(context),
        ItemTouchCallback.Callback {

        override fun getViewBinding(parent: ViewGroup): ItemTocRegexBinding {
            return ItemTocRegexBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemTocRegexBinding,
            item: TxtTocRule,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                if (payloads.isEmpty()) {
                    root.setBackgroundColor(context.backgroundColor)
                    rbRegexName.text = item.name
                    rbRegexName.isChecked = item.name == selectedName
                    swtEnabled.isChecked = item.enable
                } else {
                    rbRegexName.isChecked = item.name == selectedName
                }
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemTocRegexBinding) {
            binding.apply {
                rbRegexName.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed && isChecked) {
                        selectedName = getItem(holder.layoutPosition)?.name
                        updateItems(0, itemCount - 1, true)
                    }
                }
                swtEnabled.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        getItem(holder.layoutPosition)?.let {
                            it.enable = isChecked
                            launch(IO) {
                                App.db.txtTocRule.update(it)
                            }
                        }
                    }
                }
                ivEdit.onClick {
                    editRule(getItem(holder.layoutPosition))
                }
                ivDelete.onClick {
                    getItem(holder.layoutPosition)?.let { item ->
                        launch(IO) {
                            App.db.txtTocRule.delete(item)
                        }
                    }
                }
            }
        }

        private var isMoved = false

        override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
            swapItem(srcPosition, targetPosition)
            isMoved = true
            return super.swap(srcPosition, targetPosition)
        }

        override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.onClearView(recyclerView, viewHolder)
            if (isMoved) {
                for ((index, item) in getItems().withIndex()) {
                    item.serialNumber = index + 1
                }
                launch(IO) {
                    App.db.txtTocRule.update(*getItems().toTypedArray())
                }
            }
            isMoved = false
        }
    }

    companion object {
        fun show(fragmentManager: FragmentManager, tocRegex: String? = null) {
            val dialog = TocRegexDialog()
            val bundle = Bundle()
            bundle.putString("tocRegex", tocRegex)
            dialog.arguments = bundle
            dialog.show(fragmentManager, "tocRegexDialog")
        }
    }

    interface CallBack {
        fun onTocRegexDialogResult(tocRegex: String) {}
    }

}