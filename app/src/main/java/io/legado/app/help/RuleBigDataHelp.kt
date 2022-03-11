package io.legado.app.help

import io.legado.app.data.appDb
import io.legado.app.utils.FileUtils
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.externalFiles
import io.legado.app.utils.getFile
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.File

object RuleBigDataHelp {

    private val ruleDataDir = FileUtils.createFolderIfNotExist(appCtx.externalFiles, "ruleData")
    private val bookData = FileUtils.createFolderIfNotExist(ruleDataDir, "book")
    private val rssData = FileUtils.createFolderIfNotExist(ruleDataDir, "rss")

    suspend fun clearInvalid() {
        withContext(IO) {
            bookData.listFiles()?.forEach {
                if (it.isFile) {
                    FileUtils.delete(it)
                } else {
                    val bookUrlFile = it.getFile("bookUrl.txt")
                    if (!bookUrlFile.exists()) {
                        FileUtils.delete(it)
                    } else {
                        val bookUrl = bookUrlFile.readText()
                        if (appDb.bookDao.has(bookUrl) != true) {
                            FileUtils.delete(it)
                        }
                    }
                }
            }
            rssData.listFiles()?.forEach {
                if (it.isFile) {
                    FileUtils.delete(it)
                } else {
                    val originFile = it.getFile("origin.txt")
                    if (!originFile.exists()) {
                        FileUtils.delete(it)
                    } else {
                        val origin = originFile.readText()
                        if (appDb.rssSourceDao.has(origin) != true) {
                            FileUtils.delete(it)
                        }
                    }
                }
            }
        }
    }

    fun putBookVariable(bookUrl: String, key: String, value: String?) {
        val md5BookUrl = MD5Utils.md5Encode(bookUrl)
        val md5Key = MD5Utils.md5Encode(key)
        if (value == null) {
            FileUtils.delete(FileUtils.getPath(bookData, md5BookUrl, "$md5Key.txt"), true)
        } else {
            val valueFile = FileUtils.createFileIfNotExist(bookData, md5BookUrl, "$md5Key.txt")
            valueFile.writeText(value)
            val bookUrlFile = File(FileUtils.getPath(bookData, md5BookUrl, "bookUrl.txt"))
            if (!bookUrlFile.exists()) {
                bookUrlFile.writeText(bookUrl)
            }
        }
    }

    fun getBookVariable(bookUrl: String, key: String?): String? {
        val md5BookUrl = MD5Utils.md5Encode(bookUrl)
        val md5Key = MD5Utils.md5Encode(key)
        val file = File(FileUtils.getPath(bookData, md5BookUrl, "$md5Key.txt"))
        if (file.exists()) {
            return file.readText()
        }
        return null
    }


    fun putChapterVariable(bookUrl: String, chapterUrl: String, key: String, value: String?) {
        val md5BookUrl = MD5Utils.md5Encode(bookUrl)
        val md5ChapterUrl = MD5Utils.md5Encode(chapterUrl)
        val md5Key = MD5Utils.md5Encode(key)
        if (value == null) {
            FileUtils.delete(FileUtils.getPath(bookData, md5BookUrl, md5ChapterUrl, "$md5Key.txt"))
        } else {
            val valueFile =
                FileUtils.createFileIfNotExist(bookData, md5BookUrl, md5ChapterUrl, "$md5Key.txt")
            valueFile.writeText(value)
            val bookUrlFile = File(FileUtils.getPath(bookData, md5BookUrl, "bookUrl.txt"))
            if (!bookUrlFile.exists()) {
                bookUrlFile.writeText(bookUrl)
            }
        }
    }

    fun getChapterVariable(bookUrl: String, chapterUrl: String, key: String): String? {
        val md5BookUrl = MD5Utils.md5Encode(bookUrl)
        val md5ChapterUrl = MD5Utils.md5Encode(chapterUrl)
        val md5Key = MD5Utils.md5Encode(key)
        val file = File(FileUtils.getPath(bookData, md5BookUrl, md5ChapterUrl, "$md5Key.txt"))
        if (file.exists()) {
            return file.readText()
        }
        return null
    }

    fun putRssVariable(origin: String, link: String, key: String, value: String?) {
        val md5Origin = MD5Utils.md5Encode(origin)
        val md5Link = MD5Utils.md5Encode(link)
        val md5Key = MD5Utils.md5Encode(key)
        val filePath = FileUtils.getPath(rssData, md5Origin, md5Link, "$md5Key.txt")
        if (value == null) {
            FileUtils.delete(filePath)
        } else {
            val valueFile = FileUtils.createFileIfNotExist(filePath)
            valueFile.writeText(value)
            val originFile = File(FileUtils.getPath(rssData, md5Origin, "origin.txt"))
            if (!originFile.exists()) {
                originFile.writeText(origin)
            }
            val linFile = File(FileUtils.getPath(rssData, md5Origin, md5Link, "origin.txt"))
            if (!linFile.exists()) {
                linFile.writeText(link)
            }
        }
    }

    fun getRssVariable(origin: String, link: String, key: String): String? {
        val md5Origin = MD5Utils.md5Encode(origin)
        val md5Link = MD5Utils.md5Encode(link)
        val md5Key = MD5Utils.md5Encode(key)
        val filePath = FileUtils.getPath(rssData, md5Origin, md5Link, "$md5Key.txt")
        val file = File(filePath)
        if (file.exists()) {
            return file.readText()
        }
        return null
    }
}