package io.legado.app.ui.widget.page

import android.text.Spannable
import android.text.SpannableStringBuilder

data class TextPage(
    val index: Int,
    val text: CharSequence,
    val title: String,
    var pageSize: Int = 0,
    var chapterSize: Int = 0,
    var chapterIndex: Int = 0
) {

    fun upPageAloudSpan(pageStart: Int) {
        if (text is SpannableStringBuilder) {
            text.removeSpan(ChapterProvider.readAloudSpan)
            var end = text.indexOf("\n", pageStart)
            if (end == -1) end = text.length
            var start = text.lastIndexOf("\n", pageStart)
            if (start == -1) start = 0
            text.setSpan(
                ChapterProvider.readAloudSpan,
                start,
                end,
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }
    }
}
