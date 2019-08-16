package io.legado.app.help

import android.content.Context
import android.media.MediaPlayer
import android.support.v4.media.session.PlaybackStateCompat
import io.legado.app.R

object MediaHelp {
    const val MEDIA_SESSION_ACTIONS = (PlaybackStateCompat.ACTION_PLAY
            or PlaybackStateCompat.ACTION_PAUSE
            or PlaybackStateCompat.ACTION_PLAY_PAUSE
            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            or PlaybackStateCompat.ACTION_STOP
            or PlaybackStateCompat.ACTION_SEEK_TO)

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