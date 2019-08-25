package io.legado.app.help

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import io.legado.app.R

object MediaHelp {
    const val MEDIA_SESSION_ACTIONS = (PlaybackStateCompat.ACTION_PLAY
            or PlaybackStateCompat.ACTION_PAUSE
            or PlaybackStateCompat.ACTION_PLAY_PAUSE
            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            or PlaybackStateCompat.ACTION_STOP
            or PlaybackStateCompat.ACTION_SEEK_TO)

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFocusRequest(audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener): AudioFocusRequest {
        val mPlaybackAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(mPlaybackAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
    }

    fun playSilentSound(mContext: Context) {
        kotlin.runCatching {
            // Stupid Android 8 "Oreo" hack to make media buttons work
            val mMediaPlayer = MediaPlayer.create(mContext, R.raw.silent_sound)
            mMediaPlayer.setOnCompletionListener { mMediaPlayer.release() }
            mMediaPlayer.start()
        }
    }
}