package io.legado.app.ui.book.read.config

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.fragment.app.DialogFragment
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.help.BookHelp
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.book.read.Help
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.widget.font.FontSelectDialog
import io.legado.app.utils.getIndexById
import io.legado.app.utils.getPrefString
import io.legado.app.utils.postEvent
import io.legado.app.utils.putPrefString
import kotlinx.android.synthetic.main.activity_book_read.*
import kotlinx.android.synthetic.main.dialog_read_book_style.*
import org.jetbrains.anko.sdk27.listeners.onCheckedChange
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick

class ReadStyleDialog : DialogFragment(), FontSelectDialog.CallBack {

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
            it.decorView.setPadding(0, 5, 0, 0)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
        initViewEvent()
    }

    override fun onDestroy() {
        super.onDestroy()
        ReadBookConfig.save()
    }

    private fun initView() {
        dsb_text_size.valueFormat = {
            (it + 5).toString()
        }
        dsb_text_letter_spacing.valueFormat = {
            ((it - 50) / 100f).toString()
        }
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
        tv_title_center.onClick {
            ReadBookConfig.apply {
                titleCenter = !titleCenter
                tv_title_center.isSelected = titleCenter
            }
            postEvent(EventBus.UP_CONFIG, true)
        }
        tv_text_bold.onClick {
            ReadBookConfig.apply {
                textBold = !textBold
                tv_text_bold.isSelected = textBold
            }
            postEvent(EventBus.UP_CONFIG, false)
        }
        tv_text_font.onClick {
            FontSelectDialog().show(childFragmentManager, "fontSelectDialog")
        }
        tv_text_indent.onClick {
            selector(
                title = getString(R.string.text_indent),
                items = resources.getStringArray(R.array.indent).toList()
            ) { _, index ->
                BookHelp.bodyIndentCount = index
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        tv_padding.onClick {
            dismiss()
            callBack?.showPaddingConfig()
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
        rg_page_anim.onCheckedChange { _, checkedId ->
            rg_page_anim.getIndexById(checkedId).let {
                ReadBookConfig.pageAnim = it
                callBack?.page_view?.upPageAnim()
            }
        }
        cb_share_layout.onCheckedChangeListener = { checkBox, isChecked ->
            if (checkBox.isPressed) {
                ReadBookConfig.shareLayout = isChecked
                upStyle()
                postEvent(EventBus.UP_CONFIG, true)
            }
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

    private fun changeBg(index: Int) {
        if (ReadBookConfig.styleSelect != index) {
            ReadBookConfig.styleSelect = index
            ReadBookConfig.upBg()
            upStyle()
            upBg()
            postEvent(EventBus.UP_CONFIG, true)
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
            tv_title_center.isSelected = it.titleCenter
            tv_text_bold.isSelected = it.textBold
            dsb_text_size.progress = it.textSize - 5
            dsb_text_letter_spacing.progress = (it.letterSpacing * 100).toInt() + 50
            dsb_line_size.progress = it.lineSpacingExtra
            dsb_paragraph_spacing.progress = it.paragraphSpacing
        }
    }

    private fun setBg() {
        bg0.setTextColor(ReadBookConfig.getConfig(0).textColor())
        bg1.setTextColor(ReadBookConfig.getConfig(1).textColor())
        bg2.setTextColor(ReadBookConfig.getConfig(2).textColor())
        bg3.setTextColor(ReadBookConfig.getConfig(3).textColor())
        bg4.setTextColor(ReadBookConfig.getConfig(4).textColor())
        for (i in 0..4) {
            val iv = when (i) {
                1 -> bg1
                2 -> bg2
                3 -> bg3
                4 -> bg4
                else -> bg0
            }
            iv.setImageDrawable(ReadBookConfig.getConfig(i).bgDrawable(100, 150))
        }
    }

    private fun upBg() {
        bg0.borderColor = requireContext().primaryColor
        bg1.borderColor = requireContext().primaryColor
        bg2.borderColor = requireContext().primaryColor
        bg3.borderColor = requireContext().primaryColor
        bg4.borderColor = requireContext().primaryColor
        when (ReadBookConfig.styleSelect) {
            1 -> bg1.borderColor = requireContext().accentColor
            2 -> bg2.borderColor = requireContext().accentColor
            3 -> bg3.borderColor = requireContext().accentColor
            4 -> bg4.borderColor = requireContext().accentColor
            else -> bg0.borderColor = requireContext().accentColor
        }
    }

    override val curFontPath: String
        get() = requireContext().getPrefString(PreferKey.readBookFont) ?: ""

    override fun selectFile(path: String) {
        requireContext().putPrefString(PreferKey.readBookFont, path)
        postEvent(EventBus.UP_CONFIG, true)
    }
}