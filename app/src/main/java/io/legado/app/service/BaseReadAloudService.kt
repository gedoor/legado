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

    private lateinit var audioManager: AudioManager
    private var mFocusRequest: AudioFocusRequestCompat? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private lateinit var mediaSessionCompat: MediaSessionCompat
    internal val contentList = arrayListOf<String>()
    internal var nowSpeak: Int = 0
    internal var readAloudNumber: Int = 0
    internal var textChapter: TextChapter? = null
    internal var pageIndex = 0
    private var dsJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        isRun = true
        pause = false
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mFocusRequest = MediaHelp.getFocusRequest(this)
        mediaSessionCompat = MediaSessionCompat(this, "readAloud")
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
        intent?.action?.let { action ->
            when (action) {
                IntentAction.play -> {
                    pageIndex = ReadBook.durPageIndex()
                    newReadAloud(
                        ReadBook.curTextChapter,
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
    open fun newReadAloud(textChapter: TextChapter?, play: Boolean) {
        textChapter?.let {
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
        }
    }

    open fun play() {
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
        postEvent(EventBus.TTS_DS, timeMinute)
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
                postEvent(EventBus.TTS_DS, timeMinute)
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
        val builder = NotificationCompat.Builder(this, AppConst.channelIdReadAloud)
            .setSmallIcon(R.drawable.ic_volume_up)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_read_book))
            .setOngoing(true)
            .setContentTitle(nTitle)
            .setContentText(nSubtitle)
            .setContentIntent(
                activityPendingIntent<ReadBookActivity>("activity")
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