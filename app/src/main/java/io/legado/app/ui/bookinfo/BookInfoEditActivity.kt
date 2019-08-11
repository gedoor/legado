package io.legado.app.ui.bookinfo

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.utils.getViewModel
import io.legado.app.utils.toast
import kotlinx.android.synthetic.main.activity_book_info_edit.*

class BookInfoEditActivity : VMBaseActivity<BookInfoEditViewModel>(R.layout.activity_book_info_edit) {
    override val viewModel: BookInfoEditViewModel
        get() = getViewModel(BookInfoEditViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.bookData.observe(this, Observer { upView(it) })
        if (viewModel.bookData.value == null) {
            intent.getStringExtra("bookUrl")?.let {
                viewModel.loadBook(it)
            }
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_info_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> saveData()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun upView(book: Book) {
        tie_book_name.setText(book.name)
        tie_book_author.setText(book.author)
        tie_cover_url.setText(book.getDisplayCover())
        tie_book_intro.setText(book.getDisplayIntro())
    }

    private fun saveData() {
        viewModel.bookData.value?.let { book ->
            book.name = tie_book_name.text?.toString()
            book.author = tie_book_author.text?.toString()
            val customCoverUrl = tie_cover_url.text?.toString()
            book.customCoverUrl = if (customCoverUrl == book.coverUrl) null else customCoverUrl
            book.customIntro = tie_book_intro.text?.toString()
            viewModel.saveBook(book, success = { finish() }, error = { toast(it) })
        }
    }
}