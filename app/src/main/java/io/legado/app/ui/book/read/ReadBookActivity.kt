package io.legado.app.ui.book.read

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.size
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.constant.Status
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.ReadTipConfig
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.SyncBookProgress
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.accentColor
import io.legado.app.receiver.TimeBatteryReceiver
import io.legado.app.service.BaseReadAloudService
import io.legado.app.service.help.ReadAloud
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.changesource.ChangeSourceDialog
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.read.config.*
import io.legado.app.ui.book.read.config.BgTextConfigDialog.Companion.BG_COLOR
import io.legado.app.ui.book.read.config.BgTextConfigDialog.Companion.TEXT_COLOR
import io.legado.app.ui.book.read.config.TipConfigDialog.Companion.TIP_COLOR
import io.legado.app.ui.book.read.page.ContentTextView
import io.legado.app.ui.book.read.page.PageView
import io.legado.app.ui.book.read.page.TextPageFactory
import io.legado.app.ui.book.read.page.entities.PageDirection
import io.legado.app.ui.book.searchContent.SearchContentActivity
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.book.toc.ChapterListActivity
import io.legado.app.ui.login.SourceLogin
import io.legado.app.ui.replace.ReplaceRuleActivity
import io.legado.app.ui.replace.edit.ReplaceEditActivity
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast

