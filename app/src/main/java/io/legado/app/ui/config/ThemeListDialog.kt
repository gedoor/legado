package io.legado.app.ui.config

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
import io.legado.app.help.ThemeConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.dialog_recycler_view.*
import kotlinx.android.synthetic.main.item_theme_config.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.share

class ThemeListDialog : BaseDialogFragment(), Toolbar.OnMenuItemClickListener {

    private lateinit var adapter: Adapter

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
        return inflater.inflate(R.layout.dialog_recycler_view, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        tool_bar.setBackgroundColor(primaryColor)
        tool_bar.setTitle(R.string.theme_list)
        initView()
        initMenu()
        initData()
    }

    private fun initView() {
        adapter = Adapter()
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(VerticalDivider(requireContext()))
        recycler_view.adapter = adapter
    }

    private fun initMenu() {
        tool_bar.setOnMenuItemClickListener(this)
        tool_bar.inflateMenu(R.menu.theme_list)
        tool_bar.menu.applyTint(requireContext())
    }

    fun initData() {
        adapter.setItems(ThemeConfig.configList)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_import -> {
                requireContext().getClipText()?.let {
                    if (ThemeConfig.addConfig(it)) {
                        initData()
                    } else {
                        toast("格式不对,添加失败")
                    }
                }
            }
        }
        return true
    }

    fun delete(index: Int) {
        alert(R.string.delete, R.string.sure_del) {
            okButton {
                ThemeConfig.delConfig(index)
                initData()
            }
            noButton()
        }.show().applyTint()
    }

    fun share(index: Int) {
        val json = GSON.toJson(ThemeConfig.configList[index])
        requireContext().share(json, "主题分享")
    }

    inner class Adapter : SimpleRecyclerAdapter<ThemeConfig.Config>(requireContext(), R.layout.item_theme_config) {

        override fun convert(holder: ItemViewHolder, item: ThemeConfig.Config, payloads: MutableList<Any>) {
            holder.itemView.apply {
                tv_name.text = item.themeName
            }
        }

        override fun registerListener(holder: ItemViewHolder) {
            holder.itemView.apply {
                onClick {
                    ThemeConfig.applyConfig(context, ThemeConfig.configList[holder.layoutPosition])
                }
                iv_share.onClick {
                    share(holder.layoutPosition)
                }
                iv_delete.onClick {
                    delete(holder.layoutPosition)
                }
            }
        }

    }
}