package io.legado.app.ui.book.manga

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
import io.legado.app.receiver.NetworkChangedListener
import io.legado.app.ui.book.changesource.ChangeBookSourceDialog
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.manga.config.MangaColorFilterConfig
import io.legado.app.ui.book.manga.config.MangaColorFilterDialog
import io.legado.app.ui.book.manga.config.MangaFooterConfig
import io.legado.app.ui.book.manga.config.MangaFooterSettingDialog
import io.legado.app.ui.book.manga.entities.BaseMangaPage
import io.legado.app.ui.book.manga.entities.MangaPage
import io.legado.app.ui.book.manga.recyclerview.MangaAdapter
import io.legado.app.ui.book.read.MangaMenu
import io.legado.app.ui.book.read.ReadBookActivity.Companion.RESULT_DELETED
import io.legado.app.ui.book.toc.TocActivityResult
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.utils.GSON
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.StartActivityContract
import io.legado.app.utils.buildMainHandler
import io.legado.app.utils.fastBinarySearch
import io.legado.app.utils.findCenterViewPosition
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.gone
import io.legado.app.utils.observeEvent
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.toggleSystemBar
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class ReadMangaActivity : VMBaseActivity<ActivityMangaBinding, ReadMangaViewModel>(),
    ReadManga.Callback, ChangeBookSourceDialog.CallBack, MangaMenu.CallBack,
    MangaColorFilterDialog.Callback {

    private val mLayoutManager by lazy {
        LinearLayoutManager(this)
    }
    private val mAdapter: MangaAdapter by lazy {
        MangaAdapter(this)
    }

    private val mSizeProvider by lazy {
        FixedPreloadSizeProvider<Any>(resources.displayMetrics.widthPixels, SIZE_ORIGINAL)
    }

    private val mPagerSnapHelper: PagerSnapHelper by lazy {
        PagerSnapHelper()
    }

    private var isAutoScrollPage = false

    private val mMangaColorFilter: MangaColorFilterConfig by lazy {
        GSON.fromJsonObject<MangaColorFilterConfig>(AppConfig.mangaColorFilter).getOrNull()
            ?: MangaColorFilterConfig()
    }

    private var mMangaAutoPageSpeed = AppConfig.mangaAutoPageSpeed
    private lateinit var mMangaFooterConfig: MangaFooterConfig
    private val mLabelBuilder by lazy { StringBuilder() }

    private val autoScrollHandler = buildMainHandler()
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
            setLoadingColor(R.color.white)
            setLoadingTextColor(R.color.white)
        }
    }

    //打开目录返回选择章节返回结果
    private val tocActivity = registerForActivityResult(TocActivityResult()) {
        it?.let {
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
    override val viewModel by viewModels<ReadMangaViewModel>()
    private val loadingViewVisible get() = binding.flLoading.isVisible
    private val df by lazy {
        DecimalFormat("0.0%")
    }

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
            ReadManga.loadOrUpContent()
        }
        mAdapter.addFooterView {
            ViewLoadMoreBinding.bind(loadMoreView)
        }
        loadMoreView.setOnClickListener {
            if (!loadMoreView.isLoading && ReadManga.hasNextChapter) {
                loadMoreView.startLoad()
                ReadManga.loadOrUpContent()
            }
        }
        loadMoreView.gone()
        mMangaFooterConfig =
            GSON.fromJsonObject<MangaFooterConfig>(AppConfig.mangaFooterConfig).getOrNull()
                ?: MangaFooterConfig()
    }

    override fun observeLiveBus() {
        observeEvent<MangaFooterConfig>(EventBus.UP_MANGA_CONFIG) {
            mMangaFooterConfig = it
            val item = mAdapter.getItem(binding.mRecyclerManga.findCenterViewPosition())
            upInfoBar(item)
        }
    }

    private fun initRecyclerView() {
        mAdapter.setMangaImageColorFilter(mMangaColorFilter)
        setHorizontalScroll(AppConfig.enableMangaHorizontalScroll)
        binding.mRecyclerManga.run {
            adapter = mAdapter
            itemAnimator = null
            layoutManager = mLayoutManager
            setHasFixedSize(true)
            mLayoutManager.initialPrefetchItemCount = 4
            mLayoutManager.isItemPrefetchEnabled = true
            setItemViewCacheSize(AppConfig.preDownloadNum)
            disabledClickScroll(AppConfig.disableClickScroll)
            disableMangaScaling(AppConfig.disableMangaScale)
            setRecyclerViewPreloader(AppConfig.mangaPreDownloadNum)
            setPreScrollListener { _, _, _, position ->
                if (mAdapter.isNotEmpty()) {
                    val item = mAdapter.getItem(position)
                    if (item is MangaPage) {
                        if (ReadManga.durChapterIndex < item.chapterIndex) {
                            ReadManga.moveToNextChapter()
                        } else if (ReadManga.durChapterIndex > item.chapterIndex) {
                            ReadManga.moveToPrevChapter()
                        } else {
                            ReadManga.durChapterPos = item.index
                            ReadManga.curPageChanged()
                        }
                        binding.mangaMenu.upSeekBar(item.index, item.imageCount)
                        upInfoBar(item)
                    }
                }
            }
        }
        binding.webtoonFrame.run {
            onTouchMiddle {
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

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        Looper.myQueue().addIdleHandler {
            viewModel.initData(intent)
            false
        }
        justInitData = true
    }

    override fun upContent() {
        lifecycleScope.launch {
            setTitle(ReadManga.book?.name)
            val data = withContext(IO) { ReadManga.mangaContents }
            val pos = data.pos
            val list = data.items
            val curFinish = data.curFinish
            val nextFinish = data.nextFinish
            mAdapter.submitList(list) {
                if (loadingViewVisible && curFinish) {
                    binding.infobar.isVisible = true
                    upInfoBar(list[pos])
                    mLayoutManager.scrollToPositionWithOffset(pos, 0)
                    binding.flLoading.isGone = true
                    loadMoreView.visible()
                    binding.mangaMenu.upSeekBar(
                        ReadManga.durChapterPos, ReadManga.curMangaChapter!!.imageCount
                    )
                }

                if (curFinish) {
                    if (!ReadManga.hasNextChapter) {
                        loadMoreView.noMore("暂无章节了！")
                    } else if (nextFinish) {
                        loadMoreView.stopLoad()
                    } else {
                        loadMoreView.startLoad()
                    }
                }
            }
        }
    }

    private fun upInfoBar(page: Any?) {
        if (page !is MangaPage) {
            return
        }
        val chapterIndex = page.chapterIndex
        val chapterSize = page.chapterSize
        val chapterPos = page.index
        val imageCount = page.imageCount
        val chapterName = page.mChapterName
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
                mLabelBuilder.append("${chapterPos + 1}/${imageCount}").append(" ")
            }

            if (!hideChapter) {
                if (!hideChapterLabel) {
                    mLabelBuilder.append(getString(R.string.manga_check_chapter))
                }
                mLabelBuilder.append("${chapterIndex + 1}/${chapterSize}").append(" ")
            }

            if (!hideProgressRatio) {
                if (!hideProgressRatioLabel) {
                    mLabelBuilder.append(getString(R.string.manga_check_progress))
                }
                val percent = if (chapterSize == 0 || imageCount == 0 && chapterIndex == 0) {
                    "0.0%"
                } else if (imageCount == 0) {
                    df.format((chapterIndex + 1.0f) / chapterSize.toDouble())
                } else {
                    var percent =
                        df.format(
                            chapterIndex * 1.0f / chapterSize + 1.0f / chapterSize * (chapterPos + 1) / imageCount.toDouble()
                        )
                    if (percent == "100.0%" && (chapterIndex + 1 != chapterSize || chapterPos + 1 != imageCount)) {
                        percent = "99.9%"
                    }
                    percent
                }
                mLabelBuilder.append(percent)
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
        ReadManga.cancelPreDownloadTask()
        networkChangedListener.unRegister()
        stopAutoPage()
    }

    override fun loadFail(msg: String) {
        lifecycleScope.launch {
            if (loadingViewVisible) {
                binding.llLoading.isGone = true
                binding.llRetry.isVisible = true
                binding.tvMsg.text = msg
            } else {
                loadMoreView.error(null, "加载失败，点击重试")
            }
        }
    }

    override fun onDestroy() {
        ReadManga.unregister(this)
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
                ReadManga.setProgress(progress)
            }
            noButton()
        }
    }

    override fun showLoading() {
        lifecycleScope.launch {
            binding.flLoading.isVisible = true
        }
    }

    override fun startLoad() {
        lifecycleScope.launch {
            loadMoreView.startLoad()
        }
    }

    override val oldBook: Book?
        get() = ReadManga.book

    override fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        if (book.isImage) {
            binding.flLoading.isVisible = true
            viewModel.changeTo(book, toc)
        } else {
            toastOnUi("所选择的源不是漫画源")
        }
    }

    override fun updateColorFilter(config: MangaColorFilterConfig) {
        mAdapter.setMangaImageColorFilter(config)
        updateWindowBrightness(config.l)
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
    @SuppressLint("StringFormatMatches", "NotifyDataSetChanged")
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

            R.id.menu_refresh -> {
                binding.flLoading.isVisible = true
                ReadManga.book?.let {
                    viewModel.refreshContentDur(it)
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
                    setRecyclerViewPreloader(it)
                }
            }

            R.id.menu_disable_manga_scale -> {
                item.isChecked = !item.isChecked
                AppConfig.disableMangaScale = item.isChecked
                disableMangaScaling(item.isChecked)
            }

            R.id.menu_disable_click_scroll -> {
                item.isChecked = !item.isChecked
                AppConfig.disableClickScroll = item.isChecked
                disabledClickScroll(item.isChecked)
            }

            R.id.menu_enable_auto_page -> {
                item.isChecked = !item.isChecked
                val menuMangaAutoPageSpeed = mMenu?.findItem(R.id.menu_manga_auto_page_speed)
                isAutoScrollPage = item.isChecked
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
                showDialogFragment(MangaFooterSettingDialog())
            }

            R.id.menu_enable_horizontal_scroll -> {
                item.isChecked = !item.isChecked
                AppConfig.enableMangaHorizontalScroll = item.isChecked
                setHorizontalScroll(item.isChecked)
                mAdapter.notifyDataSetChanged()
            }

            R.id.menu_manga_color_filter -> {
                binding.mangaMenu.runMenuOut()
                showDialogFragment(MangaColorFilterDialog())
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
        toggleSystemBar(menuIsVisible)
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

    private fun setRecyclerViewPreloader(maxPreload: Int) {
        if (mRecyclerViewPreloader != null) {
            binding.mRecyclerManga.removeOnScrollListener(mRecyclerViewPreloader!!)
        }
        mRecyclerViewPreloader = RecyclerViewPreloader(
            Glide.with(this), mAdapter, mSizeProvider, maxPreload
        )
        binding.mRecyclerManga.addOnScrollListener(mRecyclerViewPreloader!!)
    }

    private fun setHorizontalScroll(enable: Boolean) {
        mAdapter.isHorizontal = enable
        if (enable) {
            mPagerSnapHelper.attachToRecyclerView(binding.mRecyclerManga)
            mLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        } else {
            mPagerSnapHelper.attachToRecyclerView(null)
            mLayoutManager.orientation = LinearLayoutManager.VERTICAL
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun upMenu(menu: Menu) {
        this.mMenu = menu
        menu.findItem(R.id.menu_pre_manga_number).title =
            getString(R.string.pre_download_m, AppConfig.mangaPreDownloadNum)
        menu.findItem(R.id.menu_disable_manga_scale).isChecked = AppConfig.disableMangaScale
        menu.findItem(R.id.menu_disable_click_scroll).isChecked = AppConfig.disableClickScroll
        menu.findItem(R.id.menu_manga_auto_page_speed).title =
            getString(R.string.manga_auto_page_speed, mMangaAutoPageSpeed)
        menu.findItem(R.id.menu_enable_horizontal_scroll).isChecked =
            AppConfig.enableMangaHorizontalScroll
    }

    private fun disableMangaScaling(disable: Boolean) {
        binding.webtoonFrame.disableMangaScale = disable
        binding.mRecyclerManga.disableMangaScaling = disable
        if (disable) {
            binding.mRecyclerManga.resetZoom()
        }
    }

    private fun disabledClickScroll(disable: Boolean) {
        binding.webtoonFrame.disabledClickScroll = disable
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
        if (binding.mRecyclerManga.isAtBottom()) {
            return
        }
        val dx: Int
        val dy: Int
        if (AppConfig.enableMangaHorizontalScroll) {
            val width = binding.mRecyclerManga.run {
                width - paddingStart - paddingEnd
            }
            dx = width
            dy = 0
        } else {
            val height = binding.mRecyclerManga.run {
                height - paddingTop - paddingBottom
            }
            dx = 0
            dy = height
        }
        binding.mRecyclerManga.smoothScrollBy(dx, dy)
    }

    private fun scrollToPrev() {
        if (binding.mRecyclerManga.isAtTop()) {
            return
        }
        val dx: Int
        val dy: Int
        if (AppConfig.enableMangaHorizontalScroll) {
            val width = binding.mRecyclerManga.run {
                width - paddingStart - paddingEnd
            }
            dx = width
            dy = 0
        } else {
            val height = binding.mRecyclerManga.run {
                height - paddingTop - paddingBottom
            }
            dx = 0
            dy = height
        }
        binding.mRecyclerManga.smoothScrollBy(-dx, -dy)
    }

    private fun startAutoPage() {
        if (isAutoScrollPage) {
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
                    setResult(RESULT_OK)
                }
                noButton { viewModel.removeFromBookshelf { super.finish() } }
            }
        }
    }

    private fun RecyclerView.isAtTop(): Boolean {
        return (mLayoutManager.findFirstCompletelyVisibleItemPosition() == 0)
                && !canScrollVertically(-1)
    }

    private fun RecyclerView.isAtBottom(): Boolean {
        val adapter = adapter ?: return false
        return (mLayoutManager.findLastCompletelyVisibleItemPosition() == adapter.itemCount - 1) &&
                !canScrollVertically(1)
    }

    fun updateWindowBrightness(brightness: Int) {
        val layoutParams = window.attributes
        val normalizedBrightness = brightness.toFloat() / 255.0f
        layoutParams.screenBrightness = normalizedBrightness.coerceIn(0f, 1f)
        window.attributes = layoutParams
        // 强制刷新屏幕
        window.decorView.postInvalidate()
    }

    override fun skipToPage(index: Int) {
        val durChapterIndex = ReadManga.durChapterIndex
        val itemPos = mAdapter.getItems().fastBinarySearch {
            val chapterIndex: Int
            val pageIndex: Int
            if (it is BaseMangaPage) {
                chapterIndex = it.chapterIndex
                pageIndex = it.index
            } else {
                error("unknown item type")
            }
            val delta = chapterIndex - durChapterIndex
            if (delta != 0) {
                delta
            } else {
                pageIndex - index
            }
        }
        if (itemPos > -1) {
            mLayoutManager.scrollToPositionWithOffset(itemPos, 0)
            upInfoBar(mAdapter.getItem(itemPos))
            ReadManga.durChapterPos = index
        }
    }
}