package io.legado.app.ui.book.read.config

import android.os.Bundle
import android.view.*
import android.widget.SeekBar
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.service.BaseReadAloudService
import io.legado.app.service.help.ReadAloud
import io.legado.app.utils.ColorUtils
import kotlinx.android.synthetic.main.dialog_auto_read.*
import org.jetbrains.anko.sdk27.listeners.onClick

class AutoReadDialog : BaseDialogFragment() {
    var callBack: CallBack? = null

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
        callBack = activity as? CallBack
        return inflater.inflate(R.layout.dialog_auto_read, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val bg = requireContext().bottomBackground
        val isLight = ColorUtils.isColorLight(bg)
        val textColor = requireContext().getPrimaryTextColor(isLight)
        root_view.setBackgroundColor(bg)
        tv_read_speed_title.setTextColor(textColor)
        tv_read_speed.setTextColor(textColor)
        iv_catalog.setColorFilter(textColor)
        tv_catalog.setTextColor(textColor)
        iv_main_menu.setColorFilter(textColor)
        tv_main_menu.setTextColor(textColor)
        iv_auto_page_stop.setColorFilter(textColor)
        tv_auto_page_stop.setTextColor(textColor)
        iv_setting.setColorFilter(textColor)
        tv_setting.setTextColor(textColor)
        initOnChange()
        initData()
        initEvent()
    }

    private fun initData() {
        val speed = if (ReadBookConfig.autoReadSpeed < 10) 10 else ReadBookConfig.autoReadSpeed
        tv_read_speed.text = String.format("%ds", speed)
        seek_auto_read.progress = speed
    }

    private fun initOnChange() {
        seek_auto_read.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val speed = if (progress < 10) 10 else progress
                tv_read_speed.text = String.format("%ds", speed)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                ReadBookConfig.autoReadSpeed =
                    if (seek_auto_read.progress < 10) 10 else seek_auto_read.progress
                upTtsSpeechRate()
            }
        })
    }

    private fun initEvent() {
        ll_main_menu.onClick { callBack?.showMenuBar(); dismiss() }
        ll_setting.onClick {
            ReadAloudConfigDialog().show(childFragmentManager, "readAloudConfigDialog")
        }
        ll_catalog.onClick { callBack?.openChapterList() }
        ll_auto_page_stop.onClick {
            callBack?.autoPageStop()
            dismiss()
        }
    }

    private fun upTtsSpeechRate() {
        ReadAloud.upTtsSpeechRate(requireContext())
        if (!BaseReadAloudService.pause) {
            ReadAloud.pause(requireContext())
            ReadAloud.resume(requireContext())
        }
    }

    interface CallBack {
        fun showMenuBar()
        fun openChapterList()
        fun autoPageStop()
    }
}