package io.legado.app.ui.rss.source.edit

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
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.RssSource
import io.legado.app.databinding.ActivityRssSourceEditBinding
import io.legado.app.help.LocalConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.rss.source.debug.RssSourceDebugActivity
import io.legado.app.ui.widget.KeyboardToolPop
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlin.math.abs

class RssSourceEditActivity :
    VMBaseActivity<ActivityRssSourceEditBinding, RssSourceEditViewModel>(false),
    ViewTreeObserver.OnGlobalLayoutListener,
    KeyboardToolPop.CallBack {

    override val binding by viewBinding(ActivityRssSourceEditBinding::inflate)
    override val viewModel by viewModels<RssSourceEditViewModel>()
    private var mSoftKeyboardTool: PopupWindow? = null
    private var mIsSoftKeyBoardShowing = false
    private val adapter by lazy { RssSourceEditAdapter() }
    private val sourceEntities: ArrayList<EditEntity> = ArrayList()
    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it?.let {
            viewModel.importSource(it) { source: RssSource ->
                upRecyclerView(source)
            }
        }
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

    override fun finish() {
        val source = getRssSource()
        if (!source.equal(viewModel.rssSource)) {
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

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.source_edit, menu)
        menu.findItem(R.id.menu_login).isVisible = false
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_login)?.isVisible = !viewModel.rssSource.loginUrl.isNullOrBlank()
        return super.onMenuOpened(featureId, menu)
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
                        startActivity<RssSourceDebugActivity> {
                            putExtra("key", source.sourceUrl)
                        }
                    }
                }
            }
            R.id.menu_login -> getRssSource().let {
                if (checkSource(it)) {
                    viewModel.save(it) {
                        startActivity<SourceLoginActivity> {
                            putExtra("type", "rssSource")
                            putExtra("key", it.sourceUrl)
                        }
                    }
                }
            }
            R.id.menu_copy_source -> sendToClip(GSON.toJson(getRssSource()))
            R.id.menu_qr_code_camera -> qrCodeResult.launch()
            R.id.menu_paste_source -> viewModel.pasteSource { upRecyclerView(it) }
            R.id.menu_share_str -> share(GSON.toJson(getRssSource()))
            R.id.menu_share_qr -> shareWithQr(
                GSON.toJson(getRssSource()),
                getString(R.string.share_rss_source),
                ErrorCorrectionLevel.L
            )
            R.id.menu_help -> showRuleHelp()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        mSoftKeyboardTool = KeyboardToolPop(this, AppConst.keyboardToolChars, this)
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(this)
        binding.recyclerView.adapter = adapter
    }

    private fun upRecyclerView(source: RssSource? = viewModel.rssSource) {
        source?.let {
            binding.cbIsEnable.isChecked = source.enabled
            binding.cbSingleUrl.isChecked = source.singleUrl
            binding.cbEnableJs.isChecked = source.enableJs
            binding.cbEnableBaseUrl.isChecked = source.loadWithBaseUrl
        }
        sourceEntities.clear()
        sourceEntities.apply {
            add(EditEntity("sourceName", source?.sourceName, R.string.source_name))
            add(EditEntity("sourceUrl", source?.sourceUrl, R.string.source_url))
            add(EditEntity("sourceIcon", source?.sourceIcon, R.string.source_icon))
            add(EditEntity("sourceGroup", source?.sourceGroup, R.string.source_group))
            add(EditEntity("sourceComment", source?.sourceComment, R.string.comment))
            add(EditEntity("loginUrl", source?.loginUrl, R.string.login_url))
            add(EditEntity("loginUi", source?.loginUi, R.string.login_ui))
            add(EditEntity("loginCheckJs", source?.loginCheckJs, R.string.login_check_js))
            add(EditEntity("header", source?.header, R.string.source_http_header))
            add(
                EditEntity(
                    "concurrentRate", source?.concurrentRate, R.string.source_concurrent_rate
                )
            )
            add(EditEntity("sortUrl", source?.sortUrl, R.string.sort_url))
            add(EditEntity("ruleArticles", source?.ruleArticles, R.string.r_articles))
            add(EditEntity("ruleNextPage", source?.ruleNextPage, R.string.r_next))
            add(EditEntity("ruleTitle", source?.ruleTitle, R.string.r_title))
            add(EditEntity("rulePubDate", source?.rulePubDate, R.string.r_date))
            add(EditEntity("ruleDescription", source?.ruleDescription, R.string.r_description))
            add(EditEntity("ruleImage", source?.ruleImage, R.string.r_image))
            add(EditEntity("ruleLink", source?.ruleLink, R.string.r_link))
            add(EditEntity("ruleContent", source?.ruleContent, R.string.r_content))
            add(EditEntity("style", source?.style, R.string.r_style))
        }
        adapter.editEntities = sourceEntities
    }

    private fun getRssSource(): RssSource {
        val source = viewModel.rssSource
        source.enabled = binding.cbIsEnable.isChecked
        source.singleUrl = binding.cbSingleUrl.isChecked
        source.enableJs = binding.cbEnableJs.isChecked
        source.loadWithBaseUrl = binding.cbEnableBaseUrl.isChecked
        sourceEntities.forEach {
            when (it.key) {
                "sourceName" -> source.sourceName = it.value ?: ""
                "sourceUrl" -> source.sourceUrl = it.value ?: ""
                "sourceIcon" -> source.sourceIcon = it.value ?: ""
                "sourceGroup" -> source.sourceGroup = it.value
                "sourceComment" -> source.sourceComment = it.value
                "loginUrl" -> source.loginUrl = it.value
                "loginUi" -> source.loginUi = it.value
                "loginCheckJs" -> source.loginCheckJs = it.value
                "header" -> source.header = it.value
                "concurrentRate" -> source.concurrentRate = it.value
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
            }
        }
        return source
    }

    private fun checkSource(source: RssSource): Boolean {
        if (source.sourceName.isBlank() || source.sourceName.isBlank()) {
            toastOnUi("名称或url不能为空")
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
        val items = arrayListOf("插入URL参数", "订阅源教程", "正则教程")
        selector(getString(R.string.help), items) { _, index ->
            when (index) {
                0 -> insertText(AppConst.urlOption)
                1 -> showRuleHelp()
                2 -> showRegexHelp()
            }
        }
    }

    private fun showRuleHelp() {
        val mdText = String(assets.open("help/ruleHelp.md").readBytes())
        showDialogFragment(TextDialog(mdText, TextDialog.Mode.MD))
    }

    private fun showRegexHelp() {
        val mdText = String(assets.open("help/regexHelp.md").readBytes())
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

    override fun onGlobalLayout() {
        val rect = Rect()
        // 获取当前页面窗口的显示范围
        window.decorView.getWindowVisibleDisplayFrame(rect)
        val screenHeight = this@RssSourceEditActivity.windowSize.heightPixels
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