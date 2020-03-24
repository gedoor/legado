package io.legado.app.model.localBook

import android.content.Context
import android.net.Uri
import io.legado.app.App
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.utils.*
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.util.regex.Matcher
import java.util.regex.Pattern

object AnalyzeTxtFile {
    private const val folderName = "bookTxt"
    private const val BLANK: Byte = 0x0a

    //默认从文件中获取数据的长度
    private const val BUFFER_SIZE = 512 * 1024

    //没有标题的时候，每个章节的最大长度
    private const val MAX_LENGTH_WITH_NO_CHAPTER = 10 * 1024
    val cacheFolder: File by lazy {
        val rootFile = App.INSTANCE.getExternalFilesDir(null)
            ?: App.INSTANCE.externalCacheDir
            ?: App.INSTANCE.cacheDir
        FileUtils.createFolderIfNotExist(rootFile, subDirs = *arrayOf(folderName))
    }

    fun analyze(context: Context, book: Book): ArrayList<BookChapter> {
        val bookFile = getBookFile(context, book)
        book.charset = EncodingDetect.getEncode(bookFile)
        val charset = book.fileCharset()
        val toc = arrayListOf<BookChapter>()
        //获取文件流
        val bookStream = RandomAccessFile(bookFile, "r")
        val rulePattern = getTocRule(book, bookStream, charset)

        //加载章节
        val buffer = ByteArray(BUFFER_SIZE)
        //获取到的块起始点，在文件中的位置
        var curOffset: Long = 0
        //block的个数
        var blockPos = 0
        //读取的长度
        var length: Int
        var allLength = 0

        //获取文件中的数据到buffer，直到没有数据为止
        while (bookStream.read(buffer, 0, buffer.size).also { length = it } > 0) {
            ++blockPos
            //如果存在Chapter
            if (rulePattern != null) { //将数据转换成String
                var blockContent = String(buffer, 0, length, charset)
                val lastN = blockContent.lastIndexOf("\n")
                if (lastN != 0) {
                    blockContent = blockContent.substring(0, lastN)
                    length = blockContent.toByteArray(charset).size
                    allLength += length
                    bookStream.seek(allLength.toLong())
                }
                //当前Block下使过的String的指针
                var seekPos = 0
                //进行正则匹配
                val matcher: Matcher = rulePattern.matcher(blockContent)
                //如果存在相应章节
                while (matcher.find()) { //获取匹配到的字符在字符串中的起始位置
                    val chapterStart = matcher.start()
                    //如果 seekPos == 0 && nextChapterPos != 0 表示当前block处前面有一段内容
                    //第一种情况一定是序章 第二种情况可能是上一个章节的内容
                    if (seekPos == 0 && chapterStart != 0) { //获取当前章节的内容
                        val chapterContent = blockContent.substring(seekPos, chapterStart)
                        //设置指针偏移
                        seekPos += chapterContent.length
                        //获取上一章节
                        val lastChapter = toc.lastOrNull()
                            ?: BookChapter().apply {
                                toc.add(this)
                                start = 0
                                title = "前言"
                            }
                        //将当前段落添加上一章去
                        lastChapter.end =
                            lastChapter.end!! + chapterContent.toByteArray(charset).size
                        //创建当前章节
                        val curChapter = BookChapter()
                        curChapter.title = matcher.group()
                        curChapter.start = lastChapter.end
                        toc.add(curChapter)
                    } else { //是否存在章节
                        if (toc.size != 0) { //获取章节内容
                            val chapterContent = blockContent.substring(seekPos, matcher.start())
                            seekPos += chapterContent.length
                            //获取上一章节
                            val lastChapter = toc.last()
                            lastChapter.end =
                                lastChapter.start!! + chapterContent.toByteArray(charset).size
                            //创建当前章节
                            val curChapter = BookChapter()
                            curChapter.title = matcher.group()
                            curChapter.start = lastChapter.end
                            toc.add(curChapter)
                        } else { //如果章节不存在则创建章节
                            val curChapter = BookChapter()
                            curChapter.title = matcher.group()
                            curChapter.start = 0L
                            curChapter.end = 0L
                            toc.add(curChapter)
                        }
                    }
                }
            } else { //进行本地虚拟分章
                //章节在buffer的偏移量
                var chapterOffset = 0
                //当前剩余可分配的长度
                var strLength = length
                //分章的位置
                var chapterPos = 0
                while (strLength > 0) {
                    ++chapterPos
                    //是否长度超过一章
                    if (strLength > MAX_LENGTH_WITH_NO_CHAPTER) { //在buffer中一章的终止点
                        var end = length
                        //寻找换行符作为终止点
                        for (i in chapterOffset + MAX_LENGTH_WITH_NO_CHAPTER until length) {
                            if (buffer[i] == BLANK) {
                                end = i
                                break
                            }
                        }
                        val chapter = BookChapter()
                        chapter.title = "第${blockPos}章($chapterPos)"
                        chapter.start = curOffset + chapterOffset + 1
                        chapter.end = curOffset + end
                        toc.add(chapter)
                        //减去已经被分配的长度
                        strLength -= (end - chapterOffset)
                        //设置偏移的位置
                        chapterOffset = end
                    } else {
                        val chapter = BookChapter()
                        chapter.title = "第" + blockPos + "章" + "(" + chapterPos + ")"
                        chapter.start = curOffset + chapterOffset + 1
                        chapter.end = curOffset + length
                        toc.add(chapter)
                        strLength = 0
                    }
                }
            }

            //block的偏移点
            curOffset += length.toLong()

            if (rulePattern != null) { //设置上一章的结尾
                val lastChapter = toc.last()
                lastChapter.end = curOffset
            }

            //当添加的block太多的时候，执行GC
            if (blockPos % 15 == 0) {
                System.gc()
                System.runFinalization()
            }
        }
        bookStream.close()
        for (i in toc.indices) {
            val bean = toc[i]
            bean.index = i
            bean.bookUrl = book.bookUrl
            bean.url = (MD5Utils.md5Encode16(book.originName + i + bean.title) ?: "")
        }
        book.latestChapterTitle = toc.last().title

        System.gc()
        System.runFinalization()
        return toc
    }

