package io.legado.app.service

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.media3.common.util.UnstableApi
import com.shuyu.gsyvideoplayer.listener.VideoAllCallBack
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppLog
import io.legado.app.constant.IntentAction
import io.legado.app.constant.NotificationId
import io.legado.app.constant.SourceType
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.gsyVideo.ExoVideoManager
import io.legado.app.help.gsyVideo.FloatingPlayer
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
    private lateinit var params: WindowManager.LayoutParams
    private var mediaSessionCompat: MediaSessionCompat? = null
    private val floatingView by lazy {
        LayoutInflater.from(this).inflate(R.layout.floating_video_player, FrameLayout(this), false)
    }
    private val playerView by lazy { floatingView.findViewById<FloatingPlayer>(R.id.floatingPlayerView) }
    private var videoUrl = ""
    private var isNew = true
    private var videoTitle: String? = null
    private var sourceKey: String? = null
    private var sourceType: Int? = null
    private var bookUrl: String? = null
    private var upNotificationJob: Coroutine<*>? = null
    private var animator: SpringAnimation? = null
    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (activity is VideoPlayerActivity) {
                // 确保 Activity 创建完成后才停止服务,留够事件复制播放器
                stopSelf()
            }
        }

        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    private fun updateViewPosition() {
        try {
            windowManager.updateViewLayout(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelAnimator() {
        animator?.cancel()
    }

    private val layoutParamsXProperty =
        object : FloatPropertyCompat<WindowManager.LayoutParams>("x") {
            override fun getValue(layoutParams: WindowManager.LayoutParams): Float {
                return layoutParams.x.toFloat()
            }

            override fun setValue(layoutParams: WindowManager.LayoutParams, value: Float) {
                layoutParams.x = value.toInt()
                updateViewPosition()
            }
        }

    private fun startEdgeAnimation() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val viewWidth = floatingView.width
        val viewHeight = floatingView.height
        val currentX = params.x
        val endX = if (viewWidth == screenWidth) {
            0
        } else if (currentX + viewWidth / 2 > screenWidth / 2) {
            screenWidth - viewWidth - 30
        } else {
            30
        }
        val currentY = params.y
        var endY = currentY
        if (currentY < 30) {
            endY = 30
        } else if (currentY > screenHeight - viewHeight - 60) {
            endY = screenHeight - viewHeight - 60
        }
        if (endY != currentY) {
            ObjectAnimator.ofInt(currentY, endY).apply {
                duration = 200
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    params.y = animation.animatedValue as Int
                    updateViewPosition()
                }
                start()
            }
        }
        animator = SpringAnimation(params, layoutParamsXProperty, endX.toFloat()).apply {
            spring.stiffness = SpringForce.STIFFNESS_LOW  // 低刚度更Q弹
            spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY  // 中等阻尼
            start()
        }
    }

    override fun onCreate() {
        super.onCreate()
        initMediaSession()
        startForegroundNotification()
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
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
        if (isNew) {
            startPlayback()
        } else {
            ExoVideoManager.clonePlayState(playerView)
            playerView.setSurfaceToPlay()
            playerView.startAfterPrepared()
        }
        setupPlayerView()
        if (floatingView.parent == null) {
            createFloatingWindow()
        }
        return super.onStartCommand(intent, flags, startId)
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

    private fun updateMediaSessionState() {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(playerView.currentState, playerView.getCurrentPositionWhenPlaying(), 1.0f)
            .build()
        mediaSessionCompat?.setPlaybackState(playbackState)
    }


    private fun createMediaSessionCallback() = object : MediaSessionCompat.Callback() {
        override fun onSeekTo(pos: Long) = playerView.seekTo(pos)
        override fun onPlay() = playerView.onVideoResume()
        override fun onPause() = playerView.onVideoPause()
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
        val screenWidth = resources.displayMetrics.widthPixels
        val windowWidth = screenWidth * 3 / 4 //默认为屏幕3/4宽
        val windowHeight = (windowWidth * 9 / 16) // 默认16:9比例
        // 设置窗口参数
        params = WindowManager.LayoutParams(
            windowWidth,
            windowHeight,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = 30
            y = screenWidth / 10
        }
        floatingView.setOnTouchListener(FloatingTouchListener())
        windowManager.addView(floatingView, params)

    }

    inner class FloatingTouchListener : OnTouchListener {
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var initialX = 0
        private var initialY = 0
        private var isClick = true

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isClick = true
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    initialX = params.x
                    initialY = params.y
                    cancelAnimator()
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    if (abs(deltaX) > 20 || abs(deltaY) > 20) {
                        isClick = false
                        params.x = (initialX + (event.rawX - initialTouchX)).toInt()
                        params.y = (initialY + (event.rawY - initialTouchY)).toInt()
                        updateViewPosition()
                        cancelAnimator()
                    } else {
                        isClick = true
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        playerView.showControlUi()
                    } else {
                        startEdgeAnimation()
                    }

                }
            }
            return false
        }
    }


    private fun setupPlayerView() {
        playerView.fullscreenB.setOnClickListener {
            toggleFullScreen()
        }
        playerView.backButton.setOnClickListener { stopSelf() }
        playerView.setVideoAllCallBack(object : VideoAllCallBack {
            override fun onStartPrepared(url: String?, vararg objects: Any?) {}
            override fun onPrepared(url: String?, vararg objects: Any?) {
                // 更新媒体会话元数据
                updateMediaMetadata()
                // 更新播放状态
                updateMediaSessionState()
            }

            override fun onClickStartIcon(url: String?, vararg objects: Any?) {}
            override fun onAutoComplete(url: String?, vararg objects: Any?) {
                stopSelf()
            }

            override fun onComplete(url: String?, vararg objects: Any?) {}
            override fun onEnterFullscreen(url: String?, vararg objects: Any?) {}
            override fun onQuitFullscreen(url: String?, vararg objects: Any?) {}
            override fun onQuitSmallWidget(url: String?, vararg objects: Any?) {}
            override fun onEnterSmallWidget(url: String?, vararg objects: Any?) {}
            override fun onTouchScreenSeekVolume(url: String?, vararg objects: Any?) {}
            override fun onTouchScreenSeekPosition(url: String?, vararg objects: Any?) {}
            override fun onTouchScreenSeekLight(url: String?, vararg objects: Any?) {}
            override fun onPlayError(url: String?, vararg objects: Any?) {}
            override fun onClickStartThumb(url: String?, vararg objects: Any?) {}
            override fun onClickBlank(url: String?, vararg objects: Any?) {}
            override fun onClickBlankFullscreen(url: String?, vararg objects: Any?) {}
            override fun onClickStartError(url: String?, vararg objects: Any?) {}
            override fun onClickStop(url: String?, vararg objects: Any?) {}
            override fun onClickStopFullscreen(url: String?, vararg objects: Any?) {}
            override fun onClickResume(url: String?, vararg objects: Any?) {}
            override fun onClickResumeFullscreen(url: String?, vararg objects: Any?) {}
            override fun onClickSeekbar(url: String?, vararg objects: Any?) {}
            override fun onClickSeekbarFullscreen(url: String?, vararg objects: Any?) {}
        })
    }

    private fun toggleFullScreen() {
        ExoVideoManager.savePlayState(playerView)
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
        playerView.needDestroy = false
        // 停止服务（关闭悬浮窗）
//        stopSelf()
    }

    private fun startPlayback() {
        val source = sourceKey?.let { it ->
            when (sourceType) {
                SourceType.book -> appDb.bookSourceDao.getBookSource(it)
                SourceType.rss -> appDb.rssSourceDao.getByKey(it)
                else -> null
            }
        }
        var toc: List<BookChapter>? = null
        val book = bookUrl?.let { it ->
            toc = appDb.bookChapterDao.getChapterList(it)
            appDb.bookDao.getBook(it) ?: appDb.searchBookDao.getSearchBook(it)?.toBook()
        }
        playerView.setUp(toc, source, book)
        playerView.startPlayLogic()
    }


    private fun updateMediaMetadata() {
        videoTitle?.let { title ->
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "视频播放")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, playerView.getDuration())
                .build()
            mediaSessionCompat?.setMetadata(metadata)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::windowManager.isInitialized && floatingView.parent != null) {
                windowManager.removeView(floatingView)
            }
            mediaSessionCompat?.release()
            upNotificationJob?.invokeOnCompletion {
                notificationManager.cancel(NotificationId.VideoPlayService)
            }
            application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
            playerView.getCurrentPlayer().release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}