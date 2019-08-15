package io.legado.app.ui.bookinfo

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.help.ImageLoader
import io.legado.app.ui.changesource.ChangeSourceDialog
import io.legado.app.utils.getViewModel
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.activity_book_info.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.startActivity

class BookInfoActivity : VMBaseActivity<BookInfoViewModel>(R.layout.activity_book_info), ChangeSourceDialog.CallBack {
    override val viewModel: BookInfoViewModel
        get() = getViewModel(BookInfoViewModel::class.java)

    var changeSourceDialog: ChangeSourceDialog? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setSupportActionBar(toolbar)
        viewModel.bookData.observe(this, Observer { showBook(it) })
        viewModel.isLoadingData.observe(this, Observer { upLoading(it) })
        viewModel.loadBook(intent)
        initView()
        savedInstanceState?.let {
            changeSourceDialog = supportFragmentManager.findFragmentByTag(ChangeSourceDialog.tag) as? ChangeSourceDialog
            changeSourceDialog?.callBack = this
        }
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
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun showBook(book: Book) {
        tv_name.text = book.name
        tv_author.text = getString(R.string.author_show, book.author)
        tv_origin.text = getString(R.string.origin_show, book.originName)
        tv_lasted.text = getString(R.string.lasted_show, book.latestChapterTitle)
        tv_intro.text = getString(R.string.intro_show, book.getDisplayIntro())
        book.getDisplayCover()?.let {
            ImageLoader.load(this, it)
                .placeholder(R.drawable.img_cover_default)
                .error(R.drawable.img_cover_default)
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

    private fun initView() {
        tv_read.onClick {

        }
        tv_shelf.onClick {
            viewModel.saveBook {
                tv_shelf.text = getString(R.string.remove_from_bookshelf)
            }
        }
        tv_loading.onClick { }
        tv_change_source.onClick {
            if (changeSourceDialog == null) {
                viewModel.bookData.value?.let {
                    changeSourceDialog = ChangeSourceDialog.newInstance(it.name, it.author)
                    changeSourceDialog?.callBack = this
                }
            }
            changeSourceDialog?.show(supportFragmentManager, ChangeSourceDialog.tag)
        }
    }

    override fun changeTo(book: Book) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}