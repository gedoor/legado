package io.legado.app.ui.video

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.shuyu.gsyvideoplayer.listener.VideoAllCallBack
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.RssSource
import io.legado.app.databinding.ActivityVideoPlayerBinding
import io.legado.app.help.book.removeType
import io.legado.app.help.config.AppConfig
import io.legado.app.help.gsyVideo.VideoPlayer
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.model.VideoPlay
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
    private val bookSourceEditResult =
        registerForActivityResult(StartActivityContract(BookSourceEditActivity::class.java)) {
            if (it.resultCode == RESULT_OK) {
                VideoPlay.upSource()
            }
        }
    private val rssSourceEditResult =
        registerForActivityResult(StartActivityContract(RssSourceEditActivity::class.java)) {
            if (it.resultCode == RESULT_OK) {
                VideoPlay.upSource()
            }
        }
    private val tocActivityResult = registerForActivityResult(TocActivityResult()) {
        it?.let {
            if (it.third) {
                if (VideoPlay.volumes.isEmpty()) {
                    VideoPlay.chapterInVolumeIndex = it.first
                } else {
                    for ((index, volume) in VideoPlay.volumes.reversed().withIndex()) {
                        if (volume.index < it.first) {
                            VideoPlay.chapterInVolumeIndex = it.first - volume.index - 1
                            VideoPlay.durVolumeIndex = VideoPlay.volumes.size - index - 1
                            VideoPlay.durVolume = volume
                            break
                        } else if (volume.index == it.first) {
                            VideoPlay.chapterInVolumeIndex = 0
                            VideoPlay.durVolumeIndex = VideoPlay.volumes.size - index - 1
                            VideoPlay.durVolume = volume
                            break
                        }
                    }
                }
                VideoPlay.durChapterPos = it.second
                VideoPlay.upEpisodes()
                VideoPlay.saveRead()
                if (VideoPlay.episodes.isNullOrEmpty()) {
                    binding.chapters.visibility = View.GONE
                } else {
                    binding.chapters.visibility = View.VISIBLE
                    val adapter = binding.chapters.adapter as? ChapterAdapter
                    adapter?.updateData(VideoPlay.episodes)
                }
                upView()
                VideoPlay.startPlay(playerView)
            }
        }
    }
    private var isFullScreen = false

    @OptIn(UnstableApi::class)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        isNew = intent.getBooleanExtra("isNew", true)
        VideoPlay.videoUrl = intent.getStringExtra("videoUrl")
        VideoPlay.videoTitle = intent.getStringExtra("videoTitle")
        val sourceKey = intent.getStringExtra("sourceKey")
        val sourceType = intent.getIntExtra("sourceType", 0)
        val bookUrl = intent.getStringExtra("bookUrl")
        VideoPlay.inBookshelf = intent.getBooleanExtra("inBookshelf", true)
        if (isNew) {
            VideoPlay.initSource(sourceKey, sourceType, bookUrl)
            VideoPlay.saveRead()
            VideoPlay.startPlay(playerView)
        } else {
            VideoPlay.clonePlayState(playerView)
            playerView.setSurfaceToPlay()
            playerView.startAfterPrepared()
        }
        setupPlayerView()
        initView()
        upView()
        onBackPressedDispatcher.addCallback(this) {
            if (isFullScreen) {
                toggleFullScreen()
                return@addCallback
            }
            finish()
        }
        orientationUtils = OrientationUtils(this, playerView) //旋转辅助
    }

    private fun initView() {
        binding.root.setBackgroundColor(backgroundColor)
        if (VideoPlay.book != null) {
            VideoPlay.book?.let { showBook(it) }
            if (VideoPlay.episodes.isNullOrEmpty()) {
                binding.chapters.visibility = View.GONE
            } else {
                binding.chapters.visibility = View.VISIBLE
                showToc(VideoPlay.episodes!!)
            }
            if (VideoPlay.volumes.isEmpty()) {
                binding.volumes.visibility = View.GONE
            } else {
                binding.volumes.visibility = View.VISIBLE
                showVolumes(VideoPlay.volumes)
            }
        } else {
            binding.data.visibility = View.INVISIBLE
            binding.chaptersContainer.visibility = View.INVISIBLE
        }
    }

    private fun showBook(book: Book) {
        binding.run {
            showCover(book)
            tvName.text = book.name
            tvAuthor.text = book.getRealAuthor()
            tvIntro.text = book.getDisplayIntro()
        }
    }

    private fun showCover(book: Book) {
        binding.ivCover.load(book.getDisplayCover(), book, false, book.origin)
    }

    private fun showToc(toc: List<BookChapter>) {
        binding.ivChapter.setOnClickListener {
            VideoPlay.book?.bookUrl?.let {
                tocActivityResult.launch(it)
            }
        }
        val recyclerView = binding.chapters
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager
        val adapter = ChapterAdapter(toc,VideoPlay.chapterInVolumeIndex, false) { chapter, index ->
            if (index != VideoPlay.chapterInVolumeIndex) {
                VideoPlay.chapterInVolumeIndex = index
                VideoPlay.durChapterPos = 0
                VideoPlay.saveRead()
                upView()
                VideoPlay.startPlay(playerView)
            }
        }
        recyclerView.adapter = adapter
        scrollToDurChapter(recyclerView, VideoPlay.chapterInVolumeIndex)
    }

    private fun showVolumes(volumes: List<BookChapter>) {
        val recyclerView = binding.volumes
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager
        val adapter = ChapterAdapter(volumes,VideoPlay.durVolumeIndex, true) { chapter, index ->
            if (index != VideoPlay.durVolumeIndex) {
                VideoPlay.durVolumeIndex = index
                VideoPlay.chapterInVolumeIndex = 0
                VideoPlay.durChapterPos = 0
                VideoPlay.upEpisodes()
                if (VideoPlay.episodes.isNullOrEmpty()) {
                    binding.chapters.visibility = View.GONE
                } else {
                    binding.chapters.visibility = View.VISIBLE
                    val adapter = binding.chapters.adapter as? ChapterAdapter
                    adapter?.updateData(VideoPlay.episodes)
                }
                VideoPlay.saveRead()
                upView()
                VideoPlay.startPlay(playerView)
            }
        }
        recyclerView.adapter = adapter
        scrollToDurChapter(recyclerView, VideoPlay.durVolumeIndex)
    }

    private fun scrollToDurChapter(recyclerView: RecyclerView, index: Int) {
        recyclerView.postDelayed({
            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
            layoutManager?.run {
                val smoothScroller = object : LinearSmoothScroller(this@VideoPlayerActivity) {
                    override fun getHorizontalSnapPreference(): Int {
                        return SNAP_TO_START // 滚动到最左边
                    }
                }
                smoothScroller.targetPosition = index
                this.startSmoothScroll(smoothScroller)
            }
            val adapter = recyclerView.adapter as? ChapterAdapter
            adapter?.updateSelectedPosition(index)
        }, 200)
    }

    private fun upView() {
        if (!VideoPlay.episodes.isNullOrEmpty()) {
            scrollToDurChapter(binding.chapters, VideoPlay.chapterInVolumeIndex)
        }
        if (!VideoPlay.volumes.isEmpty()) {
            scrollToDurChapter(binding.volumes, VideoPlay.durVolumeIndex)
        }
        binding.titleBar.title = VideoPlay.videoTitle
    }

    private fun toggleFullScreen() {
        isFullScreen = !isFullScreen
        toggleSystemBar(!isFullScreen)
        if (isFullScreen) {
            orientationUtils?.isOnlyRotateLand = true //旋转时仅处理横屏
            orientationUtils?.isRotateWithSystem = false //跟随系统旋转
            orientationUtils?.resolveByClick()
            supportActionBar?.hide()
            binding.chaptersContainer.gone()
            binding.data.gone()
            playerView.startWindowFullscreen(this, false, false)
        } else {
            orientationUtils?.isOnlyRotateLand = false
            orientationUtils?.isRotateWithSystem = true
            orientationUtils?.resolveByClick()
            supportActionBar?.show()
            if (VideoPlay.book != null) {
                binding.chaptersContainer.visible()
                binding.data.visible()
            }
            playerView.backFromFull(this)
            upView()
        }
    }


    @Suppress("DEPRECATION")
    @SuppressLint("SwitchIntDef")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
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

    private fun setupPlayerView() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val layoutParams = playerView.layoutParams
        layoutParams.width = screenWidth
        val videoWidth = playerView.currentVideoWidth
        val videoHeight = playerView.currentVideoHeight
        val height = if (videoWidth > 0 && videoHeight > 0) (screenWidth * videoHeight / videoWidth) else (screenWidth * 9 / 16) //默认16:9
        //高度不超过一半屏幕
        layoutParams.height = if (height < screenHeight / 2) height else screenHeight / 2
        playerView.layoutParams = layoutParams
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
                        //高度不超过一半屏幕
                        layoutParams.height = if (height < screenHeight / 2) height else screenHeight / 2
                        playerView.layoutParams = layoutParams
                    }
                }
            }

            override fun onClickStartIcon(url: String?, vararg objects: Any?) {}
            override fun onAutoComplete(url: String?, vararg objects: Any?) {
                if (VideoPlay.upDurIndex(1)) {
                    VideoPlay.saveRead()
                    upView()
                    VideoPlay.startPlay(playerView)
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
            !VideoPlay.source?.loginUrl.isNullOrBlank()
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_float_window -> startFloatingWindow()
            R.id.menu_login -> VideoPlay.source?.let {s ->
                val type = when (s) {
                    is BookSource -> "bookSource"
                    is RssSource -> "rssSource"
                    else -> null
                }
                type?.let { it ->
                    startActivity<SourceLoginActivity> {
                        putExtra("type", it)
                        putExtra("key", s.getKey())
                    }
                }
            }

            R.id.menu_copy_video_url -> VideoPlay.videoUrl?.let { sendToClip(it) }
            R.id.menu_edit_source -> VideoPlay.source?.let {s  ->
                when (s) {
                    is BookSource -> bookSourceEditResult.launch {
                        putExtra("sourceUrl", s.getKey())
                    }
                    is RssSource -> rssSourceEditResult.launch {
                        putExtra("sourceUrl", s.getKey())
                    }
                }
            }

            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun startFloatingWindow() {
        VideoPlay.savePlayState(playerView)
        // 启动悬浮窗服务
        val intent = Intent(this, VideoPlayService::class.java).apply {
            putExtra("isNew", false)
        }
        ContextCompat.startForegroundService(this, intent)
        playerView.needDestroy = false
        finish() //如果在播放器复刻前活动被销毁，会导致状态继承异常（这里服务创建很快，没发现异常）
    }

    override fun onDestroy() {
        VideoPlay.durChapterPos = playerView.getCurrentPositionWhenPlaying().toInt()
        VideoPlay.saveRead()
        playerView.getCurrentPlayer().release()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        orientationUtils?.releaseListener()
        super.onDestroy()
    }

    override fun finish() {
        val book = VideoPlay.book ?: return super.finish()
        if (VideoPlay.inBookshelf) {
            return super.finish()
        }
        if (!AppConfig.showAddToShelfAlert) {
            viewModel.removeFromBookshelf { super.finish() }
        } else {
            alert(title = getString(R.string.add_to_bookshelf)) {
                setMessage(getString(R.string.check_add_bookshelf, book.name))
                okButton {
                    VideoPlay.book?.removeType(BookType.notShelf)
                    VideoPlay.book?.save()
                    VideoPlay.inBookshelf = true
                    setResult(RESULT_OK)
                }
                noButton { viewModel.removeFromBookshelf { super.finish() } }
            }
        }
    }
}