package io.legado.app.ui.read

import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.utils.getViewModel
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.activity_read.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.sdk27.listeners.onClick

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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {

        }
        return super.onKeyDown(keyCode, event)
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