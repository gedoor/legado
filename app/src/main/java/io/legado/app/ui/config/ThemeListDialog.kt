package io.legado.app.ui.config

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.legado.app.utils.GSON
import io.legado.app.utils.applyTint
import kotlinx.android.synthetic.main.dialog_recycler_view.*
import kotlinx.android.synthetic.main.item_theme_config.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.share

class ThemeListDialog : BaseDialogFragment() {

    private lateinit var adapter: Adapter

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
        return inflater.inflate(R.layout.dialog_recycler_view, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        tool_bar.setBackgroundColor(primaryColor)
        tool_bar.setTitle(R.string.theme_list)
        initView()
        initData()
    }

    private fun initView() {
        adapter = Adapter()
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(VerticalDivider(requireContext()))
        recycler_view.adapter = adapter
    }

    fun initData() {
        adapter.setItems(ThemeConfig.configList)
    }

    fun delete(index: Int) {
        alert(R.string.delete, R.string.sure_del) {
            okButton {
                ThemeConfig.configList.removeAt(index)
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