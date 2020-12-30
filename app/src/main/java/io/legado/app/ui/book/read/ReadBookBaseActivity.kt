package io.legado.app.ui.book.read

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Bookmark
import io.legado.app.databinding.ActivityBookReadBinding
import io.legado.app.databinding.DialogBookmarkBinding
import io.legado.app.databinding.DialogDownloadChoiceBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.AppConfig
import io.legado.app.help.LocalConfig
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.service.help.CacheBook
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.config.BgTextConfigDialog
import io.legado.app.ui.book.read.config.ClickActionConfigDialog
import io.legado.app.ui.book.read.config.PaddingConfigDialog
import io.legado.app.utils.getPrefString
import io.legado.app.utils.getViewModel
import io.legado.app.utils.requestInputMethod

/**
 * 阅读界面
 */
abstract class ReadBookBaseActivity :
    VMBaseActivity<ActivityBookReadBinding, ReadBookViewModel>() {

    override val viewModel: ReadBookViewModel
        get() = getViewModel(ReadBookViewModel::class.java)
    var bottomDialog = 0

    override fun getViewBinding(): ActivityBookReadBinding {
        return ActivityBookReadBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ReadBook.msg = null
        setOrientation()
        upLayoutInDisplayCutoutMode()
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (!LocalConfig.readHelpVersionIsLast) {
            showClickRegionalConfig()
        }
    }

    fun showPaddingConfig() {
        PaddingConfigDialog().show(supportFragmentManager, "paddingConfig")
    }

    fun showBgTextConfig() {
        BgTextConfigDialog().show(supportFragmentManager, "bgTextConfig")
    }

    fun showClickRegionalConfig() {
        ClickActionConfigDialog().show(supportFragmentManager, "clickActionConfig")
    }

    /**
     * 屏幕方向
     */
    @SuppressLint("SourceLockedOrientationActivity")
    fun setOrientation() {
        when (AppConfig.screenOrientation) {
            "0" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            "1" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "2" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "3" -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }

    /**
     * 更新状态栏,导航栏
     */
    fun upSystemUiVisibility(
        isInMultiWindow: Boolean,
        toolBarHide: Boolean = true
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                if (toolBarHide) {
                    if (ReadBookConfig.hideStatusBar) {
                        it.hide(WindowInsets.Type.statusBars())
                    }
                    if (ReadBookConfig.hideNavigationBar) {
                        it.hide(WindowInsets.Type.navigationBars())
                    }
                } else {
                    it.show(WindowInsets.Type.statusBars())
                    it.show(WindowInsets.Type.navigationBars())
                }
            }
        }
        upSystemUiVisibilityO(isInMultiWindow, toolBarHide)
        if (toolBarHide) {
            ATH.setLightStatusBar(this, ReadBookConfig.durConfig.curStatusIconDark())
        } else {
            ATH.setLightStatusBarAuto(
                this,
                ThemeStore.statusBarColor(this, AppConfig.isTransparentStatusBar)
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun upSystemUiVisibilityO(
        isInMultiWindow: Boolean,
        toolBarHide: Boolean = true
    ) {
        var flag = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        if (!isInMultiWindow) {
            flag = flag or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        flag = flag or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        if (toolBarHide) {
            if (ReadBookConfig.hideStatusBar) {
                flag = flag or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
            if (ReadBookConfig.hideNavigationBar) {
                flag = flag or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }
        }
        window.decorView.systemUiVisibility = flag
    }

    override fun upNavigationBarColor() {
        when {
            binding.readMenu.isVisible -> super.upNavigationBarColor()
            bottomDialog > 0 -> super.upNavigationBarColor()
            else -> if (AppConfig.immNavigationBar) {
                ATH.setNavigationBarColorAuto(this, Color.TRANSPARENT)
            } else {
                ATH.setNavigationBarColorAuto(this, Color.parseColor("#20000000"))
            }
        }
    }

    /**
     * 保持亮屏
     */
    fun keepScreenOn(window: Window, on: Boolean) {
        if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * 适配刘海
     */
    private fun upLayoutInDisplayCutoutMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ReadBookConfig.readBodyToLh) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    @SuppressLint("InflateParams")
    fun showDownloadDialog() {
        ReadBook.book?.let { book ->
            alert(titleResource = R.string.offline_cache) {
                val alertBinding = DialogDownloadChoiceBinding.inflate(layoutInflater).apply {
                    root.setBackgroundColor(root.context.backgroundColor)
                    editStart.setText((book.durChapterIndex + 1).toString())
                    editEnd.setText(book.totalChapterNum.toString())
                }
                customView = alertBinding.root
                yesButton {
                    alertBinding.run {
                        val start = editStart.text?.toString()?.toInt() ?: 0
                        val end = editEnd.text?.toString()?.toInt() ?: book.totalChapterNum
                        CacheBook.start(this@ReadBookBaseActivity, book.bookUrl, start - 1, end - 1)
                    }
                }
                noButton()
            }.show()
        }
    }

    @SuppressLint("InflateParams")
    fun showBookMark(bookmark: Bookmark) {
        alert(title = getString(R.string.bookmark_add)) {
            message = bookmark.chapterName
            val alertBinding = DialogBookmarkBinding.inflate(layoutInflater).apply {
                editBookText.setText(bookmark.bookText)
                editView.setText(bookmark.content)
            }
            customView = alertBinding.root
            yesButton {
                alertBinding.apply {
                    Coroutine.async {
                        bookmark.bookText = editBookText.text.toString()
                        bookmark.content = editView.text.toString()
                        App.db.bookmarkDao.insert(bookmark)
                    }
                }
            }
            noButton()
        }.show().requestInputMethod()
    }

    @SuppressLint("InflateParams")
    fun showCharsetConfig() {
        val charsets =
            arrayListOf("UTF-8", "GB2312", "GBK", "Unicode", "UTF-16", "UTF-16LE", "ASCII")
        alert(R.string.set_charset) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.setFilterValues(charsets)
                editView.setText(ReadBook.book?.charset)
            }
            customView = alertBinding.root
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    ReadBook.setCharset(it)
                }
            }
            cancelButton()
        }.show()
    }

    fun showPageAnimConfig(success: () -> Unit) {
        val items = arrayListOf<String>()
        items.add(getString(R.string.btn_default_s))
        items.add(getString(R.string.page_anim_cover))
        items.add(getString(R.string.page_anim_slide))
        items.add(getString(R.string.page_anim_simulation))
        items.add(getString(R.string.page_anim_scroll))
        items.add(getString(R.string.page_anim_none))
        selector(R.string.page_anim, items) { _, i ->
            ReadBook.book?.setPageAnim(i - 1)
            success()
        }
    }

    fun isPrevKey(keyCode: Int): Boolean {
        val prevKeysStr = getPrefString(PreferKey.prevKeys)
        return prevKeysStr?.split(",")?.contains(keyCode.toString()) ?: false
    }

    fun isNextKey(keyCode: Int): Boolean {
        val nextKeysStr = getPrefString(PreferKey.nextKeys)
        return nextKeysStr?.split(",")?.contains(keyCode.toString()) ?: false
    }
}