package io.legado.app.ui.book.local

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.base.BaseViewModel
import io.legado.app.model.localBook.LocalBook


class ImportBookViewModel(application: Application) : BaseViewModel(application) {

    fun addToBookshelf(uriList: HashSet<String>, finally: () -> Unit) {
        execute {
            uriList.forEach { uriStr ->
                DocumentFile.fromSingleUri(context, Uri.parse(uriStr))?.let { doc ->
                    LocalBook.importFile(doc)
                }
            }
        }.onFinally {
            finally.invoke()
        }
    }

    fun deleteDoc(uriList: HashSet<String>, finally: () -> Unit) {
        execute {
            uriList.forEach {
                DocumentFile.fromSingleUri(context, Uri.parse(it))?.delete()
            }
        }.onFinally {
            finally.invoke()
        }
    }

}