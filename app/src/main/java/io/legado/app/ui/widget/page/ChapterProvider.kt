package io.legado.app.ui.widget.page

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import io.legado.app.data.entities.BookChapter


object ChapterProvider {

    @Synchronized
    fun getTextChapter(textView: ContentTextView, bookChapter: BookChapter, content: String): TextChapter {
        val textPages = arrayListOf<TextPage>()
        val pageLengths = arrayListOf<Int>()
        var surplusText = content
        var pageIndex = 0
        while (surplusText.isNotEmpty()) {
            val spannableStringBuilder = SpannableStringBuilder(surplusText)
            if (pageIndex == 0) {
                val span = RelativeSizeSpan(1.5f)
                spannableStringBuilder.setSpan(span, 0, surplusText.indexOf("\n"), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            }
            textView.text = spannableStringBuilder
            pageLengths.add(textView.getCharNum())
            textPages.add(
                TextPage(
                    pageIndex,
                    spannableStringBuilder.delete(pageLengths[pageIndex], spannableStringBuilder.length)
                )
            )
            surplusText = surplusText.substring(pageLengths[pageIndex])

            pageIndex++
        }
        return TextChapter(bookChapter.index, bookChapter.title, textPages, pageLengths)
    }


}