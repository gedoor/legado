package io.legado.app.service

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppLog
import io.legado.app.constant.IntentAction
import io.legado.app.constant.NotificationId
import io.legado.app.constant.SourceType
import io.legado.app.data.appDb
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.exoplayer.ExoPlayerHelper
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.analyzeRule.AnalyzeUrl.Companion.getMediaItem
import io.legado.app.receiver.MediaButtonReceiver
import io.legado.app.ui.video.VideoPlayerActivity
import io.legado.app.utils.activityPendingIntent
import io.legado.app.utils.broadcastPendingIntent
import io.legado.app.utils.servicePendingIntent
import splitties.systemservices.notificationManager
import kotlin.math.abs


/**
 * 视频悬浮窗服务
 */

class VideoPlayService : BaseService() {
    private lateinit var windowManager: WindowManager
    private var mediaSessionCompat: MediaSessionCompat? = null
    private val floatingView by lazy {
        LayoutInflater.from(this).inflate(R.layout.floating_video_player, null)
    }
    private val dragLayer by lazy { floatingView.findViewById<View>(R.id.dragLayer) }
    private val controlsLayout by lazy { floatingView.findViewById<View>(R.id.controlsLayout) }
    private val playerView by lazy { floatingView.findViewById<PlayerView>(R.id.floatingPlayerView) }
    private var videoUrl = ""
    private var isNew = true
    private var videoTitle: String? = null
    private var sourceKey: String? = null
    private var sourceType: Int? = null
    private var bookUrl: String? = null
    private var upNotificationJob: Coroutine<*>? = null
    private var isControlsVisible = false
    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayerHelper.getExoPlayer(this)
    }
    private val playerListener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            adjustWindowSize(videoSize)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    // 播放结束自动关闭悬浮窗
                    stopSelf()
                }

                Player.STATE_READY -> {
                    // 播放准备就绪后也调整窗口大小
                    adjustWindowSize(exoPlayer.videoSize)
                    // 更新媒体会话元数据
                    updateMediaMetadata()
                    // 更新播放状态
                    updateMediaSessionState()
                }

                Player.STATE_BUFFERING -> {
                }

                Player.STATE_IDLE -> {
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateMediaSessionState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        initMediaSession()
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                checkFloatPermission()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        intent?.let {
            videoUrl = intent.getStringExtra("videoUrl") ?: ""
            isNew = intent.getBooleanExtra("isNew", true)
            videoTitle = intent.getStringExtra("videoTitle")
            sourceKey = intent.getStringExtra("sourceKey")
            sourceType = intent.getIntExtra("sourceType", 0)
            bookUrl = intent.getStringExtra("bookUrl")
        }
        if (floatingView.parent == null) {
            createFloatingWindow()
        }
        setupPlayer()
        if (isNew) {
            startPlayback()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun setupPlayer() {
        exoPlayer.run {
            playerView.player = this
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(playerListener)
        }
    }

    private fun updateMediaSessionState() {
        val state = if (exoPlayer.isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, exoPlayer.currentPosition, 1.0f)
            .build()

        mediaSessionCompat?.setPlaybackState(playbackState)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun initMediaSession() {
        mediaSessionCompat = MediaSessionCompat(this, "videoPlayService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(createMediaSessionCallback())
            setMediaButtonReceiver(
                broadcastPendingIntent<MediaButtonReceiver>(Intent.ACTION_MEDIA_BUTTON)
            )
            isActive = true
        }
    }

    private fun createMediaSessionCallback() = object : MediaSessionCompat.Callback() {
        override fun onSeekTo(pos: Long) = exoPlayer.seekTo(pos)
        override fun onPlay() = exoPlayer.play()
        override fun onPause() = exoPlayer.pause()
    }

    override fun startForegroundNotification() {
        try {
            val notification = createNotification()
            startForeground(NotificationId.VideoPlayService, notification.build())
        } catch (e: Exception) {
            AppLog.put("创建视频播放通知出错,${e.localizedMessage}", e, true)
            //创建通知出错不结束服务就会崩溃,服务必须绑定通知
            stopSelf()
        }
    }

    private fun createNotification(): NotificationCompat.Builder {
        val nTitle = getString(R.string.audio_play_t) + ": $videoTitle"
        val nSubtitle = getString(R.string.audio_play_s)
        val builder = NotificationCompat.Builder(this@VideoPlayService, AppConst.channelIdReadAloud)
            .setSmallIcon(R.drawable.ic_volume_up)
            .setSubText(getString(R.string.video))
            .setOngoing(true).setOnlyAlertOnce(true)
            .setContentTitle(nTitle)
            .setContentText(nSubtitle).setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(
                activityPendingIntent<VideoPlayerActivity>("activity")
            )
//        builder.setLargeIcon(cover)
        // 添加操作按钮后再设置紧凑视图
        builder.addAction(
            R.drawable.ic_pause_24dp,
            getString(R.string.pause),
            servicePendingIntent<VideoPlayService>(IntentAction.pause)
        )

        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.stop),
            servicePendingIntent<VideoPlayService>(IntentAction.stop)
        )

        // 关联媒体会话到通知
        mediaSessionCompat?.sessionToken?.let { token ->
            builder.setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(token)
                    .setShowActionsInCompactView(0, 1)
            )
        }
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        return builder
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    private fun createFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val windowWidth = screenWidth //默认为屏幕宽
        val windowHeight = (windowWidth * 9 / 16) // 默认16:9比例
        // 设置窗口参数
        val params = WindowManager.LayoutParams(
            windowWidth,
            windowHeight,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = screenWidth / 10
            y = screenWidth / 10
        }
        windowManager.addView(floatingView, params)
        setupPlayerView()
        setupDragListener(params)
        setupControls()
    }


    @OptIn(UnstableApi::class)
    private fun setupPlayerView() {
        playerView.setShowFastForwardButton(false)
        playerView.setShowRewindButton(false)
        playerView.setShowPreviousButton(false)
        playerView.setShowNextButton(false)
        playerView.useController = true
        playerView.controllerAutoShow = false
        playerView.controllerShowTimeoutMs = 3000
        playerView.setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener { visibility ->
                if (visibility == View.VISIBLE) {
                    if (!isControlsVisible) {
                        isControlsVisible = true
                        showControls()
                    }
                } else {
                    if (isControlsVisible) {
                        isControlsVisible = false
                        hideControls()
                    }
                }
            }
        )
    }

    private fun setupDragListener(params: WindowManager.LayoutParams) {
        dragLayer.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false
            private val dragThreshold = 10
            private val handler = Handler(Looper.getMainLooper())
            private var isLongPressed = false
            private val longPressTimeout = ViewConfiguration.getLongPressTimeout()
            private val longPressRunnable = Runnable {
                exoPlayer.setPlaybackSpeed(2.0f)
                isLongPressed = true
            }

            @OptIn(UnstableApi::class)
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                return when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        handler.postDelayed(longPressRunnable, longPressTimeout.toLong())
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY

                        if (abs(deltaX) > dragThreshold || abs(deltaY) > dragThreshold) {
                            isDragging = true
                            handler.removeCallbacks(longPressRunnable)
                        }
                        params.x = initialX + deltaX.toInt()
                        params.y = initialY + deltaY.toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        isDragging
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        handler.removeCallbacks(longPressRunnable)
                        if (!isDragging && !isLongPressed) {
                            playerView.showController()
                        }
                        if (isLongPressed) {
                            exoPlayer.setPlaybackSpeed(1.0f)
                            isLongPressed = false
                        }
                        true
                    }
                    else -> false
                }
            }
        })
        dragLayer.setOnLongClickListener {
            exoPlayer.setPlaybackSpeed(2.0f)
            true
        }
    }

    private fun setupControls() {
        floatingView.findViewById<View>(R.id.btnClose).setOnClickListener {
            stopSelf()
        }
        floatingView.findViewById<View>(R.id.btnFullscreen).setOnClickListener {
            toggleFullscreen()
        }
    }

    private fun showControls() {
        controlsLayout.visibility = View.VISIBLE
        dragLayer.visibility = View.GONE
    }

    @OptIn(UnstableApi::class)
    private fun hideControls() {
        controlsLayout.visibility = View.GONE
        dragLayer.visibility = View.VISIBLE

    }


    private fun toggleFullscreen() {
        exoPlayer.removeListener(playerListener)
        playerView.player = null
        val fullscreenIntent = Intent(this, VideoPlayerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("videoUrl", videoUrl)
            putExtra("isNew", false)
            putExtra("videoTitle", videoTitle)
            putExtra("sourceKey", sourceKey)
            putExtra("sourceType", sourceType)
            putExtra("bookUrl", bookUrl)
        }
        startActivity(fullscreenIntent)
        // 停止服务（关闭悬浮窗）
        stopSelf()
    }

    private fun startPlayback() {
        val source = sourceKey?.let { it ->
            when (sourceType) {
                SourceType.book -> appDb.bookSourceDao.getBookSource(it)
                SourceType.rss -> appDb.rssSourceDao.getByKey(it)
                else -> null
            }
        }
        val book = bookUrl?.let { it ->
            appDb.bookDao.getBook(it) ?: appDb.searchBookDao.getSearchBook(it)?.toBook()
        }
        val chapter =
            book?.let { it -> appDb.bookChapterDao.getChapter(it.bookUrl, it.durChapterIndex) }
        exoPlayer.setMediaItem(
            AnalyzeUrl(
                videoUrl,
                source = source,
                ruleData = book,
                chapter = chapter
            ).getMediaItem()
        )
        exoPlayer.prepare()
        exoPlayer.play()
    }

    private fun updateMediaMetadata() {
        videoTitle?.let { title ->
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "视频播放")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, exoPlayer.duration)
                .build()
            mediaSessionCompat?.setMetadata(metadata)
        }
    }


    private fun adjustWindowSize(videoSize: VideoSize) {
        if (videoSize.width > 0 && videoSize.height > 0) {
            try {
                val displayMetrics = resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val windowWidth = screenWidth //和屏幕宽度一致
                // 视频内容帧宽高比
                var aspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                // 像素宽高比校正
                val pixelRatio = videoSize.pixelWidthHeightRatio
                if (pixelRatio > 0) {
                    aspectRatio *= pixelRatio
                }
                val windowHeight = (windowWidth / aspectRatio).toInt()
                val params = floatingView.layoutParams as WindowManager.LayoutParams
                params.width = windowWidth
                params.height = windowHeight
                windowManager.updateViewLayout(floatingView, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::windowManager.isInitialized && floatingView.parent != null) {
                windowManager.removeView(floatingView)
            }
            if (playerView.player != null) {
                ExoPlayerHelper.release()
            }
            mediaSessionCompat?.release()
            upNotificationJob?.invokeOnCompletion {
                notificationManager.cancel(NotificationId.VideoPlayService)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}