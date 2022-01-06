package io.legado.app.model.localBook

import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.help.DefaultData
import io.legado.app.utils.EncodingDetect
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.StringUtils
import java.io.FileNotFoundException
import java.nio.charset.Charset
import java.util.regex.Matcher
import java.util.regex.Pattern

class TextFile(private val book: Book) {

    private val tocRules = arrayListOf<TxtTocRule>()
    private var charset: Charset = book.fileCharset()

    @Throws(FileNotFoundException::class)
    fun getChapterList(): ArrayList<BookChapter> {
        var rulePattern: Pattern? = null
        if (book.charset == null || book.tocUrl.isNotEmpty()) {
            LocalBook.getBookInputStream(book).use { bis ->
                val buffer = ByteArray(BUFFER_SIZE)
                var blockContent: String
                bis.read(buffer)
                book.charset = EncodingDetect.getEncode(buffer)
                charset = book.fileCharset()
                blockContent = String(buffer, charset)
                rulePattern = if (book.tocUrl.isNotEmpty()) {
                    Pattern.compile(book.tocUrl, Pattern.MULTILINE)
                } else {
                    if (blockContent.isEmpty()) {
                        bis.read(buffer)
                        book.charset = EncodingDetect.getEncode(buffer)
                        blockContent = String(buffer, charset)
                    }
                    getTocRule(blockContent)?.let {
                        Pattern.compile(it.rule, Pattern.MULTILINE)
                    }
                }
            }
        }
        return analyze(rulePattern)
    }

