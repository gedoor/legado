package io.legado.app.ui.code

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityCodeEditBinding
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.code.config.ChangeThemeDialog
import io.legado.app.ui.widget.keyboard.KeyboardToolPop
import io.legado.app.utils.imeHeight
import io.legado.app.utils.setOnApplyWindowInsetsListenerCompat
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.showHelp
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlin.getValue
import androidx.core.widget.addTextChangedListener
import io.github.rosemoe.sora.widget.EditorSearcher
import androidx.core.view.isGone
import io.github.rosemoe.sora.event.PublishSearchResultEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent

class CodeEditActivity :
    VMBaseActivity<ActivityCodeEditBinding, CodeEditViewModel>(),
    KeyboardToolPop.CallBack, ChangeThemeDialog.CallBack {
    override val binding by viewBinding(ActivityCodeEditBinding::inflate)
    override val viewModel by viewModels<CodeEditViewModel>()
    private val softKeyboardTool by lazy {
        KeyboardToolPop(this, lifecycleScope, binding.root, this)
    }


    private val editor: CodeEditor by lazy { binding.editText }
    private val editorSearcher: EditorSearcher by lazy { editor.searcher }
    private var options = EditorSearcher.SearchOptions(false, true)


    private fun initView() {
        binding.root.setOnApplyWindowInsetsListenerCompat { _, windowInsets ->
            softKeyboardTool.initialPadding = windowInsets.imeHeight
            windowInsets
        }
        editor.apply {
            colorScheme = TextMateColorScheme2(ThemeRegistry.getInstance(), ThemeRegistry.getInstance().currentThemeModel)
            isWordwrap = true
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        softKeyboardTool.attachToWindow(window)
        viewModel.initData(intent) {
            editor.apply {
                setText(viewModel.initialText)
                setEditorLanguage(viewModel.language)
            }
        }
        initView()
    }

    override fun onDestroy() {
        super.onDestroy()
        editorSearcher.stopSearch()
        editor.release()
    }

    /**
    * 使用super.finish(),防止循环回调
    * */
    private fun save(check: Boolean) {
        val text = editor.text.toString()
        if (text == viewModel.initialText) {
            super.finish()
        } else if (check) {
            alert(R.string.exit) {
                setMessage(R.string.exit_no_save)
                positiveButton(R.string.yes)
                negativeButton(R.string.no) {
                    super.finish()
                }
            }
        } else {
            val result = Intent().apply {
                putExtra("text", text)
                putExtra("cursorPosition", editor.cursor?.left ?: 0)
            }
            setResult(RESULT_OK, result)
            super.finish()
        }
    }

    override fun upTheme(index: Int){
        viewModel.loadTextMateThemes(index)
        editor.setEditorLanguage(viewModel.language) //每次更改颜色后需要再执行一次语言设置,防止切换主题后高亮颜色不正确
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.code_edit_activity, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    private fun search() {
        val receiptSearch = editor.subscribeEvent(PublishSearchResultEvent::class.java) { event, _ ->
            if (event.editor == editor) {
                updateSearchResults()
            }
        }
        val receiptChange = editor.subscribeEvent(SelectionChangeEvent::class.java) { event, _ ->
            if (event.cause == SelectionChangeEvent.CAUSE_SEARCH) {
                updateSearchResults()
            }
        }
        binding.searchGroup.visibility = View.VISIBLE
        binding.etFind.requestFocus()
        binding.btnCloseFind.setOnClickListener {
            binding.searchGroup.visibility = View.GONE
            editorSearcher.stopSearch()
            receiptSearch.unsubscribe()
            receiptChange.unsubscribe()
            editor.requestFocus()
            editor.invalidate()
        }
        binding.etFind.addTextChangedListener { text ->
            if (!text.isNullOrEmpty()) {
                editorSearcher.search(text.toString(), options)
            }
            else {
                editorSearcher.stopSearch()
                editor.invalidate()
            }
        }
        binding.btnPrevious.setOnClickListener {
            editorSearcher.gotoPrevious()
        }
        binding.btnNext.setOnClickListener {
            editorSearcher.gotoNext()
        }
        binding.btnReplace.setOnClickListener {
            if (binding.replaceGroup.isGone) {
                binding.replaceGroup.visibility = View.VISIBLE
                binding.btnReplaceAll.isEnabled = true
                binding.etReplace.requestFocus()
            }
            else {
                if (editorSearcher.hasQuery()) {
                    editorSearcher.replaceCurrentMatch(binding.etReplace.text.toString())
                }
            }
        }
        binding.btnCloseReplace.setOnClickListener {
            binding.replaceGroup.visibility = View.GONE
            binding.btnReplaceAll.isEnabled = false
        }
        binding.btnReplaceAll.setOnClickListener {
            if (editorSearcher.hasQuery()) {
                editorSearcher.replaceAll(binding.etReplace.text.toString())
            }
        }
        binding.switchRegex.setOnCheckedChangeListener { _, isChecked ->
            options = EditorSearcher.SearchOptions(!isChecked,isChecked)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateSearchResults() {
        if (editorSearcher.hasQuery()) {
            val totalResults = editorSearcher.matchedPositionCount
            val currentPosition = editorSearcher.currentMatchedPositionIndex + 1
            binding.tvSearchResult.text = "${if (currentPosition > 0) "$currentPosition/" else ""}$totalResults"
        }
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_search -> search()
            R.id.menu_save -> save(false)
            R.id.menu_change_theme -> showDialogFragment(ChangeThemeDialog(this))
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun finish() {
        save(true)
    }

    override fun helpActions(): List<SelectItem<String>> {
        return arrayListOf(
            SelectItem("源教程", "ruleHelp"),
            SelectItem("js教程", "jsHelp"),
            SelectItem("正则教程", "regexHelp")
        )
    }

    override fun onHelpActionSelect(action: String) {
        when (action) {
            "ruleHelp" -> showHelp("ruleHelp")
            "jsHelp" -> showHelp("jsHelp")
            "regexHelp" -> showHelp("regexHelp")
        }
    }

    override fun sendText(text: String) {
        editor.insertText(text, text.length)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onUndoClicked() {
        editor.undo()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRedoClicked() {
        editor.redo()
    }
}