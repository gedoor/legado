package io.legado.app.ui.book.audio

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.SeekBar
import io.legado.app.databinding.PopupSeekBarBinding
import io.legado.app.model.AudioPlay
import io.legado.app.service.AudioPlayService
import io.legado.app.ui.widget.seekbar.SeekBarChangeListener

class TimerSliderPopup(context: Context) :
    PopupWindow(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {

    private val binding = PopupSeekBarBinding.inflate(LayoutInflater.from(context))

    init {
        contentView = binding.root

        isTouchable = true
        isOutsideTouchable = true
        isFocusable = false

        binding.seekBar.max = 180

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    AudioPlay.setTimer(progress)
                }
            }

        })
    }

    override fun showAsDropDown(anchor: View?) {
        super.showAsDropDown(anchor)
        binding.seekBar.progress = AudioPlayService.timeMinute
    }

}