package io.legado.app.ui.book.read

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.core.view.isVisible
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Bookmark
import io.legado.app.help.AppConfig
import io.legado.app.help.LocalConfig
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.*
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.service.help.CacheBook
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.config.BgTextConfigDialog
import io.legado.app.ui.book.read.config.ClickActionConfigDialog
import io.legado.app.ui.book.read.config.PaddingConfigDialog
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.widget.text.AutoCompleteTextView
import io.legado.app.utils.applyTint
import io.legado.app.utils.getPrefString
import io.legado.app.utils.getViewModel
import io.legado.app.utils.requestInputMethod
import kotlinx.android.synthetic.main.activity_book_read.*
import kotlinx.android.synthetic.main.dialog_download_choice.view.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.view_read_menu.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.startActivityForResult

abstract class ReadBookBaseActivity :
    VMBaseActivity<ReadBookViewModel>(R.layout.activity_book_read) {

    override val viewModel: ReadBookViewModel
        get() = getViewModel(ReadBookViewModel::class.java)
    private val requestCodeEditSource = 111
    var bottomDialog = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        ReadBook.msg = null
        setOrientation()
        upLayoutInDisplayCutoutMode()
        super.onCreate(savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        if (LocalConfig.isFirstRead) {
            showClickRegionalConfig()
        }
    }

    /**
     * 初始化View
     */
    private fun initView() {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                requestCodeEditSource -> viewModel.upBookSource()
            }
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
        when (AppConfig.requestedDirection) {
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
        if (ReadBookConfig.hideNavigationBar) {
            flag = flag or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
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
            read_menu == null -> return
            read_menu.isVisible -> ATH.setNavigationBarColorAuto(this)
            bottomDialog > 0 -> ATH.setNavigationBarColorAuto(this, bottomBackground)
            ReadBookConfig.bg is ColorDrawable -> {
                ATH.setNavigationBarColorAuto(this, ReadBookConfig.bgMeanColor)
            }
            else -> {
                ATH.setNavigationBarColorAuto(this, Color.BLACK)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && AppConfig.readBodyToLh) {
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
                var view: View? = null
                customView {
                    LayoutInflater.from(this@ReadBookBaseActivity)
                        .inflate(R.layout.dialog_download_choice, null)
                        .apply {
                            view = this
                            setBackgroundColor(context.backgroundColor)
                            edit_start.setText((book.durChapterIndex + 1).toString())
                            edit_end.setText(book.totalChapterNum.toString())
                        }
                }
                yesButton {
                    view?.apply {
                        val start = edit_start?.text?.toString()?.toInt() ?: 0
                        val end = edit_end?.text?.toString()?.toInt() ?: book.totalChapterNum
                        CacheBook.start(context, book.bookUrl, start - 1, end - 1)
                    }
                }
                noButton()
            }.show().applyTint()
        }
    }

    @SuppressLint("InflateParams")
    fun showBookMark() {
        val book = ReadBook.book ?: return
        val textChapter = ReadBook.curTextChapter ?: return
        alert(title = getString(R.string.bookmark_add)) {
            var editText: EditText? = null
            message = book.name + " • " + textChapter.title
            customView {
                layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                    editText = edit_view.apply {
                        setHint(R.string.note_content)
                    }
                }
            }
            yesButton {
                editText?.text?.toString()?.let { editContent ->
                    Coroutine.async {
                        val bookmark = Bookmark(
                            bookUrl = book.bookUrl,
                            bookName = book.name,
                            chapterIndex = ReadBook.durChapterIndex,
                            pageIndex = ReadBook.durPageIndex,
                            chapterName = textChapter.title,
                            content = editContent
                        )
                        App.db.bookmarkDao().insert(bookmark)
                    }
                }
            }
            noButton()
        }.show().applyTint().requestInputMethod()
    }

    @SuppressLint("InflateParams")
    fun showCharsetConfig() {
        val charsets =
            arrayListOf("UTF-8", "GB2312", "GBK", "Unicode", "UTF-16", "UTF-16LE", "ASCII")
        alert(R.string.set_charset) {
            var editText: AutoCompleteTextView? = null
            customView {
                layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                    editText = edit_view
                    edit_view.setFilterValues(charsets)
                    edit_view.setText(ReadBook.book?.charset)
                }
            }
            okButton {
                val text = editText?.text?.toString()
                text?.let {
                    ReadBook.setCharset(it)
                }
            }
            cancelButton()
        }.show().applyTint()
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