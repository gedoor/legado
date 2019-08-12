package io.legado.app.ui.bookinfo

import android.os.Bundle
import androidx.lifecycle.Observer
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.help.ImageLoader
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_book_info.*

class BookInfoActivity : VMBaseActivity<BookInfoViewModel>(R.layout.activity_book_info) {
    override val viewModel: BookInfoViewModel
        get() = getViewModel(BookInfoViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.bookData.observe(this, Observer { showBook(it) })
        viewModel.loadData(intent)
    }

    private fun showBook(book: Book) {
        tv_name.text = book.name
        tv_author.text = book.author
        tv_origin.text = book.originName
        tv_lasted.text = book.latestChapterTitle
        tv_intro.text = book.getDisplayIntro()
        book.getDisplayCover()?.let {
            ImageLoader.load(this, it)
                .placeholder(R.drawable.img_cover_default)
                .error(R.drawable.img_cover_default)
                .centerCrop()
                .setAsDrawable(iv_cover)
        }
    }


}