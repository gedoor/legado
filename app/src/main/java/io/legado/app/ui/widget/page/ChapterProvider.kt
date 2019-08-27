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
    private val titleSpan = RelativeSizeSpan(1.3f)

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
                var end = surplusText.indexOf("\n")
                if (end == -1) end = surplusText.length.minus(1)
                spannableStringBuilder.setSpan(
                    titleSpan,
                    0,
                    end,
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }
            textView.text = spannableStringBuilder
            pageLengths.add(textView.getCharNum())
            textPages.add(
                TextPage(
                    index = pageIndex,
                    text = spannableStringBuilder.delete(
                        pageLengths[pageIndex],
                        spannableStringBuilder.length
                    ),
                    title = bookChapter.title,
                    chapterSize = chapterSize,
                    chapterIndex = bookChapter.index
                )
            )
            surplusText = surplusText.substring(pageLengths[pageIndex])

            pageIndex++
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