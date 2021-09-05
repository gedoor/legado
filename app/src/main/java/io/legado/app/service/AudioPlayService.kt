package io.legado.app.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.AudioFocusRequestCompat
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.constant.Status
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.ExoPlayerHelper
import io.legado.app.help.MediaHelp
import io.legado.app.help.http.okHttpClient
import io.legado.app.model.AudioPlay
import io.legado.app.model.ReadAloud
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.webBook.WebBook
import io.legado.app.receiver.MediaButtonReceiver
import io.legado.app.ui.book.audio.AudioPlayActivity
import io.legado.app.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main


class AudioPlayService : BaseService(),
    AudioManager.OnAudioFocusChangeListener,
    Player.Listener {

    companion object {
        var isRun = false
        var pause = false
        var timeMinute: Int = 0
        var url: String = ""
    }

    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private val mFocusRequest: AudioFocusRequestCompat by lazy {
        MediaHelp.getFocusRequest(this)
    }
    private val exoPlayer: SimpleExoPlayer by lazy {
        SimpleExoPlayer.Builder(this).build()
    }
    private var title: String = ""
    private var subtitle: String = ""
    private var mediaSessionCompat: MediaSessionCompat? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var position = 0
    private var dsJob: Job? = null
    private var upPlayProgressJob: Job? = null
    private var playSpeed: Float = 1f

    override fun onCreate() {
        super.onCreate()
        isRun = true
        upNotification()
        exoPlayer.addListener(this)
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
        exoPlayer.release()
        mediaSessionCompat?.release()
        unregisterReceiver(broadcastReceiver)
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_STOPPED)
        AudioPlay.status = Status.STOP
        postEvent(EventBus.AUDIO_STATE, Status.STOP)
    }

    /**
     * 播放音频
     */
    private fun play() {
        upNotification()
        if (requestFocus()) {
            kotlin.runCatching {
                AudioPlay.status = Status.STOP
                postEvent(EventBus.AUDIO_STATE, Status.STOP)
                upPlayProgressJob?.cancel()
                val analyzeUrl =
                    AnalyzeUrl(
                        url,
                        headerMapF = AudioPlay.headers(),
                        source = AudioPlay.bookSource,
                        useWebView = true
                    )
                val uri = Uri.parse(analyzeUrl.url)
                val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
                    .setDefaultRequestProperties(analyzeUrl.headerMap)
                val mediaSource = ExoPlayerHelper
                    .createMediaSource(uri, dataSourceFactory)
                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.playWhenReady = true
                exoPlayer.prepare()
            }.onFailure {
                it.printStackTrace()
                toastOnUi("$url ${it.localizedMessage}")
                stopSelf()
            }
        }
    }

    /**
     * 暂停播放
     */
    private fun pause(pause: Boolean) {
        try {
            AudioPlayService.pause = pause
            upPlayProgressJob?.cancel()
            position = exoPlayer.currentPosition.toInt()
            if (exoPlayer.isPlaying) exoPlayer.pause()
            upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            AudioPlay.status = Status.PAUSE
            postEvent(EventBus.AUDIO_STATE, Status.PAUSE)
            upNotification()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 恢复播放
     */
    private fun resume() {
        try {
            pause = false
            if (!exoPlayer.isPlaying) {
                exoPlayer.play()
            }
            upPlayProgress()
            upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            AudioPlay.status = Status.PLAY
            postEvent(EventBus.AUDIO_STATE, Status.PLAY)
            upNotification()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    /**
     * 调节进度
     */
    private fun adjustProgress(position: Int) {
        this.position = position
        exoPlayer.seekTo(position.toLong())
    }

    /**
     * 调节速度
     */
    private fun upSpeed(adjust: Float) {
        kotlin.runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                playSpeed += adjust
                exoPlayer.setPlaybackSpeed(playSpeed)
                postEvent(EventBus.AUDIO_SPEED, playSpeed)
            }
        }
    }

    /**
     * 播放状态监控
     */
    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when (playbackState) {
            Player.STATE_IDLE -> {
                // 空闲
            }
            Player.STATE_BUFFERING -> {
                // 缓冲中
            }
            Player.STATE_READY -> {
                // 准备好
                if (exoPlayer.currentPosition != position.toLong()) {
                    exoPlayer.seekTo(position.toLong())
                }
                if (exoPlayer.playWhenReady) {
                    AudioPlay.status = Status.PLAY
                    postEvent(EventBus.AUDIO_STATE, Status.PLAY)
                } else {
                    AudioPlay.status = Status.PAUSE
                    postEvent(EventBus.AUDIO_STATE, Status.PAUSE)
                }
                postEvent(EventBus.AUDIO_SIZE, exoPlayer.duration)
                upPlayProgress()
                AudioPlay.saveDurChapter(exoPlayer.duration)
            }
            Player.STATE_ENDED -> {
                // 结束
                upPlayProgressJob?.cancel()
                AudioPlay.next(this)
            }
        }
    }

    /**
     * 播放错误事件
     */
    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        AudioPlay.status = Status.STOP
        postEvent(EventBus.AUDIO_STATE, Status.STOP)
        error.printStackTrace()
    }

    override fun onPlayerErrorChanged(error: PlaybackException?) {
        super.onPlayerErrorChanged(error)
        postEvent(EventBus.AUDIO_ERROR, error?.localizedMessage)
    }

    private fun setTimer(minute: Int) {
        timeMinute = minute
        doDs()
    }

    private fun addTimer() {
        if (timeMinute == 60) {
            timeMinute = 0
        } else {
            timeMinute += 10
            if (timeMinute > 60) timeMinute = 60
        }
        doDs()
    }

    /**
     * 定时
     */
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
                        ReadAloud.stop(this@AudioPlayService)
                    }
                }
                postEvent(EventBus.TTS_DS, timeMinute)
                upNotification()
            }
        }
    }

    /**
     * 每隔1秒发送播放进度
     */
    private fun upPlayProgress() {
        upPlayProgressJob?.cancel()
        upPlayProgressJob = launch {
            while (isActive) {
                AudioPlay.book?.let {
                    it.durChapterPos = exoPlayer.currentPosition.toInt()
                    postEvent(EventBus.AUDIO_PROGRESS, it.durChapterPos)
                    saveProgress(it)
                }
                delay(1000)
            }
        }
    }

    /**
     * 加载播放URL
     */
    private fun loadContent() = with(AudioPlay) {
        durChapter?.let { chapter ->
            if (addLoading(chapter.index)) {
                val book = AudioPlay.book
                val bookSource = AudioPlay.bookSource
                if (book != null && bookSource != null) {
                    WebBook.getContent(this@AudioPlayService, bookSource, book, chapter)
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
        if (chapter.index == AudioPlay.book?.durChapterIndex) {
            subtitle = chapter.title
            url = content
            play()
        }
    }

    /**
     * 保存播放进度
     */
    private fun saveProgress(book: Book) {
        execute {
            appDb.bookDao.upProgress(book.bookUrl, book.durChapterPos)
        }
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
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun initMediaSession() {
        mediaSessionCompat = MediaSessionCompat(this, "readAloud")
        mediaSessionCompat?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
                return MediaButtonReceiver.handleIntent(this@AudioPlayService, mediaButtonEvent)
            }
        })
        mediaSessionCompat?.setMediaButtonReceiver(
            broadcastPendingIntent<MediaButtonReceiver>(Intent.ACTION_MEDIA_BUTTON)
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
                activityPendingIntent<AudioPlayActivity>("activity")
            )
        if (pause) {
            builder.addAction(
                R.drawable.ic_play_24dp,
                getString(R.string.resume),
                servicePendingIntent<AudioPlayService>(IntentAction.resume)
            )
        } else {
            builder.addAction(
                R.drawable.ic_pause_24dp,
                getString(R.string.pause),
                servicePendingIntent<AudioPlayService>(IntentAction.pause)
            )
        }
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.stop),
            servicePendingIntent<AudioPlayService>(IntentAction.stop)
        )
        builder.addAction(
            R.drawable.ic_time_add_24dp,
            getString(R.string.set_timer),
            servicePendingIntent<AudioPlayService>(IntentAction.addTimer)
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

}