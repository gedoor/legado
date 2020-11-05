package io.legado.app.ui.book.read.config

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.core.view.get
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.constant.EventBus
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.widget.font.FontSelectDialog
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_book_read.*
import kotlinx.android.synthetic.main.dialog_read_book_style.*
import kotlinx.android.synthetic.main.dialog_title_config.view.*
import kotlinx.android.synthetic.main.item_read_style.view.*
import org.jetbrains.anko.sdk27.listeners.onCheckedChange
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick

class ReadStyleDialog : BaseDialogFragment(), FontSelectDialog.CallBack {

    val callBack get() = activity as? ReadBookActivity
    private lateinit var styleAdapter: StyleAdapter

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            it.setBackgroundDrawableResource(R.color.background)
            it.decorView.setPadding(0, 0, 0, 0)
            val attr = it.attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            it.attributes = attr
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_read_book_style, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
        initViewEvent()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        ReadBookConfig.save()
    }

    private fun initView() {
        val bg = requireContext().bottomBackground
        val isLight = ColorUtils.isColorLight(bg)
        val textColor = requireContext().getPrimaryTextColor(isLight)
        root_view.setBackgroundColor(bg)
        tv_page_anim.setTextColor(textColor)
        tv_bg_ts.setTextColor(textColor)
        tv_share_layout.setTextColor(textColor)
        dsb_text_size.valueFormat = {
            (it + 5).toString()
        }
        dsb_text_letter_spacing.valueFormat = {
            ((it - 50) / 100f).toString()
        }
        dsb_line_size.valueFormat = { ((it - 10) / 10f).toString() }
        dsb_paragraph_spacing.valueFormat = { (it / 10f).toString() }
        styleAdapter = StyleAdapter()
        rv_style.adapter = styleAdapter
        val footerView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_read_style, rv_style, false)
        footerView.iv_style.setPadding(6.dp, 6.dp, 6.dp, 6.dp)
        footerView.iv_style.setText(null)
        footerView.iv_style.setColorFilter(textColor)
        footerView.iv_style.borderColor = textColor
        footerView.iv_style.setImageResource(R.drawable.ic_add)
        styleAdapter.addFooterView(footerView)
        footerView.onClick {
            ReadBookConfig.configList.add(ReadBookConfig.Config())
            showBgTextConfig(ReadBookConfig.configList.lastIndex)
        }
    }

    private fun initData() {
        cb_share_layout.isChecked = ReadBookConfig.shareLayout
        upView()
        styleAdapter.setItems(ReadBookConfig.configList)
    }

    private fun initViewEvent() {
        chinese_converter.onChanged {
            postEvent(EventBus.UP_CONFIG, true)
        }
        tv_title_mode.onClick {
            showTitleConfig()
        }
        text_font_weight_converter.onChanged {
            postEvent(EventBus.UP_CONFIG, true)
        }
        tv_text_font.onClick {
            FontSelectDialog().show(childFragmentManager, "fontSelectDialog")
        }
        tv_text_indent.onClick {
            selector(
                title = getString(R.string.text_indent),
                items = resources.getStringArray(R.array.indent).toList()
            ) { _, index ->
                ReadBookConfig.paragraphIndent = "　".repeat(index)
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        tv_padding.onClick {
            dismiss()
            callBack?.showPaddingConfig()
        }
        tv_tip.onClick {
            TipConfigDialog().show(childFragmentManager, "tipConfigDialog")
        }
        rg_page_anim.onCheckedChange { _, checkedId ->
            ReadBook.book?.setPageAnim(-1)
            ReadBookConfig.pageAnim = rg_page_anim.getIndexById(checkedId)
            callBack?.page_view?.upPageAnim()
        }
        cb_share_layout.onCheckedChangeListener = { checkBox, isChecked ->
            if (checkBox.isPressed) {
                ReadBookConfig.shareLayout = isChecked
                upView()
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        dsb_text_size.onChanged = {
            ReadBookConfig.textSize = it + 5
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_text_letter_spacing.onChanged = {
            ReadBookConfig.letterSpacing = (it - 50) / 100f
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_line_size.onChanged = {
            ReadBookConfig.lineSpacingExtra = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_paragraph_spacing.onChanged = {
            ReadBookConfig.paragraphSpacing = it
            postEvent(EventBus.UP_CONFIG, true)
        }
    }

    @SuppressLint("InflateParams")
    private fun showTitleConfig() = ReadBookConfig.apply {
        alert(R.string.title) {
            val rootView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_title_config, null).apply {
                    rg_title_mode.checkByIndex(titleMode)
                    dsb_title_size.progress = titleSize
                    dsb_title_top.progress = titleTopSpacing
                    dsb_title_bottom.progress = titleBottomSpacing
                    rg_title_mode.onCheckedChange { _, checkedId ->
                        titleMode = rg_title_mode.getIndexById(checkedId)
                        postEvent(EventBus.UP_CONFIG, true)
                    }
                    dsb_title_size.onChanged = {
                        titleSize = it
                        postEvent(EventBus.UP_CONFIG, true)
                    }
                    dsb_title_top.onChanged = {
                        titleTopSpacing = it
                        postEvent(EventBus.UP_CONFIG, true)
                    }
                    dsb_title_bottom.onChanged = {
                        titleBottomSpacing = it
                        postEvent(EventBus.UP_CONFIG, true)
                    }
                }
            customView = rootView
        }.show().applyTint()
    }

    private fun changeBg(index: Int) {
        val oldIndex = ReadBookConfig.styleSelect
        if (index != oldIndex) {
            ReadBookConfig.styleSelect = index
            ReadBookConfig.upBg()
            upView()
            styleAdapter.notifyItemChanged(oldIndex)
            styleAdapter.notifyItemChanged(index)
            postEvent(EventBus.UP_CONFIG, true)
        }
    }

    private fun showBgTextConfig(index: Int): Boolean {
        dismiss()
        changeBg(index)
        callBack?.showBgTextConfig()
        return true
    }

    private fun upView() {
        ReadBook.pageAnim().let {
            if (it >= 0 && it < rg_page_anim.childCount) {
                rg_page_anim.check(rg_page_anim[it].id)
            }
        }
        ReadBookConfig.let {
            dsb_text_size.progress = it.textSize - 5
            dsb_text_letter_spacing.progress = (it.letterSpacing * 100).toInt() + 50
            dsb_line_size.progress = it.lineSpacingExtra
            dsb_paragraph_spacing.progress = it.paragraphSpacing
        }
    }

    override val curFontPath: String
        get() = ReadBookConfig.textFont

    override fun selectFont(path: String) {
        if (path != ReadBookConfig.textFont) {
            ReadBookConfig.textFont = path
            postEvent(EventBus.UP_CONFIG, true)
        }
    }

    inner class StyleAdapter :
        SimpleRecyclerAdapter<ReadBookConfig.Config>(requireContext(), R.layout.item_read_style) {

        override fun convert(
            holder: ItemViewHolder,
            item: ReadBookConfig.Config,
            payloads: MutableList<Any>
        ) {
            holder.itemView.apply {
                iv_style.setText(item.name.ifBlank { "文字" })
                iv_style.setTextColor(item.curTextColor())
                iv_style.setImageDrawable(item.curBgDrawable(100, 150))
                if (ReadBookConfig.styleSelect == holder.layoutPosition) {
                    iv_style.borderColor = accentColor
                    iv_style.setTextBold(true)
                } else {
                    iv_style.borderColor = item.curTextColor()
                    iv_style.setTextBold(false)
                }
            }
        }

        override fun registerListener(holder: ItemViewHolder) {
            holder.itemView.apply {
                iv_style.onClick {
                    if (iv_style.isInView) {
                        changeBg(holder.layoutPosition)
                    }
                }
                iv_style.onLongClick {
                    if (iv_style.isInView) {
                        showBgTextConfig(holder.layoutPosition)
                    } else {
                        false
                    }
                }
            }
        }

    }
}