package io.legado.app.ui.rss.source.edit

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.RssSource
import io.legado.app.help.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.qrcode.QrCodeActivity
import io.legado.app.ui.rss.source.debug.RssSourceDebugActivity
import io.legado.app.ui.widget.KeyboardToolPop
import io.legado.app.utils.GSON
import io.legado.app.utils.applyTint
import io.legado.app.utils.getViewModel
import io.legado.app.utils.shareWithQr
import kotlinx.android.synthetic.main.activity_rss_source_edit.*
import org.jetbrains.anko.*
import kotlin.math.abs

class RssSourceEditActivity :
    VMBaseActivity<RssSourceEditViewModel>(R.layout.activity_rss_source_edit, false),
    ViewTreeObserver.OnGlobalLayoutListener,
    KeyboardToolPop.CallBack {

    private var mSoftKeyboardTool: PopupWindow? = null
    private var mIsSoftKeyBoardShowing = false
    private val qrRequestCode = 101
    private val adapter = RssSourceEditAdapter()
    private val sourceEntities: ArrayList<EditEntity> = ArrayList()

    override val viewModel: RssSourceEditViewModel
        get() = getViewModel(RssSourceEditViewModel::class.java)


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        viewModel.initData(intent) {
            upRecyclerView()
        }
    }

    override fun finish() {
        val source = getRssSource()
        if (!source.equal(viewModel.rssSource ?: RssSource())) {
            alert(R.string.exit) {
                messageResource = R.string.exit_no_save
                positiveButton(R.string.yes)
                negativeButton(R.string.no) {
                    super.finish()
                }
            }.show().applyTint()
        } else {
            super.finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSoftKeyboardTool?.dismiss()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.source_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> {
                val source = getRssSource()
                if (checkSource(source)) {
                    viewModel.save(source) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            }
            R.id.menu_debug_source -> {
                val source = getRssSource()
                if (checkSource(source)) {
                    viewModel.save(source) {
                        startActivity<RssSourceDebugActivity>(Pair("key", source.sourceUrl))
                    }
                }
            }
            R.id.menu_copy_source -> {
                GSON.toJson(getRssSource())?.let { sourceStr ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText(null, sourceStr))
                }
            }
            R.id.menu_qr_code_camera -> startActivityForResult<QrCodeActivity>(qrRequestCode)
            R.id.menu_paste_source -> viewModel.pasteSource { upRecyclerView(it) }
            R.id.menu_share_str -> GSON.toJson(getRssSource())?.let { sourceStr ->
                share(sourceStr)
            }
            R.id.menu_share_qr -> GSON.toJson(getRssSource())?.let { sourceStr ->
                shareWithQr(getString(R.string.share_rss_source), sourceStr)
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        ATH.applyEdgeEffectColor(recycler_view)
        mSoftKeyboardTool = KeyboardToolPop(this, AppConst.keyboardToolChars, this)
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(this)
        recycler_view.isEnableScroll = !AppConfig.isEInkMode
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter
    }

    private fun upRecyclerView(rssSource: RssSource? = viewModel.rssSource) {
        rssSource?.let {
            cb_is_enable.isChecked = rssSource.enabled
            cb_enable_js.isChecked = rssSource.enableJs
            cb_enable_base_url.isChecked = rssSource.loadWithBaseUrl
        }
        sourceEntities.clear()
        sourceEntities.apply {
            add(EditEntity("sourceName", rssSource?.sourceName, R.string.source_name))
            add(EditEntity("sourceUrl", rssSource?.sourceUrl, R.string.source_url))
            add(EditEntity("sourceIcon", rssSource?.sourceIcon, R.string.source_icon))
            add(EditEntity("sourceGroup", rssSource?.sourceGroup, R.string.source_group))
            add(EditEntity("sortUrl", rssSource?.sortUrl, R.string.sort_url))
            add(EditEntity("ruleArticles", rssSource?.ruleArticles, R.string.r_articles))
            add(EditEntity("ruleNextPage", rssSource?.ruleNextPage, R.string.r_next))
            add(EditEntity("ruleTitle", rssSource?.ruleTitle, R.string.r_title))
            add(EditEntity("rulePubDate", rssSource?.rulePubDate, R.string.r_date))
            add(EditEntity("ruleDescription", rssSource?.ruleDescription, R.string.r_description))
            add(EditEntity("ruleImage", rssSource?.ruleImage, R.string.r_image))
            add(EditEntity("ruleLink", rssSource?.ruleLink, R.string.r_link))
            add(EditEntity("ruleContent", rssSource?.ruleContent, R.string.r_content))
            add(EditEntity("style", rssSource?.style, R.string.r_style))
            add(EditEntity("header", rssSource?.header, R.string.source_http_header))
        }
        adapter.editEntities = sourceEntities
    }

    private fun getRssSource(): RssSource {
        val source = viewModel.rssSource?.copy() ?: RssSource()
        source.enabled = cb_is_enable.isChecked
        source.enableJs = cb_enable_js.isChecked
        source.loadWithBaseUrl = cb_enable_base_url.isChecked
        sourceEntities.forEach {
            when (it.key) {
                "sourceName" -> source.sourceName = it.value ?: ""
                "sourceUrl" -> source.sourceUrl = it.value ?: ""
                "sourceIcon" -> source.sourceIcon = it.value ?: ""
                "sourceGroup" -> source.sourceGroup = it.value
                "sortUrl" -> source.sortUrl = it.value
                "ruleArticles" -> source.ruleArticles = it.value
                "ruleNextPage" -> source.ruleNextPage = it.value
                "ruleTitle" -> source.ruleTitle = it.value
                "rulePubDate" -> source.rulePubDate = it.value
                "ruleDescription" -> source.ruleDescription = it.value
                "ruleImage" -> source.ruleImage = it.value
                "ruleLink" -> source.ruleLink = it.value
                "ruleContent" -> source.ruleContent = it.value
                "style" -> source.style = it.value
                "header" -> source.header = it.value
            }
        }
        return source
    }

    private fun checkSource(source: RssSource): Boolean {
        if (source.sourceName.isBlank() || source.sourceName.isBlank()) {
            toast("名称或url不能为空")
            return false
        }
        return true
    }

    private fun insertText(text: String) {
        if (text.isBlank()) return
        val view = window.decorView.findFocus()
        if (view is EditText) {
            val start = view.selectionStart
            val end = view.selectionEnd
            val edit = view.editableText//获取EditText的文字
            if (start < 0 || start >= edit.length) {
                edit.append(text)
            } else {
                edit.replace(start, end, text)//光标所在位置插入文字
            }
        }
    }

    override fun sendText(text: String) {
        if (text == AppConst.keyboardToolChars[0]) {
            insertText(AppConst.urlOption)
        } else {
            insertText(text)
        }
    }

    private fun showKeyboardTopPopupWindow() {
        mSoftKeyboardTool?.let {
            if (it.isShowing) return
            if (!isFinishing) {
                it.showAtLocation(ll_content, Gravity.BOTTOM, 0, 0)
            }
        }
    }

    private fun closePopupWindow() {
        mSoftKeyboardTool?.dismiss()
    }

    override fun onGlobalLayout() {
        val rect = Rect()
        // 获取当前页面窗口的显示范围
        window.decorView.getWindowVisibleDisplayFrame(rect)
        val screenHeight = this@RssSourceEditActivity.displayMetrics.heightPixels
        val keyboardHeight = screenHeight - rect.bottom // 输入法的高度
        val preShowing = mIsSoftKeyBoardShowing
        if (abs(keyboardHeight) > screenHeight / 5) {
            mIsSoftKeyBoardShowing = true // 超过屏幕五分之一则表示弹出了输入法
            recycler_view.setPadding(0, 0, 0, 100)
            showKeyboardTopPopupWindow()
        } else {
            mIsSoftKeyBoardShowing = false
            recycler_view.setPadding(0, 0, 0, 0)
            if (preShowing) {
                closePopupWindow()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            qrRequestCode -> if (resultCode == RESULT_OK) {
                data?.getStringExtra("result")?.let {
                    viewModel.importSource(it) { source: RssSource ->
                        upRecyclerView(source)
                    }
                }
            }
        }
    }
}