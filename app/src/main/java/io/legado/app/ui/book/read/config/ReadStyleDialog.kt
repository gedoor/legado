package io.legado.app.ui.book.read.config

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import androidx.core.view.get
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppConfig
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.book.read.Help
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.widget.font.FontSelectDialog
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_book_read.*
import kotlinx.android.synthetic.main.dialog_read_book_style.*
import kotlinx.android.synthetic.main.dialog_title_config.view.*
import org.jetbrains.anko.sdk27.listeners.onCheckedChange
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick

class ReadStyleDialog : BaseDialogFragment(), FontSelectDialog.CallBack {

    val callBack get() = activity as? ReadBookActivity

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.let {
            Help.upSystemUiVisibility(it)
            it.windowManager?.defaultDisplay?.getMetrics(dm)
        }
        dialog?.window?.let {
            it.setBackgroundDrawableResource(R.color.background)
            it.decorView.setPadding(0, 0, 0, 0)
            val attr = it.attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            it.attributes = attr
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onAttach(context: Context) {
        val bg = requireContext().bottomBackground
        val contextThemeWrapper = if (ColorUtils.isColorLight(bg)) {
            ContextThemeWrapper(requireContext(), R.style.AppTheme_Light)
        } else {
            ContextThemeWrapper(requireContext(), R.style.AppTheme_Dark)
        }
        super.onAttach(contextThemeWrapper)
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
        root_view.setBackgroundColor(requireContext().bottomBackground)
        dsb_text_size.valueFormat = {
            (it + 5).toString()
        }
        dsb_text_letter_spacing.valueFormat = {
            ((it - 50) / 100f).toString()
        }
        dsb_line_size.valueFormat = { ((it - 10) / 10f).toString() }
        dsb_paragraph_spacing.valueFormat = { (it / 10f).toString() }
    }

    private fun initData() {
        cb_share_layout.isChecked = ReadBookConfig.shareLayout
        ReadBookConfig.pageAnim.let {
            if (it >= 0 && it < rg_page_anim.childCount) {
                rg_page_anim.check(rg_page_anim[it].id)
            }
        }
        upStyle()
        setBg()
        upBg()
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
                ReadBookConfig.bodyIndentCount = index
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
            ReadBookConfig.pageAnim = rg_page_anim.getIndexById(checkedId)
            callBack?.page_view?.upPageAnim()
        }
        cb_share_layout.onCheckedChangeListener = { checkBox, isChecked ->
            if (checkBox.isPressed) {
                ReadBookConfig.shareLayout = isChecked
                upStyle()
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
        bg0.onClick { changeBg(0) }
        bg0.onLongClick { showBgTextConfig(0) }
        bg1.onClick { changeBg(1) }
        bg1.onLongClick { showBgTextConfig(1) }
        bg2.onClick { changeBg(2) }
        bg2.onLongClick { showBgTextConfig(2) }
        bg3.onClick { changeBg(3) }
        bg3.onLongClick { showBgTextConfig(3) }
        bg4.onClick { changeBg(4) }
        bg4.onLongClick { showBgTextConfig(4) }
    }

    @SuppressLint("InflateParams")
    private fun showTitleConfig() = ReadBookConfig.apply {
        requireContext().alert(R.string.title) {
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
        if (ReadBookConfig.styleSelect != index) {
            ReadBookConfig.styleSelect = index
            ReadBookConfig.upBg()
            upStyle()
            upBg()
            postEvent(EventBus.UP_CONFIG, true)
        }
        if (AppConfig.isEInkMode) {
            toast(R.string.e_ink_change_bg)
        }
    }

    private fun showBgTextConfig(index: Int): Boolean {
        dismiss()
        changeBg(index)
        callBack?.showBgTextConfig()
        return true
    }

    private fun upStyle() {
        ReadBookConfig.let {
            dsb_text_size.progress = it.textSize - 5
            dsb_text_letter_spacing.progress = (it.letterSpacing * 100).toInt() + 50
            dsb_line_size.progress = it.lineSpacingExtra
            dsb_paragraph_spacing.progress = it.paragraphSpacing
        }
    }

    private fun setBg() = ReadBookConfig.apply {
        bg0.setTextColor(getConfig(0).textColor())
        bg1.setTextColor(getConfig(1).textColor())
        bg2.setTextColor(getConfig(2).textColor())
        bg3.setTextColor(getConfig(3).textColor())
        bg4.setTextColor(getConfig(4).textColor())
        for (i in 0..4) {
            val iv = when (i) {
                1 -> bg1
                2 -> bg2
                3 -> bg3
                4 -> bg4
                else -> bg0
            }
            iv.setImageDrawable(getConfig(i).bgDrawable(100, 150))
        }
    }

    private fun upBg() = requireContext().apply {
        bg0.borderColor = primaryColor
        bg1.borderColor = primaryColor
        bg2.borderColor = primaryColor
        bg3.borderColor = primaryColor
        bg4.borderColor = primaryColor
        when (ReadBookConfig.styleSelect) {
            1 -> bg1.borderColor = accentColor
            2 -> bg2.borderColor = accentColor
            3 -> bg3.borderColor = accentColor
            4 -> bg4.borderColor = accentColor
            else -> bg0.borderColor = accentColor
        }
    }

    override val curFontPath: String
        get() = requireContext().getPrefString(PreferKey.readBookFont) ?: ""

    override fun selectFile(path: String) {
        requireContext().putPrefString(PreferKey.readBookFont, path)
        postEvent(EventBus.UP_CONFIG, true)
    }
}