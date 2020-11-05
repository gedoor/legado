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
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.constant.Status
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.help.IntentHelp
import io.legado.app.help.MediaHelp
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.receiver.MediaButtonReceiver
import io.legado.app.service.help.AudioPlay
import io.legado.app.ui.audio.AudioPlayActivity
import io.legado.app.utils.postEvent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast


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
    private var mFocusRequest: AudioFocusRequest? = null
    private var title: String = ""
    private var subtitle: String = ""
    private val mediaPlayer = MediaPlayer()
    private var mediaSessionCompat: MediaSessionCompat? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var url: String = ""
    private var position = 0
    private val dsRunnable: Runnable = Runnable { doDs() }
    private var mpRunnable: Runnable = Runnable { upPlayProgress() }
    private var bookChapter: BookChapter? = null
    private var playSpeed: Float = 1f

    override fun onCreate() {
        super.onCreate()
        isRun = true
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mFocusRequest = MediaHelp.getFocusRequest(this)
        mediaPlayer.setOnErrorListener(this)
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnCompletionListener(this)
        initMediaSession()
        initBroadcastReceiver()
        upNotification()
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                IntentAction.play -> {
                    AudioPlay.book?.let {
                        title = it.name
                        position = it.durChapterPos
                        loadContent(it.durChapterIndex)
                    }
                }
                IntentAction.pause -> pause(true)
                IntentAction.resume -> resume()
                IntentAction.prev -> moveToPrev()
                IntentAction.next -> moveToNext()
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
            try {
                AudioPlay.status = Status.PLAY
                postEvent(EventBus.AUDIO_STATE, Status.PLAY)
                mediaPlayer.reset()
                val analyzeUrl =
                    AnalyzeUrl(url, headerMapF = AudioPlay.headers(), useWebView = true)
                val uri = Uri.parse(analyzeUrl.url)
                mediaPlayer.setDataSource(this, uri, analyzeUrl.headerMap)
                mediaPlayer.prepareAsync()
            } catch (e: Exception) {
                launch {
                    toast("$url ${e.localizedMessage}")
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
                mediaPlayer.pause()
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
        mediaPlayer.start()
        mediaPlayer.seekTo(position)
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
    override fun onPrepared(mp: MediaPlayer?) {
        if (pause) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer.playbackParams = mediaPlayer.playbackParams.apply { speed = playSpeed }
        } else {
            mediaPlayer.start()
        }
        mediaPlayer.seekTo(position)
        postEvent(EventBus.AUDIO_SIZE, mediaPlayer.duration)
        bookChapter?.let {
            it.end = mediaPlayer.duration.toLong()
        }
        handler.removeCallbacks(mpRunnable)
        handler.post(mpRunnable)
    }

    /**
     * 播放出错
     */
    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        if (!mediaPlayer.isPlaying) {
            AudioPlay.status = Status.STOP
            postEvent(EventBus.AUDIO_STATE, Status.STOP)
            launch { toast("error: $what $extra $url") }
        }
        return true
    }

    /**
     * 播放结束
     */
    override fun onCompletion(mp: MediaPlayer?) {
        handler.removeCallbacks(mpRunnable)
        moveToNext()
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


    private fun loadContent(index: Int) {
        AudioPlay.book?.let { book ->
            if (addLoading(index)) {
                launch(IO) {
                    App.db.bookChapterDao().getChapter(book.bookUrl, index)?.let { chapter ->
                        if (index == AudioPlay.durChapterIndex) {
                            bookChapter = chapter
                            subtitle = chapter.title
                            postEvent(EventBus.AUDIO_SUB_TITLE, subtitle)
                            postEvent(EventBus.AUDIO_SIZE, chapter.end?.toInt() ?: 0)
                            postEvent(EventBus.AUDIO_PROGRESS, position)
                        }
                        loadContent(chapter)
                    } ?: removeLoading(index)
                }
            }
        }
    }

    private fun loadContent(chapter: BookChapter) {
        AudioPlay.book?.let { book ->
            AudioPlay.webBook?.getContent(book, chapter, scope = this)
                ?.onSuccess(IO) { content ->
                    if (content.isEmpty()) {
                        withContext(Main) {
                            toast("未获取到资源链接")
                        }
                        removeLoading(chapter.index)
                    } else {
                        BookHelp.saveContent(book, chapter, content)
                        contentLoadFinish(chapter, content)
                        removeLoading(chapter.index)
                    }
                }?.onError {
                    contentLoadFinish(chapter, it.localizedMessage ?: toString())
                    removeLoading(chapter.index)
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

    private fun moveToPrev() {
        if (AudioPlay.durChapterIndex > 0) {
            mediaPlayer.pause()
            AudioPlay.durChapterIndex--
            AudioPlay.durPageIndex = 0
            AudioPlay.book?.durChapterIndex = AudioPlay.durChapterIndex
            saveRead()
            position = 0
            loadContent(AudioPlay.durChapterIndex)
        }
    }

    private fun moveToNext() {
        if (AudioPlay.durChapterIndex < AudioPlay.chapterSize - 1) {
            mediaPlayer.pause()
            AudioPlay.durChapterIndex++
            AudioPlay.durPageIndex = 0
            AudioPlay.book?.durChapterIndex = AudioPlay.durChapterIndex
            saveRead()
            position = 0
            loadContent(AudioPlay.durChapterIndex)
        } else {
            stopSelf()
        }
    }

    private fun saveRead() {
        launch(IO) {
            AudioPlay.book?.let { book ->
                book.lastCheckCount = 0
                book.durChapterTime = System.currentTimeMillis()
                book.durChapterIndex = AudioPlay.durChapterIndex
                book.durChapterPos = AudioPlay.durPageIndex
                book.durChapterTitle = subtitle
                App.db.bookDao().update(book)
            }
        }
    }

    private fun saveProgress() {
        launch(IO) {
            AudioPlay.book?.let {
                App.db.bookDao().upProgress(it.bookUrl, AudioPlay.durPageIndex)
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
                R.string.read_aloud_timer,
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
        return MediaHelp.requestFocus(audioManager, this, mFocusRequest)
    }

    private fun thisPendingIntent(action: String): PendingIntent? {
        return IntentHelp.servicePendingIntent<AudioPlayService>(this, action)
    }
}