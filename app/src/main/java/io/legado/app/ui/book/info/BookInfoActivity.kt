package io.legado.app.ui.book.info

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.ImageLoader
import io.legado.app.help.IntentDataHelp
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.audio.AudioPlayActivity
import io.legado.app.ui.book.info.edit.BookInfoEditActivity
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.changesource.ChangeSourceDialog
import io.legado.app.utils.getCompatDrawable
import io.legado.app.utils.getViewModel
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.activity_book_info.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.startActivity


class BookInfoActivity : VMBaseActivity<BookInfoViewModel>(R.layout.activity_book_info),
    ChapterListAdapter.CallBack,
    ChangeSourceDialog.CallBack {
    override val viewModel: BookInfoViewModel
        get() = getViewModel(BookInfoViewModel::class.java)

    private lateinit var adapter: ChapterListAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setSupportActionBar(toolbar)
        initRecyclerView()
        viewModel.bookData.observe(this, Observer { showBook(it) })
        viewModel.isLoadingData.observe(this, Observer { upLoading(it) })
        viewModel.chapterListData.observe(this, Observer { showChapter(it) })
        viewModel.bookData.value?.let {
            showBook(it)
            upLoading(false)
            viewModel.chapterListData.value?.let { chapters ->
                showChapter(chapters)
            }
        } ?: viewModel.initData(intent)
        initOnClick()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_info, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_edit -> {
                if (viewModel.inBookshelf) {
                    viewModel.bookData.value?.let {
                        startActivity<BookInfoEditActivity>(Pair("bookUrl", it.bookUrl))
                    }
                }
            }
            R.id.menu_refresh -> {
                upLoading(true)
                viewModel.bookData.value?.let {
                    viewModel.loadBookInfo(it)
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun showBook(book: Book) {
        tv_name.text = book.name
        tv_author.text = getString(R.string.author_show, book.author)
        tv_origin.text = getString(R.string.origin_show, book.originName)
        tv_lasted.text = getString(R.string.lasted_show, book.latestChapterTitle)
        tv_intro.text =
            book.getDisplayIntro() // getString(R.string.intro_show, book.getDisplayIntro())
        book.getDisplayCover()?.let {
            ImageLoader.load(this, it)
                .placeholder(R.drawable.image_cover_default)
                .error(R.drawable.image_cover_default)
                .centerCrop()
                .setAsDrawable(iv_cover)
        }
        val kinds = book.getKindList()
        if (kinds.isEmpty()) {
            ll_kind.gone()
        } else {
            ll_kind.visible()
            for (index in 0..2) {
                if (kinds.size > index) {
                    when (index) {
                        0 -> {
                            tv_kind.text = kinds[index]
                            tv_kind.visible()
                        }
                        1 -> {
                            tv_kind_1.text = kinds[index]
                            tv_kind_1.visible()
                        }
                        2 -> {
                            tv_kind_2.text = kinds[index]
                            tv_kind_2.visible()
                        }
                    }
                } else {
                    when (index) {
                        0 -> tv_kind.gone()
                        1 -> tv_kind_1.gone()
                        2 -> tv_kind_2.gone()
                    }
                }
            }
        }
    }

    private fun showChapter(chapterList: List<BookChapter>) {
        viewModel.bookData.value?.let {
            if (it.durChapterIndex < chapterList.size) {
                tv_current_chapter_info.text = chapterList[it.durChapterIndex].title
            } else {
                tv_current_chapter_info.text = chapterList.last().title
            }
        }
        adapter.clearItems()
        adapter.addItems(chapterList)
        rv_chapter_list.scrollToPosition(viewModel.durChapterIndex)
        upLoading(false)
    }

    private fun upLoading(isLoading: Boolean) {
        if (isLoading) {
            tv_loading.visible()
        } else {
            if (viewModel.inBookshelf) {
                tv_shelf.text = getString(R.string.remove_from_bookshelf)
            } else {
                tv_shelf.text = getString(R.string.add_to_shelf)
            }
            tv_loading.gone()
        }
    }

    private fun initRecyclerView() {
        adapter = ChapterListAdapter(this, this)
        ATH.applyEdgeEffectColor(rv_chapter_list)
        rv_chapter_list.layoutManager = LinearLayoutManager(this)
        getCompatDrawable(R.drawable.recyclerview_item_divider)?.let { drawable ->
            rv_chapter_list.addItemDecoration(
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                    setDrawable(drawable)
                }
            )
        }
        rv_chapter_list.adapter = adapter
    }

    private fun initOnClick() {
        tv_read.onClick {
            viewModel.bookData.value?.let {
                readBook(it)
            }
        }
        tv_shelf.onClick {
            if (viewModel.inBookshelf) {
                viewModel.delBook {
                    tv_shelf.text = getString(R.string.add_to_shelf)
                }
            } else {
                viewModel.addToBookshelf {
                    tv_shelf.text = getString(R.string.remove_from_bookshelf)
                }
            }
        }
        tv_loading.onClick {
            viewModel.bookData.value?.let {
                viewModel.loadBookInfo(it)
            }
        }
        tv_origin.onClick {
            viewModel.bookData.value?.let {
                startActivity<BookSourceEditActivity>(Pair("data", it.origin))
            }
        }
        tv_change_source.onClick {
            viewModel.bookData.value?.let {
                ChangeSourceDialog.show(supportFragmentManager, it.name, it.author)
            }
        }
        tv_current_chapter_info.onClick {
            viewModel.bookData.value?.let {
                rv_chapter_list.scrollToPosition(it.durChapterIndex)
            }
        }
        iv_chapter_top.onClick {
            rv_chapter_list.scrollToPosition(0)
        }
        iv_chapter_bottom.onClick {
            rv_chapter_list.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun readBook(book: Book) {
        if (!viewModel.inBookshelf) {
            viewModel.saveBook {
                viewModel.saveChapterList {
                    startReadActivity(book)
                }
            }
        } else {
            viewModel.saveBook {
                startReadActivity(book)
            }
        }
    }

    private fun startReadActivity(book: Book) {
        when (book.type) {
            BookType.audio -> startActivity<AudioPlayActivity>(
                Pair("bookUrl", book.bookUrl),
                Pair("inBookshelf", viewModel.inBookshelf)
            )
            else -> startActivity<ReadBookActivity>(
                Pair("bookUrl", book.bookUrl),
                Pair("inBookshelf", viewModel.inBookshelf),
                Pair("key", IntentDataHelp.putData(book))
            )
        }
    }

    override val curOrigin: String?
        get() = viewModel.bookData.value?.origin

    override val oldBook: Book?
        get() = viewModel.bookData.value

    override fun changeTo(book: Book) {
        upLoading(true)
        viewModel.changeTo(book)
    }

    override fun openChapter(chapter: BookChapter) {
        if (chapter.index != viewModel.durChapterIndex) {
            viewModel.bookData.value?.let {
                it.durChapterIndex = chapter.index
                it.durChapterPos = 0
                readBook(it)
            }
        }
    }

    override fun durChapterIndex(): Int {
        return viewModel.durChapterIndex
    }
}