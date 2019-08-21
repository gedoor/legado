package io.legado.app.ui.readbook.config

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.PopupWindow
import io.legado.app.R
import io.legado.app.constant.Bus
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.utils.postEvent
import kotlinx.android.synthetic.main.pop_read_book_style.view.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ReadStylePopup(context: Context?) : PopupWindow(context) {

    init {
        @SuppressLint("InflateParams")
        contentView = LayoutInflater.from(context).inflate(R.layout.pop_read_book_style, null)

        initData()
        initOnClick()
    }

    private fun initData() = with(contentView) {
        upBg()
    }


    private fun initOnClick() = with(contentView) {
        bg0.onClick {
            ReadBookConfig.styleSelect = 0
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, 0)
        }
        bg1.onClick {
            ReadBookConfig.styleSelect = 1
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, 0)
        }
        bg2.onClick {
            ReadBookConfig.styleSelect = 2
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, 0)
        }
        bg3.onClick {
            ReadBookConfig.styleSelect = 3
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, 0)
        }
        bg4.onClick {
            ReadBookConfig.styleSelect = 4
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, 0)
        }
    }

    private fun upBg() = with(contentView) {
        bg0.borderColor = context.primaryTextColor
        bg1.borderColor = context.primaryTextColor
        bg2.borderColor = context.primaryTextColor
        bg3.borderColor = context.primaryTextColor
        bg4.borderColor = context.primaryTextColor
        when (ReadBookConfig.styleSelect) {
            1 -> {
                bg1.borderColor = context.accentColor
            }
            2 -> {
                bg2.borderColor = context.accentColor
            }
            3 -> {
                bg3.borderColor = context.accentColor
            }
            4 -> {
                bg4.borderColor = context.accentColor
            }
            else -> {
                bg0.borderColor = context.accentColor
            }
        }
    }
}