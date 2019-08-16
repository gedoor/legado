package io.legado.app.ui.readbook

import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
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
import io.legado.app.ui.replacerule.ReplaceRuleActivity
import io.legado.app.ui.widget.page.ChapterProvider
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_read_book.*
import kotlinx.android.synthetic.main.view_book_page.*
import kotlinx.android.synthetic.main.view_title_bar.*
import kotlinx.coroutines.launch
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.startActivity

class ReadBookActivity : VMBaseActivity<ReadBookViewModel>(R.layout.activity_read_book), ChangeSourceDialog.CallBack,
    ReadBookViewModel.CallBack {
    override val viewModel: ReadBookViewModel
        get() = getViewModel(ReadBookViewModel::class.java)

    private var changeSourceDialog: ChangeSourceDialog? = null
    private var timeElectricityReceiver: TimeElectricityReceiver? = null
    private var menuBarShow: Boolean = false
    private lateinit var menuTopIn: Animation
    private lateinit var menuTopOut: Animation
    private lateinit var menuBottomIn: Animation
    private lateinit var menuBottomOut: Animation
    private var readAloudStatus = Status.STOP

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setSupportActionBar(toolbar)
        initAnimation()
        initView()
        viewModel.callBack = this
        viewModel.chapterMaxIndex.observe(this, Observer { bookLoadFinish() })
        viewModel.bookData.observe(this, Observer { title_bar.title = it.name })
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

    private fun initAnimation() {
        menuTopIn = AnimationUtils.loadAnimation(this, R.anim.anim_readbook_top_in)
        menuBottomIn = AnimationUtils.loadAnimation(this, R.anim.anim_readbook_bottom_in)
        menuBottomIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {

            }

            override fun onAnimationEnd(animation: Animation) {
                vw_menu_bg.onClick { runMenuOut() }
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })

        //隐藏菜单
        menuTopOut = AnimationUtils.loadAnimation(this, R.anim.anim_readbook_top_out)
        menuBottomOut = AnimationUtils.loadAnimation(this, R.anim.anim_readbook_bottom_out)
        menuBottomOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                vw_menu_bg.setOnClickListener(null)
            }

            override fun onAnimationEnd(animation: Animation) {
                fl_menu.invisible()
                title_bar.invisible()
                bottom_menu.invisible()
                menuBarShow = false
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })
    }

    private fun initView() {
        tv_chapter_name.onClick {

        }
        tv_chapter_url.onClick {

        }
        bottom_menu.setListener(object : ReadBottomMenu.Callback {
            override fun skipToPage(page: Int) {

            }

            override fun clickReadAloud() {
                onClickReadAloud()
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

            override fun openReadInterface() {

            }

            override fun openMoreSetting() {

            }

            override fun toast(id: Int) {
                this@ReadBookActivity.toast(id)
            }

            override fun dismiss() {
                runMenuOut()
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
            if (isDown && !menuBarShow) {
                runMenuIn()
                return true
            }
            if (!isDown && !menuBarShow) {
                menuBarShow = true
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun runMenuIn() {
        fl_menu.visible()
        title_bar.visible()
        bottom_menu.visible()
        title_bar.startAnimation(menuTopIn)
        bottom_menu.startAnimation(menuBottomIn)
    }

    private fun runMenuOut() {
        if (fl_menu.isVisible) {
            if (bottom_menu.isVisible) {
                title_bar.startAnimation(menuTopOut)
                bottom_menu.startAnimation(menuBottomOut)
            }
        }
    }

    private fun bookLoadFinish() {
        viewModel.bookData.value?.let {
            viewModel.loadContent(it, it.durChapterIndex)
        }
    }

    override fun loadContentFinish(bookChapter: BookChapter, content: String) {
        launch {
            val textChapter = ChapterProvider().getTextChapter(content_text_view, bookChapter, content)
            content_text_view.text = textChapter.pages[0].text
        }
    }

    override fun changeTo(book: Book) {

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
        observeEvent<String>(Bus.TIME_CHANGED) {}
        observeEvent<Int>(Bus.BATTERY_CHANGED) {}
        observeEventSticky<String>(Bus.READ_ALOUD) { onClickReadAloud() }
    }
}