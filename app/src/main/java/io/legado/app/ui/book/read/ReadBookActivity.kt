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
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Bus
import io.legado.app.constant.Status
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.receiver.TimeElectricityReceiver
import io.legado.app.service.BaseReadAloudService
import io.legado.app.service.help.ReadAloud
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.config.*
import io.legado.app.ui.book.read.config.BgTextConfigDialog.Companion.BG_COLOR
import io.legado.app.ui.book.read.config.BgTextConfigDialog.Companion.TEXT_COLOR
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.changesource.ChangeSourceDialog
import io.legado.app.ui.chapterlist.ChapterListActivity
import io.legado.app.ui.replacerule.ReplaceRuleActivity
import io.legado.app.ui.replacerule.edit.ReplaceEditDialog
import io.legado.app.ui.widget.page.ChapterProvider
import io.legado.app.ui.widget.page.PageView
import io.legado.app.ui.widget.page.delegate.PageDelegate
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_book_read.*
import kotlinx.android.synthetic.main.view_book_page.*
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

class ReadBookActivity : VMBaseActivity<ReadBookViewModel>(R.layout.activity_book_read),
    PageView.CallBack,
    ReadMenu.CallBack,
    ReadAloudDialog.CallBack,
    ChangeSourceDialog.CallBack,
    ReadBook.CallBack,
    ColorPickerDialogListener {
    override val viewModel: ReadBookViewModel
        get() = getViewModel(ReadBookViewModel::class.java)

    private val requestCodeChapterList = 568
    private val requestCodeEditSource = 111
    private var timeElectricityReceiver: TimeElectricityReceiver? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Help.upLayoutInDisplayCutoutMode(window)
        setSupportActionBar(toolbar)
        initView()
        ReadBook.callBack = this
        ReadBook.titleDate.observe(this, Observer { title_bar.title = it })
        viewModel.initData(intent)
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
        ChapterProvider.textView = content_text_view
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
                finish()
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
                        if (BaseReadAloudService.isPlay()) {
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
                    || BaseReadAloudService.pause
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
     * 内容加载完成
     */
    override fun contentLoadFinish() {
        if (intent.getBooleanExtra("readAloud", false)) {
            intent.removeExtra("readAloud")
            ReadBook.readAloud()
        }
    }

    override fun upContent(position: Int) {
        page_view.upContent(position)
    }

    override fun upView() {
        ReadBook.curTextChapter?.let {
            tv_chapter_name.text = it.title
            tv_chapter_name.visible()
            if (!ReadBook.isLocalBook) {
                tv_chapter_url.text = it.url
                tv_chapter_url.visible()
            }
            seek_read_page.max = it.pageSize().minus(1)
            tv_pre.isEnabled = ReadBook.durChapterIndex != 0
            tv_next.isEnabled = ReadBook.durChapterIndex != ReadBook.chapterSize - 1
        }
    }

    override fun upPageProgress() {
        seek_read_page.progress = ReadBook.durPageIndex
    }

    override fun showMenu() {
        read_menu.runMenuIn()
    }

    override val oldBook: Book?
        get() = ReadBook.book

    override fun changeTo(book: Book) {
        viewModel.changeTo(book)
    }

    override fun setPageIndex(pageIndex: Int) {
        ReadBook.durPageIndex = pageIndex
        ReadBook.saveRead()
        ReadBook.curPageChanged()
    }

    override fun clickCenter() {
        if (BaseReadAloudService.isRun) {
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

    override fun openReplaceRule() {
        startActivity<ReplaceRuleActivity>()
    }

    override fun openChapterList() {
        ReadBook.book?.let {
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
            SystemUtils.ignoreBatteryOptimization(this)
        }
        when {
            !BaseReadAloudService.isRun -> ReadBook.readAloud()
            BaseReadAloudService.pause -> ReadAloud.resume(this)
            else -> ReadAloud.pause(this)
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
                requestCodeEditSource -> viewModel.upBookSource()
                requestCodeChapterList ->
                    data?.getIntExtra("index", ReadBook.durChapterIndex)?.let {
                        viewModel.openChapter(it)
                    }
            }
        }
    }

    override fun finish() {
        ReadBook.book?.let {
            if (!ReadBook.inBookshelf) {
                this.alert(title = getString(R.string.add_to_shelf)) {
                    message = getString(R.string.check_add_bookshelf, it.name)
                    okButton { ReadBook.inBookshelf = true }
                    noButton { viewModel.removeFromBookshelf { super.finish() } }
                }.show().applyTint()
            } else {
                super.finish()
            }
        } ?: super.finish()
    }

    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<Int>(Bus.ALOUD_STATE) {
            if (it == Status.STOP || it == Status.PAUSE) {
                ReadBook.curTextChapter?.let { textChapter ->
                    val page = textChapter.page(ReadBook.durPageIndex)
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
            viewModel.openChapter(it.index)
            page_view.upContent()
        }
        observeEvent<Boolean>(Bus.MEDIA_BUTTON) {
            if (it) {
                onClickReadAloud()
            } else {
                ReadBook.readAloud(!BaseReadAloudService.pause)
            }
        }
        observeEvent<Boolean>(Bus.UP_CONFIG) {
            upSystemUiVisibility()
            content_view.upStyle()
            page_view.upBg()
            page_view.upStyle()
            if (it) {
                ReadBook.loadContent()
            } else {
                page_view.upContent()
            }
        }
        observeEventSticky<Int>(Bus.TTS_START) { chapterStart ->
            launch(IO) {
                if (BaseReadAloudService.isPlay()) {
                    ReadBook.curTextChapter?.let {
                        val pageStart = chapterStart - it.getReadLength(ReadBook.durPageIndex)
                        it.page(ReadBook.durPageIndex)?.upPageAloudSpan(pageStart)
                        withContext(Main) {
                            page_view.upContent()
                        }
                    }
                }
            }
        }
        observeEvent<String>(Bus.REPLACE) {
            ReplaceEditDialog().show(supportFragmentManager, "replaceEditDialog")
        }
    }

}