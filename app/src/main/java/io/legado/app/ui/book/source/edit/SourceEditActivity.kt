package io.legado.app.ui.book.source.edit

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
import com.google.android.material.tabs.TabLayout
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.EditEntity
import io.legado.app.data.entities.rule.*
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.book.source.debug.SourceDebugActivity
import io.legado.app.ui.widget.KeyboardToolPop
import io.legado.app.utils.GSON
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_book_source_edit.*
import org.jetbrains.anko.displayMetrics
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import kotlin.math.abs

class SourceEditActivity :
    VMBaseActivity<SourceEditViewModel>(R.layout.activity_book_source_edit, false),
    KeyboardToolPop.CallBack {
    override val viewModel: SourceEditViewModel
        get() = getViewModel(SourceEditViewModel::class.java)

    private val adapter = SourceEditAdapter()
    private val sourceEntities: ArrayList<EditEntity> = ArrayList()
    private val searchEntities: ArrayList<EditEntity> = ArrayList()
    private val findEntities: ArrayList<EditEntity> = ArrayList()
    private val infoEntities: ArrayList<EditEntity> = ArrayList()
    private val tocEntities: ArrayList<EditEntity> = ArrayList()
    private val contentEntities: ArrayList<EditEntity> = ArrayList()

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
            R.id.menu_debug_source -> {
                val bookSource = getSource()
                if (bookSource == null) {
                    toast("书源名称和URL不能为空")
                } else {
                    viewModel.save(bookSource) {
                        startActivity<SourceDebugActivity>(Pair("key", bookSource.bookSourceUrl))
                    }
                }
            }
            R.id.menu_copy_source -> {
                GSON.toJson(getSource())?.let { sourceStr ->
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
            1 -> adapter.editEntities = searchEntities
            2 -> adapter.editEntities = findEntities
            3 -> adapter.editEntities = infoEntities
            4 -> adapter.editEntities = tocEntities
            5 -> adapter.editEntities = contentEntities
            else -> adapter.editEntities = sourceEntities
        }
        recycler_view.scrollToPosition(0)
    }

    private fun upRecyclerView(bookSource: BookSource?) {
        bookSource?.let {
            cb_is_enable.isChecked = it.enabled
            cb_is_enable_find.isChecked = it.enabledExplore
        }
        //基本信息
        sourceEntities.clear()
        searchEntities.apply {
            add(EditEntity("bookSourceUrl", bookSource?.bookSourceUrl, R.string.book_source_url))
            add(EditEntity("bookSourceName", bookSource?.bookSourceName, R.string.book_source_name))
            add(
                EditEntity(
                    "bookSourceGroup",
                    bookSource?.bookSourceGroup,
                    R.string.book_source_group
                )
            )
            add(EditEntity("loginUrl", bookSource?.loginUrl, R.string.book_source_login_url))
            add(EditEntity("bookUrlPattern", bookSource?.bookUrlPattern, R.string.book_url_pattern))
            add(EditEntity("header", bookSource?.header, R.string.source_http_header))
        }
        //搜索
        (bookSource?.getSearchRule()).let { searchRule ->
            searchEntities.clear()
            searchEntities.apply {
                add(EditEntity("searchUrl", bookSource?.searchUrl, R.string.rule_search_url))
                add(EditEntity("bookList", searchRule?.bookList, R.string.rule_book_list))
                add(EditEntity("name", searchRule?.name, R.string.rule_book_name))
                add(EditEntity("author", searchRule?.author, R.string.rule_book_author))
                add(EditEntity("kind", searchRule?.kind, R.string.rule_book_kind))
                add(EditEntity("wordCount", searchRule?.wordCount, R.string.rule_word_count))
                add(EditEntity("lastChapter", searchRule?.lastChapter, R.string.rule_last_chapter))
                add(EditEntity("intro", searchRule?.intro, R.string.rule_book_intro))
                add(EditEntity("coverUrl", searchRule?.coverUrl, R.string.rule_cover_url))
                add(EditEntity("bookUrl", searchRule?.bookUrl, R.string.rule_book_url))
            }
        }
        //详情页
        (bookSource?.getBookInfoRule()).let { infoRule ->
            infoEntities.clear()
            infoEntities.apply {
                add(EditEntity("init", infoRule?.init, R.string.rule_book_info_init))
                add(EditEntity("name", infoRule?.name, R.string.rule_book_name))
                add(EditEntity("author", infoRule?.author, R.string.rule_book_author))
                add(EditEntity("coverUrl", infoRule?.coverUrl, R.string.rule_cover_url))
                add(EditEntity("intro", infoRule?.intro, R.string.rule_book_intro))
                add(EditEntity("kind", infoRule?.kind, R.string.rule_book_kind))
                add(EditEntity("wordCount", infoRule?.wordCount, R.string.rule_word_count))
                add(EditEntity("lastChapter", infoRule?.lastChapter, R.string.rule_last_chapter))
                add(EditEntity("tocUrl", infoRule?.tocUrl, R.string.rule_toc_url))
            }
        }
        //目录页
        (bookSource?.getTocRule()).let { tocRule ->
            tocEntities.clear()
            tocEntities.apply {
                add(EditEntity("chapterList", tocRule?.chapterList, R.string.rule_chapter_list))
                add(EditEntity("chapterName", tocRule?.chapterName, R.string.rule_chapter_name))
                add(EditEntity("chapterUrl", tocRule?.chapterUrl, R.string.rule_chapter_url))
                add(EditEntity("nextTocUrl", tocRule?.nextTocUrl, R.string.rule_next_toc_url))
            }
        }
        //正文页
        (bookSource?.getContentRule()).let { contentRule ->
            contentEntities.clear()
            contentEntities.apply {
                add(EditEntity("content", contentRule?.content, R.string.rule_book_content))
                add(
                    EditEntity(
                        "nextContentUrl",
                        contentRule?.nextContentUrl,
                        R.string.rule_content_url_next
                    )
                )
            }
        }

        //发现
        (bookSource?.getExploreRule()).let { exploreRule ->
            findEntities.clear()
            findEntities.apply {
                add(EditEntity("exploreUrl", bookSource?.exploreUrl, R.string.rule_find_url))
                add(EditEntity("bookList", exploreRule?.bookList, R.string.rule_book_list))
                add(EditEntity("name", exploreRule?.name, R.string.rule_book_name))
                add(EditEntity("author", exploreRule?.author, R.string.rule_book_author))
                add(EditEntity("kind", exploreRule?.kind, R.string.rule_book_kind))
                add(EditEntity("wordCount", exploreRule?.wordCount, R.string.rule_word_count))
                add(EditEntity("intro", exploreRule?.intro, R.string.rule_book_intro))
                add(EditEntity("lastChapter", exploreRule?.lastChapter, R.string.rule_last_chapter))
                add(EditEntity("coverUrl", exploreRule?.coverUrl, R.string.rule_cover_url))
                add(EditEntity("bookUrl", exploreRule?.bookUrl, R.string.rule_book_url))
            }
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
        for (entity in sourceEntities) {
            with(entity) {
                when (key) {
                    "bookSourceUrl" -> value?.let { source.bookSourceUrl = it } ?: return null
                    "bookSourceName" -> value?.let { source.bookSourceName = it } ?: return null
                    "bookSourceGroup" -> source.bookSourceGroup = value
                    "loginUrl" -> source.loginUrl = value
                    "bookUrlPattern" -> source.bookUrlPattern = value
                    "header" -> source.header = value
                }
            }
        }
        for (entity in searchEntities) {
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
        for (entity in findEntities) {
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
        for (entity in infoEntities) {
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
        for (entity in tocEntities) {
            with(entity) {
                when (key) {
                    "chapterList" -> tocRule.chapterList = value
                    "chapterName" -> tocRule.chapterName = value
                    "chapterUrl" -> tocRule.chapterUrl = value
                    "nextTocUrl" -> tocRule.nextTocUrl = value
                }
            }
        }
        for (entity in contentEntities) {
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

}