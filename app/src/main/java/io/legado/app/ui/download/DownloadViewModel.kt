package io.legado.app.ui.download

import android.app.Application
import android.net.Uri
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book


class DownloadViewModel(application: Application) : BaseViewModel(application) {


    fun export(book: Book, uri: Uri) {
        execute {

        }
    }


}