package io.legado.app.service

import android.app.PendingIntent
import android.content.*
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.Action
import io.legado.app.constant.Bus
import io.legado.app.constant.Status
import io.legado.app.help.IntentDataHelp
import io.legado.app.help.IntentHelp
import io.legado.app.help.MediaHelp
import io.legado.app.receiver.MediaButtonReceiver
import io.legado.app.service.notification.ReadAloudNotification
import io.legado.app.ui.widget.page.TextChapter
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.postEvent
import kotlinx.coroutines.launch
import org.jetbrains.anko.toast
import java.util.*

class ReadAloudService : BaseService(), TextToSpeech.OnInitListener, AudioManager.OnAudioFocusChangeListener {

    companion object {
        val tag: String = ReadAloudService::class.java.simpleName
        var isRun = false
        var textToSpeech: TextToSpeech? = null

        fun play(
            context: Context,
            title: String,
            subtitle: String,
            pageIndex: Int,
            dataKey: String,
            play: Boolean = true
        ) {
            val readAloudIntent = Intent(context, ReadAloudService::class.java)
            readAloudIntent.action = Action.play
            readAloudIntent.putExtra("title", title)
            readAloudIntent.putExtra("subtitle", subtitle)
            readAloudIntent.putExtra("pageIndex", pageIndex)
            readAloudIntent.putExtra("dataKey", dataKey)
            readAloudIntent.putExtra("play", play)
            context.startService(readAloudIntent)
        }

        fun pause(context: Context) {
            if (isRun) {
                val intent = Intent(context, ReadAloudService::class.java)
                intent.action = Action.pause
                context.startService(intent)
            }
        }

        fun resume(context: Context) {
            if (isRun) {
                val intent = Intent(context, ReadAloudService::class.java)
                intent.action = Action.resume
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            if (isRun) {
                val intent = Intent(context, ReadAloudService::class.java)
                intent.action = Action.stop
                context.startService(intent)
            }
        }

        fun prevParagraph(context: Context) {
            if (isRun) {
                val intent = Intent(context, ReadAloudService::class.java)
                intent.action = Action.prevParagraph
                context.startService(intent)
            }
        }

        fun nextParagraph(context: Context) {
            if (isRun) {
                val intent = Intent(context, ReadAloudService::class.java)
                intent.action = Action.nextParagraph
                context.startService(intent)
            }
        }

        fun upTtsSpeechRate(context: Context) {
            if (isRun) {
                val intent = Intent(context, ReadAloudService::class.java)
                intent.action = Action.upTtsSpeechRate
                context.startService(intent)
            }
        }

        fun clearTTS() {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        }
    }

    private val handler = Handler()
    private var ttsIsSuccess: Boolean = false
    private lateinit var audioManager: AudioManager
    private lateinit var mFocusRequest: AudioFocusRequest
    private var broadcastReceiver: BroadcastReceiver? = null
    private var speak: Boolean = true
    private var nowSpeak: Int = 0
    private val contentList = arrayListOf<String>()
    private var readAloudNumber: Int = 0
    private var textChapter: TextChapter? = null
    private var pageIndex = 0
    var mediaSessionCompat: MediaSessionCompat? = null
    private val dsRunnable: Runnable? = Runnable { doDs() }
    var pause = false
    var title: String = ""
    var subtitle: String = ""
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
        upSpeechRate()
        ReadAloudNotification.upNotification(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        clearTTS()
        unregisterReceiver(broadcastReceiver)
        postEvent(Bus.ALOUD_STATE, Status.STOP)
        isRun = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                Action.play -> {
                    title = intent.getStringExtra("title") ?: ""
                    subtitle = intent.getStringExtra("subtitle") ?: ""
                    pageIndex = intent.getIntExtra("pageIndex", 0)
                    newReadAloud(
                        intent.getStringExtra("dataKey"),
                        intent.getBooleanExtra("play", true)
                    )
                }
                Action.pause -> pauseReadAloud(true)
                Action.resume -> resumeReadAloud()
                Action.upTtsSpeechRate -> upSpeechRate(true)
                Action.prevParagraph -> prevP()
                Action.nextParagraph -> nextP()
                Action.addTimer -> addTimer()
                Action.setTimer -> setTimer(intent.getIntExtra("minute", 0))
                else -> stopSelf()
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
                    stopSelf()
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

    private fun newReadAloud(dataKey: String?, play: Boolean) {
        dataKey?.let {
            textChapter = IntentDataHelp.getData(dataKey) as? TextChapter
            textChapter?.let {
                nowSpeak = 0
                readAloudNumber = it.getReadLength(pageIndex)
                contentList.clear()
                contentList.addAll(it.getUnRead(pageIndex).split("\n"))
                if (play) playTTS()
            } ?: stopSelf()
        } ?: stopSelf()
    }

    @Suppress("DEPRECATION")
    private fun playTTS() {
        if (contentList.size < 1 || !ttsIsSuccess) {
            return
        }
        if (requestFocus()) {
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

    private fun setTimer(minute: Int) {
        timeMinute = minute
        if (minute > 0) {
            handler.removeCallbacks(dsRunnable)
            handler.postDelayed(dsRunnable, 6000)
        }
        ReadAloudNotification.upNotification(this)
    }

    private fun addTimer() {
        if (timeMinute == 60) {
            timeMinute = 0
            handler.removeCallbacks(dsRunnable)
        } else {
            timeMinute += 10
            if (timeMinute > 60) timeMinute = 60
            handler.removeCallbacks(dsRunnable)
            handler.postDelayed(dsRunnable, 6000)
        }
        ReadAloudNotification.upNotification(this)
    }

    private fun doDs() {
        if (!pause) {
            timeMinute--
            if (timeMinute == 0) {
                stopSelf()
            } else if (timeMinute > 0) {
                handler.postDelayed(dsRunnable, 6000)
            }
        }
        ReadAloudNotification.upNotification(this)
    }

    private fun upSpeechRate(reset: Boolean = false) {
        if (this.getPrefBoolean("ttsFollowSys", true)) {
            if (reset) {
                clearTTS()
                textToSpeech = TextToSpeech(this, this)
            }
        } else {
            textToSpeech?.setSpeechRate((this.getPrefInt("ttsSpeechRate", 5) + 5) / 10f)
        }
    }

    private fun prevP() {
        if (nowSpeak > 0) {
            textToSpeech?.stop()
            nowSpeak--
            readAloudNumber -= contentList[nowSpeak].length.minus(1)
            playTTS()
        }
    }

    private fun nextP() {
        if (nowSpeak < contentList.size - 1) {
            textToSpeech?.stop()
            readAloudNumber += contentList[nowSpeak].length.plus(1)
            nowSpeak++
            playTTS()
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
        if (pause) postEvent(Bus.ALOUD_STATE, Status.PAUSE)
        this.pause = pause
        textToSpeech?.stop()
        ReadAloudNotification.upNotification(this)
    }

    private fun resumeReadAloud() {
        pause = false
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
                if (!pause) resumeReadAloud()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 永久丢失焦点除非重新主动获取，这种情况是被其他播放器抢去了焦点，  为避免与其他播放器混音，可将音乐暂停
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 暂时丢失焦点，这种情况是被其他应用申请了短暂的焦点，可压低后台音量
                if (!pause) pauseReadAloud(false)
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
        }

        override fun onDone(s: String) {
            readAloudNumber += contentList[nowSpeak].length + 1
            nowSpeak += 1
            if (nowSpeak >= contentList.size) {
                postEvent(Bus.TTS_NEXT, true)
            }
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            super.onRangeStart(utteranceId, start, end, frame)
            textChapter?.let {
                if (readAloudNumber + start > it.getReadLength(pageIndex + 1)) {
                    pageIndex++
                    postEvent(Bus.TTS_NEXT, false)
                    postEvent(Bus.TTS_START, readAloudNumber + start)
                }
            }
        }

        override fun onError(s: String) {
            launch {
                toast(s)
            }
        }

    }

}