package io.legado.app.ui.book.source.edit

import android.app.Activity
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.PopupWindow
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.*
import io.legado.app.databinding.ActivityBookSourceEditBinding
import io.legado.app.help.LocalConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.ui.book.source.debug.BookSourceDebugActivity
import io.legado.app.ui.document.FilePicker
import io.legado.app.ui.document.FilePickerParam
import io.legado.app.ui.login.SourceLogin
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.widget.KeyboardToolPop
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.*
import kotlin.math.abs

class BookSourceEditActivity :
    VMBaseActivity<ActivityBookSourceEditBinding, BookSourceEditViewModel>(false),
    KeyboardToolPop.CallBack {
    override val viewModel: BookSourceEditViewModel
            by viewModels()

    private val adapter = BookSourceEditAdapter()
    private val sourceEntities: ArrayList<EditEntity> = ArrayList()
    private val searchEntities: ArrayList<EditEntity> = ArrayList()
    private val findEntities: ArrayList<EditEntity> = ArrayList()
    private val infoEntities: ArrayList<EditEntity> = ArrayList()
    private val tocEntities: ArrayList<EditEntity> = ArrayList()
    private val contentEntities: ArrayList<EditEntity> = ArrayList()
    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        viewModel.importSource(it) { source ->
            upRecyclerView(source)
        }
    }
    private val selectDoc = registerForActivityResult(FilePicker()) { uri ->
        uri ?: return@registerForActivityResult
        if (uri.isContentScheme()) {
            sendText(uri.toString())
        } else {
            sendText(uri.path.toString())
        }
    }

    private var mSoftKeyboardTool: PopupWindow? = null
    private var mIsSoftKeyBoardShowing = false

    override fun getViewBinding(): ActivityBookSourceEditBinding {
        return ActivityBookSourceEditBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        viewModel.initData(intent) {
            upRecyclerView()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (!LocalConfig.ruleHelpVersionIsLast) {
            showRuleHelp()
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.source_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> getSource().let { source ->
                if (!source.equal(viewModel.bookSource ?: BookSource())) {
                    source.lastUpdateTime = System.currentTimeMillis()
                }
                if (checkSource(source)) {
                    viewModel.save(source) { setResult(Activity.RESULT_OK); finish() }
                }
            }
            R.id.menu_debug_source -> getSource().let { source ->
                if (checkSource(source)) {
                    viewModel.save(source) {
                        startActivity<BookSourceDebugActivity> {
                            putExtra("key", source.bookSourceUrl)
                        }
                    }
                }
            }
            R.id.menu_copy_source -> sendToClip(GSON.toJson(getSource()))
            R.id.menu_paste_source -> viewModel.pasteSource { upRecyclerView(it) }
            R.id.menu_qr_code_camera -> qrCodeResult.launch(null)
            R.id.menu_share_str -> share(GSON.toJson(getSource()))
            R.id.menu_share_qr -> shareWithQr(
                GSON.toJson(getSource()),
                getString(R.string.share_book_source)
            )
            R.id.menu_help -> showRuleHelp()
            R.id.menu_login -> getSource().let {
                if (checkSource(it)) {
                    if (it.loginUrl.isNullOrEmpty()) {
                        toastOnUi(R.string.source_no_login)
                    } else {
                        startActivity<SourceLogin> {
                            putExtra("sourceUrl", it.bookSourceUrl)
                            putExtra("loginUrl", it.loginUrl)
                            putExtra("userAgent", it.getHeaderMap()[AppConst.UA_NAME])
                        }
                    }
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        ATH.applyEdgeEffectColor(binding.recyclerView)
        mSoftKeyboardTool = KeyboardToolPop(this, AppConst.keyboardToolChars, this)
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(KeyboardOnGlobalChangeListener())
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.tabLayout.setBackgroundColor(backgroundColor)
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                setEditEntities(tab?.position)
            }
        })
    }

    override fun finish() {
        val source = getSource()
        if (!source.equal(viewModel.bookSource ?: BookSource())) {
            alert(R.string.exit) {
                setMessage(R.string.exit_no_save)
                positiveButton(R.string.yes)
                negativeButton(R.string.no) {
                    super.finish()
                }
            }.show()
        } else {
            super.finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSoftKeyboardTool?.dismiss()
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
        binding.recyclerView.scrollToPosition(0)
    }

    private fun upRecyclerView(source: BookSource? = viewModel.bookSource) {
        source?.let {
            binding.cbIsEnable.isChecked = it.enabled
            binding.cbIsEnableFind.isChecked = it.enabledExplore
            binding.spType.setSelection(it.bookSourceType)
        }
        //基本信息
        sourceEntities.clear()
        sourceEntities.apply {
            add(EditEntity("bookSourceUrl", source?.bookSourceUrl, R.string.source_url))
            add(EditEntity("bookSourceName", source?.bookSourceName, R.string.source_name))
            add(EditEntity("bookSourceGroup", source?.bookSourceGroup, R.string.source_group))
            add(EditEntity("bookSourceComment", source?.bookSourceComment, R.string.comment))
            add(EditEntity("loginUrl", source?.loginUrl, R.string.login_url))
            add(EditEntity("bookUrlPattern", source?.bookUrlPattern, R.string.book_url_pattern))
            add(EditEntity("header", source?.header, R.string.source_http_header))

        }
        //搜索
        val sr = source?.getSearchRule()
        searchEntities.clear()
        searchEntities.apply {
            add(EditEntity("searchUrl", source?.searchUrl, R.string.r_search_url))
            add(EditEntity("bookList", sr?.bookList, R.string.r_book_list))
            add(EditEntity("name", sr?.name, R.string.r_book_name))
            add(EditEntity("author", sr?.author, R.string.r_author))
            add(EditEntity("kind", sr?.kind, R.string.rule_book_kind))
            add(EditEntity("wordCount", sr?.wordCount, R.string.rule_word_count))
            add(EditEntity("lastChapter", sr?.lastChapter, R.string.rule_last_chapter))
            add(EditEntity("intro", sr?.intro, R.string.rule_book_intro))
            add(EditEntity("coverUrl", sr?.coverUrl, R.string.rule_cover_url))
            add(EditEntity("bookUrl", sr?.bookUrl, R.string.r_book_url))
        }
        //详情页
        val ir = source?.getBookInfoRule()
        infoEntities.clear()
        infoEntities.apply {
            add(EditEntity("init", ir?.init, R.string.rule_book_info_init))
            add(EditEntity("name", ir?.name, R.string.r_book_name))
            add(EditEntity("author", ir?.author, R.string.r_author))
            add(EditEntity("kind", ir?.kind, R.string.rule_book_kind))
            add(EditEntity("wordCount", ir?.wordCount, R.string.rule_word_count))
            add(EditEntity("lastChapter", ir?.lastChapter, R.string.rule_last_chapter))
            add(EditEntity("intro", ir?.intro, R.string.rule_book_intro))
            add(EditEntity("coverUrl", ir?.coverUrl, R.string.rule_cover_url))
            add(EditEntity("tocUrl", ir?.tocUrl, R.string.rule_toc_url))
            add(EditEntity("canReName", ir?.canReName, R.string.rule_can_re_name))
        }
        //目录页
        val tr = source?.getTocRule()
        tocEntities.clear()
        tocEntities.apply {
            add(EditEntity("chapterList", tr?.chapterList, R.string.rule_chapter_list))
            add(EditEntity("chapterName", tr?.chapterName, R.string.rule_chapter_name))
            add(EditEntity("chapterUrl", tr?.chapterUrl, R.string.rule_chapter_url))
            add(EditEntity("isVip", tr?.isVip, R.string.rule_is_vip))
            add(EditEntity("updateTime", tr?.updateTime, R.string.rule_update_time))
            add(EditEntity("nextTocUrl", tr?.nextTocUrl, R.string.rule_next_toc_url))
        }
        //正文页
        val cr = source?.getContentRule()
        contentEntities.clear()
        contentEntities.apply {
            add(EditEntity("content", cr?.content, R.string.rule_book_content))
            add(EditEntity("nextContentUrl", cr?.nextContentUrl, R.string.rule_next_content))
            add(EditEntity("webJs", cr?.webJs, R.string.rule_web_js))
            add(EditEntity("sourceRegex", cr?.sourceRegex, R.string.rule_source_regex))
            add(EditEntity("replaceRegex", cr?.replaceRegex, R.string.rule_replace_regex))
            add(EditEntity("imageStyle", cr?.imageStyle, R.string.rule_image_style))
        }
        //发现
        val er = source?.getExploreRule()
        findEntities.clear()
        findEntities.apply {
            add(EditEntity("exploreUrl", source?.exploreUrl, R.string.r_find_url))
            add(EditEntity("bookList", er?.bookList, R.string.r_book_list))
            add(EditEntity("name", er?.name, R.string.r_book_name))
            add(EditEntity("author", er?.author, R.string.r_author))
            add(EditEntity("kind", er?.kind, R.string.rule_book_kind))
            add(EditEntity("wordCount", er?.wordCount, R.string.rule_word_count))
            add(EditEntity("lastChapter", er?.lastChapter, R.string.rule_last_chapter))
            add(EditEntity("intro", er?.intro, R.string.rule_book_intro))
            add(EditEntity("coverUrl", er?.coverUrl, R.string.rule_cover_url))
            add(EditEntity("bookUrl", er?.bookUrl, R.string.r_book_url))
        }
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
        setEditEntities(0)
    }

    private fun getSource(): BookSource {
        val source = viewModel.bookSource?.copy() ?: BookSource()
        source.enabled = binding.cbIsEnable.isChecked
        source.enabledExplore = binding.cbIsEnableFind.isChecked
        source.bookSourceType = binding.spType.selectedItemPosition
        val searchRule = SearchRule()
        val exploreRule = ExploreRule()
        val bookInfoRule = BookInfoRule()
        val tocRule = TocRule()
        val contentRule = ContentRule()
        sourceEntities.forEach {
            when (it.key) {
                "bookSourceUrl" -> source.bookSourceUrl = it.value ?: ""
                "bookSourceName" -> source.bookSourceName = it.value ?: ""
                "bookSourceGroup" -> source.bookSourceGroup = it.value
                "loginUrl" -> source.loginUrl = it.value
                "bookUrlPattern" -> source.bookUrlPattern = it.value
                "header" -> source.header = it.value
                "bookSourceComment" -> source.bookSourceComment = it.value ?: ""
            }
        }
        searchEntities.forEach {
            when (it.key) {
                "searchUrl" -> source.searchUrl = it.value
                "bookList" -> searchRule.bookList = it.value
                "name" -> searchRule.name = it.value
                "author" -> searchRule.author = it.value
                "kind" -> searchRule.kind = it.value
                "intro" -> searchRule.intro = it.value
                "updateTime" -> searchRule.updateTime = it.value
                "wordCount" -> searchRule.wordCount = it.value
                "lastChapter" -> searchRule.lastChapter = it.value
                "coverUrl" -> searchRule.coverUrl = it.value
                "bookUrl" -> searchRule.bookUrl = it.value
            }
        }
        findEntities.forEach {
            when (it.key) {
                "exploreUrl" -> source.exploreUrl = it.value
                "bookList" -> exploreRule.bookList = it.value
                "name" -> exploreRule.name = it.value
                "author" -> exploreRule.author = it.value
                "kind" -> exploreRule.kind = it.value
                "intro" -> exploreRule.intro = it.value
                "updateTime" -> exploreRule.updateTime = it.value
                "wordCount" -> exploreRule.wordCount = it.value
                "lastChapter" -> exploreRule.lastChapter = it.value
                "coverUrl" -> exploreRule.coverUrl = it.value
                "bookUrl" -> exploreRule.bookUrl = it.value
            }
        }
        infoEntities.forEach {
            when (it.key) {
                "init" -> bookInfoRule.init = it.value
                "name" -> bookInfoRule.name = it.value
                "author" -> bookInfoRule.author = it.value
                "kind" -> bookInfoRule.kind = it.value
                "intro" -> bookInfoRule.intro = it.value
                "updateTime" -> bookInfoRule.updateTime = it.value
                "wordCount" -> bookInfoRule.wordCount = it.value
                "lastChapter" -> bookInfoRule.lastChapter = it.value
                "coverUrl" -> bookInfoRule.coverUrl = it.value
                "tocUrl" -> bookInfoRule.tocUrl = it.value
                "canReName" -> bookInfoRule.canReName = it.value
            }
        }
        tocEntities.forEach {
            when (it.key) {
                "chapterList" -> tocRule.chapterList = it.value
                "chapterName" -> tocRule.chapterName = it.value
                "chapterUrl" -> tocRule.chapterUrl = it.value
                "nextTocUrl" -> tocRule.nextTocUrl = it.value
                "isVip" -> tocRule.isVip = it.value
                "updateTime" -> tocRule.updateTime = it.value
            }
        }
        contentEntities.forEach {
            when (it.key) {
                "content" -> contentRule.content = it.value
                "nextContentUrl" -> contentRule.nextContentUrl = it.value
                "webJs" -> contentRule.webJs = it.value
                "sourceRegex" -> contentRule.sourceRegex = it.value
                "replaceRegex" -> contentRule.replaceRegex = it.value
                "imageStyle" -> contentRule.imageStyle = it.value
            }
        }
        source.ruleSearch = searchRule
        source.ruleExplore = exploreRule
        source.ruleBookInfo = bookInfoRule
        source.ruleToc = tocRule
        source.ruleContent = contentRule
        return source
    }

    private fun checkSource(source: BookSource): Boolean {
        if (source.bookSourceUrl.isBlank() || source.bookSourceName.isBlank()) {
            toastOnUi(R.string.non_null_name_url)
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
            showHelpDialog()
        } else {
            insertText(text)
        }
    }

    private fun showHelpDialog() {
        val items = arrayListOf("插入URL参数", "书源教程", "正则教程", "选择文件")
        selector(getString(R.string.help), items) { _, index ->
            when (index) {
                0 -> insertText(AppConst.urlOption)
                1 -> showRuleHelp()
                2 -> showRegexHelp()
                3 -> selectDoc.launch(
                    FilePickerParam(
                        mode = FilePicker.FILE
                    )
                )
            }
        }
    }

    private fun showRuleHelp() {
        val mdText = String(assets.open("help/ruleHelp.md").readBytes())
        TextDialog.show(supportFragmentManager, mdText, TextDialog.MD)
    }

    private fun showRegexHelp() {
        val mdText = String(assets.open("help/regexHelp.md").readBytes())
        TextDialog.show(supportFragmentManager, mdText, TextDialog.MD)
    }

    private fun showKeyboardTopPopupWindow() {
        mSoftKeyboardTool?.let {
            if (it.isShowing) return
            if (!isFinishing) {
                it.showAtLocation(binding.root, Gravity.BOTTOM, 0, 0)
            }
        }
    }

    private fun closePopupWindow() {
        mSoftKeyboardTool?.dismiss()
    }

    private inner class KeyboardOnGlobalChangeListener : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            val rect = Rect()
            // 获取当前页面窗口的显示范围
            window.decorView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = this@BookSourceEditActivity.getSize().heightPixels
            val keyboardHeight = screenHeight - rect.bottom // 输入法的高度
            val preShowing = mIsSoftKeyBoardShowing
            if (abs(keyboardHeight) > screenHeight / 5) {
                mIsSoftKeyBoardShowing = true // 超过屏幕五分之一则表示弹出了输入法
                binding.recyclerView.setPadding(0, 0, 0, 100)
                showKeyboardTopPopupWindow()
            } else {
                mIsSoftKeyBoardShowing = false
                binding.recyclerView.setPadding(0, 0, 0, 0)
                if (preShowing) {
                    closePopupWindow()
                }
            }
        }
    }

}