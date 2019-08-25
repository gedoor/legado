package io.legado.app.ui.readbook.config

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import io.legado.app.R
import io.legado.app.constant.Bus
import io.legado.app.help.ImageLoader
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.readbook.Help
import io.legado.app.utils.postEvent
import kotlinx.android.synthetic.main.dialog_read_book_style.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ReadStyleDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_read_book_style, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        initOnClick()
    }

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.let {
            it.setBackgroundDrawableResource(R.color.transparent)
            it.decorView.setPadding(0, 0, 0, 0)
            val attr = it.attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            it.attributes = attr
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            Help.upSystemUiVisibility(it)
        }
    }

    private fun initData() {
        setBg()
        upBg()
    }


    private fun initOnClick() {
        tv_bg0.onClick {
            ReadBookConfig.styleSelect = 0
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, true)
        }
        tv_bg1.onClick {
            ReadBookConfig.styleSelect = 1
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, true)
        }
        tv_bg2.onClick {
            ReadBookConfig.styleSelect = 2
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, true)
        }
        tv_bg3.onClick {
            ReadBookConfig.styleSelect = 3
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, true)
        }
        tv_bg4.onClick {
            ReadBookConfig.styleSelect = 4
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, true)
        }
    }

    private fun setBg() {
        tv_bg0.setTextColor(ReadBookConfig.getConfig(0).textColor())
        tv_bg1.setTextColor(ReadBookConfig.getConfig(1).textColor())
        tv_bg2.setTextColor(ReadBookConfig.getConfig(2).textColor())
        tv_bg3.setTextColor(ReadBookConfig.getConfig(3).textColor())
        tv_bg4.setTextColor(ReadBookConfig.getConfig(4).textColor())
        for (i in 0..4) {
            val iv = when (i) {
                1 -> bg1
                2 -> bg2
                3 -> bg3
                4 -> bg4
                else -> bg0
            }
            ReadBookConfig.getConfig(i).apply {
                when (bgType) {
                    2 -> {
                        ImageLoader.load(requireContext(), bgStr)
                            .centerCrop()
                            .setAsFile(iv)
                    }
                    else -> iv.setImageDrawable(bgDrawable())
                }
            }
        }
    }

    private fun upBg() {
        bg0.borderColor = requireContext().primaryColor
        bg1.borderColor = requireContext().primaryColor
        bg2.borderColor = requireContext().primaryColor
        bg3.borderColor = requireContext().primaryColor
        bg4.borderColor = requireContext().primaryColor
        when (ReadBookConfig.styleSelect) {
            1 -> {
                bg1.borderColor = requireContext().accentColor
            }
            2 -> {
                bg2.borderColor = requireContext().accentColor
            }
            3 -> {
                bg3.borderColor = requireContext().accentColor
            }
            4 -> {
                bg4.borderColor = requireContext().accentColor
            }
            else -> {
                bg0.borderColor = requireContext().accentColor
            }
        }
    }
}