    private fun analyze(pattern: Pattern?): ArrayList<BookChapter> {
        val toc = arrayListOf<BookChapter>()
        LocalBook.getBookInputStream(book).use { bis ->
            var blockContent: String
            //加载章节
            var curOffset: Long = 0
            //block的个数
            var blockPos = 0
            //读取的长度
            var length: Int
            val buffer = ByteArray(BUFFER_SIZE)
            var bufferStart = 0
            //获取文件中的数据到buffer，直到没有数据为止
            while (bis.read(buffer, bufferStart, BUFFER_SIZE - bufferStart)
                    .also { length = it } > 0
            ) {
                blockPos++
                //如果存在Chapter
                if (pattern != null) {
                    //将数据转换成String, 不能超过length
                    blockContent = String(buffer, 0, bufferStart + length, charset)
                    val lastN = blockContent.lastIndexOf("\n")
                    if (lastN > 0) {
                        blockContent = blockContent.substring(0, lastN)
                        val blockContentSize = blockContent.toByteArray(charset).size
                        buffer.copyInto(buffer, 0, blockContentSize - bufferStart, length)
                        bufferStart = length + bufferStart - blockContentSize
                        length = blockContentSize
                    }
                    //当前Block下使过的String的指针
                    var seekPos = 0
                    //进行正则匹配
                    val matcher: Matcher = pattern.matcher(blockContent)
                    //如果存在相应章节
                    while (matcher.find()) { //获取匹配到的字符在字符串中的起始位置
                        val chapterStart = matcher.start()
                        //获取章节内容
                        val chapterContent = blockContent.substring(seekPos, chapterStart)
                        val chapterLength = chapterContent.toByteArray(charset).size
                        val lastStart = toc.lastOrNull()?.start ?: 0
                        if (curOffset + chapterLength - lastStart > 50000) {
                            bis.close()
                            //移除不匹配的规则
                            tocRules.removeFirstOrNull()
                            return analyze(tocRules.firstOrNull()?.rule?.toPattern(Pattern.MULTILINE))
                        }
                        //如果 seekPos == 0 && nextChapterPos != 0 表示当前block处前面有一段内容
                        //第一种情况一定是序章 第二种情况是上一个章节的内容
                        if (seekPos == 0 && chapterStart != 0) { //获取当前章节的内容
                            if (toc.isEmpty()) { //如果当前没有章节，那么就是序章
                                //加入简介
                                if (StringUtils.trim(chapterContent).isNotEmpty()) {
                                    val qyChapter = BookChapter()
                                    qyChapter.title = "前言"
                                    qyChapter.start = 0
                                    qyChapter.end = chapterLength.toLong()
                                    toc.add(qyChapter)
                                }
                                //创建当前章节
                                val curChapter = BookChapter()
                                curChapter.title = matcher.group()
                                curChapter.start = chapterLength.toLong()
                                toc.add(curChapter)
                            } else { //否则就block分割之后，上一个章节的剩余内容
                                //获取上一章节
                                val lastChapter = toc.last()
                                //将当前段落添加上一章去
                                lastChapter.end =
                                    lastChapter.end!! + chapterLength.toLong()
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
                                lastChapter.end =
                                    lastChapter.start!! + chapterContent.toByteArray(charset).size.toLong()
                                //创建当前章节
                                val curChapter = BookChapter()
                                curChapter.title = matcher.group()
                                curChapter.start = lastChapter.end
                                toc.add(curChapter)
                            } else { //如果章节不存在则创建章节
                                val curChapter = BookChapter()
                                curChapter.title = matcher.group()
                                curChapter.start = 0
                                curChapter.end = 0
                                toc.add(curChapter)
                            }
                        }
                        //设置指针偏移
                        seekPos += chapterContent.length
                    }
                    if (seekPos == 0 && length > 50000) {
                        bis.close()
                        //移除不匹配的规则
                        tocRules.remove(tocRules.removeFirstOrNull())
                        return analyze(tocRules.firstOrNull()?.rule?.toPattern(Pattern.MULTILINE))
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

                if (pattern != null) {
                    //设置上一章的结尾
                    val lastChapter = toc.last()
                    lastChapter.end = curOffset
                }

                //当添加的block太多的时候，执行GC
                if (blockPos % 15 == 0) {
                    System.gc()
                    System.runFinalization()
                }
            }
        }
        for (i in toc.indices) {
            val bean = toc[i]
            bean.index = i
            bean.bookUrl = book.bookUrl
            bean.url = (MD5Utils.md5Encode16(book.originName + i + bean.title))
        }
        book.latestChapterTitle = toc.last().title
        book.totalChapterNum = toc.size

        System.gc()
        System.runFinalization()
        book.tocUrl = pattern?.pattern() ?: ""
        book.save()
        return toc
    }

    /**
     * 获取匹配次数最多的目录规则
     */
    private fun getTocRule(content: String): TxtTocRule? {
        tocRules.clear()
        val rules = getTocRules().reversed()
        var txtTocRule: TxtTocRule? = null
        var maxCs = 0
        for (tocRule in rules) {
            val pattern = Pattern.compile(tocRule.rule, Pattern.MULTILINE)
            val matcher = pattern.matcher(content)
            var cs = 0
            while (matcher.find()) {
                cs++
            }
            if (cs >= maxCs) {
                tocRules.add(0, tocRule)
                maxCs = cs
                txtTocRule = tocRule
            } else if (cs > 0) {
                tocRules.add(tocRule)
            }
        }
        return txtTocRule
    }

    companion object {

        private const val BLANK: Byte = 0x0a

        //默认从文件中获取数据的长度
        private const val BUFFER_SIZE = 512 * 1024

        //没有标题的时候，每个章节的最大长度
        private const val MAX_LENGTH_WITH_NO_CHAPTER = 10 * 1024

        @Throws(FileNotFoundException::class)
        fun getChapterList(book: Book): ArrayList<BookChapter> {
            return TextFile(book).getChapterList()
        }

        @Throws(FileNotFoundException::class)
        fun getContent(book: Book, bookChapter: BookChapter): String {
            val count = (bookChapter.end!! - bookChapter.start!!).toInt()
            val buffer = ByteArray(count)
            LocalBook.getBookInputStream(book).use { bis ->
                bis.skip(bookChapter.start!!)
                bis.read(buffer)
            }
            return String(buffer, book.fileCharset())
                .substringAfter(bookChapter.title)
                .replace("^[\\n\\s]+".toRegex(), "　　")
        }

        private fun getTocRules(): List<TxtTocRule> {
            var rules = appDb.txtTocRuleDao.enabled
            if (rules.isEmpty()) {
                rules = DefaultData.txtTocRules.apply {
                    appDb.txtTocRuleDao.insert(*this.toTypedArray())
                }.filter {
                    it.enable
                }
            }
            return rules
        }

    }

}