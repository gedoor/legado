package io.legado.app.ui.book.read.config

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.constant.Theme
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.help.ItemTouchCallback
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.cancelButton
import io.legado.app.lib.dialogs.customView
import io.legado.app.lib.dialogs.okButton
import io.legado.app.model.localBook.AnalyzeTxtFile
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.applyTint
import kotlinx.android.synthetic.main.dialog_toc_regex.*
import kotlinx.android.synthetic.main.dialog_toc_regex_edit.view.*
import kotlinx.android.synthetic.main.item_toc_regex.view.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.*


class TocRegexDialog : BaseDialogFragment(), Toolbar.OnMenuItemClickListener {

    private lateinit var adapter: TocRegexAdapter
    private var tocRegexLiveData: LiveData<List<TxtTocRule>>? = null
    var selectedName: String? = null
    private var durRegex: String? = null

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.8).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_toc_regex, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        durRegex = arguments?.getString("tocRegex")
        tool_bar.setTitle(R.string.txt_toc_regex)
        tool_bar.inflateMenu(R.menu.txt_toc_regex)
        tool_bar.menu.applyTint(requireContext(), Theme.getTheme())
        tool_bar.setOnMenuItemClickListener(this)
        initView()
        initData()
    }

    private fun initView() {
        adapter = TocRegexAdapter(requireContext())
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(VerticalDivider(requireContext()))
        recycler_view.adapter = adapter
        val itemTouchCallback = ItemTouchCallback()
        itemTouchCallback.onItemTouchCallbackListener = adapter
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recycler_view)
        tv_cancel.onClick {
            dismiss()
        }
        tv_ok.onClick {
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
        tocRegexLiveData = App.db.txtTocRule().observeAll()
        tocRegexLiveData?.observe(viewLifecycleOwner, Observer { tocRules ->
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
            R.id.menu_default -> importDefault()
        }
        return false
    }

    private fun importDefault() {
        launch(IO) {
            AnalyzeTxtFile.getDefaultRules().let {
                App.db.txtTocRule().insert(*it.toTypedArray())
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun editRule(rule: TxtTocRule? = null) {
        val tocRule = rule?.copy() ?: TxtTocRule()
        requireContext().alert(titleResource = R.string.txt_toc_regex) {
            var rootView: View? = null
            customView {
                LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_toc_regex_edit, null).apply {
                        rootView = this
                        tv_rule_name.setText(tocRule.name)
                        tv_rule_regex.setText(tocRule.rule)
                    }
            }
            okButton {
                rootView?.apply {
                    tocRule.name = tv_rule_name.text.toString()
                    tocRule.rule = tv_rule_regex.text.toString()
                    saveRule(tocRule, rule)
                }
            }
            cancelButton()
        }.show().applyTint()
    }

    private fun saveRule(rule: TxtTocRule, oldRule: TxtTocRule? = null) {
        launch(IO) {
            if (rule.serialNumber < 0) {
                rule.serialNumber = adapter.getItems().lastOrNull()?.serialNumber ?: 0 + 1
            }
            oldRule?.let {
                App.db.txtTocRule().delete(oldRule)
            }
            App.db.txtTocRule().insert(rule)
        }
    }

    inner class TocRegexAdapter(context: Context) :
        SimpleRecyclerAdapter<TxtTocRule>(context, R.layout.item_toc_regex),
        ItemTouchCallback.OnItemTouchCallbackListener {

        override fun convert(holder: ItemViewHolder, item: TxtTocRule, payloads: MutableList<Any>) {
            holder.itemView.apply {
                if (payloads.isEmpty()) {
                    rb_regex_name.text = item.name
                    rb_regex_name.isChecked = item.name == selectedName
                    swt_enabled.isChecked = item.enable
                } else {
                    rb_regex_name.isChecked = item.name == selectedName
                }
            }
        }

        override fun registerListener(holder: ItemViewHolder) {
            holder.itemView.apply {
                rb_regex_name.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed && isChecked) {
                        selectedName = getItem(holder.layoutPosition)?.name
                        updateItems(0, itemCount - 1, true)
                    }
                }
                swt_enabled.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        getItem(holder.layoutPosition)?.let {
                            it.enable = isChecked
                            launch(IO) {
                                App.db.txtTocRule().update(it)
                            }
                        }
                    }
                }
                iv_edit.onClick {
                    editRule(getItem(holder.layoutPosition))
                }
                iv_delete.onClick {
                    getItem(holder.layoutPosition)?.let { item ->
                        launch(IO) {
                            App.db.txtTocRule().delete(item)
                        }
                    }
                }
            }
        }

        private var isMoved = false

        override fun onMove(srcPosition: Int, targetPosition: Int): Boolean {
            Collections.swap(getItems(), srcPosition, targetPosition)
            notifyItemMoved(srcPosition, targetPosition)
            isMoved = true
            return super.onMove(srcPosition, targetPosition)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            if (isMoved) {
                for ((index, item) in getItems().withIndex()) {
                    item.serialNumber = index + 1
                }
                launch(IO) {
                    App.db.txtTocRule().update(*getItems().toTypedArray())
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