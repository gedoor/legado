package io.legado.app.ui.download

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book


class DownloadViewModel(application: Application) : BaseViewModel(application) {


    fun export(uri: Uri, book: Book) {
        execute {
            DocumentFile.fromTreeUri(context, uri)
                ?.createFile("txt", book.name)
                ?.let {
                    context.contentResolver.openOutputStream(it.uri)?.let {
                        App.db.bookChapterDao().getChapterList(book.bookUrl).forEach {

                        }
                    }
                }
        }
    }


}