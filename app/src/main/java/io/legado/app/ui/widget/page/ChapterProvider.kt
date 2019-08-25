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

    @Synchronized
    fun getTextChapter(textView: ContentTextView, bookChapter: BookChapter, content: String): TextChapter {
        val textPages = arrayListOf<TextPage>()
        val pageLengths = arrayListOf<Int>()
        var surplusText = content
        var pageIndex = 0
        while (surplusText.isNotEmpty()) {
            val spannableStringBuilder = SpannableStringBuilder(surplusText)
            if (pageIndex == 0) {
                spannableStringBuilder.setSpan(
                    titleSpan,
                    0,
                    surplusText.indexOf("\n"),
                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }
            textView.text = spannableStringBuilder
            pageLengths.add(textView.getCharNum())
            textPages.add(
                TextPage(
                    pageIndex,
                    spannableStringBuilder.delete(
                        pageLengths[pageIndex],
                        spannableStringBuilder.length
                    ),
                    bookChapter.title
                )
            )
            surplusText = surplusText.substring(pageLengths[pageIndex])

            pageIndex++
        }
        return TextChapter(bookChapter.index, bookChapter.title, textPages, pageLengths)
    }


}