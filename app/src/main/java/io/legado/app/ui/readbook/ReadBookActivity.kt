package io.legado.app.ui.readbook

import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Bus
import io.legado.app.constant.Status
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.receiver.TimeElectricityReceiver
import io.legado.app.service.ReadAloudService
import io.legado.app.ui.changesource.ChangeSourceDialog
import io.legado.app.ui.chapterlist.ChapterListActivity
import io.legado.app.ui.readbook.config.MoreConfigDialog
import io.legado.app.ui.readbook.config.ReadStyleDialog
import io.legado.app.ui.replacerule.ReplaceRuleActivity
import io.legado.app.ui.widget.page.ChapterProvider
import io.legado.app.ui.widget.page.PageView
import io.legado.app.ui.widget.page.TextChapter
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_read_book.*
import kotlinx.android.synthetic.main.view_book_page.*
import kotlinx.android.synthetic.main.view_read_menu.*
import kotlinx.android.synthetic.main.view_title_bar.*
import kotlinx.coroutines.launch
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.startActivity

class ReadBookActivity : VMBaseActivity<ReadBookViewModel>(R.layout.activity_read_book),
    PageView.CallBack,
    ChangeSourceDialog.CallBack,
    ReadBookViewModel.CallBack {
    override val viewModel: ReadBookViewModel
        get() = getViewModel(ReadBookViewModel::class.java)

    private var changeSourceDialog: ChangeSourceDialog? = null
    private var timeElectricityReceiver: TimeElectricityReceiver? = null
    private var readAloudStatus = Status.STOP

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setSupportActionBar(toolbar)
        initView()
        page_view.callback = this
        viewModel.callBack = this
        viewModel.bookData.observe(this, Observer { title_bar.title = it.name })
        viewModel.chapterListFinish.observe(this, Observer { bookLoadFinish() })
        viewModel.initData(intent)
        savedInstanceState?.let {
            changeSourceDialog = supportFragmentManager.findFragmentByTag(ChangeSourceDialog.tag) as? ChangeSourceDialog
            changeSourceDialog?.callBack = this
        }
    }

    override fun onResume() {
        super.onResume()
        timeElectricityReceiver = TimeElectricityReceiver.register(this)
    }

    override fun onPause() {
        super.onPause()
        timeElectricityReceiver?.let {
            unregisterReceiver(it)
            timeElectricityReceiver = null
        }
    }



    private fun initView() {
        tv_chapter_name.onClick {

        }
        tv_chapter_url.onClick {

        }
        fl_menu.setListener(object : ReadMenu.Callback {
            override fun skipToPage(page: Int) {

            }

            override fun autoPage() {

            }

            override fun setNightTheme() {
                putPrefBoolean("isNightTheme", !isNightTheme)
                App.INSTANCE.applyDayNight()
            }

            override fun skipPreChapter() {

            }

            override fun skipNextChapter() {

            }

            override fun openReplaceRule() {
                startActivity<ReplaceRuleActivity>()
            }

            override fun openChapterList() {
                viewModel.bookData.value?.let {
                    startActivity<ChapterListActivity>(Pair("bookUrl", it.bookUrl))
                }
            }

            override fun openAdjust() {

            }

            override fun showReadStyle() {
                fl_menu.runMenuOut()
                ReadStyleDialog().show(supportFragmentManager, "readStyle")
            }

            override fun showMoreSetting() {
                fl_menu.runMenuOut()
                MoreConfigDialog().show(supportFragmentManager, "moreConfig")
            }

            override fun toast(id: Int) {
                this@ReadBookActivity.toast(id)
            }

            override fun dismiss() {
                fl_menu.runMenuOut()
            }

        })
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.read_book, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_source -> {
                if (changeSourceDialog == null) {
                    viewModel.bookData.value?.let {
                        changeSourceDialog = ChangeSourceDialog.newInstance(it.name, it.author)
                        changeSourceDialog?.callBack = this
                    }
                }
                changeSourceDialog?.show(supportFragmentManager, ChangeSourceDialog.tag)
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        val keyCode = event?.keyCode
        val action = event?.action
        val isDown = action == 0

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (isDown && !fl_menu.menuBarShow) {
                fl_menu.runMenuIn()
                return true
            }
            if (!isDown && !fl_menu.menuBarShow) {
                fl_menu.menuBarShow = true
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }



    private fun bookLoadFinish() {
        viewModel.bookData.value?.let {
            viewModel.loadContent(it, viewModel.durChapterIndex)
            viewModel.loadContent(it, viewModel.durChapterIndex + 1)
            viewModel.loadContent(it, viewModel.durChapterIndex - 1)
        }
    }

    override fun loadChapter(index: Int) {
        viewModel.bookData.value?.let {
            viewModel.loadContent(it, index)
        }
    }

    override fun contentLoadFinish(bookChapter: BookChapter, content: String) {
        launch {
            when (bookChapter.index) {
                viewModel.durChapterIndex -> {
                    tv_chapter_name.text = bookChapter.title
                    tv_chapter_name.visible()
                    if (!viewModel.isLocalBook) {
                        tv_chapter_url.text = bookChapter.url
                        tv_chapter_url.visible()
                    }
                    viewModel.curTextChapter = ChapterProvider.getTextChapter(content_text_view, bookChapter, content)
                    page_view.chapterLoadFinish()

                }
                viewModel.durChapterIndex - 1 -> {
                    viewModel.prevTextChapter = ChapterProvider.getTextChapter(content_text_view, bookChapter, content)
                    page_view.chapterLoadFinish(-1)
                }
                viewModel.durChapterIndex + 1 -> {
                    viewModel.nextTextChapter = ChapterProvider.getTextChapter(content_text_view, bookChapter, content)
                    page_view.chapterLoadFinish(1)
                }
            }
        }
    }

    override fun chapterSize(): Int {
        return viewModel.chapterSize
    }

    override fun oldBook(): Book? {
        return viewModel.bookData.value
    }

    override fun changeTo(book: Book) {
        viewModel.changeTo(book)
    }

    override fun durChapterIndex(): Int {
        return viewModel.durChapterIndex
    }

    override fun durChapterPos(pageSize: Int): Int {
        if (viewModel.durPageIndex < pageSize) {
            return viewModel.durPageIndex
        }
        return pageSize - 1
    }

    override fun textChapter(chapterOnDur: Int): TextChapter? {
        return when (chapterOnDur) {
            0 -> viewModel.curTextChapter
            1 -> viewModel.nextTextChapter
            -1 -> viewModel.prevTextChapter
            else -> null
        }
    }

    private fun onClickReadAloud() {
        if (!ReadAloudService.isRun) {
            readAloudStatus = Status.STOP
            SystemUtils.ignoreBatteryOptimization(this)
        }
        when (readAloudStatus) {
            Status.STOP -> {
                viewModel.bookData.value?.let {
                    ReadAloudService.paly(this, it.name, "", "")
                }
            }
            Status.PLAY -> ReadAloudService.pause(this)
            Status.PAUSE -> ReadAloudService.resume(this)
        }
    }

    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<Int>(Bus.ALOUD_STATE) { readAloudStatus = it }
        observeEvent<Int>(Bus.TTS_START) {}
        observeEvent<Int>(Bus.TTS_RANGE_START) {}
        observeEvent<String>(Bus.TIME_CHANGED) { page_view.upTime() }
        observeEvent<Int>(Bus.BATTERY_CHANGED) { page_view.upBattery(it) }
        observeEvent<BookChapter>(Bus.OPEN_CHAPTER) { viewModel.openChapter(it) }
        observeEventSticky<Boolean>(Bus.READ_ALOUD) { onClickReadAloud() }
        observeEventSticky<Int>(Bus.UP_CONFIG) {
            when (it) {
                0 -> {
                    page_view.upBg()
                    content_view.upStyle()
                    page_view.upStyle()
                }
                1 -> {
                    content_view.upStyle()
                    page_view.upStyle()
                }
            }
        }
    }
}