package io.legado.app.ui.rss.source.edit

import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
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
import io.legado.app.ui.widget.KeyboardToolPop
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_book_source_edit.*
import org.jetbrains.anko.displayMetrics
import kotlin.math.abs

class RssSourceEditActivity :
    VMBaseActivity<RssSourceEditViewModel>(R.layout.activity_rss_source_edit, false),
    KeyboardToolPop.CallBack {

    private var mSoftKeyboardTool: PopupWindow? = null
    private var mIsSoftKeyBoardShowing = false

    private lateinit var adapter: RssSourceEditAdapter
    private val sourceEntities: ArrayList<EditEntity> = ArrayList()

    override val viewModel: RssSourceEditViewModel
        get() = getViewModel(RssSourceEditViewModel::class.java)


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        viewModel.sourceLiveData.observe(this, Observer {
            upRecyclerView(it)
        })
        if (viewModel.sourceLiveData.value == null) {
            val sourceID = intent.getStringExtra("data")
            if (sourceID == null) {
                upRecyclerView(null)
            } else {
                sourceID.let { viewModel.setSource(sourceID) }
            }
        } else {
            upRecyclerView(viewModel.sourceLiveData.value)
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
            add(EditEntity("iconUrl", rssSource?.iconUrl, R.string.rss_source_icon))
            add(EditEntity("ruleTitle", rssSource?.ruleTitle, R.string.rss_rule_title))
            add(EditEntity("ruleAuthor", rssSource?.ruleAuthor, R.string.rss_rule_author))
            add(EditEntity("ruleGuid", rssSource?.ruleGuid, R.string.rss_rule_guid))
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
            add(EditEntity("ruleContent", rssSource?.ruleContent, R.string.rss_rule_content))
            add(EditEntity("ruleLink", rssSource?.ruleLink, R.string.rss_rule_link))
        }

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