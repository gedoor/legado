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
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.constant.BookType
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.*
import io.legado.app.databinding.ActivityBookSourceEditBinding
import io.legado.app.help.config.LocalConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.book.source.debug.BookSourceDebugActivity
import io.legado.app.ui.document.HandleFileContract
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.widget.KeyboardToolPop
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlin.math.abs

class BookSourceEditActivity :
    VMBaseActivity<ActivityBookSourceEditBinding, BookSourceEditViewModel>(false),
    KeyboardToolPop.CallBack {

    override val binding by viewBinding(ActivityBookSourceEditBinding::inflate)
    override val viewModel by viewModels<BookSourceEditViewModel>()

    private val adapter by lazy { BookSourceEditAdapter() }
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
    private val selectDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            if (uri.isContentScheme()) {
                sendText(uri.toString())
            } else {
                sendText(uri.path.toString())
            }
        }
    }

    private var mSoftKeyboardTool: PopupWindow? = null
    private var mIsSoftKeyBoardShowing = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        viewModel.initData(intent) {
            upRecyclerView()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (!LocalConfig.ruleHelpVersionIsLast) {
            showHelp("ruleHelp")
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.source_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_login)?.isVisible = !viewModel.bookSource?.loginUrl.isNullOrBlank()
        menu.findItem(R.id.menu_auto_complete)?.isChecked = viewModel.autoComplete
        return super.onMenuOpened(featureId, menu)
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
            R.id.menu_auto_complete -> viewModel.autoComplete = !viewModel.autoComplete
            R.id.menu_copy_source -> sendToClip(GSON.toJson(getSource()))
            R.id.menu_paste_source -> viewModel.pasteSource { upRecyclerView(it) }
            R.id.menu_qr_code_camera -> qrCodeResult.launch()
            R.id.menu_share_str -> share(GSON.toJson(getSource()))
            R.id.menu_share_qr -> shareWithQr(
                GSON.toJson(getSource()),
                getString(R.string.share_book_source),
                ErrorCorrectionLevel.L
            )
            R.id.menu_help -> showHelp("ruleHelp")
            R.id.menu_login -> getSource().let { source ->
                if (checkSource(source)) {
                    viewModel.save(source) {
                        startActivity<SourceLoginActivity> {
                            putExtra("type", "bookSource")
                            putExtra("key", source.bookSourceUrl)
                        }
                    }
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
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
            }
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
            binding.spType.setSelection(
                when (it.bookSourceType) {
                    BookType.image -> 2
                    BookType.audio -> 1
                    else -> 0
                }
            )
        }
        //基本信息
        sourceEntities.clear()
        sourceEntities.apply {
            add(EditEntity("bookSourceUrl", source?.bookSourceUrl, R.string.source_url))
            add(EditEntity("bookSourceName", source?.bookSourceName, R.string.source_name))
            add(EditEntity("bookSourceGroup", source?.bookSourceGroup, R.string.source_group))
            add(EditEntity("bookSourceComment", source?.bookSourceComment, R.string.comment))
            add(EditEntity("loginUrl", source?.loginUrl, R.string.login_url))
            add(EditEntity("loginUi", source?.loginUi, R.string.login_ui))
            add(EditEntity("loginCheckJs", source?.loginCheckJs, R.string.login_check_js))
            add(EditEntity("bookUrlPattern", source?.bookUrlPattern, R.string.book_url_pattern))
            add(EditEntity("header", source?.header, R.string.source_http_header))
            add(
                EditEntity(
                    "concurrentRate", source?.concurrentRate, R.string.source_concurrent_rate
                )
            )
        }
        //搜索
        val sr = source?.getSearchRule()
        searchEntities.clear()
        searchEntities.apply {
            add(EditEntity("searchUrl", source?.searchUrl, R.string.r_search_url))
            add(EditEntity("checkKeyWord", sr?.checkKeyWord, R.string.check_key_word))
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
            add(EditEntity("isVolume", tr?.isVolume, R.string.rule_is_volume))
            add(EditEntity("updateTime", tr?.updateTime, R.string.rule_update_time))
            add(EditEntity("isVip", tr?.isVip, R.string.rule_is_vip))
            add(EditEntity("isPay", tr?.isPay, R.string.rule_is_pay))
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
            add(EditEntity("payAction", cr?.payAction, R.string.rule_pay_action))
        }
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
        setEditEntities(0)
    }

    private fun getSource(): BookSource {
        val source = viewModel.bookSource?.copy() ?: BookSource()
        source.enabled = binding.cbIsEnable.isChecked
        source.enabledExplore = binding.cbIsEnableFind.isChecked
        source.bookSourceType = when (binding.spType.selectedItemPosition) {
            2 -> BookType.image
            1 -> BookType.audio
            else -> BookType.default
        }
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
                "loginUi" -> source.loginUi = it.value
                "loginCheckJs" -> source.loginCheckJs = it.value
                "bookUrlPattern" -> source.bookUrlPattern = it.value
                "header" -> source.header = it.value
                "bookSourceComment" -> source.bookSourceComment = it.value ?: ""
                "concurrentRate" -> source.concurrentRate = it.value
            }
        }
        searchEntities.forEach {
            when (it.key) {
                "searchUrl" -> source.searchUrl = it.value
                "checkKeyWord" -> searchRule.checkKeyWord = it.value
                "bookList" -> searchRule.bookList = it.value ?: ""
                "name" -> searchRule.name =
                    viewModel.ruleComplete(it.value, searchRule.bookList)
                "author" -> searchRule.author =
                    viewModel.ruleComplete(it.value, searchRule.bookList)
                "kind" -> searchRule.kind =
                    viewModel.ruleComplete(it.value, searchRule.bookList)
                "intro" -> searchRule.intro =
                    viewModel.ruleComplete(it.value, searchRule.bookList)
                "updateTime" -> searchRule.updateTime =
                    viewModel.ruleComplete(it.value, searchRule.bookList)
                "wordCount" -> searchRule.wordCount =
                    viewModel.ruleComplete(it.value, searchRule.bookList)
                "lastChapter" -> searchRule.lastChapter =
                    viewModel.ruleComplete(it.value, searchRule.bookList)
                "coverUrl" -> searchRule.coverUrl =
                    viewModel.ruleComplete(it.value, searchRule.bookList, 3)
                "bookUrl" -> searchRule.bookUrl =
                    viewModel.ruleComplete(it.value, searchRule.bookList, 2)
            }
        }
        findEntities.forEach {
            when (it.key) {
                "exploreUrl" -> source.exploreUrl = it.value
                "bookList" -> exploreRule.bookList = it.value ?: ""
                "name" -> exploreRule.name =
                    viewModel.ruleComplete(it.value, exploreRule.bookList)
                "author" -> exploreRule.author =
                    viewModel.ruleComplete(it.value, exploreRule.bookList)
                "kind" -> exploreRule.kind =
                    viewModel.ruleComplete(it.value, exploreRule.bookList)
                "intro" -> exploreRule.intro =
                    viewModel.ruleComplete(it.value, exploreRule.bookList)
                "updateTime" -> exploreRule.updateTime =
                    viewModel.ruleComplete(it.value, exploreRule.bookList)
                "wordCount" -> exploreRule.wordCount =
                    viewModel.ruleComplete(it.value, exploreRule.bookList)
                "lastChapter" -> exploreRule.lastChapter =
                    viewModel.ruleComplete(it.value, exploreRule.bookList)
                "coverUrl" -> exploreRule.coverUrl =
                    viewModel.ruleComplete(it.value, exploreRule.bookList, 3)
                "bookUrl" -> exploreRule.bookUrl =
                    viewModel.ruleComplete(it.value, exploreRule.bookList, 2)
            }
        }
        infoEntities.forEach {
            when (it.key) {
                "init" -> bookInfoRule.init = it.value ?: ""
                "name" -> bookInfoRule.name = viewModel.ruleComplete(it.value, bookInfoRule.init)
                "author" -> bookInfoRule.author =
                    viewModel.ruleComplete(it.value, bookInfoRule.init)
                "kind" -> bookInfoRule.kind =
                    viewModel.ruleComplete(it.value, bookInfoRule.init)
                "intro" -> bookInfoRule.intro =
                    viewModel.ruleComplete(it.value, bookInfoRule.init)
                "updateTime" -> bookInfoRule.updateTime =
                    viewModel.ruleComplete(it.value, bookInfoRule.init)
                "wordCount" -> bookInfoRule.wordCount =
                    viewModel.ruleComplete(it.value, bookInfoRule.init)
                "lastChapter" -> bookInfoRule.lastChapter =
                    viewModel.ruleComplete(it.value, bookInfoRule.init)
                "coverUrl" -> bookInfoRule.coverUrl =
                    viewModel.ruleComplete(it.value, bookInfoRule.init, 3)
                "tocUrl" -> bookInfoRule.tocUrl =
                    viewModel.ruleComplete(it.value, bookInfoRule.init, 2)
                "canReName" -> bookInfoRule.canReName = it.value
            }
        }
        tocEntities.forEach {
            when (it.key) {
                "chapterList" -> tocRule.chapterList = it.value ?: ""
                "chapterName" -> tocRule.chapterName =
                    viewModel.ruleComplete(it.value, tocRule.chapterList)
                "chapterUrl" -> tocRule.chapterUrl =
                    viewModel.ruleComplete(it.value, tocRule.chapterList, 2)
                "isVolume" -> tocRule.isVolume = it.value
                "updateTime" -> tocRule.updateTime = it.value
                "isVip" -> tocRule.isVip = it.value
                "isPay" -> tocRule.isPay = it.value
                "nextTocUrl" -> tocRule.nextTocUrl =
                    viewModel.ruleComplete(it.value, tocRule.chapterList, 2)
            }
        }
        contentEntities.forEach {
            when (it.key) {
                "content" -> contentRule.content =
                    viewModel.ruleComplete(it.value)
                "nextContentUrl" -> contentRule.nextContentUrl =
                    viewModel.ruleComplete(it.value, type = 2)
                "webJs" -> contentRule.webJs = it.value
                "sourceRegex" -> contentRule.sourceRegex = it.value
                "replaceRegex" -> contentRule.replaceRegex = it.value
                "imageStyle" -> contentRule.imageStyle = it.value
                "payAction" -> contentRule.payAction = it.value
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
        val items = arrayListOf("插入URL参数", "书源教程", "js教程", "正则教程", "选择文件")
        selector(getString(R.string.help), items) { _, index ->
            when (index) {
                0 -> insertText(AppConst.urlOption)
                1 -> showHelp("ruleHelp")
                2 -> showHelp("jsHelp")
                3 -> showHelp("regexHelp")
                4 -> selectDoc.launch {
                    mode = HandleFileContract.FILE
                }
            }
        }
    }

    private fun showHelp(fileName: String) {
        //显示目录help下的帮助文档
        val mdText = String(assets.open("help/${fileName}.md").readBytes())
        showDialogFragment(TextDialog(mdText, TextDialog.Mode.MD))
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
            val screenHeight = this@BookSourceEditActivity.windowSize.heightPixels
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
