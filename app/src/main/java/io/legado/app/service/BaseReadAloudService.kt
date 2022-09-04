package io.legado.app.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.CallSuper
import androidx.core.app.NotificationCompat
import androidx.media.AudioFocusRequestCompat
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.*
import io.legado.app.help.MediaHelp
import io.legado.app.help.glide.ImageLoader
import io.legado.app.model.ReadAloud
import io.legado.app.model.ReadBook
import io.legado.app.receiver.MediaButtonReceiver
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.book.read.page.entities.TextChapter
import io.legado.app.utils.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import splitties.systemservices.audioManager

/**
 * 朗读服务
 */
abstract class BaseReadAloudService : BaseService(),
    AudioManager.OnAudioFocusChangeListener {

    companion object {
        @JvmStatic
        var isRun = false
            private set

        @JvmStatic
        var pause = true
            private set

        @JvmStatic
        var timeMinute: Int = 0
            private set

        fun isPlay(): Boolean {
            return isRun && !pause
        }
    }

    private val mFocusRequest: AudioFocusRequestCompat by lazy {
        MediaHelp.getFocusRequest(this)
    }
    private val mediaSessionCompat: MediaSessionCompat by lazy {
        MediaSessionCompat(this, "readAloud")
    }
    private var audioFocusLossTransient = false
    internal val contentList = arrayListOf<String>()
    internal var nowSpeak: Int = 0
    internal var readAloudNumber: Int = 0
    internal var textChapter: TextChapter? = null
    internal var pageIndex = 0
    private var dsJob: Job? = null

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                pauseReadAloud(true)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRun = true
        pause = false
        initMediaSession()
        initBroadcastReceiver()
        upNotification()
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        doDs()
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
        when (intent?.action) {
            IntentAction.play -> newReadAloud(
                intent.getBooleanExtra("play", true),
                intent.getIntExtra("pageIndex", ReadBook.durPageIndex),
                intent.getIntExtra("startPos", 0)
            )
            IntentAction.pause -> pauseReadAloud(true)
            IntentAction.resume -> resumeReadAloud()
            IntentAction.upTtsSpeechRate -> upSpeechRate(true)
            IntentAction.prevParagraph -> prevP()
            IntentAction.nextParagraph -> nextP()
            IntentAction.addTimer -> addTimer()
            IntentAction.setTimer -> setTimer(intent.getIntExtra("minute", 0))
            else -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun newReadAloud(play: Boolean, pageIndex: Int, startPos: Int) {
        this.pageIndex = pageIndex
        textChapter = ReadBook.curTextChapter
        textChapter?.let { textChapter ->
            nowSpeak = 0
            readAloudNumber = textChapter.getReadLength(pageIndex) + startPos
            contentList.clear()
            val readAloudByPage = getPrefBoolean(PreferKey.readAloudByPage)
            textChapter.getNeedReadAloud(pageIndex, readAloudByPage, startPos)
                .split("\n").forEach { text ->
                    if (text.isNotEmpty()) {
                        contentList.add(text)
                    }
                }
            if (play) play()
        }
    }

    open fun play() {
        isRun = true
        pause = false
        upNotification()
        postEvent(EventBus.ALOUD_STATE, Status.PLAY)
    }

    abstract fun playStop()

    @CallSuper
    open fun pauseReadAloud(pause: Boolean) {
        BaseReadAloudService.pause = pause
        upNotification()
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED)
        postEvent(EventBus.ALOUD_STATE, Status.PAUSE)
        ReadBook.uploadProgress()
        doDs()
    }

    @CallSuper
    open fun resumeReadAloud() {
        pause = false
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        postEvent(EventBus.ALOUD_STATE, Status.PLAY)
    }

    abstract fun upSpeechRate(reset: Boolean = false)

    private fun prevP() {
        if (nowSpeak > 0) {
            playStop()
            nowSpeak--
            readAloudNumber -= contentList[nowSpeak].length.minus(1)
            play()
        } else {
            ReadBook.moveToPrevChapter(true)
        }
    }

    private fun nextP() {
        if (nowSpeak < contentList.size - 1) {
            playStop()
            readAloudNumber += contentList[nowSpeak].length.plus(1)
            nowSpeak++
            play()
        } else {
            nextChapter()
        }
    }

    private fun setTimer(minute: Int) {
        timeMinute = minute
        doDs()
    }

    private fun addTimer() {
        if (timeMinute == 180) {
            timeMinute = 0
        } else {
            timeMinute += 10
            if (timeMinute > 180) timeMinute = 180
        }
        doDs()
    }

    /**
     * 定时
     */
    @Synchronized
    private fun doDs() {
        postEvent(EventBus.READ_ALOUD_DS, timeMinute)
        upNotification()
        dsJob?.cancel()
        dsJob = launch {
            while (isActive) {
                delay(60000)
                if (!pause) {
                    if (timeMinute >= 0) {
                        timeMinute--
                    }
                    if (timeMinute == 0) {
                        ReadAloud.stop(this@BaseReadAloudService)
                    }
                }
                postEvent(EventBus.READ_ALOUD_DS, timeMinute)
                upNotification()
            }
        }
    }

    /**
     * @return 音频焦点
     */
    fun requestFocus(): Boolean {
        val requestFocus = MediaHelp.requestFocus(audioManager, mFocusRequest)
        if (!requestFocus) {
            toastOnUi("未获取到音频焦点")
        }
        return requestFocus
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
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun initMediaSession() {
        mediaSessionCompat.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
                return MediaButtonReceiver.handleIntent(this@BaseReadAloudService, mediaButtonEvent)
            }
        })
        mediaSessionCompat.setMediaButtonReceiver(
            broadcastPendingIntent<MediaButtonReceiver>(Intent.ACTION_MEDIA_BUTTON)
        )
        mediaSessionCompat.isActive = true
    }

    /**
     * 断开耳机监听
     */
    private fun initBroadcastReceiver() {
        val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(broadcastReceiver, intentFilter)
    }

    /**
     * 音频焦点变化
     */
    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                audioFocusLossTransient = false
                if (!pause) resumeReadAloud()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                if (audioFocusLossTransient) {
                    pauseReadAloud(true)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                audioFocusLossTransient = true
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
        execute {
            var nTitle: String = when {
                pause -> getString(R.string.read_aloud_pause)
                timeMinute > 0 -> getString(
                    R.string.read_aloud_timer,
                    timeMinute
                )
                else -> getString(R.string.read_aloud_t)
            }
            nTitle += ": ${ReadBook.book?.name}"
            var nSubtitle = ReadBook.curTextChapter?.title
            if (nSubtitle.isNullOrBlank())
                nSubtitle = getString(R.string.read_aloud_s)
            val builder = NotificationCompat
                .Builder(this@BaseReadAloudService, AppConst.channelIdReadAloud)
                .setSmallIcon(R.drawable.ic_volume_up)
                .setSubText(getString(R.string.read_aloud))
                .setOngoing(true)
                .setContentTitle(nTitle)
                .setContentText(nSubtitle)
                .setContentIntent(
                    activityPendingIntent<ReadBookActivity>("activity")
                )
            kotlin.runCatching {
                ImageLoader
                    .loadBitmap(this@BaseReadAloudService, ReadBook.book?.getDisplayCover())
                    .submit()
                    .get()
            }.getOrElse {
                BitmapFactory.decodeResource(resources, R.drawable.icon_read_book)
            }.let {
                builder.setLargeIcon(it)
            }
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
            builder
        }.onSuccess {
            startForeground(AppConst.notificationIdRead, it.build())
        }
    }

    abstract fun aloudServicePendingIntent(actionStr: String): PendingIntent?

    open fun nextChapter() {
        ReadBook.upReadTime()
        if (!ReadBook.moveToNextChapter(true)) {
            stopSelf()
        }
    }

}