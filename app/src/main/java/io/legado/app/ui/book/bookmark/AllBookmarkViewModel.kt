package io.legado.app.ui.book.bookmark

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.utils.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AllBookmarkViewModel(application: Application) : BaseViewModel(application) {


    @Suppress("BlockingMethodInNonBlockingContext")
    fun saveToFile(treeUri: Uri) {
        execute {
            val dateFormat = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
            val bookmark = appDb.bookmarkDao.all
            if (treeUri.isContentScheme()) {
                val doc = DocumentFile.fromTreeUri(context, treeUri)
                    ?.createFile("", "bookmark-${dateFormat.format(Date())}")
                doc?.let {
                    context.contentResolver.openOutputStream(doc.uri)!!.use {
                        GSON.writeToOutputStream(it, bookmark)
                    }
                }
            } else {
                val path = treeUri.path!!
                val file = FileUtils.createFileIfNotExist(
                    File(path),
                    "bookmark-${dateFormat.format(Date())}"
                )
                FileOutputStream(file).use {
                    GSON.writeToOutputStream(it, bookmark)
                }
            }
        }.onError {
            AppLog.put("导出失败\n${it.localizedMessage}", it, true)
        }.onSuccess {
            context.toastOnUi("导出成功")
        }
    }

}