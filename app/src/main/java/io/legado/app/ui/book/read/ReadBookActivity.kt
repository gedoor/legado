package io.legado.app.ui.book.read

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
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
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.constant.Status
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.SyncBookProgress
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.lib.theme.ATH
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
import io.legado.app.ui.book.read.page.ContentTextView
import io.legado.app.ui.book.read.page.PageView
import io.legado.app.ui.book.read.page.TextPageFactory
import io.legado.app.ui.book.read.page.delegate.PageDelegate
import io.legado.app.ui.book.searchContent.SearchContentActivity
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.book.toc.ChapterListActivity
import io.legado.app.ui.login.SourceLogin
import io.legado.app.ui.replace.ReplaceRuleActivity
import io.legado.app.ui.replace.edit.ReplaceEditActivity
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_book_read.*
import kotlinx.android.synthetic.main.view_read_menu.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast

class ReadBookActivity : VMBaseActivity<ReadBookViewModel>(R.layout.activity_book_read),
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
    private val requestCodeEditSource = 111
    private val requestCodeReplace = 312
    private val requestCodeSearchResult = 123
    private var menu: Menu? = null
    private var textActionMenu: TextActionMenu? = null

    override val viewModel: ReadBookViewModel
        get() = getViewModel(ReadBookViewModel::class.java)

    override val scope: CoroutineScope get() = this
    override val isInitFinish: Boolean get() = viewModel.isInitFinish
    override val isScroll: Boolean get() = page_view.isScroll
    private val mHandler = Handler(Looper.getMainLooper())
    private val keepScreenRunnable: Runnable =
        Runnable { ReadBookActivityHelp.keepScreenOn(window, false) }
    private val autoPageRunnable: Runnable = Runnable { autoPagePlus() }
    override var autoPageProgress = 0
    override var isAutoPage = false
    private var screenTimeOut: Long = 0
    private var timeBatteryReceiver: TimeBatteryReceiver? = null
    private var loadStates: Boolean = false
    override val pageFactory: TextPageFactory get() = page_view.pageFactory
    override val headerHeight: Int get() = page_view.curPage.headerHeight

    override fun onCreate(savedInstanceState: Bundle?) {
        ReadBook.msg = null
        ReadBookActivityHelp.setOrientation(this)
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        ReadBookActivityHelp.upLayoutInDisplayCutoutMode(window)
        initView()
        upScreenTimeOut()
        ReadBook.callBack = this
        ReadBook.titleDate.observe(this) {
            title_bar.title = it
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
        page_view.upStatusBar()
        ReadBook.loadContent(resetPageOffset = false)
    }

    override fun onResume() {
        super.onResume()
        ReadBook.readStartTime = System.currentTimeMillis()
        upSystemUiVisibility()
        timeBatteryReceiver = TimeBatteryReceiver.register(this)
        page_view.upTime()
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

    override fun upNavigationBarColor() {
        when {
            read_menu == null -> return
            read_menu.isVisible -> {
                ATH.setNavigationBarColorAuto(this)
            }
            ReadBookConfig.bg is ColorDrawable -> {
                ATH.setNavigationBarColorAuto(this, ReadBookConfig.bgMeanColor)
            }
            else -> {
                ATH.setNavigationBarColorAuto(this, Color.BLACK)
            }
        }
    }

    /**
     * 初始化View
     */
    private fun initView() {
        cursor_left.setColorFilter(accentColor)
        cursor_right.setColorFilter(accentColor)
        cursor_left.setOnTouchListener(this)
        cursor_right.setOnTouchListener(this)
        tv_chapter_name.onClick {
            ReadBook.webBook?.let {
                startActivityForResult<BookSourceEditActivity>(
                    requestCodeEditSource,
                    Pair("data", it.bookSource.bookSourceUrl)
                )
            }
        }
        tv_chapter_url.onClick {
            runCatching {
                val url = tv_chapter_url.text.toString()
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
        }
    }

    fun showPaddingConfig() {
        PaddingConfigDialog().show(supportFragmentManager, "paddingConfig")
    }

    fun showBgTextConfig() {
        BgTextConfigDialog().show(supportFragmentManager, "bgTextConfig")
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
                        R.id.menu_group_on_line -> item.isVisible = onLine
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
                read_menu.runMenuOut()
                ReadBook.book?.let {
                    ChangeSourceDialog.show(supportFragmentManager, it.name, it.author)
                }
            }
            R.id.menu_refresh -> {
                ReadBook.book?.let {
                    ReadBook.curTextChapter = null
                    page_view.upContent()
                    viewModel.refreshContent(it)
                }
            }
            R.id.menu_download -> ReadBookActivityHelp.showDownloadDialog(this)
            R.id.menu_add_bookmark -> ReadBookActivityHelp.showBookMark(this)
            R.id.menu_copy_text ->
                TextDialog.show(supportFragmentManager, ReadBook.curTextChapter?.getContent())
            R.id.menu_update_toc -> ReadBook.book?.let {
                loadChapterList(it)
            }
            R.id.menu_enable_replace -> ReadBook.book?.let {
                it.setUseReplaceRule(!it.getUseReplaceRule())
                menu?.findItem(R.id.menu_enable_replace)?.isChecked = it.getUseReplaceRule()
                onReplaceRuleSave()
            }
            R.id.menu_re_segment -> ReadBook.book?.let {
                it.setReSegment(!it.getReSegment())
                menu?.findItem(R.id.menu_re_segment)?.isChecked = it.getReSegment()
                ReadBook.loadContent(false)
            }
            R.id.menu_page_anim -> ReadBookActivityHelp.showPageAnimConfig(this) {
                page_view.upPageAnim()
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
            R.id.menu_set_charset -> ReadBookActivityHelp.showCharsetConfig(this)
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
            if (isDown && !read_menu.cnaShowMenu) {
                read_menu.runMenuIn()
                return true
            }
            if (!isDown && !read_menu.cnaShowMenu) {
                read_menu.cnaShowMenu = true
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
            ReadBookActivityHelp.isPrevKey(this, keyCode) -> {
                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    page_view.pageDelegate?.keyTurnPage(PageDelegate.Direction.PREV)
                    return true
                }
            }
            ReadBookActivityHelp.isNextKey(this, keyCode) -> {
                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    page_view.pageDelegate?.keyTurnPage(PageDelegate.Direction.NEXT)
                    return true
                }
            }
            keyCode == KeyEvent.KEYCODE_VOLUME_UP -> {
                if (volumeKeyPage(PageDelegate.Direction.PREV)) {
                    return true
                }
            }
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (volumeKeyPage(PageDelegate.Direction.NEXT)) {
                    return true
                }
            }
            keyCode == KeyEvent.KEYCODE_SPACE -> {
                page_view.pageDelegate?.keyTurnPage(PageDelegate.Direction.NEXT)
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
                if (volumeKeyPage(PageDelegate.Direction.NONE)) {
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
                    R.id.cursor_left -> page_view.curPage.selectStartMove(
                        event.rawX + cursor_left.width,
                        event.rawY - cursor_left.height
                    )
                    R.id.cursor_right -> page_view.curPage.selectEndMove(
                        event.rawX - cursor_right.width,
                        event.rawY - cursor_right.height
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
        cursor_left.x = x - cursor_left.width
        cursor_left.y = y
        cursor_left.visible(true)
        text_menu_position.x = x
        text_menu_position.y = top
    }

    /**
     * 更新文字选择结束位置
     */
    override fun upSelectedEnd(x: Float, y: Float) {
        cursor_right.x = x
        cursor_right.y = y
        cursor_right.visible(true)
    }

    /**
     * 取消文字选择
     */
    override fun onCancelSelect() {
        cursor_left.invisible()
        cursor_right.invisible()
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
            val x = text_menu_position.x.toInt()
            var y = text_menu_position.y.toInt() - popupHeight
            if (y < statusBarHeight) {
                y = (cursor_left.y + cursor_left.height).toInt()
            }
            if (cursor_right.y > y && cursor_right.y < y + popupHeight) {
                y = (cursor_right.y + cursor_right.height).toInt()
            }
            if (!popup.isShowing) {
                popup.showAtLocation(text_menu_position, Gravity.TOP or Gravity.START, x, y)
            } else {
                popup.update(x, y, WRAP_CONTENT, WRAP_CONTENT)
            }
        }
    }

    /**
     * 当前选择的文本
     */
    override val selectedText: String get() = page_view.curPage.selectedText

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
        page_view.curPage.cancelSelect()
        page_view.isTextSelected = false
    }

    /**
     * 音量键翻页
     */
    private fun volumeKeyPage(direction: PageDelegate.Direction): Boolean {
        if (!read_menu.isVisible) {
            if (getPrefBoolean("volumeKeyPage", true)) {
                if (getPrefBoolean("volumeKeyPageOnPlay")
                    || BaseReadAloudService.pause
                ) {
                    page_view.pageDelegate?.isCancel = false
                    page_view.pageDelegate?.keyTurnPage(direction)
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
            page_view.upContent(relativePosition, resetPageOffset)
            seek_read_page.progress = ReadBook.durPageIndex
        }
        loadStates = false
    }

    /**
     * 更新视图
     */
    override fun upView() {
        launch {
            ReadBook.curTextChapter?.let {
                tv_chapter_name.text = it.title
                tv_chapter_name.visible()
                if (!ReadBook.isLocalBook) {
                    tv_chapter_url.text = it.url
                    tv_chapter_url.visible()
                } else {
                    tv_chapter_url.gone()
                }
                seek_read_page.max = it.pageSize.minus(1)
                seek_read_page.progress = ReadBook.durPageIndex
                tv_pre.isEnabled = ReadBook.durChapterIndex != 0
                tv_next.isEnabled = ReadBook.durChapterIndex != ReadBook.chapterSize - 1
            } ?: let {
                tv_chapter_name.gone()
                tv_chapter_url.gone()
            }
        }
    }

    override fun upPageAnim() {
        launch {
            page_view.upPageAnim()
        }
    }

    /**
     * 页面改变
     */
    override fun pageChanged() {
        autoPageProgress = 0
        launch {
            seek_read_page.progress = ReadBook.durPageIndex
        }
    }

    /**
     * 显示菜单
     */
    override fun showMenuBar() {
        read_menu.runMenuIn()
    }

    override val oldBook: Book?
        get() = ReadBook.book

    override fun changeTo(book: Book) {
        viewModel.changeTo(book)
    }

    override fun clickCenter() {
        when {
            BaseReadAloudService.isRun -> {
                showReadAloudDialog()
            }
            isAutoPage -> {
                AutoReadDialog().show(supportFragmentManager, "autoRead")
            }
            else -> {
                read_menu.runMenuIn()
            }
        }
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
        if (isAutoPage) {
            autoPageStop()
        } else {
            isAutoPage = true
            page_view.upContent()
            page_view.upContent(1)
            autoPagePlus()
        }
        read_menu.setAutoPage(isAutoPage)
    }

    override fun autoPageStop() {
        isAutoPage = false
        mHandler.removeCallbacks(autoPageRunnable)
        page_view.upContent()
    }

    private fun autoPagePlus() {
        mHandler.removeCallbacks(autoPageRunnable)
        autoPageProgress++
        if (autoPageProgress >= ReadBookConfig.autoReadSpeed * 10) {
            autoPageProgress = 0
            page_view.fillPage(PageDelegate.Direction.NEXT)
        } else {
            page_view.invalidate()
        }
        mHandler.postDelayed(autoPageRunnable, 100)
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
     * 替换规则变化
     */
    private fun onReplaceRuleSave() {
        Coroutine.async {
            BookHelp.upReplaceRules()
            ReadBook.loadContent(resetPageOffset = false)
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
        ReadBookActivityHelp.upSystemUiVisibility(this, isInMultiWindow, !read_menu.isVisible)
        upNavigationBarColor()
    }

    /**
     * 朗读按钮
     */
    override fun onClickReadAloud() {
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
                                page_view.curPage.selectStartMoveIndex(
                                    0,
                                    positions[1],
                                    positions[2]
                                )
                                delay(20L)
                                when (positions[3]) {
                                    0 -> page_view.curPage.selectEndMoveIndex(
                                        0,
                                        positions[1],
                                        positions[2] + viewModel.searchContentQuery.length - 1
                                    )
                                    1 -> page_view.curPage.selectEndMoveIndex(
                                        0,
                                        positions[1] + 1,
                                        positions[4]
                                    )
                                    //consider change page, jump to scroll position
                                    -1 -> page_view.curPage.selectEndMoveIndex(1, 0, positions[4])
                                }
                                page_view.isTextSelected = true
                                delay(100L)
                            }
                        }
                    }
                requestCodeReplace -> onReplaceRuleSave()
            }

        }
    }

    override fun finish() {
        ReadBook.book?.let {
            if (!ReadBook.inBookshelf) {
                this.alert(title = getString(R.string.add_to_shelf)) {
                    message = getString(R.string.check_add_bookshelf, it.name)
                    okButton {
                        ReadBook.inBookshelf = true
                        setResult(Activity.RESULT_OK)
                    }
                    noButton { viewModel.removeFromBookshelf { super.finish() } }
                }.show().applyTint()
            } else {
                super.finish()
            }
        } ?: super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacks(keepScreenRunnable)
        textActionMenu?.dismiss()
        page_view.onDestroy()
        ReadBook.msg = null
        if (!BuildConfig.DEBUG) {
            SyncBookProgress.uploadBookProgress()
            Backup.autoBack(this)
        }
    }

    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.TIME_CHANGED) { page_view.upTime() }
        observeEvent<Int>(EventBus.BATTERY_CHANGED) { page_view.upBattery(it) }
        observeEvent<BookChapter>(EventBus.OPEN_CHAPTER) {
            viewModel.openChapter(it.index, ReadBook.durPageIndex)
            page_view.upContent()
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
            page_view.upBg()
            page_view.upTipStyle()
            page_view.upStyle()
            if (it) {
                ReadBook.loadContent(resetPageOffset = false)
            } else {
                page_view.upContent(resetPageOffset = false)
            }
        }
        observeEvent<Int>(EventBus.ALOUD_STATE) {
            if (it == Status.STOP || it == Status.PAUSE) {
                ReadBook.curTextChapter?.let { textChapter ->
                    val page = textChapter.page(ReadBook.durPageIndex)
                    if (page != null) {
                        page.removePageAloudSpan()
                        page_view.upContent(resetPageOffset = false)
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
            page_view.curPage.upSelectAble(it)
        }
        observeEvent<String>(PreferKey.showBrightnessView) {
            read_menu.upBrightnessState()
        }
        observeEvent<String>(EventBus.REPLACE_RULE_SAVE) {
            onReplaceRuleSave()
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
            ReadBookActivityHelp.keepScreenOn(window, true)
            return
        }
        val t = screenTimeOut - sysScreenOffTime
        if (t > 0) {
            mHandler.removeCallbacks(keepScreenRunnable)
            ReadBookActivityHelp.keepScreenOn(window, true)
            mHandler.postDelayed(keepScreenRunnable, screenTimeOut)
        } else {
            ReadBookActivityHelp.keepScreenOn(window, false)
        }
    }
}