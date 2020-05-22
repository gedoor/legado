package io.legado.app.ui.book.read.config

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.help.AppConfig
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.service.BaseReadAloudService
import io.legado.app.service.help.ReadAloud
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.Help
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.observeEvent
import io.legado.app.utils.putPrefBoolean
import kotlinx.android.synthetic.main.dialog_read_aloud.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ReadAloudDialog : BaseDialogFragment() {
    var callBack: CallBack? = null

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        callBack = activity as? CallBack
        return inflater.inflate(R.layout.dialog_read_aloud, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        root_view.setBackgroundColor(requireContext().bottomBackground)
        initOnChange()
        initData()
        initEvent()
    }

    private fun initData() {
        upPlayState()
        upTimerText(BaseReadAloudService.timeMinute)
        seek_timer.progress = BaseReadAloudService.timeMinute
        cb_tts_follow_sys.isChecked = requireContext().getPrefBoolean("ttsFollowSys", true)
        seek_tts_SpeechRate.isEnabled = !cb_tts_follow_sys.isChecked
        seek_tts_SpeechRate.progress = AppConfig.ttsSpeechRate
    }

    private fun initOnChange() {
        cb_tts_follow_sys.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                requireContext().putPrefBoolean("ttsFollowSys", isChecked)
                seek_tts_SpeechRate.isEnabled = !isChecked
                upTtsSpeechRate()
            }
        }
        seek_tts_SpeechRate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                AppConfig.ttsSpeechRate = seek_tts_SpeechRate.progress
                upTtsSpeechRate()
            }
        })
        seek_timer.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                upTimerText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                ReadAloud.setTimer(requireContext(), seek_timer.progress)
            }
        })
    }

    private fun initEvent() {
        ll_main_menu.onClick { callBack?.showMenuBar(); dismiss() }
        ll_setting.onClick {
            ReadAloudConfigDialog().show(childFragmentManager, "readAloudConfigDialog")
        }
        tv_pre.onClick { ReadBook.moveToPrevChapter(upContent = true, toLast = false) }
        tv_next.onClick { ReadBook.moveToNextChapter(true) }
        iv_stop.onClick { ReadAloud.stop(requireContext()); dismiss() }
        iv_play_pause.onClick { callBack?.onClickReadAloud() }
        iv_play_prev.onClick { ReadAloud.prevParagraph(requireContext()) }
        iv_play_next.onClick { ReadAloud.nextParagraph(requireContext()) }
        ll_catalog.onClick { callBack?.openChapterList() }
        ll_to_backstage.onClick { callBack?.finish() }
    }

    private fun upPlayState() {
        if (!BaseReadAloudService.pause) {
            iv_play_pause.setImageResource(R.drawable.ic_pause_24dp)
        } else {
            iv_play_pause.setImageResource(R.drawable.ic_play_24dp)
        }
    }

    private fun upTimerText(timeMinute: Int) {
        tv_timer.text = requireContext().getString(R.string.timer_m, timeMinute)
    }

    private fun upTtsSpeechRate() {
        ReadAloud.upTtsSpeechRate(requireContext())
        if (!BaseReadAloudService.pause) {
            ReadAloud.pause(requireContext())
            ReadAloud.resume(requireContext())
        }
    }

    override fun observeLiveBus() {
        observeEvent<Int>(EventBus.ALOUD_STATE) { upPlayState() }
        observeEvent<Int>(EventBus.TTS_DS) { seek_timer.progress = it }
    }

    interface CallBack {
        fun showMenuBar()
        fun openChapterList()
        fun onClickReadAloud()
        fun finish()
    }
}