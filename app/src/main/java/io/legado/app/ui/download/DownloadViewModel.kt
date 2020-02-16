package io.legado.app.ui.download

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book
import io.legado.app.utils.FileUtils
import io.legado.app.utils.isContentPath
import java.io.File


class DownloadViewModel(application: Application) : BaseViewModel(application) {


    fun export(path: String, book: Book) {
        execute {
            if (path.isContentPath()) {
                val uri = Uri.parse(path)
                DocumentFile.fromTreeUri(context, uri)
                    ?.createFile("txt", book.name)
                    ?.let {
                        export(it.uri, book)
                    }
            } else {
                FileUtils.createFolderIfNotExist(path).let {
                    export(it, book)
                }
            }
        }.onError {
            toast(it.localizedMessage ?: "ERROR")
        }
    }


    private fun export(uri: Uri, book: Book) {

    }

    private fun export(file: File, book: Book) {

    }

}