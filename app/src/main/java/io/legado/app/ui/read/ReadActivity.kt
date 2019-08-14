package io.legado.app.ui.read

import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.ui.chapterlist.ChapterListActivity
import io.legado.app.ui.replacerule.ReplaceRuleActivity
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_read.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.startActivity

class ReadActivity : VMBaseActivity<ReadViewModel>(R.layout.activity_read) {
    override val viewModel: ReadViewModel
        get() = getViewModel(ReadViewModel::class.java)

    private var menuBarShow: Boolean = false
    private lateinit var menuTopIn: Animation
    private lateinit var menuTopOut: Animation
    private lateinit var menuBottomIn: Animation
    private lateinit var menuBottomOut: Animation

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setSupportActionBar(toolbar)
        initAnimation()
        initView()
        viewModel.initData(intent)
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

            override fun onMediaButton() {

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
                viewModel.book?.let {
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
                this@ReadActivity.toast(id)
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
}