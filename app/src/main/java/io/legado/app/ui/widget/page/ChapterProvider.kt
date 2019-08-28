package io.legado.app.ui.widget.page

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import io.legado.app.App
import io.legado.app.data.entities.BookChapter
import io.legado.app.lib.theme.accentColor


object ChapterProvider {
    val readAloudSpan = ForegroundColorSpan(App.INSTANCE.accentColor)
    private val titleSpan = RelativeSizeSpan(1.2f)

    fun getTextChapter(
        textView: ContentTextView, bookChapter: BookChapter,
        content: String, chapterSize: Int
    ): TextChapter {
        val textPages = arrayListOf<TextPage>()
        val pageLengths = arrayListOf<Int>()
        var surplusText = content
        var pageIndex = 0
        while (surplusText.isNotEmpty()) {
            val spannableStringBuilder = SpannableStringBuilder(surplusText)
            if (pageIndex == 0) {
                val end = surplusText.indexOf("\n")
                if (end > 0) {
                    spannableStringBuilder.setSpan(
                        titleSpan,
                        0,
                        end,
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                }
            }
            textView.text = spannableStringBuilder
            val lastCharNum = textView.getCharNum()
            if (lastCharNum == 0) {
                break
            } else {
                pageLengths.add(lastCharNum)
                textPages.add(
                    TextPage(
                        index = pageIndex,
                        text = spannableStringBuilder.delete(
                            lastCharNum,
                            spannableStringBuilder.length
                        ),
                        title = bookChapter.title,
                        chapterSize = chapterSize,
                        chapterIndex = bookChapter.index
                    )
                )
                surplusText = surplusText.substring(lastCharNum)
                pageIndex++
            }
        }
        for (item in textPages) {
            item.pageSize = textPages.size
        }
        return TextChapter(
            bookChapter.index, bookChapter.title, bookChapter.url,
            textPages, pageLengths
        )
    }

}