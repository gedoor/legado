package io.legado.app.ui.book.bookmark

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Bookmark
import io.legado.app.utils.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AllBookmarkViewModel(application: Application) : BaseViewModel(application) {


    fun initData(onSuccess: (bookmarks: List<Bookmark>) -> Unit) {
        execute {
            appDb.bookmarkDao.all
        }.onSuccess {
            onSuccess.invoke(it)
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        execute {
            appDb.bookmarkDao.delete(bookmark)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @SuppressLint("SimpleDateFormat")
    fun saveToFile(treeUri: Uri) {
        execute {
            val dateFormat = SimpleDateFormat("yyMMddHHmmss")
            if (treeUri.isContentScheme()) {
                val doc = DocumentFile.fromTreeUri(context, treeUri)
                    ?.createFile("", "bookmark-${dateFormat.format(Date())}")
                doc?.let {
                    context.contentResolver.openOutputStream(doc.uri)!!.use {
                        GSON.writeToOutputStream(it, appDb.bookmarkDao.all)
                    }
                }
            } else {
                val path = treeUri.path!!
                val file = FileUtils.createFileIfNotExist(File(path), "bookmark-${dateFormat.format(Date())}")
                FileOutputStream(file).use {
                    GSON.writeToOutputStream(it, appDb.bookmarkDao.all)
                }
            }
        }
    }

}