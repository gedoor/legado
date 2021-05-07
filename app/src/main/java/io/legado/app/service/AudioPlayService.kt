package io.legado.app.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.AudioFocusRequestCompat
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.constant.Status
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.IntentHelp
import io.legado.app.help.MediaHelp
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.receiver.MediaButtonReceiver
import io.legado.app.service.help.AudioPlay
import io.legado.app.ui.audio.AudioPlayActivity
import io.legado.app.utils.postEvent
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.init.appCtx


class AudioPlayService : BaseService(),
    AudioManager.OnAudioFocusChangeListener,
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {

    companion object {
        var isRun = false
        var pause = false
        var timeMinute: Int = 0
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var audioManager: AudioManager
    private var mFocusRequest: AudioFocusRequestCompat? = null
    private var title: String = ""
    private var subtitle: String = ""
    private val mediaPlayer = MediaPlayer()
    private var mediaSessionCompat: MediaSessionCompat? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var url: String = ""
    private var position = 0
    private val dsRunnable: Runnable = Runnable { doDs() }
    private var mpRunnable: Runnable = Runnable { upPlayProgress() }
    private var playSpeed: Float = 1f

    override fun onCreate() {
        super.onCreate()
        isRun = true
        upNotification()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mFocusRequest = MediaHelp.getFocusRequest(this)
        mediaPlayer.setOnErrorListener(this)
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnCompletionListener(this)
        initMediaSession()
        initBroadcastReceiver()
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                IntentAction.play -> {
                    AudioPlay.book?.let {
                        title = it.name
                        subtitle = AudioPlay.durChapter?.title ?: ""
                        position = it.durChapterPos
                        loadContent()
                    }
                }
                IntentAction.pause -> pause(true)
                IntentAction.resume -> resume()
                IntentAction.prev -> AudioPlay.prev(this)
                IntentAction.next -> AudioPlay.next(this)
                IntentAction.adjustSpeed -> upSpeed(intent.getFloatExtra("adjust", 1f))
                IntentAction.addTimer -> addTimer()
                IntentAction.setTimer -> setTimer(intent.getIntExtra("minute", 0))
                IntentAction.adjustProgress -> {
                    adjustProgress(intent.getIntExtra("position", position))
                }
                else -> stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRun = false
        handler.removeCallbacks(dsRunnable)
        handler.removeCallbacks(mpRunnable)
        mediaPlayer.release()
        mediaSessionCompat?.release()
        unregisterReceiver(broadcastReceiver)
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_STOPPED)
        AudioPlay.status = Status.STOP
        postEvent(EventBus.AUDIO_STATE, Status.STOP)
    }

    private fun play() {
        upNotification()
        if (requestFocus()) {
            kotlin.runCatching {
                AudioPlay.status = Status.STOP
                postEvent(EventBus.AUDIO_STATE, Status.STOP)
                mediaPlayer.reset()
                val analyzeUrl =
                    AnalyzeUrl(url, headerMapF = AudioPlay.headers(), useWebView = true)
                val uri = Uri.parse(analyzeUrl.url)
                mediaPlayer.setDataSource(this, uri, analyzeUrl.headerMap)
                mediaPlayer.prepareAsync()
                handler.removeCallbacks(mpRunnable)
            }.onFailure {
                it.printStackTrace()
                launch {
                    toastOnUi("$url ${it.localizedMessage}")
                    stopSelf()
                }
            }
        }
    }

    private fun pause(pause: Boolean) {
        if (url.contains(".m3u8", false)) {
            stopSelf()
        } else {
            try {
                AudioPlayService.pause = pause
                handler.removeCallbacks(mpRunnable)
                position = mediaPlayer.currentPosition
                if (mediaPlayer.isPlaying) mediaPlayer.pause()
                upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED)
                AudioPlay.status = Status.PAUSE
                postEvent(EventBus.AUDIO_STATE, Status.PAUSE)
                upNotification()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun resume() {
        pause = false
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            mediaPlayer.seekTo(position)
        }
        handler.removeCallbacks(mpRunnable)
        handler.postDelayed(mpRunnable, 1000)
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        AudioPlay.status = Status.PLAY
        postEvent(EventBus.AUDIO_STATE, Status.PLAY)
        upNotification()
    }

    private fun adjustProgress(position: Int) {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.seekTo(position)
        } else {
            this.position = position
        }
    }

    private fun upSpeed(adjust: Float) {
        kotlin.runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                playSpeed += adjust
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.playbackParams =
                        mediaPlayer.playbackParams.apply { speed = playSpeed }
                }
                postEvent(EventBus.AUDIO_SPEED, playSpeed)
            }
        }
    }

    /**
     * 加载完成
     */
    override fun onPrepared(mp: MediaPlayer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer.playbackParams = mediaPlayer.playbackParams.apply { speed = playSpeed }
        } else {
            mediaPlayer.start()
        }
        mediaPlayer.seekTo(position)
        AudioPlay.status = Status.PLAY
        postEvent(EventBus.AUDIO_STATE, Status.PLAY)
        postEvent(EventBus.AUDIO_SIZE, mediaPlayer.duration)
        handler.removeCallbacks(mpRunnable)
        handler.post(mpRunnable)
        AudioPlay.saveDurChapter(mediaPlayer.duration.toLong())
    }

    /**
     * 播放出错
     */
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        if (!mediaPlayer.isPlaying) {
            AudioPlay.status = Status.STOP
            postEvent(EventBus.AUDIO_STATE, Status.STOP)
            launch { toastOnUi("error: $what $extra $url") }
        }
        return true
    }

    /**
     * 播放结束
     */
    override fun onCompletion(mp: MediaPlayer) {
        handler.removeCallbacks(mpRunnable)
        AudioPlay.next(this)
    }

    private fun setTimer(minute: Int) {
        timeMinute = minute
        if (minute > 0) {
            handler.removeCallbacks(dsRunnable)
            handler.postDelayed(dsRunnable, 60000)
        }
        upNotification()
    }

    private fun addTimer() {
        if (timeMinute == 60) {
            timeMinute = 0
            handler.removeCallbacks(dsRunnable)
        } else {
            timeMinute += 10
            if (timeMinute > 60) timeMinute = 60
            handler.removeCallbacks(dsRunnable)
            handler.postDelayed(dsRunnable, 60000)
        }
        postEvent(EventBus.TTS_DS, timeMinute)
        upNotification()
    }

    /**
     * 更新播放进度
     */
    private fun upPlayProgress() {
        saveProgress()
        postEvent(EventBus.AUDIO_PROGRESS, mediaPlayer.currentPosition)
        handler.postDelayed(mpRunnable, 1000)
    }

    private fun loadContent() = with(AudioPlay) {
        durChapter?.let { chapter ->
            if (addLoading(durChapterIndex)) {
                val book = AudioPlay.book
                val webBook = AudioPlay.webBook
                if (book != null && webBook != null) {
                    webBook.getContent(this@AudioPlayService, book, chapter)
                        .onSuccess { content ->
                            if (content.isEmpty()) {
                                withContext(Main) {
                                    toastOnUi("未获取到资源链接")
                                }
                            } else {
                                contentLoadFinish(chapter, content)
                            }
                        }.onError {
                            contentLoadFinish(chapter, it.localizedMessage ?: toString())
                        }.onFinally {
                            removeLoading(chapter.index)
                        }
                } else {
                    removeLoading(chapter.index)
                    toastOnUi("book or source is null")
                }
            }
        }
    }

    private fun addLoading(index: Int): Boolean {
        synchronized(this) {
            if (AudioPlay.loadingChapters.contains(index)) return false
            AudioPlay.loadingChapters.add(index)
            return true
        }
    }

    private fun removeLoading(index: Int) {
        synchronized(this) {
            AudioPlay.loadingChapters.remove(index)
        }
    }

    /**
     * 加载完成
     */
    private fun contentLoadFinish(chapter: BookChapter, content: String) {
        if (chapter.index == AudioPlay.durChapterIndex) {
            subtitle = chapter.title
            url = content
            play()
        }
    }

    private fun saveProgress() {
        execute {
            AudioPlay.book?.let {
                AudioPlay.durChapterPos = mediaPlayer.currentPosition
                appDb.bookDao.upProgress(it.bookUrl, AudioPlay.durChapterPos)
            }
        }
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
     * 更新媒体状态
     */
    private fun upMediaSessionPlaybackState(state: Int) {
        mediaSessionCompat?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(MediaHelp.MEDIA_SESSION_ACTIONS)
                .setState(state, position.toLong(), 1f)
                .build()
        )
    }

    /**
     * 初始化MediaSession, 注册多媒体按钮
     */
    private fun initMediaSession() {
        mediaSessionCompat = MediaSessionCompat(this, "readAloud")
        mediaSessionCompat?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
                return MediaButtonReceiver.handleIntent(this@AudioPlayService, mediaButtonEvent)
            }
        })
        mediaSessionCompat?.setMediaButtonReceiver(
            PendingIntent.getBroadcast(
                this, 0,
                Intent(
                    Intent.ACTION_MEDIA_BUTTON,
                    null,
                    appCtx,
                    MediaButtonReceiver::class.java
                ),
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        )
        mediaSessionCompat?.isActive = true
    }

    /**
     * 断开耳机监听
     */
    private fun initBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                    pause(true)
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
                if (!pause) resume()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 永久丢失焦点除非重新主动获取，这种情况是被其他播放器抢去了焦点，  为避免与其他播放器混音，可将音乐暂停
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 暂时丢失焦点，这种情况是被其他应用申请了短暂的焦点，可压低后台音量
                if (!pause) pause(false)
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
            pause -> getString(R.string.audio_pause)
            timeMinute in 1..60 -> getString(
                R.string.playing_timer,
                timeMinute
            )
            else -> getString(R.string.audio_play_t)
        }
        nTitle += ": $title"
        var nSubtitle = subtitle
        if (subtitle.isEmpty()) {
            nSubtitle = getString(R.string.audio_play_s)
        }
        val builder = NotificationCompat.Builder(this, AppConst.channelIdReadAloud)
            .setSmallIcon(R.drawable.ic_volume_up)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_read_book))
            .setOngoing(true)
            .setContentTitle(nTitle)
            .setContentText(nSubtitle)
            .setContentIntent(
                IntentHelp.activityPendingIntent<AudioPlayActivity>(this, "activity")
            )
        if (pause) {
            builder.addAction(
                R.drawable.ic_play_24dp,
                getString(R.string.resume),
                thisPendingIntent(IntentAction.resume)
            )
        } else {
            builder.addAction(
                R.drawable.ic_pause_24dp,
                getString(R.string.pause),
                thisPendingIntent(IntentAction.pause)
            )
        }
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.stop),
            thisPendingIntent(IntentAction.stop)
        )
        builder.addAction(
            R.drawable.ic_time_add_24dp,
            getString(R.string.set_timer),
            thisPendingIntent(IntentAction.addTimer)
        )
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        val notification = builder.build()
        startForeground(AppConst.notificationIdAudio, notification)
    }

    /**
     * @return 音频焦点
     */
    private fun requestFocus(): Boolean {
        return MediaHelp.requestFocus(audioManager, mFocusRequest)
    }

    private fun thisPendingIntent(action: String): PendingIntent? {
        return IntentHelp.servicePendingIntent<AudioPlayService>(this, action)
    }
}