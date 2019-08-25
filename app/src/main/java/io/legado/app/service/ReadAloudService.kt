package io.legado.app.service

import android.app.PendingIntent
import android.content.*
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.Bus
import io.legado.app.constant.Status
import io.legado.app.help.IntentHelp
import io.legado.app.help.MediaHelp
import io.legado.app.receiver.MediaButtonReceiver
import io.legado.app.service.notification.ReadAloudNotification
import io.legado.app.utils.postEvent
import io.legado.app.utils.toast
import kotlinx.coroutines.launch
import java.util.*

class ReadAloudService : BaseService(), TextToSpeech.OnInitListener, AudioManager.OnAudioFocusChangeListener {

    companion object {
        val tag: String = ReadAloudService::class.java.simpleName
        var isRun = false

        fun play(context: Context, title: String, subtitle: String, body: String) {
            val readAloudIntent = Intent(context, ReadAloudService::class.java)
            readAloudIntent.action = "play"
            readAloudIntent.putExtra("title", title)
            readAloudIntent.putExtra("subtitle", subtitle)
            readAloudIntent.putExtra("body", body)
            context.startService(readAloudIntent)
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

    private var textToSpeech: TextToSpeech? = null
    private var ttsIsSuccess: Boolean = false
    private lateinit var audioManager: AudioManager
    private lateinit var mFocusRequest: AudioFocusRequest
    var mediaSessionCompat: MediaSessionCompat? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var speak: Boolean = true
    private var nowSpeak: Int = 0
    private val contentList = arrayListOf<String>()
    var pause = false
    var title: String = ""
    var subtitle: String = ""
    private var readAloudNumber: Int = 0
    var timeMinute: Int = 0

    override fun onCreate() {
        super.onCreate()
        isRun = true
        textToSpeech = TextToSpeech(this, this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mFocusRequest = MediaHelp.getFocusRequest(this)
        }
        initMediaSession()
        initBroadcastReceiver()
        upMediaSessionPlaybackState()
        ReadAloudNotification.upNotification(this)
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
                    title = intent.getStringExtra("title") ?: ""
                    subtitle = intent.getStringExtra("subtitle") ?: ""
                    newReadAloud(intent.getStringExtra("body"))
                }
                "pause" -> {
                    pauseReadAloud(true)
                }
                "resume" -> {
                    resumeReadAloud()
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
                    IntentHelp.toTTSSetting(this@ReadAloudService)
                } else {
                    textToSpeech?.setOnUtteranceProgressListener(TTSUtteranceListener())
                    ttsIsSuccess = true
                    playTTS()
                }
            } else {
                toast(R.string.tts_init_failed)
            }
        }
    }

    private fun newReadAloud(body: String?) {
        if (body.isNullOrEmpty()) {
            stopSelf()
        } else {
            nowSpeak = 0
            readAloudNumber = 0
            contentList.clear()
            contentList.addAll(body.split("\n"))
        }
    }

    @Suppress("DEPRECATION")
    private fun playTTS() {
        if (contentList.size < 1 || !ttsIsSuccess) {
            return
        }
        if (!pause && requestFocus()) {
            postEvent(Bus.ALOUD_STATE, Status.PLAY)
            ReadAloudNotification.upNotification(this)
            for (i in nowSpeak until contentList.size) {
                if (i == 0) {
                    textToSpeech?.speak(contentList[i], TextToSpeech.QUEUE_FLUSH, null, "content")
                } else {
                    textToSpeech?.speak(contentList[i], TextToSpeech.QUEUE_ADD, null, "content")
                }
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
        this.pause = pause
        textToSpeech?.stop()
    }

    private fun resumeReadAloud() {
        playTTS()
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
                .setActions(MediaHelp.MEDIA_SESSION_ACTIONS)
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
                if (!pause) {
                    resumeReadAloud()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 永久丢失焦点除非重新主动获取，这种情况是被其他播放器抢去了焦点，  为避免与其他播放器混音，可将音乐暂停
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 暂时丢失焦点，这种情况是被其他应用申请了短暂的焦点，可压低后台音量
                if (!pause) {
                    pauseReadAloud(false)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // 短暂丢失焦点，这种情况是被其他应用申请了短暂的焦点希望其他声音能压低音量（或者关闭声音）凸显这个声音（比如短信提示音），
            }
        }
    }

    /**
     * 朗读监听
     */
    private inner class TTSUtteranceListener : UtteranceProgressListener() {

        override fun onStart(s: String) {
            postEvent(Bus.TTS_START, readAloudNumber + 1)
            postEvent(Bus.TTS_RANGE_START, readAloudNumber + 1)
        }

        override fun onDone(s: String) {
            readAloudNumber += contentList[nowSpeak].length + 1
            nowSpeak += 1
            if (nowSpeak >= contentList.size) {
                postEvent(Bus.ALOUD_STATE, Status.NEXT)
            }
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            super.onRangeStart(utteranceId, start, end, frame)
            postEvent(Bus.TTS_RANGE_START, readAloudNumber + start)
        }

        override fun onError(s: String) {

        }

    }

}