package io.legado.app.ui.book.manga

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.bumptech.glide.util.FixedPreloadSizeProvider
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookProgress
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.ActivityMangaBinding
import io.legado.app.databinding.ViewLoadMoreBinding
import io.legado.app.help.book.isImage
import io.legado.app.help.book.removeType
import io.legado.app.help.config.AppConfig
import io.legado.app.help.storage.Backup
import io.legado.app.lib.dialogs.alert
import io.legado.app.model.ReadManga
import io.legado.app.model.ReadManga.mFirstLoading
import io.legado.app.model.recyclerView.MangaContent
import io.legado.app.model.recyclerView.ReaderLoading
import io.legado.app.receiver.NetworkChangedListener
import io.legado.app.ui.book.changesource.ChangeBookSourceDialog
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.manga.config.MangaFooterConfig
import io.legado.app.ui.book.manga.config.MangaFooterSettingDialog
import io.legado.app.ui.book.manga.rv.MangaAdapter
import io.legado.app.ui.book.read.MangaMenu
import io.legado.app.ui.book.read.ReadBookActivity.Companion.RESULT_DELETED
import io.legado.app.ui.book.toc.TocActivityResult
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.utils.GSON
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.StartActivityContract
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.gone
import io.legado.app.utils.observeEvent
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.toggleStatusBar
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible

