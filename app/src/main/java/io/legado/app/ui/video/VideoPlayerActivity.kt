package io.legado.app.ui.video

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.SourceType
import io.legado.app.databinding.ActivityVideoPlayerBinding
import io.legado.app.help.exoplayer.ExoPlayerHelper
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.analyzeRule.AnalyzeUrl.Companion.getMediaItem
import io.legado.app.service.VideoPlayService
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.utils.StartActivityContract
import io.legado.app.utils.sendToClip
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.toggleSystemBar
import io.legado.app.utils.viewbindingdelegate.viewBinding

class VideoPlayerActivity : VMBaseActivity<ActivityVideoPlayerBinding, VideoPlayerViewModel>() {
    override val binding by viewBinding(ActivityVideoPlayerBinding::inflate)
    override val viewModel by viewModels<VideoPlayerViewModel>()

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayerHelper.getExoPlayer(this)
    }
    private val playerView: PlayerView by lazy { binding.playerView }
    private val bookSourceEditResult =
        registerForActivityResult(StartActivityContract(BookSourceEditActivity::class.java)) {
            if (it.resultCode == RESULT_OK) {
                viewModel.upSource()
            }
        }
    private val rssSourceEditResult =
        registerForActivityResult(StartActivityContract(RssSourceEditActivity::class.java)) {
            if (it.resultCode == RESULT_OK) {
                viewModel.upSource()
            }
        }
    private var isFullScreen = false
    private val originalOrientation = requestedOrientation
    private var originalSpeed = 1.0f
    private var isLongPressTriggered = false
    private val gestureDetector: GestureDetector by lazy {
        GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onLongPress(e: MotionEvent) {
                    super.onLongPress(e)
                    isLongPressTriggered = true
                    originalSpeed = exoPlayer.playbackParameters.speed
                    exoPlayer.setPlaybackSpeed(3.0f)
                }
            }
        )
    }

    @OptIn(UnstableApi::class)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.initData(intent) {
            setupPlayer()
            setupPlayerView()
            binding.titleBar.title = viewModel.videoTitle
        }
        onBackPressedDispatcher.addCallback(this) {
            if (isFullScreen) {
                playerView.setFullscreenButtonState(false)
                return@addCallback
            }
            finish()
        }
    }

    private fun toggleFullScreen() {
        isFullScreen = !isFullScreen
        toggleSystemBar(!isFullScreen)
        val layoutParams = playerView.layoutParams
        requestedOrientation = if (isFullScreen) {
            supportActionBar?.hide()
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            playerView.layoutParams = layoutParams
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            supportActionBar?.show()
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            playerView.layoutParams = layoutParams
            originalOrientation
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @OptIn(UnstableApi::class)
    private fun setupPlayerView() {
        playerView.setShowPreviousButton(false)
        playerView.setShowNextButton(false)
        playerView.useController = true
        playerView.controllerAutoShow = false
        playerView.controllerShowTimeoutMs = 3000
        playerView.setFullscreenButtonClickListener {
            toggleFullScreen()
        }
        playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLongPressTriggered = false
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    exoPlayer.setPlaybackSpeed(originalSpeed)
                }
            }
            isLongPressTriggered
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.video_play, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_float_window -> startFloatingWindow()
            R.id.menu_login -> viewModel.source?.let {
                when (viewModel.sourceType) {
                    SourceType.book -> "bookSource"
                    SourceType.rss -> "rssSource"
                    else -> null
                }?.let {
                    startActivity<SourceLoginActivity> {
                        putExtra("type", it)
                        putExtra("key", viewModel.sourceKey)
                    }
                }
            }

            R.id.menu_copy_video_url -> sendToClip(viewModel.videoUrl)
            R.id.menu_edit_source -> viewModel.sourceKey?.let {
                when (viewModel.sourceType) {
                    SourceType.book -> bookSourceEditResult.launch {
                        putExtra("sourceUrl", it)
                    }
                    SourceType.rss -> rssSourceEditResult.launch {
                        putExtra("sourceUrl", it)
                    }
                }
            }

            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayer() {
        exoPlayer.run {
            playerView.player = this
            if (viewModel.isNew) {
                setMediaItem(
                    AnalyzeUrl(
                        viewModel.videoUrl,
                        source = viewModel.source,
                        ruleData = viewModel.book,
                        chapter = viewModel.chapter,
                        coroutineContext = viewModel.viewModelScope.coroutineContext
                    ).getMediaItem()
                )
            }
            prepare()
            // 自动开始播放
            playWhenReady = true
        }
    }

    private fun startFloatingWindow() {
        //解绑
        playerView.player = null
        // 启动悬浮窗服务
        val intent = Intent(this, VideoPlayService::class.java).apply {
            putExtra("videoUrl", viewModel.videoUrl)
            putExtra("isNew", false)
            putExtra("videoTitle", viewModel.videoTitle)
            putExtra("sourceKey", viewModel.sourceKey)
            putExtra("sourceType", viewModel.sourceType)
            putExtra("bookUrl", viewModel.bookUrl)
        }
        ContextCompat.startForegroundService(this, intent)
        finish()
    }


    override fun onResume() {
        super.onResume()
        exoPlayer.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放
        if (playerView.player != null) {
            ExoPlayerHelper.release()
        }
    }
}