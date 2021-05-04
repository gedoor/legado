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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.core.view.size
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.constant.Status
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookProgress
import io.legado.app.data.entities.Bookmark
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.ReadTipConfig
import io.legado.app.help.storage.Backup
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
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
import io.legado.app.ui.book.read.page.ReadView
import io.legado.app.ui.book.read.page.entities.PageDirection
import io.legado.app.ui.book.read.page.provider.TextPageFactory
import io.legado.app.ui.book.searchContent.SearchContentActivity
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.book.toc.TocActivityResult
import io.legado.app.ui.login.SourceLogin
import io.legado.app.ui.replace.ReplaceRuleActivity
import io.legado.app.ui.replace.edit.ReplaceEditActivity
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReadBookActivity : ReadBookBaseActivity(),
    View.OnTouchListener,
    ReadView.CallBack,
    TextActionMenu.CallBack,
    ContentTextView.CallBack,
    ReadMenu.CallBack,
    ReadAloudDialog.CallBack,
    ChangeSourceDialog.CallBack,
    ReadBook.CallBack,
    AutoReadDialog.CallBack,
    TocRegexDialog.CallBack,
    ColorPickerDialogListener {

    private val tocActivity =
        registerForActivityResult(TocActivityResult()) {
            it?.let {
                if (it.first != ReadBook.durChapterIndex) {
                    viewModel.openChapter(it.first, it.second)
                }
            }
        }
    private val sourceEditActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it ?: return@registerForActivityResult
            if (it.resultCode == RESULT_OK) {
                viewModel.upBookSource {
                    upView()
                }
            }
        }
    private val replaceActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it ?: return@registerForActivityResult
            if (it.resultCode == RESULT_OK) {
                viewModel.replaceRuleChanged()
            }
        }
    private val searchContentActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it ?: return@registerForActivityResult
            it.data?.let { data ->
                data.getIntExtra("index", ReadBook.durChapterIndex).let { index ->
                    viewModel.searchContentQuery = data.getStringExtra("query") ?: ""
                    val indexWithinChapter = data.getIntExtra("indexWithinChapter", 0)
                    skipToSearch(index, indexWithinChapter)
                }
            }
        }
    private var menu: Menu? = null
    private val textActionMenu: TextActionMenu by lazy {
        TextActionMenu(this, this)
    }

    override val scope: CoroutineScope get() = this
    override val isInitFinish: Boolean get() = viewModel.isInitFinish
    override val isScroll: Boolean get() = binding.readView.isScroll
    private val mHandler = Handler(Looper.getMainLooper())
    private val keepScreenRunnable = Runnable { keepScreenOn(window, false) }
    private val autoPageRunnable = Runnable { autoPagePlus() }
    private val backupRunnable = Runnable {
        if (!BuildConfig.DEBUG) {
            ReadBook.uploadProgress()
            Backup.autoBack(this)
        }
    }
    override var autoPageProgress = 0
    override var isAutoPage = false
    private var screenTimeOut: Long = 0
    private var timeBatteryReceiver: TimeBatteryReceiver? = null
    private var loadStates: Boolean = false
    override val pageFactory: TextPageFactory get() = binding.readView.pageFactory
    override val headerHeight: Int get() = binding.readView.curPage.headerHeight

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
        binding.readView.upStatusBar()
    }

    override fun onResume() {
        super.onResume()
        ReadBook.readStartTime = System.currentTimeMillis()
        upSystemUiVisibility()
        timeBatteryReceiver = TimeBatteryReceiver.register(this)
        binding.readView.upTime()
    }

    override fun onPause() {
        super.onPause()
        mHandler.removeCallbacks(backupRunnable)
        ReadBook.saveRead()
        timeBatteryReceiver?.let {
            unregisterReceiver(it)
            timeBatteryReceiver = null
        }
        upSystemUiVisibility()
        if (!BuildConfig.DEBUG) {
            ReadBook.uploadProgress()
            Backup.autoBack(this)
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_read, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        upMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    /**
     * 更新菜单
     */
    private fun upMenu() {
        val menu = menu
        val book = ReadBook.book
        if (menu != null && book != null) {
            val onLine = !book.isLocalBook()
            for (i in 0 until menu.size) {
                val item = menu[i]
                when (item.groupId) {
                    R.id.menu_group_on_line,
                    R.id.menu_group_on_line_ns -> item.isVisible = onLine
                    R.id.menu_group_local -> item.isVisible = !onLine
                    R.id.menu_group_text -> item.isVisible = book.isLocalTxt()
                    else -> when (item.itemId) {
                        R.id.menu_enable_replace -> item.isChecked = book.getUseReplaceRule()
                        R.id.menu_re_segment -> item.isChecked = book.getReSegment()
                        R.id.menu_reverse_content -> item.isVisible = onLine
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
                if (ReadBook.bookSource == null) {
                    upContent()
                } else {
                    ReadBook.book?.let {
                        ReadBook.curTextChapter = null
                        binding.readView.upContent()
                        viewModel.refreshContent(it)
                    }
                }
            }
            R.id.menu_download -> showDownloadDialog()
            R.id.menu_add_bookmark -> {
                val book = ReadBook.book
                val page = ReadBook.curTextChapter?.page(ReadBook.durPageIndex())
                if (book != null && page != null) {
                    val bookmark = Bookmark(
                        bookUrl = book.bookUrl,
                        bookName = book.name,
                        chapterIndex = ReadBook.durChapterIndex,
                        chapterPos = ReadBook.durChapterPos,
                        chapterName = page.title,
                        bookText = page.text.trim()
                    )
                    showBookMark(bookmark)
                }
            }
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
                binding.readView.upPageAnim()
            }
            R.id.menu_book_info -> ReadBook.book?.let {
                startActivity<BookInfoActivity> {
                    putExtra("name", it.name)
                    putExtra("author", it.author)
                }
            }
            R.id.menu_toc_regex -> TocRegexDialog.show(
                supportFragmentManager,
                ReadBook.book?.tocUrl
            )
            R.id.menu_reverse_content -> ReadBook.book?.let {
                viewModel.reverseContent(it)
            }
            R.id.menu_set_charset -> showCharsetConfig()
            R.id.menu_image_style -> {
                val imgStyles =
                    arrayListOf(Book.imgStyleDefault, Book.imgStyleFull, Book.imgStyleText)
                selector(
                    R.string.image_style,
                    imgStyles
                ) { _, index ->
                    ReadBook.book?.setImageStyle(imgStyles[index])
                    ReadBook.loadContent(false)
                }
            }
            R.id.menu_get_progress -> ReadBook.book?.let {
                viewModel.syncBookProgress(it) { progress ->
                    sureSyncProgress(progress)
                }
            }
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
                    binding.readView.pageDelegate?.keyTurnPage(PageDirection.PREV)
                    return true
                }
            }
            isNextKey(keyCode) -> {
                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    binding.readView.pageDelegate?.keyTurnPage(PageDirection.NEXT)
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
            keyCode == KeyEvent.KEYCODE_PAGE_UP -> {
                binding.readView.pageDelegate?.keyTurnPage(PageDirection.PREV)
                return true
            }
            keyCode == KeyEvent.KEYCODE_PAGE_DOWN -> {
                binding.readView.pageDelegate?.keyTurnPage(PageDirection.NEXT)
                return true
            }
            keyCode == KeyEvent.KEYCODE_SPACE -> {
                binding.readView.pageDelegate?.keyTurnPage(PageDirection.NEXT)
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
                            toastOnUi(R.string.read_aloud_pause)
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
    override fun onTouch(v: View, event: MotionEvent): Boolean = with(binding) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> textActionMenu.dismiss()
            MotionEvent.ACTION_MOVE -> {
                when (v.id) {
                    R.id.cursor_left -> readView.curPage.selectStartMove(
                        event.rawX + cursorLeft.width,
                        event.rawY - cursorLeft.height
                    )
                    R.id.cursor_right -> readView.curPage.selectEndMove(
                        event.rawX - cursorRight.width,
                        event.rawY - cursorRight.height
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
    override fun upSelectedStart(x: Float, y: Float, top: Float) = with(binding) {
        cursorLeft.x = x - cursorLeft.width
        cursorLeft.y = y
        cursorLeft.visible(true)
        textMenuPosition.x = x
        textMenuPosition.y = top
    }

    /**
     * 更新文字选择结束位置
     */
    override fun upSelectedEnd(x: Float, y: Float) = with(binding) {
        cursorRight.x = x
        cursorRight.y = y
        cursorRight.visible(true)
    }

    /**
     * 取消文字选择
     */
    override fun onCancelSelect() = with(binding) {
        cursorLeft.invisible()
        cursorRight.invisible()
        textActionMenu.dismiss()
    }

    /**
     * 显示文本操作菜单
     */
    override fun showTextActionMenu() = with(binding) {
        textActionMenu.contentView.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )
        val popupHeight = textActionMenu.contentView.measuredHeight
        val x = textMenuPosition.x.toInt()
        var y = textMenuPosition.y.toInt() - popupHeight
        if (y < statusBarHeight) {
            y = (cursorLeft.y + cursorLeft.height).toInt()
        }
        if (cursorRight.y > y && cursorRight.y < y + popupHeight) {
            y = (cursorRight.y + cursorRight.height).toInt()
        }
        if (!textActionMenu.isShowing) {
            textActionMenu.showAtLocation(
                textMenuPosition, Gravity.TOP or Gravity.START, x, y
            )
        } else {
            textActionMenu.update(x, y, WRAP_CONTENT, WRAP_CONTENT)
        }
    }

    /**
     * 当前选择的文本
     */
    override val selectedText: String get() = binding.readView.curPage.selectedText

    /**
     * 文本选择菜单操作
     */
    override fun onMenuItemSelected(itemId: Int): Boolean {
        when (itemId) {
            R.id.menu_bookmark -> binding.readView.curPage.let {
                val bookmark = it.createBookmark()
                if (bookmark == null) {
                    toastOnUi(R.string.create_bookmark_error)
                } else {
                    showBookMark(bookmark)
                }
                return true
            }
            R.id.menu_replace -> {
                val scopes = arrayListOf<String>()
                ReadBook.book?.name?.let {
                    scopes.add(it)
                }
                ReadBook.bookSource?.bookSourceUrl?.let {
                    scopes.add(it)
                }
                replaceActivity.launch(
                    ReplaceEditActivity.startIntent(
                        this,
                        pattern = selectedText,
                        scope = scopes.joinToString(";")
                    )
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
    override fun onMenuActionFinally() = with(binding) {
        textActionMenu.dismiss()
        readView.curPage.cancelSelect()
        readView.isTextSelected = false
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
                    binding.readView.pageDelegate?.isCancel = false
                    binding.readView.pageDelegate?.keyTurnPage(direction)
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
    override fun upContent(
        relativePosition: Int,
        resetPageOffset: Boolean,
        success: (() -> Unit)?
    ) {
        launch {
            autoPageProgress = 0
            binding.readView.upContent(relativePosition, resetPageOffset)
            binding.readMenu.setSeekPage(ReadBook.durPageIndex())
            loadStates = false
            success?.invoke()
        }
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
            binding.readView.upPageAnim()
        }
    }

    /**
     * 页面改变
     */
    override fun pageChanged() {
        autoPageProgress = 0
        launch {
            binding.readMenu.setSeekPage(ReadBook.durPageIndex())
        }
        mHandler.postDelayed(backupRunnable, 600000)
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
            binding.readView.upContent()
            binding.readView.upContent(1)
            autoPagePlus()
            binding.readMenu.setAutoPage(true)
        }
    }

    override fun autoPageStop() {
        isAutoPage = false
        mHandler.removeCallbacks(autoPageRunnable)
        binding.readView.upContent()
        binding.readMenu.setAutoPage(false)
    }

    private fun autoPagePlus() {
        var delayMillis = ReadBookConfig.autoReadSpeed * 1000L / binding.readView.height
        var scrollOffset = 1
        if (delayMillis < 20) {
            scrollOffset = 20 / delayMillis.toInt()
            delayMillis = 20
        }
        mHandler.removeCallbacks(autoPageRunnable)
        if (binding.readView.isScroll) {
            binding.readView.curPage.scroll(-scrollOffset)
        } else {
            autoPageProgress += scrollOffset
            if (autoPageProgress >= binding.readView.height) {
                autoPageProgress = 0
                if (!binding.readView.fillPage(PageDirection.NEXT)) {
                    autoPageStop()
                }
            } else {
                binding.readView.invalidate()
            }
        }
        mHandler.postDelayed(autoPageRunnable, delayMillis)
    }

    override fun openSourceEditActivity() {
        ReadBook.webBook?.let {
            sourceEditActivity.launch(Intent(this, BookSourceEditActivity::class.java).apply {
                putExtra("data", it.bookSource.bookSourceUrl)
            })
        }
    }

    /**
     * 替换
     */
    override fun openReplaceRule() {
        replaceActivity.launch(Intent(this, ReplaceRuleActivity::class.java))
    }

    /**
     * 打开目录
     */
    override fun openChapterList() {
        ReadBook.book?.let {
            tocActivity.launch(it.bookUrl)
        }
    }

    /**
     * 打开搜索界面
     */
    override fun openSearchActivity(searchWord: String?) {
        ReadBook.book?.let {
            searchContentActivity.launch(Intent(this, SearchContentActivity::class.java).apply {
                putExtra("bookUrl", it.bookUrl)
                putExtra("searchWord", searchWord ?: viewModel.searchContentQuery)
            })
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

    override fun showLogin() {
        ReadBook.webBook?.bookSource?.let {
            startActivity<SourceLogin> {
                putExtra("sourceUrl", it.bookSourceUrl)
                putExtra("loginUrl", it.loginUrl)
                putExtra("userAgent", it.getHeaderMap()[AppConst.UA_NAME])
            }
        }
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

    private fun sureSyncProgress(progress: BookProgress) {
        alert(R.string.get_book_progress) {
            setMessage(R.string.current_progress_exceeds_cloud)
            okButton {
                ReadBook.setProgress(progress)
            }
            noButton()
        }.show()
    }

    private fun skipToSearch(index: Int, indexWithinChapter: Int) {
        viewModel.openChapter(index) {
            val pages = ReadBook.curTextChapter?.pages ?: return@openChapter
            val positions = ReadBook.searchResultPositions(
                pages,
                indexWithinChapter,
                viewModel.searchContentQuery
            )
            ReadBook.skipToPage(positions[0]) {
                launch {
                    binding.readView.curPage.selectStartMoveIndex(0, positions[1], positions[2])
                    delay(20L)
                    when (positions[3]) {
                        0 -> binding.readView.curPage.selectEndMoveIndex(
                            0,
                            positions[1],
                            positions[2] + viewModel.searchContentQuery.length - 1
                        )
                        1 -> binding.readView.curPage.selectEndMoveIndex(
                            0,
                            positions[1] + 1,
                            positions[4]
                        )
                        //consider change page, jump to scroll position
                        -1 -> binding.readView.curPage
                            .selectEndMoveIndex(1, 0, positions[4])
                    }
                    binding.readView.isTextSelected = true
                    delay(100L)
                }
            }
        }
    }

    override fun finish() {
        ReadBook.book?.let {
            if (!ReadBook.inBookshelf) {
                alert(title = getString(R.string.add_to_shelf)) {
                    setMessage(getString(R.string.check_add_bookshelf, it.name))
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
        textActionMenu.dismiss()
        binding.readView.onDestroy()
        ReadBook.msg = null
        if (!BuildConfig.DEBUG) {
            Backup.autoBack(this)
        }
    }

    override fun observeLiveBus() = with(binding) {
        super.observeLiveBus()
        observeEvent<String>(EventBus.TIME_CHANGED) { readView.upTime() }
        observeEvent<Int>(EventBus.BATTERY_CHANGED) { readView.upBattery(it) }
        observeEvent<BookChapter>(EventBus.OPEN_CHAPTER) {
            viewModel.openChapter(it.index, ReadBook.durChapterPos)
            readView.upContent()
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
            readView.upBg()
            readView.upStyle()
            if (it) {
                ReadBook.loadContent(resetPageOffset = false)
            } else {
                readView.upContent(resetPageOffset = false)
            }
        }
        observeEvent<Int>(EventBus.ALOUD_STATE) {
            if (it == Status.STOP || it == Status.PAUSE) {
                ReadBook.curTextChapter?.let { textChapter ->
                    val page = textChapter.getPageByReadPos(ReadBook.durChapterPos)
                    if (page != null) {
                        page.removePageAloudSpan()
                        readView.upContent(resetPageOffset = false)
                    }
                }
            }
        }
        observeEventSticky<Int>(EventBus.TTS_PROGRESS) { chapterStart ->
            launch(IO) {
                if (BaseReadAloudService.isPlay()) {
                    ReadBook.curTextChapter?.let { textChapter ->
                        val pageStart = chapterStart - ReadBook.durChapterPos
                        textChapter.getPageByReadPos(ReadBook.durChapterPos)
                            ?.upPageAloudSpan(pageStart)
                        upContent()
                    }
                }
            }
        }
        observeEvent<Boolean>(PreferKey.keepLight) {
            upScreenTimeOut()
        }
        observeEvent<Boolean>(PreferKey.textSelectAble) {
            readView.curPage.upSelectAble(it)
        }
        observeEvent<String>(PreferKey.showBrightnessView) {
            readMenu.upBrightnessState()
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