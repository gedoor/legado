package io.legado.app.ui.book.local

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.base.BaseViewModel
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.DocumentUtils
import io.legado.app.utils.FileDoc
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.toastOnUi
import java.io.File
import java.util.*


class ImportBookViewModel(application: Application) : BaseViewModel(application) {

    fun addToBookshelf(uriList: HashSet<String>, finally: () -> Unit) {
        execute {
            uriList.forEach {
                LocalBook.importFile(Uri.parse(it))
            }
        }.onFinally {
            finally.invoke()
        }
    }

    fun deleteDoc(uriList: HashSet<String>, finally: () -> Unit) {
        execute {
            uriList.forEach {
                val uri = Uri.parse(it)
                if (uri.isContentScheme()) {
                    DocumentFile.fromSingleUri(context, uri)?.delete()
                } else {
                    uri.path?.let { path ->
                        File(path).delete()
                    }
                }
            }
        }.onFinally {
            finally.invoke()
        }
    }

    fun scanDoc(
        documentFile: DocumentFile,
        isRoot: Boolean,
        find: (docItem: FileDoc) -> Unit,
        finally: (() -> Unit)? = null
    ) {
        kotlin.runCatching {
            DocumentUtils.listFiles(documentFile.uri).forEach { docItem ->
                if (docItem.isDir) {
                    DocumentFile.fromSingleUri(context, docItem.uri)?.let {
                        scanDoc(it, false, find)
                    }
                } else if (docItem.name.endsWith(".txt", true)
                    || docItem.name.endsWith(".epub", true)
                ) {
                    find(docItem)
                }
            }
        }.onFailure {
            context.toastOnUi("扫描文件夹出错\n${it.localizedMessage}")
        }
        if (isRoot) {
            finally?.invoke()
        }
    }

    fun scanFile(
        file: File,
        isRoot: Boolean,
        find: (docItem: FileDoc) -> Unit,
        finally: (() -> Unit)? = null
    ) {
        file.listFiles()?.forEach {
            if (it.isDirectory) {
                scanFile(it, false, find)
            } else if (it.name.endsWith(".txt", true)
                || it.name.endsWith(".epub", true)
            ) {
                find(
                    FileDoc(
                        it.name,
                        it.isDirectory,
                        it.length(),
                        Date(it.lastModified()),
                        Uri.parse(it.absolutePath)
                    )
                )
            }
        }
        if (isRoot) {
            finally?.invoke()
        }
    }

}