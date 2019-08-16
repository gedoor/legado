package io.legado.app.service

import android.app.PendingIntent
import android.content.*
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.Bus
import io.legado.app.constant.Status
import io.legado.app.help.MediaHelp
import io.legado.app.help.PendingIntentHelp
import io.legado.app.receiver.MediaButtonReceiver
import io.legado.app.utils.postEvent
import io.legado.app.utils.toast
import kotlinx.coroutines.launch
import java.util.*

class ReadAloudService : BaseService(), TextToSpeech.OnInitListener, AudioManager.OnAudioFocusChangeListener {

    companion object {
        val tag: String = ReadAloudService::class.java.simpleName
        var isRun = false
        const val MEDIA_SESSION_ACTIONS = (PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                or PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_SEEK_TO)

        fun paly(context: Context, title: String, body: String) {

        }

        fun pause(context: Context) {
            if (isRun) {
                val intent = Intent(context, ReadAloudService::class.java)
                intent.action = "pause"
                context.startService(intent)
            }
        }

        fun resume(context: Context) {
            if (isRun) {
                val intent = Intent(context, ReadAloudService::class.java)
                intent.action = "resume"
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            if (isRun) {
                val intent = Intent(context, ReadAloudService::class.java)
                intent.action = "stop"
                context.startService(intent)
            }
        }
    }

    private val notificationId = 112201
    private var textToSpeech: TextToSpeech? = null
    private var ttsIsSuccess: Boolean = false
    private lateinit var audioManager: AudioManager
    private lateinit var mFocusRequest: AudioFocusRequest
    private var mediaSessionCompat: MediaSessionCompat? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var speak: Boolean = true
    private var nowSpeak: Int = 0
    private val contentList = arrayListOf<String>()
    private var pause = false
    private var title: String = ""
    private var subtitle: String = ""
    private var timeMinute: Int = 0

    override fun onCreate() {
        super.onCreate()
        isRun = true
        textToSpeech = TextToSpeech(this, this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initFocusRequest()
        initMediaSession()
        initBroadcastReceiver()
        upMediaSessionPlaybackState()
        upNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRun = false
        unregisterReceiver(broadcastReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                "play" -> {

                }
                "pause" -> {

                }
                "resume" -> {

                }
                "stop" -> {
                    stopSelf()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onInit(status: Int) {
        launch {
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.CHINA)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    toast(R.string.tts_fix)
                    toTTSSetting()
                } else {
                    textToSpeech?.setOnUtteranceProgressListener(TTSUtteranceListener())
                    ttsIsSuccess = true
                }
            } else {
                toast(R.string.tts_init_failed)
            }
        }
    }

    /**
     * 初始化MediaSession
     */
    private fun initMediaSession() {
        val mComponent = ComponentName(packageName, MediaButtonReceiver::class.java.name)
        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.component = mComponent
        val mediaButtonReceiverPendingIntent = PendingIntent.getBroadcast(
            this, 0,
            mediaButtonIntent, PendingIntent.FLAG_CANCEL_CURRENT
        )

        mediaSessionCompat = MediaSessionCompat(this, tag, mComponent, mediaButtonReceiverPendingIntent)
        mediaSessionCompat?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
                return MediaButtonReceiver.handleIntent(this@ReadAloudService, mediaButtonEvent)
            }
        })
        mediaSessionCompat?.setMediaButtonReceiver(mediaButtonReceiverPendingIntent)
    }

    private fun initFocusRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mPlaybackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            mFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(mPlaybackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this)
                .build()
        }
    }

    private fun initBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == action) {
                    pauseReadAloud(true)
                }
            }
        }
        val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun pauseReadAloud(pause: Boolean) {
        textToSpeech?.stop()
    }

    private fun resumeReadAloud() {

    }

    private fun playTTS() {
        if (contentList.size < 1) {
            postEvent(Bus.ALOUD_STATE, Status.NEXT)
            return
        }
        if (ttsIsSuccess && !speak && requestFocus()) {

        }
    }

    /**
     * @return 音频焦点
     */
    private fun requestFocus(): Boolean {
        MediaHelp.playSilentSound(this)
        val request: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(mFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return request == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun upMediaSessionPlaybackState() {
        mediaSessionCompat?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(MEDIA_SESSION_ACTIONS)
                .setState(
                    if (speak) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    nowSpeak.toLong(), 1f
                )
                .build()
        )
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // 重新获得焦点,  可做恢复播放，恢复后台音量的操作
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 永久丢失焦点除非重新主动获取，这种情况是被其他播放器抢去了焦点，  为避免与其他播放器混音，可将音乐暂停
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 暂时丢失焦点，这种情况是被其他应用申请了短暂的焦点，可压低后台音量
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // 短暂丢失焦点，这种情况是被其他应用申请了短暂的焦点希望其他声音能压低音量（或者关闭声音）凸显这个声音（比如短信提示音），
            }
        }
    }

    private fun toTTSSetting() {
        //跳转到文字转语音设置界面
        try {
            val intent = Intent()
            intent.action = "com.android.settings.TTS_SETTINGS"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (ignored: Exception) {
            toast(R.string.tip_cannot_jump_setting_page)
        }
    }

    /**
     * 更新通知
     */
    private fun upNotification() {
        var nTitle: String = when {
            pause -> getString(R.string.read_aloud_pause)
            timeMinute in 1..60 -> getString(R.string.read_aloud_timer, timeMinute)
            else -> getString(R.string.read_aloud_t)
        }
        nTitle += ": $title"
        if (subtitle.isEmpty())
            subtitle = getString(R.string.read_aloud_s)
        val builder = NotificationCompat.Builder(this, AppConst.channelIdReadAloud)
            .setSmallIcon(R.drawable.ic_volume_up)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_read_book))
            .setOngoing(true)
            .setContentTitle(nTitle)
            .setContentText(subtitle)
            .setContentIntent(PendingIntentHelp.readBookActivityPendingIntent(this))
        if (pause) {
            builder.addAction(
                R.drawable.ic_play_24dp,
                getString(R.string.resume),
                PendingIntentHelp.aloudServicePendingIntent(this, "resume")
            )
        } else {
            builder.addAction(
                R.drawable.ic_pause_24dp,
                getString(R.string.pause),
                PendingIntentHelp.aloudServicePendingIntent(this, "pause")
            )
        }
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.stop),
            PendingIntentHelp.aloudServicePendingIntent(this, "stop")
        )
        builder.addAction(
            R.drawable.ic_time_add_24dp,
            getString(R.string.set_timer),
            PendingIntentHelp.aloudServicePendingIntent(this, "setTimer")
        )
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSessionCompat?.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        val notification = builder.build()
        startForeground(notificationId, notification)
    }

    /**
     * 朗读监听
     */
    private inner class TTSUtteranceListener : UtteranceProgressListener() {

        override fun onStart(s: String) {

        }

        override fun onDone(s: String) {

        }

        override fun onError(s: String) {

        }

    }

}