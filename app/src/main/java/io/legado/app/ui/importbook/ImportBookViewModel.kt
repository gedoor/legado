package io.legado.app.ui.importbook

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.Book


class ImportBookViewModel(application: Application) : BaseViewModel(application) {

    fun addToBookshelf(uriList: HashSet<String>, finally: () -> Unit) {
        execute {
            uriList.forEach { uriStr ->
                DocumentFile.fromSingleUri(context, Uri.parse(uriStr))?.let { doc ->
                    doc.name?.let { fileName ->
                        val str = fileName.substringBeforeLast(".")
                        var name = str.substringBefore("作者")
                        val author = str.substringAfter("作者")
                        val smhStart = name.indexOf("《")
                        val smhEnd = name.indexOf("》")
                        if (smhStart != -1 && smhEnd != -1) {
                            name = (name.substring(smhStart + 1, smhEnd))
                        }
                        val book = Book(
                            bookUrl = uriStr,
                            name = name,
                            author = author,
                            originName = fileName
                        )
                        App.db.bookDao().insert(book)
                    }
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