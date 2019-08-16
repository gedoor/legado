package io.legado.app.help

import android.content.Context
import android.media.MediaPlayer
import io.legado.app.R

object MediaHelp {


    fun playSilentSound(mContext: Context) {
        try {
            // Stupid Android 8 "Oreo" hack to make media buttons work
            val mMediaPlayer = MediaPlayer.create(mContext, R.raw.silent_sound)
            mMediaPlayer.setOnCompletionListener { it.release() }
            mMediaPlayer.start()
        } catch (ignored: Exception) {
        }

    }
}