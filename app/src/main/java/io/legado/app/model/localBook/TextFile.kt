package io.legado.app.model.localBook

import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.exception.EmptyFileException
import io.legado.app.help.DefaultData
import io.legado.app.utils.EncodingDetect
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.StringUtils
import io.legado.app.utils.Utf8BomUtils
import java.io.FileNotFoundException
import java.nio.charset.Charset
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.min

class TextFile(private var book: Book) {

    @Suppress("ConstPropertyName")
    companion object {
        private val padRegex = "^[\\n\\s]+".toRegex()
        private const val txtBufferSize = 8 * 1024 * 1024
        private var textFile: TextFile? = null

        @Synchronized
        private fun getTextFile(book: Book): TextFile {
            if (textFile == null || textFile?.book?.bookUrl != book.bookUrl) {
                textFile = TextFile(book)
                return textFile!!
            }
            textFile?.book = book
            return textFile!!
        }

        @Throws(FileNotFoundException::class)
        fun getChapterList(book: Book): ArrayList<BookChapter> {
            return getTextFile(book).getChapterList()
        }

        @Synchronized
        @Throws(FileNotFoundException::class)
        fun getContent(book: Book, bookChapter: BookChapter): String {
            return getTextFile(book).getContent(bookChapter)
        }

        fun clear() {
            textFile = null
        }

    }

    private val blank: Byte = 0x0a

    //默认从文件中获取数据的长度
    private val bufferSize = 512000

    //没有标题的时候，每个章节的最大长度
    private val maxLengthWithNoToc = 10 * 1024

    //使用正则划分目录，每个章节的最大允许长度
    private val maxLengthWithToc = 102400

    private var charset: Charset = book.fileCharset()

    private var txtBuffer: ByteArray? = null
    private var bufferStart = -1L
    private var bufferEnd = -1L

    /**
     * 获取目录
     */
    @Throws(FileNotFoundException::class, SecurityException::class, EmptyFileException::class)
    fun getChapterList(): ArrayList<BookChapter> {
        if (book.charset == null || book.tocUrl.isBlank()) {
            LocalBook.getBookInputStream(book).use { bis ->
                val buffer = ByteArray(bufferSize)
                val length = bis.read(buffer)
                if (length == -1) throw EmptyFileException("Unexpected Empty Txt File")
                if (book.charset.isNullOrBlank()) {
                    book.charset = EncodingDetect.getEncode(buffer.copyOf(length))
                }
                charset = book.fileCharset()
                if (book.tocUrl.isBlank()) {
                    val blockContent = String(buffer, 0, length, charset)
                    book.tocUrl = getTocRule(blockContent)?.pattern() ?: ""
                }
            }
        }
        val toc = analyze(book.tocUrl.toPattern(Pattern.MULTILINE))
        toc.forEachIndexed { index, bookChapter ->
            bookChapter.index = index
            bookChapter.bookUrl = book.bookUrl
            bookChapter.url = MD5Utils.md5Encode16(book.originName + index + bookChapter.title)
        }
        return toc
    }

    fun getContent(chapter: BookChapter): String {
        val start = chapter.start!!
        val end = chapter.end!!
        if (txtBuffer == null || start > bufferEnd || end < bufferStart) {
            LocalBook.getBookInputStream(book).use { bis ->
                bufferStart = txtBufferSize * (start / txtBufferSize)
                txtBuffer = ByteArray(min(txtBufferSize, bis.available() - bufferStart.toInt()))
                bufferEnd = bufferStart + txtBuffer!!.size
                bis.skip(bufferStart)
                bis.read(txtBuffer)
            }
        }

        val count = (end - start).toInt()
        val buffer = ByteArray(count)

        @Suppress("ConvertTwoComparisonsToRangeCheck")
        if (start < bufferEnd && end > bufferEnd || start < bufferStart && end > bufferStart) {
            /** 章节内容在缓冲区交界处 */
            LocalBook.getBookInputStream(book).use { bis ->
                bis.skip(start)
                bis.read(buffer)
            }
        } else {
            /** 章节内容在缓冲区内 */
            txtBuffer!!.copyInto(
                buffer,
                0,
                (start - bufferStart).toInt(),
                (end - bufferStart).toInt()
            )
        }

        return String(buffer, charset)
            .substringAfter(chapter.title)
            .replace(padRegex, "　　")
    }

