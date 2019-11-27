package io.legado.app.ui.book.read

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.constant.Bus
import io.legado.app.constant.Status
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.IntentDataHelp
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.receiver.TimeElectricityReceiver
import io.legado.app.service.BaseReadAloudService
import io.legado.app.service.help.ReadAloud
import io.legado.app.ui.book.read.config.*
import io.legado.app.ui.book.read.config.BgTextConfigDialog.Companion.BG_COLOR
import io.legado.app.ui.book.read.config.BgTextConfigDialog.Companion.TEXT_COLOR
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.changesource.ChangeSourceDialog
import io.legado.app.ui.chapterlist.ChapterListActivity
import io.legado.app.ui.main.MainActivity
import io.legado.app.ui.replacerule.ReplaceRuleActivity
import io.legado.app.ui.replacerule.edit.ReplaceEditDialog
import io.legado.app.ui.widget.page.ChapterProvider
import io.legado.app.ui.widget.page.PageView
import io.legado.app.ui.widget.page.TextChapter
import io.legado.app.ui.widget.page.delegate.PageDelegate
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_book_read.*
import kotlinx.android.synthetic.main.view_read_menu.*
import kotlinx.android.synthetic.main.view_title_bar.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast

class ReadBookActivity : BaseActivity(R.layout.activity_book_read),
    PageView.CallBack,
    ReadMenu.CallBack,
    ReadAloudDialog.CallBack,
    ChangeSourceDialog.CallBack,
    ReadBookViewModel.CallBack,
    ColorPickerDialogListener {

    private val requestCodeChapterList = 568
    private val requestCodeEditSource = 111
    private var timeElectricityReceiver: TimeElectricityReceiver? = null
    override var readAloudStatus = Status.STOP

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Help.upLayoutInDisplayCutoutMode(window)
        setSupportActionBar(toolbar)
        initView()
        ReadBookViewModel.callBack = this
        ReadBookViewModel.titleDate.observe(this, Observer { title_bar.title = it })
        ReadBookViewModel.initData(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        upSystemUiVisibility()
    }

    override fun onResume() {
        super.onResume()
        upSystemUiVisibility()
        timeElectricityReceiver = TimeElectricityReceiver.register(this)
        page_view.upTime()
    }

    override fun onPause() {
        super.onPause()
        timeElectricityReceiver?.let {
            unregisterReceiver(it)
            timeElectricityReceiver = null
        }
        upSystemUiVisibility()
    }

    /**
     * 初始化View
     */
    private fun initView() {
        tv_chapter_name.onClick {
            ReadBookViewModel.webBook?.let {
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

    /**
     * 菜单
     */
    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_source -> {
                read_menu.runMenuOut()
                ReadBookViewModel.book?.let {
                    ChangeSourceDialog.show(supportFragmentManager, it.name, it.author)
                }
            }
            R.id.menu_refresh -> {
                ReadBookViewModel.book?.let {
                    ReadBookViewModel.curTextChapter = null
                    page_view.upContent()
                    ReadBookViewModel.refreshContent(it)
                }
            }
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (volumeKeyPage(PageDelegate.Direction.PREV)) {
                    return true
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (volumeKeyPage(PageDelegate.Direction.NEXT)) {
                    return true
                }
            }
            KeyEvent.KEYCODE_SPACE -> {
                page_view.moveToNextPage()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                page_view.snackbar(R.string.to_backstage, R.string.ok) {
                    startActivity<MainActivity>()
                }
                return true
            }
        }
        return super.onKeyLongPress(keyCode, event)
    }

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
                        if (readAloudStatus == Status.PLAY) {
                            ReadAloud.pause(this)
                            toast(R.string.read_aloud_pause)
                            return true
                        }
                    }
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun volumeKeyPage(direction: PageDelegate.Direction): Boolean {
        if (!read_menu.isVisible) {
            if (getPrefBoolean("volumeKeyPage", true)) {
                if (getPrefBoolean("volumeKeyPageOnPlay")
                    || readAloudStatus != Status.PLAY
                ) {
                    when (direction) {
                        PageDelegate.Direction.PREV -> page_view.moveToPrevPage()
                        PageDelegate.Direction.NEXT -> page_view.moveToNextPage()
                        else -> return true
                    }
                    return true
                }
            }
        }
        return false
    }

    /**
     * 加载章节内容
     */
    override fun loadContent() {
        ReadBookViewModel.loadContent(ReadBookViewModel.durChapterIndex)
        ReadBookViewModel.loadContent(ReadBookViewModel.durChapterIndex + 1)
        ReadBookViewModel.loadContent(ReadBookViewModel.durChapterIndex - 1)
    }

    /**
     * 加载章节内容, index章节序号
     */
    override fun loadContent(index: Int) {
        ReadBookViewModel.loadContent(index)
    }

    /**
     * 内容加载完成
     */
    override fun contentLoadFinish(bookChapter: BookChapter, content: String) {
        when (bookChapter.index) {
            ReadBookViewModel.durChapterIndex -> launch {
                ReadBookViewModel.curTextChapter = ChapterProvider
                    .getTextChapter(bookChapter, content, ReadBookViewModel.chapterSize)
                page_view.upContent()
                curChapterChanged()
                if (intent.getBooleanExtra("readAloud", false)) {
                    intent.removeExtra("readAloud")
                    readAloud()
                }
            }
            ReadBookViewModel.durChapterIndex - 1 -> launch {
                ReadBookViewModel.prevTextChapter = ChapterProvider
                    .getTextChapter(bookChapter, content, ReadBookViewModel.chapterSize)
                page_view.upContent(-1)
            }
            ReadBookViewModel.durChapterIndex + 1 -> launch {
                ReadBookViewModel.nextTextChapter = ChapterProvider
                    .getTextChapter(bookChapter, content, ReadBookViewModel.chapterSize)
                page_view.upContent(1)
            }
        }
    }

    override fun upContent() {
        page_view.upContent()
    }

    private fun curChapterChanged() {
        ReadBookViewModel.curTextChapter?.let {
            tv_chapter_name.text = it.title
            tv_chapter_name.visible()
            if (!ReadBookViewModel.isLocalBook) {
                tv_chapter_url.text = it.url
                tv_chapter_url.visible()
            }
            seek_read_page.max = it.pageSize().minus(1)
            tv_pre.isEnabled = ReadBookViewModel.durChapterIndex != 0
            tv_next.isEnabled =
                ReadBookViewModel.durChapterIndex != ReadBookViewModel.chapterSize - 1
            curPageChanged()
        }
    }

    private fun curPageChanged() {
        seek_read_page.progress = ReadBookViewModel.durPageIndex
        when (readAloudStatus) {
            Status.PLAY -> readAloud()
            Status.PAUSE -> {
                readAloud(false)
            }
        }
    }

    override fun showMenu() {
        read_menu.runMenuIn()
    }

    override fun chapterSize(): Int {
        return ReadBookViewModel.chapterSize
    }

    override val curOrigin: String?
        get() = ReadBookViewModel.book?.origin

    override val oldBook: Book?
        get() = ReadBookViewModel.book

    override fun changeTo(book: Book) {
        ReadBookViewModel.changeTo(book)
    }

    override fun durChapterIndex(): Int {
        return ReadBookViewModel.durChapterIndex
    }

    override fun durChapterPos(): Int {
        ReadBookViewModel.curTextChapter?.let {
            if (ReadBookViewModel.durPageIndex < it.pageSize()) {
                return ReadBookViewModel.durPageIndex
            }
            return it.pageSize() - 1
        }
        return ReadBookViewModel.durPageIndex
    }

    override fun setPageIndex(pageIndex: Int) {
        ReadBookViewModel.durPageIndex = pageIndex
        ReadBookViewModel.saveRead()
        curPageChanged()
    }

    /**
     * chapterOnDur: 0为当前页,1为下一页,-1为上一页
     */
    override fun textChapter(chapterOnDur: Int): TextChapter? {
        return when (chapterOnDur) {
            0 -> ReadBookViewModel.curTextChapter
            1 -> ReadBookViewModel.nextTextChapter
            -1 -> ReadBookViewModel.prevTextChapter
            else -> null
        }
    }

    /**
     * 下一页
     */
    override fun moveToNextChapter(upContent: Boolean): Boolean {
        return if (ReadBookViewModel.durChapterIndex < ReadBookViewModel.chapterSize - 1) {
            ReadBookViewModel.moveToNextChapter(upContent)
            ReadBookViewModel.saveRead()
            curChapterChanged()
            true
        } else {
            false
        }
    }

    /**
     * 上一页
     */
    override fun moveToPrevChapter(upContent: Boolean, last: Boolean): Boolean {
        return if (ReadBookViewModel.durChapterIndex > 0) {
            ReadBookViewModel.durPageIndex =
                if (last) ReadBookViewModel.prevTextChapter?.lastIndex() ?: 0 else 0
            ReadBookViewModel.moveToPrevChapter(upContent)
            ReadBookViewModel.saveRead()
            curChapterChanged()
            true
        } else {
            false
        }
    }

    override fun clickCenter() {
        if (readAloudStatus != Status.STOP) {
            showReadAloudDialog()
        } else {
            read_menu.runMenuIn()
        }
    }

    override fun showReadAloudDialog() {
        ReadAloudDialog().show(supportFragmentManager, "readAloud")
    }

    override fun autoPage() {

    }

    override fun skipToPage(page: Int) {
        ReadBookViewModel.durPageIndex = page
        page_view.upContent()
        curPageChanged()
        ReadBookViewModel.saveRead()
    }

    override fun openReplaceRule() {
        startActivity<ReplaceRuleActivity>()
    }

    override fun openChapterList() {
        ReadBookViewModel.book?.let {
            startActivityForResult<ChapterListActivity>(
                requestCodeChapterList,
                Pair("bookUrl", it.bookUrl)
            )
        }
    }

    override fun showReadStyle() {
        ReadStyleDialog().show(supportFragmentManager, "readStyle")
    }

    override fun showMoreSetting() {
        MoreConfigDialog().show(supportFragmentManager, "moreConfig")
    }

    override fun upSystemUiVisibility() {
        Help.upSystemUiVisibility(this, !read_menu.isVisible)
    }

    /**
     * 朗读按钮
     */
    override fun onClickReadAloud() {
        if (!BaseReadAloudService.isRun) {
            readAloudStatus = Status.STOP
            SystemUtils.ignoreBatteryOptimization(this)
        }
        when (readAloudStatus) {
            Status.STOP -> readAloud()
            Status.PLAY -> ReadAloud.pause(this)
            Status.PAUSE -> ReadAloud.resume(this)
        }
    }

    /**
     * 朗读
     */
    private fun readAloud(play: Boolean = true) {
        val book = ReadBookViewModel.book
        val textChapter = ReadBookViewModel.curTextChapter
        if (book != null && textChapter != null) {
            val key = IntentDataHelp.putData(textChapter)
            ReadAloud.play(
                this,
                book.name,
                textChapter.title,
                ReadBookViewModel.durPageIndex,
                key,
                play
            )
        }
    }

    override fun onColorSelected(dialogId: Int, color: Int) = with(ReadBookConfig.getConfig()) {
        when (dialogId) {
            TEXT_COLOR -> {
                setTextColor(color)
                postEvent(Bus.UP_CONFIG, false)
            }
            BG_COLOR -> {
                setBg(0, "#${color.hexString}")
                ReadBookConfig.upBg()
                postEvent(Bus.UP_CONFIG, false)
            }
        }
    }

    /**
     * colorSelectDialog
     */
    override fun onDialogDismissed(dialogId: Int) = Unit

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                requestCodeEditSource -> ReadBookViewModel.upBookSource()
                requestCodeChapterList ->
                    data?.getIntExtra("index", ReadBookViewModel.durChapterIndex)?.let {
                        ReadBookViewModel.openChapter(it)
                    }
            }
        }
    }

    override fun finish() {
        ReadBookViewModel.book?.let {
            if (!ReadBookViewModel.inBookshelf) {
                this.alert(title = getString(R.string.add_to_shelf)) {
                    message = getString(R.string.check_add_bookshelf, it.name)
                    okButton { ReadBookViewModel.inBookshelf = true }
                    noButton { ReadBookViewModel.removeFromBookshelf { super.finish() } }
                }.show().applyTint()
            } else {
                super.finish()
            }
        } ?: super.finish()
    }

    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<Int>(Bus.ALOUD_STATE) {
            readAloudStatus = it
            if (it == Status.STOP || it == Status.PAUSE) {
                ReadBookViewModel.curTextChapter?.let { textChapter ->
                    val page = textChapter.page(ReadBookViewModel.durPageIndex)
                    if (page != null && page.text is SpannableStringBuilder) {
                        page.text.removeSpan(ChapterProvider.readAloudSpan)
                        page_view.upContent()
                    }
                }
            }
        }
        observeEvent<String>(Bus.TIME_CHANGED) { page_view.upTime() }
        observeEvent<Int>(Bus.BATTERY_CHANGED) { page_view.upBattery(it) }
        observeEvent<BookChapter>(Bus.OPEN_CHAPTER) {
            ReadBookViewModel.openChapter(it.index)
            page_view.upContent()
        }
        observeEvent<Boolean>(Bus.MEDIA_BUTTON) {
            if (it) {
                onClickReadAloud()
            } else {
                readAloud(readAloudStatus == Status.PLAY)
            }
        }
        observeEvent<Boolean>(Bus.UP_CONFIG) {
            upSystemUiVisibility()
            page_view.upBg()
            page_view.upStyle()
            if (it) {
                loadContent()
            } else {
                page_view.upContent()
            }
        }
        observeEvent<Int>(Bus.TTS_START) { chapterStart ->
            launch(IO) {
                ReadBookViewModel.curTextChapter?.let {
                    val pageStart = chapterStart - it.getReadLength(ReadBookViewModel.durPageIndex)
                    it.page(ReadBookViewModel.durPageIndex)?.upPageAloudSpan(pageStart)
                    withContext(Main) {
                        page_view.upContent()
                    }
                }
            }
        }
        observeEvent<Int>(Bus.TTS_TURN_PAGE) {
            when (it) {
                1 -> {
                    if (page_view.isScrollDelegate) {
                        page_view.moveToNextPage()
                    } else {
                        ReadBookViewModel.durPageIndex = ReadBookViewModel.durPageIndex + 1
                        page_view.upContent()
                        ReadBookViewModel.saveRead()
                    }
                }
                2 -> if (!moveToNextChapter(true)) ReadAloud.stop(this)
                -1 -> {
                    if (ReadBookViewModel.durPageIndex > 0) {
                        if (page_view.isScrollDelegate) {
                            page_view.moveToPrevPage()
                        } else {
                            ReadBookViewModel.durPageIndex = ReadBookViewModel.durPageIndex - 1
                            page_view.upContent()
                            ReadBookViewModel.saveRead()
                        }
                    } else {
                        moveToPrevChapter(true)
                    }
                }
                -2 -> moveToPrevChapter(false)
            }
        }
        observeEvent<String>(Bus.REPLACE) {
            ReplaceEditDialog().show(supportFragmentManager, "replaceEditDialog")
        }
    }

}