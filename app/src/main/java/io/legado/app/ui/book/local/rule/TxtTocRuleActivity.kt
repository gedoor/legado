package io.legado.app.ui.book.local.rule

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import com.google.android.material.snackbar.Snackbar
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.databinding.ActivityTxtTocRuleBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.DialogTocRegexEditBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.ACache
import io.legado.app.utils.snackbar
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.viewbindingdelegate.viewBinding

class TxtTocRuleActivity : VMBaseActivity<ActivityTxtTocRuleBinding, TxtTocRuleViewModel>(),
    TxtTocRuleAdapter.Callback {

    override val viewModel: TxtTocRuleViewModel by viewModels()
    override val binding: ActivityTxtTocRuleBinding by viewBinding(ActivityTxtTocRuleBinding::inflate)
    private val adapter: TxtTocRuleAdapter by lazy {
        TxtTocRuleAdapter(this, this)
    }
    private val importTocRuleKey = "tocRuleUrl"

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
    }

    private fun initView() = binding.run {
        recyclerView.adapter = adapter

    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.txt_toc_regex, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> edit(TxtTocRule())
            R.id.menu_default -> viewModel.importDefault()
            R.id.menu_import -> showImportDialog()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun del(source: TxtTocRule) {
        viewModel.del(source)
    }

    override fun edit(source: TxtTocRule) {
        alert(titleResource = R.string.txt_toc_regex) {
            val alertBinding = DialogTocRegexEditBinding.inflate(layoutInflater)
            alertBinding.apply {
                tvRuleName.setText(source.name)
                tvRuleRegex.setText(source.rule)
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.apply {
                    source.name = tvRuleName.text.toString()
                    source.rule = tvRuleRegex.text.toString()
                    viewModel.save(source)
                }
            }
            cancelButton()
        }
    }

    override fun update(vararg source: TxtTocRule) {
        viewModel.update(*source)
    }

    override fun toTop(source: TxtTocRule) {

    }

    override fun toBottom(source: TxtTocRule) {

    }

    override fun upOrder() {

    }

    override fun upCountView() {
        binding.selectActionBar
            .upCountView(adapter.selection.size, adapter.itemCount)
    }

    @SuppressLint("InflateParams")
    private fun showImportDialog() {
        val aCache = ACache.get(this, cacheDir = false)
        val defaultUrl = "https://gitee.com/fisher52/YueDuJson/raw/master/myTxtChapterRule.json"
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importTocRuleKey)
            ?.splitNotBlank(",")
            ?.toMutableList()
            ?: mutableListOf()
        if (!cacheUrls.contains(defaultUrl)) {
            cacheUrls.add(0, defaultUrl)
        }
        alert(titleResource = R.string.import_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
                editView.setFilterValues(cacheUrls)
                editView.delCallBack = {
                    cacheUrls.remove(it)
                    aCache.put(importTocRuleKey, cacheUrls.joinToString(","))
                }
            }
            customView { alertBinding.root }
            okButton {
                val text = alertBinding.editView.text?.toString()
                text?.let {
                    if (!cacheUrls.contains(it)) {
                        cacheUrls.add(0, it)
                        aCache.put(importTocRuleKey, cacheUrls.joinToString(","))
                    }
                    Snackbar.make(binding.root, R.string.importing, Snackbar.LENGTH_INDEFINITE)
                        .show()
                    viewModel.importOnLine(it) { msg ->
                        binding.root.snackbar(msg)
                    }
                }
            }
            cancelButton()
        }
    }

}