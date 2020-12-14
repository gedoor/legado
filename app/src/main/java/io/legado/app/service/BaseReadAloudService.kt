package io.legado.app.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.CallSuper
import androidx.core.app.NotificationCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.*
import io.legado.app.help.IntentDataHelp
import io.legado.app.help.IntentHelp
import io.legado.app.help.MediaHelp
import io.legado.app.receiver.MediaButtonReceiver
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.book.read.page.entities.TextChapter
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.postEvent

abstract class BaseReadAloudService : BaseService(),
    AudioManager.OnAudioFocusChangeListener {

    companion object {
        var isRun = false
        var timeMinute: Int = 0
        var pause = true

        fun isPlay(): Boolean {
            return isRun && !pause
        }
    }

    internal val handler = Handler(Looper.getMainLooper())
    private lateinit var audioManager: AudioManager
    private var mFocusRequest: AudioFocusRequest? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private lateinit var mediaSessionCompat: MediaSessionCompat
    private var title: String = ""
    private var subtitle: String = ""
    internal val contentList = arrayListOf<String>()
    internal var nowSpeak: Int = 0
    internal var readAloudNumber: Int = 0
    internal var textChapter: TextChapter? = null
    internal var pageIndex = 0
    private val dsRunnable: Runnable = Runnable { doDs() }

    override fun onCreate() {
        super.onCreate()
        isRun = true
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mFocusRequest = MediaHelp.getFocusRequest(this)
        mediaSessionCompat = MediaSessionCompat(this, "readAloud")
        initMediaSession()
        initBroadcastReceiver()
        upNotification()
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRun = false
        pause = true
        unregisterReceiver(broadcastReceiver)
        postEvent(EventBus.ALOUD_STATE, Status.STOP)
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_STOPPED)
        mediaSessionCompat.release()
        ReadBook.uploadProgress()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                IntentAction.play -> {
                    title = intent.getStringExtra("title") ?: ""
                    subtitle = intent.getStringExtra("subtitle") ?: ""
                    pageIndex = intent.getIntExtra("pageIndex", 0)
                    newReadAloud(
                        intent.getStringExtra("dataKey"),
                        intent.getBooleanExtra("play", true)
                    )
                }
                IntentAction.pause -> pauseReadAloud(true)
                IntentAction.resume -> resumeReadAloud()
                IntentAction.upTtsSpeechRate -> upSpeechRate(true)
                IntentAction.prevParagraph -> prevP()
                IntentAction.nextParagraph -> nextP()
                IntentAction.addTimer -> addTimer()
                IntentAction.setTimer -> setTimer(intent.getIntExtra("minute", 0))
                else -> stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @CallSuper
    open fun newReadAloud(dataKey: String?, play: Boolean) {
        dataKey?.let {
            textChapter = IntentDataHelp.getData<TextChapter>(dataKey)
            textChapter?.let { textChapter ->
                nowSpeak = 0
                readAloudNumber = textChapter.getReadLength(pageIndex)
                contentList.clear()
                if (getPrefBoolean(PreferKey.readAloudByPage)) {
                    for (index in pageIndex..textChapter.lastIndex) {
                        textChapter.page(index)?.text?.split("\n")?.let {
                            contentList.addAll(it)
                        }
                    }
                } else {
                    textChapter.getUnRead(pageIndex).split("\n").forEach {
                        if (it.isNotEmpty()) {
                            contentList.add(it)
                        }
                    }
                }
                if (play) play()
            } ?: stopSelf()
        } ?: stopSelf()
    }

    open fun play() {
        pause = false
        upNotification()
        postEvent(EventBus.ALOUD_STATE, Status.PLAY)
    }

    @CallSuper
    open fun pauseReadAloud(pause: Boolean) {
        BaseReadAloudService.pause = pause
        upNotification()
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED)
        postEvent(EventBus.ALOUD_STATE, Status.PAUSE)
        ReadBook.uploadProgress()
    }

    @CallSuper
    open fun resumeReadAloud() {
        pause = false
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        if (timeMinute > 1) {
            doDs()
        }
    }

    abstract fun upSpeechRate(reset: Boolean = false)

    abstract fun prevP()

    abstract fun nextP()

    private fun setTimer(minute: Int) {
        timeMinute = minute
        if (minute > 0) {
            handler.removeCallbacks(dsRunnable)
            handler.postDelayed(dsRunnable, 60000)
        }
        upNotification()
    }

    private fun addTimer() {
        if (timeMinute == 180) {
            timeMinute = 0
            handler.removeCallbacks(dsRunnable)
        } else {
            timeMinute += 10
            if (timeMinute > 180) timeMinute = 180
            handler.removeCallbacks(dsRunnable)
            handler.postDelayed(dsRunnable, 60000)
        }
        postEvent(EventBus.TTS_DS, timeMinute)
        upNotification()
    }

    /**
     * 定时
     */
    private fun doDs() {
        if (!pause) {
            timeMinute--
            if (timeMinute == 0) {
                stopSelf()
            } else if (timeMinute > 0) {
                handler.postDelayed(dsRunnable, 60000)
            }
        }
        postEvent(EventBus.TTS_DS, timeMinute)
        upNotification()
    }

    /**
     * @return 音频焦点
     */
    fun requestFocus(): Boolean {
        return MediaHelp.requestFocus(audioManager, this, mFocusRequest)
    }

    /**
     * 更新媒体状态
     */
    private fun upMediaSessionPlaybackState(state: Int) {
        mediaSessionCompat.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(MediaHelp.MEDIA_SESSION_ACTIONS)
                .setState(state, nowSpeak.toLong(), 1f)
                .build()
        )
    }

    /**
     * 初始化MediaSession, 注册多媒体按钮
     */
    private fun initMediaSession() {
        mediaSessionCompat.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
                return MediaButtonReceiver.handleIntent(this@BaseReadAloudService, mediaButtonEvent)
            }
        })
        mediaSessionCompat.setMediaButtonReceiver(
            PendingIntent.getBroadcast(
                this,
                0,
                Intent(
                    Intent.ACTION_MEDIA_BUTTON,
                    null,
                    App.INSTANCE,
                    MediaButtonReceiver::class.java
                ),
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        )
        mediaSessionCompat.isActive = true
    }

    /**
     * 断开耳机监听
     */
    private fun initBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                    pauseReadAloud(true)
                }
            }
        }
        val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(broadcastReceiver, intentFilter)
    }

    /**
     * 音频焦点变化
     */
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
     * 更新通知
     */
    private fun upNotification() {
        var nTitle: String = when {
            pause -> getString(R.string.read_aloud_pause)
            timeMinute in 1..180 -> getString(
                R.string.read_aloud_timer,
                timeMinute
            )
            else -> getString(R.string.read_aloud_t)
        }
        nTitle += ": $title"
        var nSubtitle = subtitle
        if (subtitle.isEmpty())
            nSubtitle = getString(R.string.read_aloud_s)
        val builder = NotificationCompat.Builder(this, AppConst.channelIdReadAloud)
            .setSmallIcon(R.drawable.ic_volume_up)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_read_book))
            .setOngoing(true)
            .setContentTitle(nTitle)
            .setContentText(nSubtitle)
            .setContentIntent(
                IntentHelp.activityPendingIntent<ReadBookActivity>(this, "activity")
            )
        if (pause) {
            builder.addAction(
                R.drawable.ic_play_24dp,
                getString(R.string.resume),
                aloudServicePendingIntent(IntentAction.resume)
            )
        } else {
            builder.addAction(
                R.drawable.ic_pause_24dp,
                getString(R.string.pause),
                aloudServicePendingIntent(IntentAction.pause)
            )
        }
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.stop),
            aloudServicePendingIntent(IntentAction.stop)
        )
        builder.addAction(
            R.drawable.ic_time_add_24dp,
            getString(R.string.set_timer),
            aloudServicePendingIntent(IntentAction.addTimer)
        )
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        val notification = builder.build()
        startForeground(AppConst.notificationIdRead, notification)
    }

    abstract fun aloudServicePendingIntent(actionStr: String): PendingIntent?

    open fun nextChapter() {
        ReadBook.upReadStartTime()
        if (!ReadBook.moveToNextChapter(true)) {
            stopSelf()
        }
    }
}