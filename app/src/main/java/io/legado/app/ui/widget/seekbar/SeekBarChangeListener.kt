package io.legado.app.ui.widget.seekbar

import android.widget.SeekBar

interface SeekBarChangeListener : SeekBar.OnSeekBarChangeListener {

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {

    }

}