class ReadBookActivity : ReadBookBaseActivity(),
    View.OnTouchListener,
    PageView.CallBack,
    TextActionMenu.CallBack,
    ContentTextView.CallBack,
    ReadMenu.CallBack,
    ReadAloudDialog.CallBack,
    ChangeSourceDialog.CallBack,
    ReadBook.CallBack,
    AutoReadDialog.CallBack,
    TocRegexDialog.CallBack,
    ColorPickerDialogListener {
    private val requestCodeChapterList = 568
    private val requestCodeReplace = 312
    private val requestCodeSearchResult = 123
    private val requestCodeEditSource = 111
    private var menu: Menu? = null
    private var textActionMenu: TextActionMenu? = null

    override val scope: CoroutineScope get() = this
    override val isInitFinish: Boolean get() = viewModel.isInitFinish
    override val isScroll: Boolean get() = binding.pageView.isScroll
    private val mHandler = Handler(Looper.getMainLooper())
    private val keepScreenRunnable: Runnable =
        Runnable { keepScreenOn(window, false) }
    private val autoPageRunnable: Runnable = Runnable { autoPagePlus() }
    override var autoPageProgress = 0
    override var isAutoPage = false
    private var screenTimeOut: Long = 0
    private var timeBatteryReceiver: TimeBatteryReceiver? = null
    private var loadStates: Boolean = false
    override val pageFactory: TextPageFactory get() = binding.pageView.pageFactory
    override val headerHeight: Int get() = binding.pageView.curPage.headerHeight

    @SuppressLint("ClickableViewAccessibility")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.cursorLeft.setColorFilter(accentColor)
        binding.cursorRight.setColorFilter(accentColor)
        binding.cursorLeft.setOnTouchListener(this)
        binding.cursorRight.setOnTouchListener(this)
        upScreenTimeOut()
        ReadBook.callBack = this
        ReadBook.titleDate.observe(this) {
            binding.readMenu.setTitle(it)
            upMenu()
            upView()
        }
        viewModel.initData(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        upSystemUiVisibility()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.pageView.upStatusBar()
        ReadBook.loadContent(resetPageOffset = false)
    }

    override fun onResume() {
        super.onResume()
        ReadBook.readStartTime = System.currentTimeMillis()
        upSystemUiVisibility()
        timeBatteryReceiver = TimeBatteryReceiver.register(this)
        binding.pageView.upTime()
    }

    override fun onPause() {
        super.onPause()
        ReadBook.saveRead()
        timeBatteryReceiver?.let {
            unregisterReceiver(it)
            timeBatteryReceiver = null
        }
        upSystemUiVisibility()
        if (!BuildConfig.DEBUG) {
            SyncBookProgress.uploadBookProgress()
            Backup.autoBack(this)
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.read_book, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        upMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    private fun upMenu() {
        menu?.let { menu ->
            ReadBook.book?.let { book ->
                val onLine = !book.isLocalBook()
                for (i in 0 until menu.size) {
                    val item = menu[i]
                    when (item.groupId) {
                        R.id.menu_group_on_line,
                        R.id.menu_group_on_line_ns -> item.isVisible = onLine
                        R.id.menu_group_local -> item.isVisible = !onLine
                        R.id.menu_group_text -> item.isVisible = book.isLocalTxt()
                        R.id.menu_group_login ->
                            item.isVisible = !ReadBook.webBook?.bookSource?.loginUrl.isNullOrEmpty()
                        else -> when (item.itemId) {
                            R.id.menu_enable_replace -> item.isChecked = book.getUseReplaceRule()
                            R.id.menu_re_segment -> item.isChecked = book.getReSegment()
                        }
                    }
                }
            }
        }
    }

    /**
     * 菜单
     */
    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_source -> {
                binding.readMenu.runMenuOut()
                ReadBook.book?.let {
                    ChangeSourceDialog.show(supportFragmentManager, it.name, it.author)
                }
            }
            R.id.menu_refresh -> {
                ReadBook.book?.let {
                    ReadBook.curTextChapter = null
                    binding.pageView.upContent()
                    viewModel.refreshContent(it)
                }
            }
            R.id.menu_download -> showDownloadDialog()
            R.id.menu_add_bookmark -> showBookMark()
            R.id.menu_copy_text ->
                TextDialog.show(supportFragmentManager, ReadBook.curTextChapter?.getContent())
            R.id.menu_update_toc -> ReadBook.book?.let {
                loadChapterList(it)
            }
            R.id.menu_enable_replace -> ReadBook.book?.let {
                it.setUseReplaceRule(!it.getUseReplaceRule())
                menu?.findItem(R.id.menu_enable_replace)?.isChecked = it.getUseReplaceRule()
                viewModel.replaceRuleChanged()
            }
            R.id.menu_re_segment -> ReadBook.book?.let {
                it.setReSegment(!it.getReSegment())
                menu?.findItem(R.id.menu_re_segment)?.isChecked = it.getReSegment()
                ReadBook.loadContent(false)
            }
            R.id.menu_page_anim -> showPageAnimConfig {
                binding.pageView.upPageAnim()
            }
            R.id.menu_book_info -> ReadBook.book?.let {
                startActivity<BookInfoActivity>(
                    Pair("name", it.name),
                    Pair("author", it.author)
                )
            }
            R.id.menu_toc_regex -> TocRegexDialog.show(
                supportFragmentManager,
                ReadBook.book?.tocUrl
            )
            R.id.menu_login -> ReadBook.webBook?.bookSource?.let {
                startActivity<SourceLogin>(
                    Pair("sourceUrl", it.bookSourceUrl),
                    Pair("loginUrl", it.loginUrl)
                )
            }
            R.id.menu_set_charset -> showCharsetConfig()
            R.id.menu_help -> showReadMenuHelp()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    /**
     * 按键拦截,显示菜单
     */
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        val keyCode = event?.keyCode
        val action = event?.action
        val isDown = action == 0

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (isDown && !binding.readMenu.cnaShowMenu) {
                binding.readMenu.runMenuIn()
                return true
            }
            if (!isDown && !binding.readMenu.cnaShowMenu) {
                binding.readMenu.cnaShowMenu = true
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * 按键事件
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when {
            isPrevKey(keyCode) -> {
                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    binding.pageView.pageDelegate?.keyTurnPage(PageDirection.PREV)
                    return true
                }
            }
            isNextKey(keyCode) -> {
                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    binding.pageView.pageDelegate?.keyTurnPage(PageDirection.NEXT)
                    return true
                }
            }
            keyCode == KeyEvent.KEYCODE_VOLUME_UP -> {
                if (volumeKeyPage(PageDirection.PREV)) {
                    return true
                }
            }
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (volumeKeyPage(PageDirection.NEXT)) {
                    return true
                }
            }
            keyCode == KeyEvent.KEYCODE_SPACE -> {
                binding.pageView.pageDelegate?.keyTurnPage(PageDirection.NEXT)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 长按事件
     */
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                finish()
                return true
            }
        }
        return super.onKeyLongPress(keyCode, event)
    }

    /**
     * 松开按键事件
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (volumeKeyPage(PageDirection.NONE)) {
                    return true
                }
            }
            KeyEvent.KEYCODE_BACK -> {
                event?.let {
                    if ((event.flags and KeyEvent.FLAG_CANCELED_LONG_PRESS == 0)
                        && event.isTracking
                        && !event.isCanceled
                    ) {
                        if (BaseReadAloudService.isPlay()) {
                            ReadAloud.pause(this)
                            toast(R.string.read_aloud_pause)
                            return true
                        }
                        if (isAutoPage) {
                            autoPageStop()
                            return true
                        }
                    }
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    /**
     * view触摸,文字选择
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> textActionMenu?.dismiss()
            MotionEvent.ACTION_MOVE -> {
                when (v.id) {
                    R.id.cursor_left -> binding.pageView.curPage.selectStartMove(
                        event.rawX + binding.cursorLeft.width,
                        event.rawY - binding.cursorLeft.height
                    )
                    R.id.cursor_right -> binding.pageView.curPage.selectEndMove(
                        event.rawX - binding.cursorRight.width,
                        event.rawY - binding.cursorRight.height
                    )
                }
            }
            MotionEvent.ACTION_UP -> showTextActionMenu()
        }
        return true
    }

    /**
     * 更新文字选择开始位置
     */
    override fun upSelectedStart(x: Float, y: Float, top: Float) {
        binding.cursorLeft.x = x - binding.cursorLeft.width
        binding.cursorLeft.y = y
        binding.cursorLeft.visible(true)
        binding.textMenuPosition.x = x
        binding.textMenuPosition.y = top
    }

    /**
     * 更新文字选择结束位置
     */
    override fun upSelectedEnd(x: Float, y: Float) {
        binding.cursorRight.x = x
        binding.cursorRight.y = y
        binding.cursorRight.visible(true)
    }

    /**
     * 取消文字选择
     */
    override fun onCancelSelect() {
        binding.cursorLeft.invisible()
        binding.cursorRight.invisible()
        textActionMenu?.dismiss()
    }

    /**
     * 显示文本操作菜单
     */
    override fun showTextActionMenu() {
        textActionMenu ?: let {
            textActionMenu = TextActionMenu(this, this)
        }
        textActionMenu?.let { popup ->
            popup.contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val popupHeight = popup.contentView.measuredHeight
            val x = binding.textMenuPosition.x.toInt()
            var y = binding.textMenuPosition.y.toInt() - popupHeight
            if (y < statusBarHeight) {
                y = (binding.cursorLeft.y + binding.cursorLeft.height).toInt()
            }
            if (binding.cursorRight.y > y && binding.cursorRight.y < y + popupHeight) {
                y = (binding.cursorRight.y + binding.cursorRight.height).toInt()
            }
            if (!popup.isShowing) {
                popup.showAtLocation(binding.textMenuPosition, Gravity.TOP or Gravity.START, x, y)
            } else {
                popup.update(x, y, WRAP_CONTENT, WRAP_CONTENT)
            }
        }
    }

    /**
     * 当前选择的文本
     */
    override val selectedText: String get() = binding.pageView.curPage.selectedText

    /**
     * 文本选择菜单操作
     */
    override fun onMenuItemSelected(itemId: Int): Boolean {
        when (itemId) {
            R.id.menu_replace -> {
                val scopes = arrayListOf<String>()
                ReadBook.book?.name?.let {
                    scopes.add(it)
                }
                ReadBook.bookSource?.bookSourceUrl?.let {
                    scopes.add(it)
                }
                ReplaceEditActivity.show(
                    this,
                    pattern = selectedText,
                    scope = scopes.joinToString(";")
                )
                return true
            }
            R.id.menu_search_content -> {
                viewModel.searchContentQuery = selectedText
                openSearchActivity(selectedText)
                return true
            }
        }
        return false
    }

    /**
     * 文本选择菜单操作完成
     */
    override fun onMenuActionFinally() {
        textActionMenu?.dismiss()
        binding.pageView.curPage.cancelSelect()
        binding.pageView.isTextSelected = false
    }

    /**
     * 音量键翻页
     */
    private fun volumeKeyPage(direction: PageDirection): Boolean {
        if (!binding.readMenu.isVisible) {
            if (getPrefBoolean("volumeKeyPage", true)) {
                if (getPrefBoolean("volumeKeyPageOnPlay")
                    || BaseReadAloudService.pause
                ) {
                    binding.pageView.pageDelegate?.isCancel = false
                    binding.pageView.pageDelegate?.keyTurnPage(direction)
                    return true
                }
            }
        }
        return false
    }

    override fun loadChapterList(book: Book) {
        ReadBook.upMsg(getString(R.string.toc_updateing))
        viewModel.loadChapterList(book)
    }

    /**
     * 内容加载完成
     */
    override fun contentLoadFinish() {
        if (intent.getBooleanExtra("readAloud", false)) {
            intent.removeExtra("readAloud")
            ReadBook.readAloud()
        }
        loadStates = true
    }

    /**
     * 更新内容
     */
    override fun upContent(relativePosition: Int, resetPageOffset: Boolean) {
        autoPageProgress = 0
        launch {
            binding.pageView.upContent(relativePosition, resetPageOffset)
            binding.readMenu.setSeekPage(ReadBook.durPageIndex)
        }
        loadStates = false
    }

    /**
     * 更新视图
     */
    override fun upView() {
        launch {
            binding.readMenu.upBookView()
        }
    }

    override fun upPageAnim() {
        launch {
            binding.pageView.upPageAnim()
        }
    }

    /**
     * 页面改变
     */
    override fun pageChanged() {
        autoPageProgress = 0
        launch {
            binding.readMenu.setSeekPage(ReadBook.durPageIndex)
        }
    }

    /**
     * 显示菜单
     */
    override fun showMenuBar() {
        binding.readMenu.runMenuIn()
    }

    override val oldBook: Book?
        get() = ReadBook.book

    override fun changeTo(book: Book) {
        viewModel.changeTo(book)
    }

    override fun showActionMenu() {
        when {
            BaseReadAloudService.isRun -> {
                showReadAloudDialog()
            }
            isAutoPage -> {
                AutoReadDialog().show(supportFragmentManager, "autoRead")
            }
            else -> {
                binding.readMenu.runMenuIn()
            }
        }
    }

    override fun showReadMenuHelp() {
        val text = String(assets.open("help/readMenuHelp.md").readBytes())
        TextDialog.show(supportFragmentManager, text, TextDialog.MD)
    }

    /**
     * 显示朗读菜单
     */
    override fun showReadAloudDialog() {
        ReadAloudDialog().show(supportFragmentManager, "readAloud")
    }

    /**
     * 自动翻页
     */
    override fun autoPage() {
        ReadAloud.stop(this)
        if (isAutoPage) {
            autoPageStop()
        } else {
            isAutoPage = true
            binding.pageView.upContent()
            binding.pageView.upContent(1)
            autoPagePlus()
        }
        binding.readMenu.setAutoPage(isAutoPage)
    }

    override fun autoPageStop() {
        isAutoPage = false
        mHandler.removeCallbacks(autoPageRunnable)
        binding.pageView.upContent()
    }

    private fun autoPagePlus() {
        mHandler.removeCallbacks(autoPageRunnable)
        if (binding.pageView.isScroll) {
            binding.pageView.curPage.scroll(-binding.pageView.height / ReadBookConfig.autoReadSpeed / 50)
        } else {
            autoPageProgress++
            if (autoPageProgress >= ReadBookConfig.autoReadSpeed * 50) {
                autoPageProgress = 0
                binding.pageView.fillPage(PageDirection.NEXT)
            } else {
                binding.pageView.invalidate()
            }
        }
        mHandler.postDelayed(autoPageRunnable, 20)
    }

    override fun openSourceEditActivity() {
        ReadBook.webBook?.let {
            startActivityForResult<BookSourceEditActivity>(
                requestCodeEditSource,
                Pair("data", it.bookSource.bookSourceUrl)
            )
        }
    }

    /**
     * 替换
     */
    override fun openReplaceRule() {
        startActivityForResult<ReplaceRuleActivity>(requestCodeReplace)
    }

    /**
     * 打开目录
     */
    override fun openChapterList() {
        ReadBook.book?.let {
            startActivityForResult<ChapterListActivity>(
                requestCodeChapterList,
                Pair("bookUrl", it.bookUrl)
            )
        }
    }

    /**
     * 打开搜索界面
     */
    override fun openSearchActivity(searchWord: String?) {
        ReadBook.book?.let {
            startActivityForResult<SearchContentActivity>(
                requestCodeSearchResult,
                Pair("bookUrl", it.bookUrl),
                Pair("searchWord", searchWord ?: viewModel.searchContentQuery)
            )
        }
    }

    /**
     * 显示阅读样式配置
     */
    override fun showReadStyle() {
        ReadStyleDialog().show(supportFragmentManager, "readStyle")
    }

    /**
     * 显示更多设置
     */
    override fun showMoreSetting() {
        MoreConfigDialog().show(supportFragmentManager, "moreConfig")
    }

    /**
     * 更新状态栏,导航栏
     */
    override fun upSystemUiVisibility() {
        upSystemUiVisibility(isInMultiWindow, !binding.readMenu.isVisible)
        upNavigationBarColor()
    }

    /**
     * 朗读按钮
     */
    override fun onClickReadAloud() {
        autoPageStop()
        when {
            !BaseReadAloudService.isRun -> ReadBook.readAloud()
            BaseReadAloudService.pause -> ReadAloud.resume(this)
            else -> ReadAloud.pause(this)
        }
    }

    /**
     * colorSelectDialog
     */
    override fun onColorSelected(dialogId: Int, color: Int) = with(ReadBookConfig.durConfig) {
        when (dialogId) {
            TEXT_COLOR -> {
                setCurTextColor(color)
                postEvent(EventBus.UP_CONFIG, false)
            }
            BG_COLOR -> {
                setCurBg(0, "#${color.hexString}")
                ReadBookConfig.upBg()
                postEvent(EventBus.UP_CONFIG, false)
            }
            TIP_COLOR -> {
                ReadTipConfig.tipColor = color
                postEvent(EventBus.TIP_COLOR, "")
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
    }

    /**
     * colorSelectDialog
     */
    override fun onDialogDismissed(dialogId: Int) = Unit

    override fun onTocRegexDialogResult(tocRegex: String) {
        ReadBook.book?.let {
            it.tocUrl = tocRegex
            viewModel.loadChapterList(it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                requestCodeEditSource -> viewModel.upBookSource()
                requestCodeChapterList ->
                    data?.getIntExtra("index", ReadBook.durChapterIndex)?.let { index ->
                        if (index != ReadBook.durChapterIndex) {
                            val pageIndex = data.getIntExtra("pageIndex", 0)
                            viewModel.openChapter(index, pageIndex)
                        }
                    }
                requestCodeSearchResult ->
                    data?.getIntExtra("index", ReadBook.durChapterIndex)?.let { index ->
                        launch(IO) {
                            val indexWithinChapter = data.getIntExtra("indexWithinChapter", 0)
                            viewModel.searchContentQuery = data.getStringExtra("query") ?: ""
                            viewModel.openChapter(index)
                            // block until load correct chapter and pages
                            var pages = ReadBook.curTextChapter?.pages
                            while (ReadBook.durChapterIndex != index || pages == null) {
                                delay(100L)
                                pages = ReadBook.curTextChapter?.pages
                            }
                            val positions =
                                ReadBook.searchResultPositions(
                                    pages,
                                    indexWithinChapter,
                                    viewModel.searchContentQuery
                                )
                            while (ReadBook.durPageIndex != positions[0]) {
                                delay(100L)
                                ReadBook.skipToPage(positions[0])
                            }
                            withContext(Main) {
                                binding.pageView.curPage.selectStartMoveIndex(
                                    0,
                                    positions[1],
                                    positions[2]
                                )
                                delay(20L)
                                when (positions[3]) {
                                    0 -> binding.pageView.curPage.selectEndMoveIndex(
                                        0,
                                        positions[1],
                                        positions[2] + viewModel.searchContentQuery.length - 1
                                    )
                                    1 -> binding.pageView.curPage.selectEndMoveIndex(
                                        0,
                                        positions[1] + 1,
                                        positions[4]
                                    )
                                    //consider change page, jump to scroll position
                                    -1 -> binding.pageView.curPage
                                        .selectEndMoveIndex(1, 0, positions[4])
                                }
                                binding.pageView.isTextSelected = true
                                delay(100L)
                            }
                        }
                    }
                requestCodeReplace -> viewModel.replaceRuleChanged()
            }
        }
    }

    override fun finish() {
        ReadBook.book?.let {
            if (!ReadBook.inBookshelf) {
                alert(title = getString(R.string.add_to_shelf)) {
                    message = getString(R.string.check_add_bookshelf, it.name)
                    okButton {
                        ReadBook.inBookshelf = true
                        setResult(Activity.RESULT_OK)
                    }
                    noButton { viewModel.removeFromBookshelf { super.finish() } }
                }.show()
            } else {
                super.finish()
            }
        } ?: super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacks(keepScreenRunnable)
        textActionMenu?.dismiss()
        binding.pageView.onDestroy()
        ReadBook.msg = null
        if (!BuildConfig.DEBUG) {
            SyncBookProgress.uploadBookProgress()
            Backup.autoBack(this)
        }
    }

    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.TIME_CHANGED) { binding.pageView.upTime() }
        observeEvent<Int>(EventBus.BATTERY_CHANGED) { binding.pageView.upBattery(it) }
        observeEvent<BookChapter>(EventBus.OPEN_CHAPTER) {
            viewModel.openChapter(it.index, ReadBook.durPageIndex)
            binding.pageView.upContent()
        }
        observeEvent<Boolean>(EventBus.MEDIA_BUTTON) {
            if (it) {
                onClickReadAloud()
            } else {
                ReadBook.readAloud(!BaseReadAloudService.pause)
            }
        }
        observeEvent<Boolean>(EventBus.UP_CONFIG) {
            upSystemUiVisibility()
            binding.pageView.upBg()
            binding.pageView.upTipStyle()
            binding.pageView.upStyle()
            if (it) {
                ReadBook.loadContent(resetPageOffset = false)
            } else {
                binding.pageView.upContent(resetPageOffset = false)
            }
        }
        observeEvent<Int>(EventBus.ALOUD_STATE) {
            if (it == Status.STOP || it == Status.PAUSE) {
                ReadBook.curTextChapter?.let { textChapter ->
                    val page = textChapter.page(ReadBook.durPageIndex)
                    if (page != null) {
                        page.removePageAloudSpan()
                        binding.pageView.upContent(resetPageOffset = false)
                    }
                }
            }
        }
        observeEventSticky<Int>(EventBus.TTS_PROGRESS) { chapterStart ->
            launch(IO) {
                if (BaseReadAloudService.isPlay()) {
                    ReadBook.curTextChapter?.let { textChapter ->
                        val pageStart =
                            chapterStart - textChapter.getReadLength(ReadBook.durPageIndex)
                        textChapter.page(ReadBook.durPageIndex)?.upPageAloudSpan(pageStart)
                        upContent()
                    }
                }
            }
        }
        observeEvent<Boolean>(PreferKey.keepLight) {
            upScreenTimeOut()
        }
        observeEvent<Boolean>(PreferKey.textSelectAble) {
            binding.pageView.curPage.upSelectAble(it)
        }
        observeEvent<String>(PreferKey.showBrightnessView) {
            binding.readMenu.upBrightnessState()
        }
        observeEvent<String>(EventBus.REPLACE_RULE_SAVE) {
            viewModel.replaceRuleChanged()
        }
    }

    private fun upScreenTimeOut() {
        getPrefString(PreferKey.keepLight)?.let {
            screenTimeOut = it.toLong() * 1000
        }
        screenOffTimerStart()
    }

    /**
     * 重置黑屏时间
     */
    override fun screenOffTimerStart() {
        if (screenTimeOut < 0) {
            keepScreenOn(window, true)
            return
        }
        val t = screenTimeOut - sysScreenOffTime
        if (t > 0) {
            mHandler.removeCallbacks(keepScreenRunnable)
            keepScreenOn(window, true)
            mHandler.postDelayed(keepScreenRunnable, screenTimeOut)
        } else {
            keepScreenOn(window, false)
        }
    }
}