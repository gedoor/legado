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
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.*
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.MediaHelp
import io.legado.app.help.config.AppConfig
import io.legado.app.help.exoplayer.ExoPlayerHelper
import io.legado.app.help.glide.ImageLoader
import io.legado.app.model.AudioPlay
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.webBook.WebBook
import io.legado.app.receiver.MediaButtonReceiver
import io.legado.app.ui.book.audio.AudioPlayActivity
import io.legado.app.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import splitties.systemservices.audioManager

/**
 * 音频播放服务
 */
class AudioPlayService : BaseService(),
    AudioManager.OnAudioFocusChangeListener,
    Player.Listener {

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

        var url: String = ""
            private set
    }

    private val mFocusRequest: AudioFocusRequestCompat by lazy {
        MediaHelp.getFocusRequest(this)
    }
    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(this).setLoadControl(
            DefaultLoadControl.Builder().setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS / 10,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS / 10
            ).build()
        ).build()
    }
    private var mediaSessionCompat: MediaSessionCompat? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var needResumeOnAudioFocusGain = false
    private var position = AudioPlay.book?.durChapterPos ?: 0
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
        doDs()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                IntentAction.play -> {
                    pause = false
                    position = AudioPlay.book?.durChapterPos ?: 0
                    loadContent()
                }

                IntentAction.pause -> pause()
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
        abandonFocus()
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
            execute(context = Main) {
                AudioPlay.status = Status.STOP
                postEvent(EventBus.AUDIO_STATE, Status.STOP)
                upPlayProgressJob?.cancel()
                val analyzeUrl = AnalyzeUrl(
                    url,
                    source = AudioPlay.bookSource,
                    ruleData = AudioPlay.book,
                    chapter = AudioPlay.durChapter,
                    headerMapF = AudioPlay.headers(true),
                )
                val uri = Uri.parse(analyzeUrl.url)
                ExoPlayerHelper.preDownload(uri, analyzeUrl.headerMap)
                //休息1秒钟，防止403
                delay(1000)
                val mediaSource = ExoPlayerHelper
                    .createMediaSource(uri, analyzeUrl.headerMap)
                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.playWhenReady = true
                exoPlayer.prepare()
            }.onError {
                AppLog.put("播放出错\n${it.localizedMessage}", it)
                toastOnUi("$url ${it.localizedMessage}")
                stopSelf()
            }
        }
    }

    /**
     * 暂停播放
     */
    private fun pause(abandonFocus: Boolean = true) {
        try {
            pause = true
            if (abandonFocus) {
                abandonFocus()
            }
            upPlayProgressJob?.cancel()
            position = exoPlayer.currentPosition.toInt()
            if (exoPlayer.isPlaying) exoPlayer.pause()
            upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            AudioPlay.status = Status.PAUSE
            postEvent(EventBus.AUDIO_STATE, Status.PAUSE)
            upNotification()
        } catch (e: Exception) {
            e.printOnDebug()
        }
    }

    /**
     * 恢复播放
     */
    private fun resume() {
        try {
            pause = false
            if (url.isEmpty()) {
                loadContent()
                return
            }
            if (!exoPlayer.isPlaying) {
                exoPlayer.play()
            }
            upPlayProgress()
            upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            AudioPlay.status = Status.PLAY
            postEvent(EventBus.AUDIO_STATE, Status.PLAY)
            upNotification()
        } catch (e: Exception) {
            e.printOnDebug()
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
        val errorMsg = "音频播放出错\n${error.errorCodeName} ${error.errorCode}"
        AppLog.put(errorMsg, error)
        toastOnUi(errorMsg)
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
    private fun doDs() {
        postEvent(EventBus.AUDIO_DS, timeMinute)
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
                        AudioPlay.stop(this@AudioPlayService)
                    }
                }
                postEvent(EventBus.AUDIO_DS, timeMinute)
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
                    //更新buffer位置
                    postEvent(EventBus.AUDIO_BUFFER_PROGRESS, exoPlayer.bufferedPosition.toInt())
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
                    pause()
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
        if (AppConfig.ignoreAudioFocus) {
            AppLog.put("忽略音频焦点处理(有声)")
            return
        }
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (needResumeOnAudioFocusGain) {
                    AppLog.put("音频焦点获得,继续播放")
                    resume()
                } else {
                    AppLog.put("音频焦点获得")
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                AppLog.put("音频焦点丢失,暂停播放")
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                AppLog.put("音频焦点暂时丢失并会很快再次获得,暂停播放")
                needResumeOnAudioFocusGain = true
                if (!pause) {
                    needResumeOnAudioFocusGain = true
                    pause(false)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // 短暂丢失焦点，这种情况是被其他应用申请了短暂的焦点希望其他声音能压低音量（或者关闭声音）凸显这个声音（比如短信提示音），
                AppLog.put("音频焦点短暂丢失,不做处理")
            }
        }
    }

    /**
     * 更新通知
     */
    private fun upNotification() {
        execute {
            var nTitle: String = when {
                pause -> getString(R.string.audio_pause)
                timeMinute in 1..60 -> getString(
                    R.string.playing_timer,
                    timeMinute
                )
                else -> getString(R.string.audio_play_t)
            }
            nTitle += ": ${AudioPlay.book?.name}"
            var nSubtitle = AudioPlay.durChapter?.title
            if (nSubtitle.isNullOrEmpty()) {
                nSubtitle = getString(R.string.audio_play_s)
            }
            val builder = NotificationCompat
                .Builder(this@AudioPlayService, AppConst.channelIdReadAloud)
                .setSmallIcon(R.drawable.ic_volume_up)
                .setSubText(getString(R.string.audio))
                .setOngoing(true)
                .setContentTitle(nTitle)
                .setContentText(nSubtitle)
                .setContentIntent(
                    activityPendingIntent<AudioPlayActivity>("activity")
                )
            kotlin.runCatching {
                ImageLoader
                    .loadBitmap(this@AudioPlayService, AudioPlay.book?.getDisplayCover())
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
            builder
        }.onSuccess {
            startForeground(AppConst.notificationIdAudio, it.build())
        }
    }

    /**
     * 请求音频焦点
     * @return 音频焦点
     */
    private fun requestFocus(): Boolean {
        if (AppConfig.ignoreAudioFocus) {
            return true
        }
        return MediaHelp.requestFocus(audioManager, mFocusRequest)
    }

    /**
     * 放弃音频焦点
     */
    private fun abandonFocus() {
        @Suppress("DEPRECATION")
        audioManager.abandonAudioFocus(this)
    }

}