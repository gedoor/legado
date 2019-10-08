package io.legado.app.ui.rss.source.edit

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.PopupWindow
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.EditEntity
import io.legado.app.data.entities.RssSource
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.rss.source.debug.RssSourceDebugActivity
import io.legado.app.ui.widget.KeyboardToolPop
import io.legado.app.utils.GSON
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_book_source_edit.*
import org.jetbrains.anko.displayMetrics
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import kotlin.math.abs

class RssSourceEditActivity :
    VMBaseActivity<RssSourceEditViewModel>(R.layout.activity_rss_source_edit, false),
    KeyboardToolPop.CallBack {

    private var mSoftKeyboardTool: PopupWindow? = null
    private var mIsSoftKeyBoardShowing = false

    private val adapter = RssSourceEditAdapter()
    private val sourceEntities: ArrayList<EditEntity> = ArrayList()

    override val viewModel: RssSourceEditViewModel
        get() = getViewModel(RssSourceEditViewModel::class.java)


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        viewModel.sourceLiveData.observe(this, Observer {
            upRecyclerView(it)
        })
        viewModel.initData(intent)
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
                getRssSource()?.let {
                    viewModel.save(it) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            }
            R.id.menu_debug_source -> {
                getRssSource()?.let {
                    viewModel.save(it) {
                        startActivity<RssSourceDebugActivity>(Pair("key", it.sourceUrl))
                    }
                }
            }
            R.id.menu_copy_source -> {
                GSON.toJson(getRssSource())?.let { sourceStr ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                    clipboard?.primaryClip = ClipData.newPlainText(null, sourceStr)
                }
            }
            R.id.menu_paste_source -> viewModel.pasteSource()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        ATH.applyEdgeEffectColor(recycler_view)
        mSoftKeyboardTool = KeyboardToolPop(this, AppConst.keyboardToolChars, this)
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(KeyboardOnGlobalChangeListener())
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter
    }

    private fun upRecyclerView(rssSource: RssSource?) {
        sourceEntities.clear()
        sourceEntities.apply {
            add(EditEntity("sourceName", rssSource?.sourceName, R.string.rss_source_name))
            add(EditEntity("sourceUrl", rssSource?.sourceUrl, R.string.rss_source_url))
            add(EditEntity("sourceIcon", rssSource?.sourceIcon, R.string.rss_source_icon))
            add(EditEntity("sourceGroup", rssSource?.sourceGroup, R.string.rss_source_group))
            add(EditEntity("ruleArticles", rssSource?.ruleArticles, R.string.rss_rule_articles))
            add(EditEntity("ruleTitle", rssSource?.ruleTitle, R.string.rss_rule_title))
            add(EditEntity("ruleAuthor", rssSource?.ruleAuthor, R.string.rss_rule_author))
            add(EditEntity("rulePubDate", rssSource?.rulePubDate, R.string.rss_rule_date))
            add(
                EditEntity(
                    "ruleCategories",
                    rssSource?.ruleCategories,
                    R.string.rss_rule_categories
                )
            )
            add(
                EditEntity(
                    "ruleDescription",
                    rssSource?.ruleDescription,
                    R.string.rss_rule_description
                )
            )
            add(EditEntity("ruleImage", rssSource?.ruleImage, R.string.rss_rule_image))
            add(EditEntity("ruleLink", rssSource?.ruleLink, R.string.rss_rule_link))
            add(EditEntity("ruleContent", rssSource?.ruleContent, R.string.rss_rule_content))
        }
        adapter.editEntities = sourceEntities
    }

    private fun getRssSource(): RssSource? {
        val source = viewModel.sourceLiveData.value ?: RssSource()
        sourceEntities.forEach {
            when (it.key) {
                "sourceName" -> source.sourceName = it.value ?: ""
                "sourceUrl" -> source.sourceUrl = it.value ?: ""
                "sourceIcon" -> source.sourceIcon = it.value ?: ""
                "sourceGroup" -> source.sourceGroup = it.value
                "ruleArticles" -> source.ruleArticles = it.value
                "ruleTitle" -> source.ruleTitle = it.value
                "ruleAuthor" -> source.ruleAuthor = it.value
                "rulePubDate" -> source.rulePubDate = it.value
                "ruleCategories" -> source.ruleCategories = it.value
                "ruleDescription" -> source.ruleDescription = it.value
                "ruleImage" -> source.ruleImage = it.value
                "ruleLink" -> source.ruleLink = it.value
                "ruleContent" -> source.ruleContent = it.value
            }
        }
        if (source.sourceName.isBlank() || source.sourceName.isBlank()) {
            toast("名称或url不能为空")
            return null
        }
        return source
    }

    override fun sendText(text: String) {
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

    private fun showKeyboardTopPopupWindow() {
        mSoftKeyboardTool?.isShowing?.let { if (it) return }
        if (!isFinishing) {
            mSoftKeyboardTool?.showAtLocation(ll_content, Gravity.BOTTOM, 0, 0)
        }
    }

    private fun closePopupWindow() {
        mSoftKeyboardTool?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
    }

    private inner class KeyboardOnGlobalChangeListener : ViewTreeObserver.OnGlobalLayoutListener {
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
    }
}