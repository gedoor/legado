package io.legado.app.ui.sourceedit

import android.app.Activity
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
import com.google.android.material.tabs.TabLayout
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.*
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.sourcedebug.SourceDebugActivity
import io.legado.app.ui.widget.KeyboardToolPop
import io.legado.app.utils.GSON
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_source_edit.*
import org.jetbrains.anko.displayMetrics
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import kotlin.math.abs

class SourceEditActivity : VMBaseActivity<SourceEditViewModel>(R.layout.activity_source_edit, false),
    KeyboardToolPop.OnClickListener {
    override val viewModel: SourceEditViewModel
        get() = getViewModel(SourceEditViewModel::class.java)

    private val adapter = SourceEditAdapter()
    private val sourceEditList: ArrayList<EditEntity> = ArrayList()
    private val searchEditList: ArrayList<EditEntity> = ArrayList()
    private val findEditList: ArrayList<EditEntity> = ArrayList()
    private val infoEditList: ArrayList<EditEntity> = ArrayList()
    private val tocEditList: ArrayList<EditEntity> = ArrayList()
    private val contentEditList: ArrayList<EditEntity> = ArrayList()

    private var mSoftKeyboardTool: PopupWindow? = null
    private var mIsSoftKeyBoardShowing = false

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
                sourceID.let { viewModel.setBookSource(sourceID) }
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

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> {
                val bookSource = getSource()
                if (bookSource == null) {
                    toast("书源名称和URL不能为空")
                } else {
                    viewModel.save(bookSource) { setResult(Activity.RESULT_OK); finish() }
                }
            }
            R.id.action_debug_source -> {
                val bookSource = getSource()
                if (bookSource == null) {
                    toast("书源名称和URL不能为空")
                } else {
                    viewModel.save(bookSource) {
                        startActivity<SourceDebugActivity>("key" to bookSource.bookSourceUrl)
                    }
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        ATH.applyEdgeEffectColor(recycler_view)
        mSoftKeyboardTool = KeyboardToolPop(this, this)
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(KeyboardOnGlobalChangeListener())
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter
        tab_layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                setEditEntities(tab?.position)
            }
        })
    }

    private fun setEditEntities(tabPosition: Int?) {
        when (tabPosition) {
            1 -> adapter.editEntities = searchEditList
            2 -> adapter.editEntities = findEditList
            3 -> adapter.editEntities = infoEditList
            4 -> adapter.editEntities = tocEditList
            5 -> adapter.editEntities = contentEditList
            else -> adapter.editEntities = sourceEditList
        }
        recycler_view.scrollToPosition(0)
    }

    private fun upRecyclerView(bookSource: BookSource?) {
        bookSource?.let {
            cb_is_enable.isChecked = it.enabled
            cb_is_enable_find.isChecked = it.enabledExplore
        }
        //基本信息
        with(bookSource) {
            sourceEditList.clear()
            sourceEditList.add(EditEntity("bookSourceUrl", this?.bookSourceUrl, R.string.book_source_url))
            sourceEditList.add(EditEntity("bookSourceName", this?.bookSourceName, R.string.book_source_name))
            sourceEditList.add(EditEntity("bookSourceGroup", this?.bookSourceGroup, R.string.book_source_group))
            sourceEditList.add(EditEntity("loginUrl", this?.loginUrl, R.string.book_source_login_url))
            sourceEditList.add(EditEntity("bookUrlPattern", this?.bookUrlPattern, R.string.book_url_pattern))
            sourceEditList.add(EditEntity("header", this?.header, R.string.source_http_header))
        }
        //搜索
        with(bookSource?.getSearchRule()) {
            searchEditList.clear()
            searchEditList.add(
                EditEntity(
                    "searchUrl",
                    bookSource?.searchUrl,
                    R.string.rule_search_url
                )
            )
            searchEditList.add(EditEntity("bookList", this?.bookList, R.string.rule_book_list))
            searchEditList.add(EditEntity("name", this?.name, R.string.rule_book_name))
            searchEditList.add(EditEntity("author", this?.author, R.string.rule_book_author))
            searchEditList.add(EditEntity("kind", this?.kind, R.string.rule_book_kind))
            searchEditList.add(EditEntity("wordCount", this?.wordCount, R.string.rule_word_count))
            searchEditList.add(EditEntity("lastChapter", this?.lastChapter, R.string.rule_last_chapter))
            searchEditList.add(EditEntity("intro", this?.intro, R.string.rule_book_intro))
            searchEditList.add(EditEntity("coverUrl", this?.coverUrl, R.string.rule_cover_url))
            searchEditList.add(EditEntity("bookUrl", this?.bookUrl, R.string.rule_book_url))
        }
        //详情页
        with(bookSource?.getBookInfoRule()) {
            infoEditList.clear()
            infoEditList.add(EditEntity("init", this?.init, R.string.rule_book_info_init))
            infoEditList.add(EditEntity("name", this?.name, R.string.rule_book_name))
            infoEditList.add(EditEntity("author", this?.author, R.string.rule_book_author))
            infoEditList.add(EditEntity("coverUrl", this?.coverUrl, R.string.rule_cover_url))
            infoEditList.add(EditEntity("intro", this?.intro, R.string.rule_book_intro))
            infoEditList.add(EditEntity("kind", this?.kind, R.string.rule_book_kind))
            infoEditList.add(EditEntity("wordCount", this?.wordCount, R.string.rule_word_count))
            infoEditList.add(EditEntity("lastChapter", this?.lastChapter, R.string.rule_last_chapter))
            infoEditList.add(EditEntity("tocUrl", this?.tocUrl, R.string.rule_toc_url))
        }
        //目录页
        with(bookSource?.getTocRule()) {
            tocEditList.clear()
            tocEditList.add(EditEntity("chapterList", this?.chapterList, R.string.rule_chapter_list))
            tocEditList.add(EditEntity("chapterName", this?.chapterName, R.string.rule_chapter_name))
            tocEditList.add(EditEntity("chapterUrl", this?.chapterUrl, R.string.rule_chapter_url))
            tocEditList.add(EditEntity("nextTocUrl", this?.nextTocUrl, R.string.rule_next_toc_url))
        }
        //正文页
        with(bookSource?.getContentRule()) {
            contentEditList.clear()
            contentEditList.add(EditEntity("content", this?.content, R.string.rule_book_content))
            contentEditList.add(EditEntity("nextContentUrl", this?.nextContentUrl, R.string.rule_content_url_next))
        }

        //发现
        with(bookSource?.getExploreRule()) {
            findEditList.clear()
            findEditList.add(
                EditEntity(
                    "exploreUrl",
                    bookSource?.exploreUrl,
                    R.string.rule_find_url
                )
            )
            findEditList.add(EditEntity("bookList", this?.bookList, R.string.rule_book_list))
            findEditList.add(EditEntity("name", this?.name, R.string.rule_book_name))
            findEditList.add(EditEntity("author", this?.author, R.string.rule_book_author))
            findEditList.add(EditEntity("kind", this?.kind, R.string.rule_book_kind))
            findEditList.add(EditEntity("wordCount", this?.wordCount, R.string.rule_word_count))
            findEditList.add(EditEntity("intro", this?.intro, R.string.rule_book_intro))
            findEditList.add(EditEntity("lastChapter", this?.lastChapter, R.string.rule_last_chapter))
            findEditList.add(EditEntity("coverUrl", this?.coverUrl, R.string.rule_cover_url))
            findEditList.add(EditEntity("bookUrl", this?.bookUrl, R.string.rule_book_url))
        }
        setEditEntities(0)
    }

    private fun getSource(): BookSource? {
        val source = viewModel.sourceLiveData.value ?: BookSource()
        source.enabled = cb_is_enable.isChecked
        source.enabledExplore = cb_is_enable_find.isChecked
        viewModel.sourceLiveData.value?.let {
            source.customOrder = it.customOrder
            source.weight = it.weight
        }
        val searchRule = SearchRule()
        val exploreRule = ExploreRule()
        val bookInfoRule = BookInfoRule()
        val tocRule = TocRule()
        val contentRule = ContentRule()
        for (entity in sourceEditList) {
            with(entity) {
                when (key) {
                    "bookSourceUrl" -> if (value != null) source.bookSourceUrl = value!! else return null
                    "bookSourceName" -> if (value != null) source.bookSourceName = value!! else return null
                    "bookSourceGroup" -> source.bookSourceGroup = value
                    "loginUrl" -> source.loginUrl = value
                    "bookUrlPattern" -> source.bookUrlPattern = value
                    "header" -> source.header = value
                }
            }
        }
        for (entity in searchEditList) {
            with(entity) {
                when (key) {
                    "searchUrl" -> source.searchUrl = value
                    "bookList" -> searchRule.bookList = value
                    "name" -> searchRule.name = value
                    "author" -> searchRule.author = value
                    "kind" -> searchRule.kind = value
                    "intro" -> searchRule.intro = value
                    "updateTime" -> searchRule.updateTime = value
                    "wordCount" -> searchRule.wordCount = value
                    "lastChapter" -> searchRule.lastChapter = value
                    "coverUrl" -> searchRule.coverUrl = value
                    "bookUrl" -> searchRule.bookUrl = value
                }
            }
        }
        for (entity in findEditList) {
            with(entity) {
                when (key) {
                    "exploreUrl" -> source.exploreUrl = value
                    "bookList" -> exploreRule.bookList = value
                    "name" -> exploreRule.name = value
                    "author" -> exploreRule.author = value
                    "kind" -> exploreRule.kind = value
                    "intro" -> exploreRule.intro = value
                    "updateTime" -> exploreRule.updateTime = value
                    "wordCount" -> exploreRule.wordCount = value
                    "lastChapter" -> exploreRule.lastChapter = value
                    "coverUrl" -> exploreRule.coverUrl = value
                    "bookUrl" -> exploreRule.bookUrl = value
                }
            }
        }
        for (entity in infoEditList) {
            with(entity) {
                when (key) {
                    "init" -> bookInfoRule.init = value
                    "name" -> bookInfoRule.name = value
                    "author" -> bookInfoRule.author = value
                    "kind" -> bookInfoRule.kind = value
                    "intro" -> bookInfoRule.intro = value
                    "updateTime" -> bookInfoRule.updateTime = value
                    "wordCount" -> bookInfoRule.wordCount = value
                    "lastChapter" -> bookInfoRule.lastChapter = value
                    "coverUrl" -> bookInfoRule.coverUrl = value
                    "tocUrl" -> bookInfoRule.tocUrl = value
                }
            }
        }
        for (entity in tocEditList) {
            with(entity) {
                when (key) {
                    "chapterList" -> tocRule.chapterList = value
                    "chapterName" -> tocRule.chapterName = value
                    "chapterUrl" -> tocRule.chapterUrl = value
                    "nextTocUrl" -> tocRule.nextTocUrl = value
                }
            }
        }
        for (entity in contentEditList) {
            with(entity) {
                when (key) {
                    "content" -> contentRule.content = value
                    "nextContentUrl" -> contentRule.nextContentUrl = value
                }
            }
        }
        source.ruleSearch = GSON.toJson(searchRule)
        source.ruleExplore = GSON.toJson(exploreRule)
        source.ruleBookInfo = GSON.toJson(bookInfoRule)
        source.ruleToc = GSON.toJson(tocRule)
        source.ruleContent = GSON.toJson(contentRule)
        return source
    }

    override fun click(text: String) {
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
            val screenHeight = this@SourceEditActivity.displayMetrics.heightPixels
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

    class EditEntity(var key: String, var value: String?, var hint: Int)
}