package io.legado.app.help

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import io.legado.app.R

object MediaHelp {
    const val MEDIA_SESSION_ACTIONS = (PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            or PlaybackStateCompat.ACTION_REWIND
            or PlaybackStateCompat.ACTION_PLAY
            or PlaybackStateCompat.ACTION_PLAY_PAUSE
            or PlaybackStateCompat.ACTION_PAUSE
            or PlaybackStateCompat.ACTION_STOP
            or PlaybackStateCompat.ACTION_FAST_FORWARD
            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            or PlaybackStateCompat.ACTION_SEEK_TO
            or PlaybackStateCompat.ACTION_SET_RATING
            or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
            or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
            or PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
            or PlaybackStateCompat.ACTION_PLAY_FROM_URI
            or PlaybackStateCompat.ACTION_PREPARE
            or PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID
            or PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH
            or PlaybackStateCompat.ACTION_PREPARE_FROM_URI
            or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
            or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
            or PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED)

    fun getFocusRequest(audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener): AudioFocusRequest? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mPlaybackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(mPlaybackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
        } else {
            null
        }
    }

    /**
     * @return 音频焦点
     */
    fun requestFocus(
        audioManager: AudioManager,
        listener: AudioManager.OnAudioFocusChangeListener,
        focusRequest: AudioFocusRequest?
    ): Boolean {
        val request: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let {
                audioManager.requestAudioFocus(focusRequest)
            } ?: AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                listener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return request == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
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