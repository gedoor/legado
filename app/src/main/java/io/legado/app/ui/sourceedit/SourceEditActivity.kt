package io.legado.app.ui.sourceedit

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.entities.BookSource
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_source_edit.*
import org.jetbrains.anko.toast

class SourceEditActivity : BaseActivity<SourceEditViewModel>() {
    override val viewModel: SourceEditViewModel
        get() = getViewModel(SourceEditViewModel::class.java)
    override val layoutID: Int
        get() = R.layout.activity_source_edit

    private val adapter = SourceEditAdapter()
    private val editList: ArrayList<EditEntity> = ArrayList()
    private val findEditList: ArrayList<EditEntity> = ArrayList()

    override fun onViewModelCreated(viewModel: SourceEditViewModel, savedInstanceState: Bundle?) {
        initRecyclerView()
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

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.source_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                val bookSource = getSource()
                if (bookSource == null) {
                    toast("书源名称和URL不能为空")
                } else {
                    viewModel.save(bookSource) { finish() }
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = adapter
    }

    private fun upRecyclerView(bookSource: BookSource?) {
        bookSource?.let {
            cb_is_enable.isChecked = it.enabled
            cb_is_enable_find.isChecked = it.enabledExplore
        }
        editList.clear()
        editList.add(EditEntity("origin", bookSource?.bookSourceUrl, R.string.book_source_url))
        editList.add(EditEntity("name", bookSource?.bookSourceName, R.string.book_source_name))
        editList.add(EditEntity("group", bookSource?.bookSourceGroup, R.string.book_source_group))
        editList.add(EditEntity("loginUrl", bookSource?.loginUrl, R.string.book_source_login_url))
        editList.add(EditEntity("header", bookSource?.header, R.string.source_user_agent))
        //搜索
        with(bookSource?.getSearchRule()) {
            editList.add(EditEntity("searchUrl", this?.searchUrl, R.string.rule_search_url))
            editList.add(EditEntity("searchList", this?.bookList, R.string.rule_search_list))
            editList.add(EditEntity("searchName", this?.name, R.string.rule_search_name))
            editList.add(EditEntity("searchAuthor", this?.author, R.string.rule_search_author))
            editList.add(EditEntity("searchKind", this?.meta, R.string.rule_search_kind))
            editList.add(EditEntity("searchLastChapter", this?.lastChapter, R.string.rule_search_last_chapter))
            editList.add(EditEntity("searchIntroduce", this?.desc, R.string.rule_search_introduce))
            editList.add(EditEntity("searchCoverUrl", this?.coverUrl, R.string.rule_search_cover_url))
            editList.add(EditEntity("searchNoteUrl", this?.bookUrl, R.string.rule_search_note_url))
        }
        //详情页
        with(bookSource?.getBookInfoRule()) {
            editList.add(EditEntity("bookUrlPattern", this?.urlPattern, R.string.book_url_pattern))
            editList.add(EditEntity("bookInfoInit", this?.init, R.string.rule_book_info_init))
            editList.add(EditEntity("bookName", this?.name, R.string.rule_book_name))
            editList.add(EditEntity("bookAuthor", this?.author, R.string.rule_book_author))
            editList.add(EditEntity("ruleCoverUrl", this?.coverUrl, R.string.rule_cover_url))
            editList.add(EditEntity("ruleIntroduce", this?.desc, R.string.rule_introduce))
            editList.add(EditEntity("bookKind", this?.meta, R.string.rule_book_kind))
            editList.add(EditEntity("bookLastChapter", this?.lastChapter, R.string.rule_book_last_chapter))
            editList.add(EditEntity("tocUrl", this?.tocUrl, R.string.rule_chapter_list_url))
        }
        //目录页
        with(bookSource?.getTocRule()) {
            editList.add(EditEntity("chapterList", this?.chapterList, R.string.rule_chapter_list))
            editList.add(EditEntity("chapterName", this?.chapterName, R.string.rule_chapter_name))
            editList.add(EditEntity("chapterUrl", this?.chapterUrl, R.string.rule_content_url))
            editList.add(EditEntity("tocUrlNext", this?.nextUrl, R.string.rule_chapter_list_url_next))
        }
        //正文页
        with(bookSource?.getContentRule()) {
            editList.add(EditEntity("content", this?.content, R.string.rule_book_content))
            editList.add(EditEntity("contentUrlNext", this?.nextUrl, R.string.rule_content_url_next))

        }

        //发现
        with(bookSource?.getExploreRule()) {
            findEditList.add(EditEntity("findUrl", this?.exploreUrl, R.string.rule_find_url))
            findEditList.add(EditEntity("findList", this?.bookList, R.string.rule_find_list))
            findEditList.add(EditEntity("findName", this?.name, R.string.rule_find_name))
            findEditList.add(EditEntity("findAuthor", this?.author, R.string.rule_find_author))
            findEditList.add(EditEntity("findKind", this?.meta, R.string.rule_find_kind))
            findEditList.add(EditEntity("findIntroduce", this?.desc, R.string.rule_find_introduce))
            findEditList.add(EditEntity("findLastChapter", this?.lastChapter, R.string.rule_find_last_chapter))
            findEditList.add(EditEntity("findCoverUrl", this?.coverUrl, R.string.rule_find_cover_url))
            findEditList.add(EditEntity("findNoteUrl", this?.bookUrl, R.string.rule_find_note_url))
        }
        adapter.editEntities = editList
        adapter.notifyDataSetChanged()
    }

    private fun getSource(): BookSource? {
        val bookSource = BookSource()
        bookSource.enabled = cb_is_enable.isChecked
        bookSource.enabledExplore = cb_is_enable_find.isChecked
        for (entity in adapter.editEntities) {
            when (entity.key) {
                "origin" -> {
                    if (entity.value == null) {
                        return null
                    } else {
                        bookSource.bookSourceUrl = entity.value!!
                    }
                }
                "name" -> {
                    if (entity.value == null) {
                        return null
                    } else {
                        bookSource.bookSourceName = entity.value!!
                    }
                }
                "group" -> bookSource.bookSourceGroup = entity.value
            }
        }
        return bookSource
    }

    class EditEntity(var key: String, var value: String?, var hint: Int)
}