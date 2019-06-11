package io.legado.app.ui.bookshelf

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.BookGroup

class BookshelfViewModel(application: Application) : BaseViewModel(application) {

    var bookGroup: BookGroup? = null

}