class ReadMangaActivity : VMBaseActivity<ActivityMangaBinding, MangaViewModel>(),
    ReadManga.Callback, ChangeBookSourceDialog.CallBack, MangaMenu.CallBack {

    private val mLayoutManager by lazy {
        LinearLayoutManager(
            this@ReadMangaActivity,
            if (AppConfig.enableMangaHorizontalScroll) LinearLayoutManager.HORIZONTAL else LinearLayoutManager.VERTICAL,
            false
        )
    }
    private val mAdapter: MangaAdapter by lazy {
        MangaAdapter(this@ReadMangaActivity)
    }
    private val mSmoothScroller by lazy {
        object : LinearSmoothScroller(this@ReadMangaActivity) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }
        }
    }

    private val mSizeProvider by lazy {
        FixedPreloadSizeProvider<Any>(
            this@ReadMangaActivity.resources.displayMetrics.widthPixels, SIZE_ORIGINAL
        )
    }

    private var mPagerSnapHelper: PagerSnapHelper? = null

    private var mDisableAutoScrollPage = false
    private val mInitMangaAutoPageSpeed by lazy {
        AppConfig.mangaAutoPageSpeed
    }

    private var mMangaAutoPageSpeed = mInitMangaAutoPageSpeed
    private lateinit var mMangaFooterConfig: MangaFooterConfig
    private val mLabelBuilder by lazy { StringBuilder() }

    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            scrollToNext()
            autoScrollHandler.postDelayed(this, mMangaAutoPageSpeed.times(1000L)) // 每3秒滑动一次
        }
    }
    private var mMenu: Menu? = null

    private var mRecyclerViewPreloader: RecyclerViewPreloader<Any>? = null

    private val networkChangedListener by lazy {
        NetworkChangedListener(this)
    }

    private var justInitData: Boolean = false
    private var syncDialog: AlertDialog? = null

    private val loadMoreView by lazy {
        LoadMoreView(this).apply {
            setBackgroundColor(getCompatColor(R.color.book_ant_10))
            getLoading().loadingColor = getCompatColor(R.color.white)
            getLoadingText().setTextColor(getCompatColor(R.color.white))
        }
    }

    //打开目录返回选择章节返回结果
    private val tocActivity = registerForActivityResult(TocActivityResult()) {
        it?.let {
            binding.flLoading.isVisible = true
            viewModel.openChapter(it.first, it.second)
        }
    }
    private val bookInfoActivity =
        registerForActivityResult(StartActivityContract(BookInfoActivity::class.java)) {
            if (it.resultCode == RESULT_OK) {
                setResult(RESULT_DELETED)
                super.finish()
            }
        }
    override val binding by viewBinding(ActivityMangaBinding::inflate)
    override val viewModel by viewModels<MangaViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        upLayoutInDisplayCutoutMode()
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        ReadManga.register(this)
        upSystemUiVisibility(false)
        initRecyclerView()
        binding.tvRetry.setOnClickListener {
            binding.llLoading.isVisible = true
            binding.llRetry.isGone = true
            mFirstLoading = false
            ReadManga.loadContent()
        }

        mAdapter.addFooterView {
            ViewLoadMoreBinding.bind(loadMoreView)
        }
        loadMoreView.setOnClickListener {
            if (!loadMoreView.isLoading && !ReadManga.gameOver) {
                scrollToBottom(true, ReadManga.durChapterPagePos)
            }
        }
        loadMoreView.gone()
        mMangaFooterConfig =
            GSON.fromJsonObject<MangaFooterConfig>(AppConfig.mangaFooterConfig).getOrNull()
                ?: MangaFooterConfig()
        observeEvent<MangaFooterConfig>(EventBus.UP_MANGA_CONFIG) {
            mMangaFooterConfig = it
            upInfoBar(
                ReadManga.durChapterPagePos.plus(1),
                ReadManga.durChapterPageCount,
                ReadManga.durChapterPos.plus(1),
                ReadManga.durChapterCount,
                ReadManga.chapterTitle
            )
        }
    }

    private fun initRecyclerView() {
        mAdapter.isHorizontal = AppConfig.enableMangaHorizontalScroll
        binding.mRecyclerMange.run {
            adapter = mAdapter
            itemAnimator = null
            layoutManager = mLayoutManager
            setHasFixedSize(true)
            mLayoutManager.initialPrefetchItemCount = 4
            mLayoutManager.isItemPrefetchEnabled = true
            setItemViewCacheSize(AppConfig.preDownloadNum)
            singlePagerScroller(
                AppConfig.singlePageScroll,
                AppConfig.enableMangaHorizontalScroll
            )
            disabledClickScroller(AppConfig.disableClickScroll)
            disableMangaScaling(AppConfig.disableMangaScale)
            setPreScrollListener { _, dx, dy, position ->
                if ((dy > 0 || dx > 0) && position + 2 > mAdapter.getCurrentList().size - 3) {
                    if (mAdapter.getCurrentList().last() is ReaderLoading) {
                        val nextIndex =
                            (mAdapter.getCurrentList().last() as ReaderLoading).mNextChapterIndex
                        if (nextIndex != -1) {
                            scrollToBottom(false, nextIndex)
                        }
                    }
                }
            }
            setNestedPreScrollListener { _, _, _, position ->
                if (mAdapter.getCurrentList().isNotEmpty()
                    && position <= mAdapter.getCurrentList().lastIndex
                ) {
                    try {
                        val content = mAdapter.getCurrentList()[position]
                        if (content is MangaContent) {
                            ReadManga.durChapterPos = content.mDurChapterPos.minus(1)
                            upInfoBar(
                                content.mChapterPagePos,
                                content.mChapterPageCount,
                                content.mDurChapterPos,
                                content.mDurChapterCount,
                                content.mChapterName
                            )
                        }
                    } catch (e: Exception) {
                        e.printOnDebug()
                    }

                }
            }
            addRecyclerViewPreloader(AppConfig.mangaPreDownloadNum)
            onToucheMiddle {
                if (!binding.mangaMenu.isVisible) {
                    binding.mangaMenu.runMenuIn()
                }
            }
            onNextPage {
                scrollToNext()
            }
            onPrevPage {
                scrollToPrev()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.initData(intent)
    }

    private fun scrollToBottom(forceLoad: Boolean = false, index: Int) {
        if ((loadMoreView.hasMore && !loadMoreView.isLoading) && !ReadManga.gameOver || forceLoad) {
            loadMoreView.hasMore()
            ReadManga.moveToNextChapter(index)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        Looper.myQueue().addIdleHandler {
            viewModel.initData(intent)
            false
        }
        justInitData = true
    }

    override fun loadContentFinish(list: MutableList<Any>) {
        if (!this.isDestroyed) {
            setTitle(ReadManga.book?.name)
            mAdapter.submitList(list) {
                if (!mFirstLoading) {
                    if (list.size > 1) {
                        binding.infobar.isVisible = true
                        upInfoBar(
                            ReadManga.durChapterPagePos.plus(1),
                            ReadManga.durChapterPageCount,
                            ReadManga.durChapterPos.plus(1),
                            ReadManga.durChapterCount,
                            ReadManga.chapterTitle
                        )
                    }

                    if (ReadManga.durChapterPos + 2 > mAdapter.getCurrentList().size - 3) {
                        val nextIndex =
                            (mAdapter.getCurrentList().last() as ReaderLoading).mNextChapterIndex
                        scrollToBottom(index = nextIndex)
                    } else {
                        binding.mRecyclerMange.scrollToPosition(ReadManga.durChapterPos)
                    }
                }

                if (ReadManga.chapterChanged) {
                    binding.mRecyclerMange.scrollToPosition(ReadManga.durChapterPos)
                }

                ReadManga.chapterChanged = false
                loadMoreView.visible()
                mFirstLoading = true
                loadMoreView.stopLoad()
            }
        }
    }

    private fun upInfoBar(
        chapterPagePos: Int,
        chapterPageCount: Int,
        chapterPos: Int,
        chapterCount: Int,
        chapterName: String,
    ) {
        mMangaFooterConfig.run {
            mLabelBuilder.clear()
            binding.infobar.isGone = hideFooter
            binding.infobar.textInfoAlignment = footerOrientation

            if (!hideChapterName) {
                mLabelBuilder.append(chapterName).append(" ")
            }

            if (!hidePageNumber) {
                if (!hidePageNumberLabel) {
                    mLabelBuilder.append(getString(R.string.manga_check_page_number))
                }
                mLabelBuilder.append("${chapterPos}/${chapterCount}").append(" ")
            }

            if (!hideChapter) {
                if (!hideChapterLabel) {
                    mLabelBuilder.append(getString(R.string.manga_check_chapter))
                }
                mLabelBuilder.append("${chapterPagePos}/${chapterPageCount}").append(" ")
            }

            if (!hideProgressRatio) {
                if (!hideProgressRatioLabel) {
                    mLabelBuilder.append(getString(R.string.manga_check_progress))
                }
                mLabelBuilder.append("${chapterPagePos.div(chapterPageCount).times(100)}%")
            }
        }
        binding.infobar.update(
            if (mLabelBuilder.isEmpty()) "" else mLabelBuilder.toString()
        )
    }

    override fun onResume() {
        super.onResume()
        networkChangedListener.register()
        networkChangedListener.onNetworkChanged = {
            // 当网络是可用状态且无需初始化时同步进度（初始化中已有同步进度逻辑）
            if (AppConfig.syncBookProgressPlus && NetworkUtils.isAvailable() && !justInitData) {
                ReadManga.syncProgress({ progress -> sureNewProgress(progress) })
            }
        }
        startAutoPage()
    }

    override fun onPause() {
        super.onPause()
        if (ReadManga.inBookshelf) {
            ReadManga.saveRead()
            if (!BuildConfig.DEBUG) {
                if (AppConfig.syncBookProgressPlus) {
                    ReadManga.syncProgress()
                } else {
                    ReadManga.uploadProgress()
                }
                Backup.autoBack(this)
            }
        }
        networkChangedListener.unRegister()
        stopAutoPage()
    }

    override fun loadComplete() {
        binding.flLoading.isGone = true
    }

    override fun loadFail(msg: String) {
        if (!mFirstLoading || ReadManga.chapterChanged) {
            binding.llLoading.isGone = true
            binding.llRetry.isVisible = true
            binding.tvMsg.text = msg
        } else {
            loadMoreView.error(null, "加载失败，点击重试")
        }
    }

    override fun noData() {
        loadMoreView.noMore("暂无章节了！")
    }

    override fun adjustmentProgress() {
        if (ReadManga.chapterChanged) {
            binding.mRecyclerMange.scrollToPosition(ReadManga.durChapterPos)
            binding.flLoading.isGone = true
        }
    }

    override val chapterList: MutableList<Any>
        get() = mAdapter.getCurrentList()

    override fun onDestroy() {
        ReadManga.unregister()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Glide.get(this).clearMemory()
    }

    override fun sureNewProgress(progress: BookProgress) {
        syncDialog?.dismiss()
        syncDialog = alert(R.string.get_book_progress) {
            setMessage(R.string.cloud_progress_exceeds_current)
            okButton {
                binding.flLoading.isVisible = true
                ReadManga.setProgress(progress)
            }
            noButton()
        }
    }

    override val oldBook: Book?
        get() = ReadManga.book

    override fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        if (book.isImage) {
            binding.flLoading.isVisible = true
            ReadManga.chapterChanged = true
            viewModel.changeTo(book, toc)
        } else {
            toastOnUi("所选择的源不是漫画源")
        }
    }


    @SuppressLint("StringFormatMatches")
    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_manga, menu)
        upMenu(menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    /**
     * 菜单
     */
    @SuppressLint("StringFormatMatches")
    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_source -> {
                binding.mangaMenu.runMenuOut()
                ReadManga.book?.let {
                    showDialogFragment(ChangeBookSourceDialog(it.name, it.author))
                }
            }

            R.id.menu_catalog -> {
                ReadManga.book?.let {
                    tocActivity.launch(it.bookUrl)
                }
            }

            R.id.menu_pre_manga_number -> {
                showNumberPickerDialog(
                    0,
                    getString(R.string.pre_download),
                    AppConfig.mangaPreDownloadNum
                ) {
                    AppConfig.mangaPreDownloadNum = it
                    item.title = getString(R.string.pre_download_m, it)
                    addRecyclerViewPreloader(it)
                }
            }

            R.id.menu_scroller_page -> {
                item.isChecked = !item.isChecked
                AppConfig.singlePageScroll = item.isChecked
                singlePagerScroller(item.isChecked, AppConfig.enableMangaHorizontalScroll)
            }

            R.id.menu_disable_manga_scale -> {
                item.isChecked = !item.isChecked
                AppConfig.disableMangaScale = item.isChecked
                disableMangaScaling(item.isChecked)
            }

            R.id.menu_disable_click_scroller -> {
                item.isChecked = !item.isChecked
                AppConfig.disableClickScroll = item.isChecked
                disabledClickScroller(item.isChecked)
            }

            R.id.menu_enable_auto_page -> {
                item.isChecked = !item.isChecked
                val menuMangaAutoPageSpeed = mMenu?.findItem(R.id.menu_manga_auto_page_speed)
                mDisableAutoScrollPage = item.isChecked
                if (item.isChecked) {
                    startAutoPage()
                    menuMangaAutoPageSpeed?.isVisible = true
                } else {
                    stopAutoPage()
                    menuMangaAutoPageSpeed?.isVisible = false
                }
            }

            R.id.menu_manga_auto_page_speed -> {
                showNumberPickerDialog(1, getString(R.string.setting_manga_auto_page_speed), 3) {
                    AppConfig.mangaAutoPageSpeed = it
                    mMangaAutoPageSpeed = it
                    item.title = getString(R.string.manga_auto_page_speed, it)
                    stopAutoPage()
                    startAutoPage()
                }
            }

            R.id.menu_manga_footer_config -> {
                MangaFooterSettingDialog().show(supportFragmentManager, "mangaFooterSettingDialog")
            }

            R.id.menu_enable_horizontal_scroller -> {
                item.isChecked = !item.isChecked
                AppConfig.enableMangaHorizontalScroll = item.isChecked
                mLayoutManager.orientation =
                    if (item.isChecked) LinearLayoutManager.HORIZONTAL else LinearLayoutManager.VERTICAL
                singlePagerScroller(AppConfig.singlePageScroll, item.isChecked)
                mAdapter.isHorizontal = item.isChecked
                mAdapter.notifyItemRangeChanged(
                    ReadManga.durChapterPos.minus(2),
                    mAdapter.getCurrentList().size
                )
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun openBookInfoActivity() {
        ReadManga.book?.let {
            bookInfoActivity.launch {
                putExtra("name", it.name)
                putExtra("author", it.author)
            }
        }
    }

    override fun upSystemUiVisibility(menuIsVisible: Boolean) {
        toggleStatusBar(menuIsVisible)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action
        val isDown = action == 0

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (isDown && !binding.mangaMenu.canShowMenu) {
                binding.mangaMenu.runMenuIn()
                return true
            }
            if (!isDown && !binding.mangaMenu.canShowMenu) {
                binding.mangaMenu.canShowMenu = true
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun addRecyclerViewPreloader(maxPreload: Int) {
        if (mRecyclerViewPreloader != null) {
            binding.mRecyclerMange.removeOnScrollListener(mRecyclerViewPreloader!!)
        }
        mRecyclerViewPreloader = RecyclerViewPreloader(
            Glide.with(this), mAdapter, mSizeProvider, maxPreload
        )
        binding.mRecyclerMange.addOnScrollListener(mRecyclerViewPreloader!!)
    }

    private fun singlePagerScroller(enableSingleScroll: Boolean, enableHorizontalScroll: Boolean) {
        if (enableSingleScroll) {
            mPagerSnapHelper?.attachToRecyclerView(null)
            mPagerSnapHelper = if (enableHorizontalScroll) {
                PagerSnapHelper()
            } else {
                object : PagerSnapHelper() {
                    override fun calculateDistanceToFinalSnap(
                        layoutManager: RecyclerView.LayoutManager,
                        targetView: View,
                    ): IntArray {
                        val out = IntArray(2)
                        out[1] = targetView.top - binding.mRecyclerMange.paddingTop
                        return out
                    }
                }
            }
            mPagerSnapHelper?.attachToRecyclerView(binding.mRecyclerMange)
        } else {
            mPagerSnapHelper?.attachToRecyclerView(null)
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun upMenu(menu: Menu) {
        this.mMenu = menu
        menu.findItem(R.id.menu_pre_manga_number).title =
            getString(R.string.pre_download_m, AppConfig.mangaPreDownloadNum)
        menu.findItem(R.id.menu_scroller_page).isChecked = AppConfig.singlePageScroll
        menu.findItem(R.id.menu_disable_manga_scale).isChecked = AppConfig.disableMangaScale
        menu.findItem(R.id.menu_disable_click_scroller).isChecked = AppConfig.disableClickScroll
        menu.findItem(R.id.menu_manga_auto_page_speed).title =
            getString(R.string.manga_auto_page_speed, mMangaAutoPageSpeed)
        menu.findItem(R.id.menu_enable_horizontal_scroller).isChecked =
            AppConfig.enableMangaHorizontalScroll
    }

    private fun disableMangaScaling(disable: Boolean) {
        binding.webtoonFrame.disableMangaScale = disable
        binding.mRecyclerMange.disableMangaScaling = disable
        if (disable) {
            binding.mRecyclerMange.resetZoom()
        }
    }

    private fun disabledClickScroller(disable: Boolean) {
        binding.mRecyclerMange.disabledClickScroller = disable
    }

    private fun upLayoutInDisplayCutoutMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun scrollToNext() {
        val lastCompletelyVisiblePosition = mLayoutManager.findLastCompletelyVisibleItemPosition()
        val nextPosition = if (lastCompletelyVisiblePosition != RecyclerView.NO_POSITION) {
            lastCompletelyVisiblePosition + 1
        } else {
            mLayoutManager.findFirstVisibleItemPosition() + 1
        }
        if (nextPosition > mAdapter.getCurrentList().lastIndex) {
            return
        }
        smoothScrollToPosition(nextPosition)
    }

    private fun scrollToPrev() {
        val firstCompletelyVisiblePosition = mLayoutManager.findFirstCompletelyVisibleItemPosition()
        val prevPosition = if (firstCompletelyVisiblePosition != RecyclerView.NO_POSITION) {
            firstCompletelyVisiblePosition - 1
        } else {
            mLayoutManager.findFirstVisibleItemPosition() - 1
        }
        if (prevPosition < 0) {
            return
        }
        smoothScrollToPosition(prevPosition)
    }

    private fun smoothScrollToPosition(position: Int) {
        mSmoothScroller.targetPosition = position
        mLayoutManager.startSmoothScroll(mSmoothScroller)
    }

    private fun startAutoPage() {
        if (mDisableAutoScrollPage) {
            autoScrollHandler.postDelayed(autoScrollRunnable, mMangaAutoPageSpeed.times(1000L))
        }
    }

    private fun stopAutoPage() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }

    private fun showNumberPickerDialog(
        min: Int,
        title: String,
        initValue: Int,
        callback: (Int) -> Unit,
    ) {
        NumberPickerDialog(this)
            .setTitle(title)
            .setMaxValue(9999)
            .setMinValue(min)
            .setValue(initValue)
            .show {
                callback.invoke(it)
            }
    }

    override fun finish() {
        val book = ReadManga.book ?: return super.finish()

        if (ReadManga.inBookshelf) {
            return super.finish()
        }

        if (!AppConfig.showAddToShelfAlert) {
            viewModel.removeFromBookshelf { super.finish() }
        } else {
            alert(title = getString(R.string.add_to_bookshelf)) {
                setMessage(getString(R.string.check_add_bookshelf, book.name))
                okButton {
                    ReadManga.book?.removeType(BookType.notShelf)
                    ReadManga.book?.save()
                    ReadManga.inBookshelf = true
                    setResult(Activity.RESULT_OK)
                }
                noButton { viewModel.removeFromBookshelf { super.finish() } }
            }
        }
    }
}