package io.legado.app.ui.importbook

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.base.BaseViewModel


class ImportBookViewModel(application: Application) : BaseViewModel(application) {

    fun addToBookshelf(uriList: HashSet<String>, finally: () -> Unit) {

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