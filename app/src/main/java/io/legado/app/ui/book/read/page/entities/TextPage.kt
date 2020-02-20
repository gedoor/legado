package io.legado.app.ui.book.read.page.entities

import android.text.Spannable
import android.text.SpannableStringBuilder
import io.legado.app.App
import io.legado.app.R
import io.legado.app.ui.book.read.page.ChapterProvider

data class TextPage(
    val index: Int,
    val text: CharSequence = App.INSTANCE.getString(R.string.data_loading),
    val title: String,
    var pageSize: Int = 0,
    var chapterSize: Int = 0,
    var chapterIndex: Int = 0
) {

    fun removePageAloudSpan(): TextPage {
        if (text is SpannableStringBuilder) {
            text.removeSpan(ChapterProvider.readAloudSpan)
        }
        return this
    }

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