    /**
     * 按规则解析目录
     */
    private fun analyze(pattern: Pattern?): ArrayList<BookChapter> {
        if (pattern?.pattern().isNullOrEmpty()) {
            return analyze()
        }
        pattern ?: return analyze()
        val toc = arrayListOf<BookChapter>()
        LocalBook.getBookInputStream(book).use { bis ->
            var blockContent: String
            //加载章节
            var curOffset: Long = 0
            //读取的长度
            var length: Int
            val buffer = ByteArray(bufferSize)
            var bufferStart = 3
            bis.read(buffer, 0, 3)
            if (Utf8BomUtils.hasBom(buffer)) {
                bufferStart = 0
                curOffset = 3
            }
            //获取文件中的数据到buffer，直到没有数据为止
            while (bis.read(
                    buffer, bufferStart, bufferSize - bufferStart
                ).also { length = it } > 0
            ) {
                var end = bufferStart + length
                if (end == bufferSize) {
                    for (i in bufferStart + length - 1 downTo 0) {
                        if (buffer[i] == blank) {
                            end = i
                            break
                        }
                    }
                }
                //将数据转换成String, 不能超过length
                blockContent = String(buffer, 0, end, charset)
                buffer.copyInto(buffer, 0, end, bufferStart + length)
                bufferStart = bufferStart + length - end
                length = end
                //当前Block下使过的String的指针
                var seekPos = 0
                //进行正则匹配
                val matcher: Matcher = pattern.matcher(blockContent)
                //如果存在相应章节
                while (matcher.find()) { //获取匹配到的字符在字符串中的起始位置
                    val chapterStart = matcher.start()
                    //获取章节内容
                    val chapterContent = blockContent.substring(seekPos, chapterStart)
                    val chapterLength = chapterContent.toByteArray(charset).size.toLong()
                    val lastStart = toc.lastOrNull()?.start ?: curOffset
                    if (book.getSplitLongChapter() && curOffset + chapterLength - lastStart > maxLengthWithToc) {
                        toc.lastOrNull()?.let {
                            it.end = it.start
                        }
                        //章节字数太多进行拆分
                        val lastTitle = toc.lastOrNull()?.title
                        val lastTitleLength = lastTitle?.toByteArray(charset)?.size ?: 0
                        val chapters = analyze(
                            lastStart + lastTitleLength, curOffset + chapterLength
                        )
                        lastTitle?.let {
                            chapters.forEachIndexed { index, bookChapter ->
                                bookChapter.title = "$lastTitle(${index + 1})"
                            }
                        }
                        toc.addAll(chapters)
                        //创建当前章节
                        val curChapter = BookChapter()
                        curChapter.title = matcher.group()
                        curChapter.start = curOffset + chapterLength
                        toc.add(curChapter)
                    } else if (seekPos == 0 && chapterStart != 0) {
                        /**
                         * 如果 seekPos == 0 && chapterStart != 0 表示当前block处前面有一段内容
                         * 第一种情况一定是序章 第二种情况是上一个章节的内容
                         */
                        if (toc.isEmpty()) { //如果当前没有章节，那么就是序章
                            //加入简介
                            if (StringUtils.trim(chapterContent).isNotEmpty()) {
                                val qyChapter = BookChapter()
                                qyChapter.title = "前言"
                                qyChapter.start = curOffset
                                qyChapter.end = curOffset + chapterLength
                                toc.add(qyChapter)
                                book.intro = if (chapterContent.length <= 500) {
                                    chapterContent
                                } else {
                                    chapterContent.substring(0, 500)
                                }
                            }
                            //创建当前章节
                            val curChapter = BookChapter()
                            curChapter.title = matcher.group()
                            curChapter.start = curOffset + chapterLength
                            toc.add(curChapter)
                        } else { //否则就block分割之后，上一个章节的剩余内容
                            //获取上一章节
                            val lastChapter = toc.last()
                            lastChapter.isVolume =
                                chapterContent.substringAfter(lastChapter.title).isBlank()
                            //将当前段落添加上一章去
                            lastChapter.end = lastChapter.end!! + chapterLength
                            //创建当前章节
                            val curChapter = BookChapter()
                            curChapter.title = matcher.group()
                            curChapter.start = lastChapter.end
                            toc.add(curChapter)
                        }
                    } else {
                        if (toc.isNotEmpty()) { //获取章节内容
                            //获取上一章节
                            val lastChapter = toc.last()
                            lastChapter.isVolume =
                                chapterContent.substringAfter(lastChapter.title).isBlank()
                            lastChapter.end =
                                lastChapter.start!! + chapterLength
                            //创建当前章节
                            val curChapter = BookChapter()
                            curChapter.title = matcher.group()
                            curChapter.start = lastChapter.end
                            toc.add(curChapter)
                        } else { //如果章节不存在则创建章节
                            val curChapter = BookChapter()
                            curChapter.title = matcher.group()
                            curChapter.start = curOffset
                            curChapter.end = curOffset
                            toc.add(curChapter)
                        }
                    }
                    //设置指针偏移
                    seekPos += chapterContent.length
                }
                //block的偏移点
                curOffset += length.toLong()
                //设置上一章的结尾
                toc.lastOrNull()?.end = curOffset

            }
            toc.lastOrNull()?.let { chapter ->
                //章节字数太多进行拆分
                if (book.getSplitLongChapter() && chapter.end!! - chapter.start!! > maxLengthWithToc) {
                    val end = chapter.end!!
                    chapter.end = chapter.start
                    val lastTitle = chapter.title
                    val lastTitleLength = lastTitle.toByteArray(charset).size
                    val chapters = analyze(
                        chapter.start!! + lastTitleLength, end
                    )
                    chapters.forEachIndexed { index, bookChapter ->
                        bookChapter.title = "$lastTitle(${index + 1})"
                    }
                    toc.addAll(chapters)
                }
            }
        }
        System.gc()
        System.runFinalization()
        return toc
    }