    fun getContent(book: Book, bookChapter: BookChapter): String {
        val bookFile = getBookFile(App.INSTANCE, book)
        //获取文件流
        val bookStream = RandomAccessFile(bookFile, "r")
        bookStream.seek(bookChapter.start ?: 0)
        val extent = (bookChapter.end!! - bookChapter.start!!).toInt()
        val content = ByteArray(extent)
        bookStream.read(content, 0, extent)
        return String(content, book.fileCharset())
    }

    private fun getBookFile(context: Context, book: Book): File {
        if (book.bookUrl.isContentPath()) {
            val uri = Uri.parse(book.bookUrl)
            val bookFile = FileUtils.getFile(cacheFolder, book.originName, subDirs = *arrayOf())
            if (!bookFile.exists()) {
                bookFile.createNewFile()
                DocumentUtils.readBytes(context, uri)?.let {
                    bookFile.writeBytes(it)
                }
            }
            return bookFile
        }
        return File(book.bookUrl)
    }

    private fun getTocRule(book: Book, bookStream: RandomAccessFile, charset: Charset): Pattern? {
        if (book.tocUrl.isNotEmpty()) {
            return Pattern.compile(book.tocUrl, Pattern.MULTILINE)
        }
        val tocRules = getTocRules()
        var rulePattern: Pattern? = null
        //首先获取128k的数据
        val buffer = ByteArray(BUFFER_SIZE / 4)
        val length = bookStream.read(buffer, 0, buffer.size)
        val content = String(buffer, 0, length, charset)
        for (tocRule in tocRules) {
            val pattern = Pattern.compile(tocRule.rule, Pattern.MULTILINE)
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                book.tocUrl = tocRule.rule
                rulePattern = pattern
                break
            }
        }
        bookStream.seek(0)
        return rulePattern
    }

    private fun getTocRules(): List<TxtTocRule> {
        val rules = App.db.txtTocRule().all
        if (rules.isEmpty()) {
            return getDefaultRules()
        }
        return rules
    }

    fun getDefaultRules(): List<TxtTocRule> {
        App.INSTANCE.assets.open("txtTocRule.json").readBytes().let { byteArray ->
            GSON.fromJsonArray<TxtTocRule>(String(byteArray))?.let {
                App.db.txtTocRule().insert(*it.toTypedArray())
                return it
            }
        }
        return emptyList()
    }
}