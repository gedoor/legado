package io.legado.app.ui.readbook.config

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import io.legado.app.R
import io.legado.app.constant.Bus
import io.legado.app.constant.Status
import io.legado.app.service.ReadAloudService
import io.legado.app.ui.readbook.Help
import io.legado.app.ui.readbook.ReadBookActivity
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_read_book.*
import kotlinx.android.synthetic.main.dialog_read_aloud.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ReadAloudDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_read_aloud, container)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
        initOnChange()
        initOnClick()
    }

    private fun initData() {
        observeEvent<Int>(Bus.ALOUD_STATE) { upPlayState(it) }
        val activity = activity
        if (activity is ReadBookActivity) {
            upPlayState(activity.readAloudStatus)
        }
        cb_by_page.isChecked = requireContext().getPrefBoolean("readAloudByPage")
        cb_tts_follow_sys.isChecked = requireContext().getPrefBoolean("ttsFollowSys", true)
        seek_tts_SpeechRate.isEnabled = !cb_tts_follow_sys.isChecked
        seek_tts_SpeechRate.progress = requireContext().getPrefInt("ttsSpeechRate", 5)
    }

    private fun initOnChange() {
        cb_by_page.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                requireContext().putPrefBoolean("readAloudByPage", isChecked)
            }
        }
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

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                requireContext().putPrefInt("ttsSpeechRate", seek_tts_SpeechRate.progress)
                upTtsSpeechRate()
            }

        })
        seek_timer.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
    }

    private fun initOnClick() {
        iv_menu.onClick {
            val activity = activity
            if (activity is ReadBookActivity) {
                activity.read_menu.runMenuIn()
                dismiss()
            }
        }
        iv_stop.onClick { ReadAloudService.stop(requireContext()); dismiss() }
        iv_play_pause.onClick { postEvent(Bus.READ_ALOUD_BUTTON, true) }
        iv_play_prev.onClick { ReadAloudService.prevParagraph(requireContext()) }
        iv_play_next.onClick { ReadAloudService.nextParagraph(requireContext()) }
    }

    private fun upPlayState(state: Int) {
        if (state == Status.PAUSE) {
            iv_play_pause.setImageResource(R.drawable.ic_play_24dp)
        } else {
            iv_play_pause.setImageResource(R.drawable.ic_pause_24dp)
        }
    }

    private fun upTtsSpeechRate() {
        val activity = activity
        ReadAloudService.upTtsSpeechRate(requireContext())
        if (activity is ReadBookActivity) {
            if (activity.readAloudStatus == Status.PLAY) {
                ReadAloudService.pause(requireContext())
                ReadAloudService.resume(requireContext())
            }
        }
    }
}