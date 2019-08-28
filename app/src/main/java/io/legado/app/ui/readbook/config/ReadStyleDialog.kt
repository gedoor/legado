package io.legado.app.ui.readbook.config

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.view.get
import androidx.fragment.app.DialogFragment
import io.legado.app.R
import io.legado.app.constant.Bus
import io.legado.app.help.ImageLoader
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.readbook.Help
import io.legado.app.ui.readbook.ReadBookActivity
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.postEvent
import io.legado.app.utils.putPrefInt
import kotlinx.android.synthetic.main.activity_read_book.*
import kotlinx.android.synthetic.main.dialog_read_book_style.*
import org.jetbrains.anko.sdk27.listeners.onCheckedChange
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

    override fun onDestroy() {
        super.onDestroy()
        ReadBookConfig.save()
    }

    private fun initData() {
        requireContext().getPrefInt("pageAnim").let {
            if (it >= 0 && it < rg_page_anim.childCount) {
                rg_page_anim.check(rg_page_anim[it].id)
            }
        }
        ReadBookConfig.getConfig().let {
            tv_text_bold.isSelected = it.textBold
            seek_text_size.progress = it.textSize - 5
            seek_text_letter_spacing.progress = (it.letterSpacing * 10).toInt() + 5
            seek_line_size.progress = it.lineSpacingExtra.toInt()
        }
        setBg()
        upBg()
    }


    private fun initOnClick() = with(ReadBookConfig.getConfig()) {
        tv_text_bold.onClick {
            textBold = !textBold
            tv_text_bold.isSelected = textBold
            postEvent(Bus.UP_CONFIG, false)
        }
        seek_text_size.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textSize = progress + 5
                postEvent(Bus.UP_CONFIG, true)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        iv_text_size_add.onClick {
            textSize++
            if (textSize > 50) textSize = 50
            seek_text_size.progress = textSize - 5
            postEvent(Bus.UP_CONFIG, true)
        }
        iv_line_size_remove.onClick {
            textSize--
            if (textSize < 5) textSize = 5
            seek_text_size.progress = textSize - 5
            postEvent(Bus.UP_CONFIG, true)
        }
        seek_text_letter_spacing.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                letterSpacing = (progress - 5) / 10f
                postEvent(Bus.UP_CONFIG, true)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        iv_text_letter_spacing_add.onClick {
            letterSpacing += 0.1f
            seek_text_letter_spacing.progress = (letterSpacing * 10).toInt() + 5
            postEvent(Bus.UP_CONFIG, true)
        }
        iv_text_letter_spacing_remove.onClick {
            letterSpacing -= 0.1f
            seek_text_letter_spacing.progress = (letterSpacing * 10).toInt() + 5
            postEvent(Bus.UP_CONFIG, true)
        }
        seek_line_size.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                lineSpacingExtra = progress.toFloat()
                postEvent(Bus.UP_CONFIG, true)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        iv_line_size_add.onClick {
            lineSpacingExtra++
            postEvent(Bus.UP_CONFIG, true)
        }
        iv_line_size_remove.onClick {
            lineSpacingExtra--
            postEvent(Bus.UP_CONFIG, true)
        }
        rg_page_anim.onCheckedChange { _, checkedId ->
            for (i in 0 until rg_page_anim.childCount) {
                if (checkedId == rg_page_anim[i].id) {
                    requireContext().putPrefInt("pageAnim", i)
                    val activity = activity
                    if (activity is ReadBookActivity) {
                        activity.page_view.upPageAnim()
                    }
                    break
                }
            }
        }
        tv_bg0.onClick {
            ReadBookConfig.styleSelect = 0
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, false)
        }
        tv_bg1.onClick {
            ReadBookConfig.styleSelect = 1
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, false)
        }
        tv_bg2.onClick {
            ReadBookConfig.styleSelect = 2
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, false)
        }
        tv_bg3.onClick {
            ReadBookConfig.styleSelect = 3
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, false)
        }
        tv_bg4.onClick {
            ReadBookConfig.styleSelect = 4
            ReadBookConfig.upBg()
            upBg()
            postEvent(Bus.UP_CONFIG, false)
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