    /**
     * 无规则拆分目录
     */
    private fun analyze(
        fileStart: Long = 0L, fileEnd: Long = Long.MAX_VALUE
    ): ArrayList<BookChapter> {
        val toc = arrayListOf<BookChapter>()
        LocalBook.getBookInputStream(book).use { bis ->
            //block的个数
            var blockPos = 0
            //加载章节
            var curOffset: Long = 0
            var chapterPos = 0
            //读取的长度
            var length = 0
            val buffer = ByteArray(bufferSize)
            var bufferStart = 3
            if (fileStart == 0L) {
                bis.read(buffer, 0, 3)
                if (Utf8BomUtils.hasBom(buffer)) {
                    bufferStart = 0
                    curOffset = 3
                }
            } else {
                bis.skip(fileStart)
                curOffset = fileStart
                bufferStart = 0
            }
            //获取文件中的数据到buffer，直到没有数据为止
            while (fileEnd - curOffset - bufferStart > 0 && bis.read(
                    buffer, bufferStart, min(
                        (bufferSize - bufferStart).toLong(), fileEnd - curOffset - bufferStart
                    ).toInt()
                ).also { length = it } > 0
            ) {
                blockPos++
                //章节在buffer的偏移量
                var chapterOffset = 0
                //当前剩余可分配的长度
                length += bufferStart
                var strLength = length
                //分章的位置
                chapterPos = 0
                while (strLength > 0) {
                    chapterPos++
                    //是否长度超过一章
                    if (strLength > maxLengthWithNoToc) { //在buffer中一章的终止点
                        var end = length
                        //寻找换行符作为终止点
                        for (i in chapterOffset + maxLengthWithNoToc until length) {
                            if (buffer[i] == blank) {
                                end = i
                                break
                            }
                        }
                        val chapter = BookChapter()
                        chapter.title = "第${blockPos}章($chapterPos)"
                        chapter.start = toc.lastOrNull()?.end ?: curOffset
                        chapter.end = chapter.start!! + end - chapterOffset
                        toc.add(chapter)
                        //减去已经被分配的长度
                        strLength -= (end - chapterOffset)
                        //设置偏移的位置
                        chapterOffset = end
                    } else {
                        buffer.copyInto(buffer, 0, length - strLength, length)
                        length -= strLength
                        bufferStart = strLength
                        strLength = 0
                    }
                }
                //block的偏移点
                curOffset += length.toLong()
            }
            //设置结尾章节
            if (bufferStart > 100 || toc.isEmpty()) {
                val chapter = BookChapter()
                chapter.title = "第${blockPos}章(${chapterPos})"
                chapter.start = toc.lastOrNull()?.end ?: curOffset
                chapter.end = chapter.start!! + bufferStart
                toc.add(chapter)
            } else {
                toc.lastOrNull()?.let {
                    it.end = it.end!! + bufferStart
                }
            }
        }
        return toc
    }

    /**
     * 获取所有匹配次数大于1的目录规则
     */
    private fun getTocRule(content: String): Pattern? {
        val rules = getTocRules().reversed()
        var maxCs = 1
        var tocPattern: Pattern? = null
        for (tocRule in rules) {
            val pattern = tocRule.rule.toPattern(Pattern.MULTILINE)
            val matcher = pattern.matcher(content)
            var cs = 0
            while (matcher.find()) {
                cs++
            }
            if (cs >= maxCs) {
                maxCs = cs
                tocPattern = pattern
            }
        }
        return tocPattern
    }

    /**
     * 获取启用的目录规则
     */
    private fun getTocRules(): List<TxtTocRule> {
        var rules = appDb.txtTocRuleDao.enabled
        if (appDb.txtTocRuleDao.count == 0) {
            rules = DefaultData.txtTocRules.apply {
                appDb.txtTocRuleDao.insert(*this.toTypedArray())
            }.filter {
                it.enable
            }
        }
        return rules
    }

}