package io.legado.app.ui.book.cache

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppPattern
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Book
import io.legado.app.help.BookHelp
import io.legado.app.help.ContentProcessor
import io.legado.app.help.storage.BookWebDav
import io.legado.app.utils.*
import java.io.File


class CacheViewModel(application: Application) : BaseViewModel(application) {


    fun export(path: String, book: Book, finally: (msg: String) -> Unit) {
        execute {
            if (path.isContentScheme()) {
                val uri = Uri.parse(path)
                DocumentFile.fromTreeUri(context, uri)?.let {
                    export(it, book)
                }
            } else {
                export(FileUtils.createFolderIfNotExist(path), book)
            }
        }.onError {
            finally(it.localizedMessage ?: "ERROR")
        }.onSuccess {
            finally(context.getString(R.string.success))
        }
    }

    private suspend fun export(doc: DocumentFile, book: Book) {
        val filename = "${book.name} by ${book.author}.txt"
        val content = getAllContents(book)
        DocumentUtils.createFileIfNotExist(doc, filename)
            ?.writeText(context, content)
        if (App.INSTANCE.getPrefBoolean(PreferKey.webDavCacheBackup, false)) {
            FileUtils.createFileIfNotExist(
                File(FileUtils.getCachePath()),
                filename
            ).writeText(content) // 写出文件到cache目录
            // 导出到webdav
            BookWebDav.exportWebDav(FileUtils.getCachePath(), filename)
            // 上传完删除cache文件
            FileUtils.deleteFile("${FileUtils.getCachePath()}${File.separator}${filename}")
        }
        getSrcList(book).forEach {
            val vFile = BookHelp.getImage(book, it.third)
            if (vFile.exists()) {
                DocumentUtils.createFileIfNotExist(
                    doc,
                    "${it.second}-${MD5Utils.md5Encode16(it.third)}.jpg",
                    subDirs = arrayOf("${book.name}_${book.author}", "images", it.first)
                )?.writeBytes(context, vFile.readBytes())
            }
        }
    }

    private suspend fun export(file: File, book: Book) {
        val filename = "${book.name} by ${book.author}.txt"
        FileUtils.createFileIfNotExist(file, filename)
            .writeText(getAllContents(book))
        if (App.INSTANCE.getPrefBoolean(PreferKey.webDavCacheBackup, false)) {
            BookWebDav.exportWebDav(file.absolutePath, filename) // 导出到webdav
        }
        getSrcList(book).forEach {
            val vFile = BookHelp.getImage(book, it.third)
            if (vFile.exists()) {
                FileUtils.createFileIfNotExist(
                    file,
                    "${book.name}_${book.author}",
                    "images",
                    it.first,
                    "${it.second}-${MD5Utils.md5Encode16(it.third)}.jpg"
                ).writeBytes(vFile.readBytes())
            }
        }
    }

    private suspend fun getAllContents(book: Book): String {
        val contentProcessor = ContentProcessor(book.name, book.origin)
        val stringBuilder = StringBuilder()
        stringBuilder.append(book.name)
            .append("\n")
            .append(context.getString(R.string.author_show, book.author))
        App.db.bookChapterDao.getChapterList(book.bookUrl).forEach { chapter ->
            BookHelp.getContent(book, chapter).let { content ->
                val content1 = contentProcessor
                    .getContent(book, chapter.title, content ?: "null", false)
                    .joinToString("\n")
                stringBuilder.append("\n\n")
                    .append(content1)
            }
        }
        return stringBuilder.toString()
    }

    private fun getSrcList(book: Book): ArrayList<Triple<String, Int, String>> {
        val srcList = arrayListOf<Triple<String, Int, String>>()
        App.db.bookChapterDao.getChapterList(book.bookUrl).forEach { chapter ->
            BookHelp.getContent(book, chapter)?.let { content ->
                content.split("\n").forEachIndexed { index, text ->
                    val matcher = AppPattern.imgPattern.matcher(text)
                    if (matcher.find()) {
                        matcher.group(1)?.let {
                            val src = NetworkUtils.getAbsoluteURL(chapter.url, it)
                            srcList.add(Triple(chapter.title, index, src))
                        }
                    }
                }
            }
        }
        return srcList
    }
}