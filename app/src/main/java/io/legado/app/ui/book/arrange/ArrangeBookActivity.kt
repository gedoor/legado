package io.legado.app.ui.book.arrange

import android.os.Bundle
import androidx.lifecycle.LiveData
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.utils.getViewModel


class ArrangeBookActivity : VMBaseActivity<ArrangeBookViewModel>(R.layout.activity_arrange_book) {
    override val viewModel: ArrangeBookViewModel
        get() = getViewModel(ArrangeBookViewModel::class.java)

    private var booksLiveData: LiveData<Book>? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {


    }


}