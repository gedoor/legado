package io.legado.app.ui.book.audio

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.SeekBar
import io.legado.app.R
import io.legado.app.databinding.PopupSeekBarBinding
import io.legado.app.model.AudioPlay
import io.legado.app.service.AudioPlayService
import io.legado.app.ui.widget.seekbar.SeekBarChangeListener

class TimerSliderPopup(private val context: Context) :
    PopupWindow(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {

    private val binding = PopupSeekBarBinding.inflate(LayoutInflater.from(context))

    init {
        contentView = binding.root

        isTouchable = true
        isOutsideTouchable = false
        isFocusable = true

        binding.seekBar.max = 180
        setProcessTextValue(binding.seekBar.progress)
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                setProcessTextValue(progress)
                if (fromUser) {
                    AudioPlay.setTimer(progress)
                }
            }

        })
    }

    override fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int, gravity: Int) {
        super.showAsDropDown(anchor, xoff, yoff, gravity)
        binding.seekBar.progress = AudioPlayService.timeMinute
    }

    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        super.showAtLocation(parent, gravity, x, y)
        binding.seekBar.progress = AudioPlayService.timeMinute
    }

    private fun setProcessTextValue(process: Int) {
        binding.tvSeekValue.text = context.getString(R.string.timer_m, process)
    }

}


class SpeedControlPopup(private val context: Context) :
    PopupWindow(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) {

    private val binding = PopupSeekBarBinding.inflate(LayoutInflater.from(context))

    init {
        contentView = binding.root
        isTouchable = true
        isOutsideTouchable = false
        isFocusable = true

        // 设置速度范围 (50%-200%)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.seekBar.min = 50
        }
        binding.seekBar.max = 200
        binding.seekBar.progress = (AudioPlayService.playSpeed * 100).toInt()
        
        // 设置初始值文本
        updateSpeedText(binding.seekBar.progress)
        
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateSpeedText(progress)
                if (fromUser) {
                    // 设置播放速度 (转换为0.5-2.0范围)
                    AudioPlay.setSpeed(progress / 100f)
                }
            }
        })
    }

    override fun showAsDropDown(anchor: View?, xoff: Int, yoff: Int, gravity: Int) {
        super.showAsDropDown(anchor, xoff, yoff, gravity)
        binding.seekBar.progress = (AudioPlayService.playSpeed * 100).toInt()
    }

    override fun showAtLocation(parent: View?, gravity: Int, x: Int, y: Int) {
        super.showAtLocation(parent, gravity, x, y)
        binding.seekBar.progress = (AudioPlayService.playSpeed * 100).toInt()
    }
    
    private fun updateSpeedText(speedPercent: Int) {
        //  ( 100 -> 1.0, 120 -> 1.2)
        val speed = speedPercent / 100f
        binding.tvSeekValue.text = if (speed % 1 == 0f) {
            // 整数速度 (如 1.0X, 2.0X)
            "${speed.toInt()}.0X"
        } else {
            // 小数速度 (如 1.2X, 1.5X)
            "%.1fX".format(speed)
        }
    }
}