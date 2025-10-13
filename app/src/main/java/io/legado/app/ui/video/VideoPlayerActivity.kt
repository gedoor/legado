package io.legado.app.ui.video

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.shuyu.gsyvideoplayer.listener.VideoAllCallBack
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.SourceType
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ActivityVideoPlayerBinding
import io.legado.app.help.gsyVideo.ExoVideoManager
import io.legado.app.help.gsyVideo.VideoPlayer
import io.legado.app.service.VideoPlayService
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.book.toc.TocActivityResult
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.utils.StartActivityContract
import io.legado.app.utils.gone
import io.legado.app.utils.sendToClip
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.toggleSystemBar
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible


class VideoPlayerActivity : VMBaseActivity<ActivityVideoPlayerBinding, VideoPlayerViewModel>() {
    override val binding by viewBinding(ActivityVideoPlayerBinding::inflate)
    override val viewModel by viewModels<VideoPlayerViewModel>()
    private var orientationUtils: OrientationUtils? = null
    private val playerView: VideoPlayer by lazy { binding.playerView }
    private var isNew = true
    private var heightNormal = 0
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
    private val tocActivityResult = registerForActivityResult(TocActivityResult()) {
        it?.let {
            if (it.first != viewModel.durChapterIndex) {
                viewModel.durChapterIndex = it.first
                viewModel.durChapterPos = it.second
                viewModel.saveRead()
                upView()
                startPlayback()
            }
        }
    }
    private var isFullScreen = false

    @OptIn(UnstableApi::class)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        isNew = intent.getBooleanExtra("isNew", true)
        if (!isNew) {
            ExoVideoManager.clonePlayState(playerView)
            playerView.setSurfaceToPlay()
            playerView.startAfterPrepared()
        }
        viewModel.initData(intent) {
            viewModel.saveRead(true)
            if (isNew) {
                startPlayback()
            }
            setupPlayerView()
            initView()
            upView()
        }
        onBackPressedDispatcher.addCallback(this) {
            if (isFullScreen) {
                toggleFullScreen()
                return@addCallback
            }
            finish()
        }
        //外部辅助的旋转，帮助全屏
        orientationUtils = OrientationUtils(this, playerView)
        orientationUtils?.isOnlyRotateLand = true
    }

    private fun initView() {
        binding.ivChapter.setOnClickListener {
            viewModel.bookUrl?.let {
                tocActivityResult.launch(it)
            }
        }
        viewModel.book?.let { showBook(it) }
        viewModel.toc?.let { showToc(it) }
    }

    private fun showBook(book: Book) {
        binding.run {
            showCover(book)
            tvName.text = book.name
            tvAuthor.text = getString(R.string.author_show, book.getRealAuthor())
            tvIntro.text = book.getDisplayIntro()
        }
    }

    private fun showCover(book: Book) {
        binding.ivCover.load(book.getDisplayCover(), book.name, book.author, false, book.origin)
    }

    private fun showToc(toc: List<BookChapter>) {
        val recyclerView = binding.chapters
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager
        val adapter = ChapterAdapter(toc) { chapter ->
            if (chapter.index != viewModel.durChapterIndex) {
                viewModel.durChapterIndex = chapter.index
                viewModel.durChapterPos = 0
                viewModel.saveRead()
                upView()
                startPlayback()
            }
        }
        recyclerView.adapter = adapter
        recyclerView.post {
            (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                viewModel.durChapterIndex,
                0
            )
        }
    }

    private fun upView() {
        binding.titleBar.title = viewModel.book?.durChapterTitle ?: viewModel.videoTitle
    }

    private fun toggleFullScreen() {
        orientationUtils?.resolveByClick()
        isFullScreen = !isFullScreen
        toggleSystemBar(!isFullScreen)
        if (isFullScreen) {
            orientationUtils?.isRotateWithSystem = false
            supportActionBar?.hide()
            binding.chaptersContainer.gone()
            binding.data.gone()
            val layoutParams = playerView.layoutParams
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            playerView.layoutParams = layoutParams
        } else {
            orientationUtils?.isRotateWithSystem = true
            supportActionBar?.show()
            binding.chaptersContainer.visible()
            binding.data.visible()
            val layoutParams = playerView.layoutParams
            layoutParams.height = heightNormal
            playerView.layoutParams = layoutParams
        }
    }


    @Suppress("DEPRECATION")
    @SuppressLint("SwitchIntDef")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        playerView.onConfigurationChanged(
            this,
            newConfig,
            orientationUtils,
            false,
            false
        ) // bar依靠这边进行隐藏处理
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }

            Configuration.ORIENTATION_PORTRAIT -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            }
        }
    }

    private fun startPlayback() {
        playerView.setUp(
            viewModel.toc,
            viewModel.source,
            viewModel.book
        )
        playerView.startPlayLogic()
    }

    private fun setupPlayerView() {
        val layoutParams = playerView.layoutParams
        val parentWidth = playerView.width
        layoutParams.height = (parentWidth * 9f / 16f).toInt()
        playerView.layoutParams = layoutParams //默认16:9
        //是否根据视频尺寸，自动选择竖屏全屏或者横屏全屏
        playerView.isAutoFullWithSize = true
        playerView.fullscreenButton.setOnClickListener { toggleFullScreen() }
        playerView.setBackFromFullScreenListener { toggleFullScreen() }
        playerView.setVideoAllCallBack(object : VideoAllCallBack {
            override fun onStartPrepared(url: String?, vararg objects: Any?) {}
            override fun onPrepared(url: String?, vararg objects: Any?) {
                playerView.post {
                    //根据实际视频比例再次调整
                    val videoWidth = playerView.currentVideoWidth
                    val videoHeight = playerView.currentVideoHeight
                    if (videoWidth > 0 && videoHeight > 0) {
                        val layoutParams = playerView.layoutParams
                        val parentWidth = playerView.width
                        val aspectRatio = videoHeight.toFloat() / videoWidth.toFloat()
                        val height = (parentWidth * aspectRatio).toInt()
                        val displayMetrics = resources.displayMetrics
                        val screenHeight = displayMetrics.heightPixels
                        heightNormal = if (height < screenHeight / 2) height else screenHeight / 2
                        //高度不超过一半屏幕
                        layoutParams.height = heightNormal
                        playerView.layoutParams = layoutParams
                    }
                }
            }

            override fun onClickStartIcon(url: String?, vararg objects: Any?) {}
            override fun onAutoComplete(url: String?, vararg objects: Any?) {
                if (viewModel.upDurIndex(1)) {
                    viewModel.saveRead()
                    upView()
                    startPlayback()
                }
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

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.video_play, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_login)?.isVisible =
            !viewModel.source?.loginUrl.isNullOrBlank()
        return super.onMenuOpened(featureId, menu)
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

    private fun startFloatingWindow() {
        ExoVideoManager.savePlayState(playerView)
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
        playerView.needDestroy = false
        finish() //如果在播放器复刻前活动被销毁，会导致状态继承异常（这里服务创建很快，没发现异常）
    }

    override fun onDestroy() {
        viewModel.durChapterPos = playerView.getCurrentPositionWhenPlaying().toInt()
        viewModel.saveRead()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        orientationUtils?.releaseListener()
        playerView.getCurrentPlayer().release()
        super.onDestroy()
    }
}