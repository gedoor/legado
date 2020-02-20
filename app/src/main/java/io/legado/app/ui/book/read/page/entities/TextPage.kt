package io.legado.app.ui.book.read.page.entities

import android.text.SpannableStringBuilder
import io.legado.app.App
import io.legado.app.R

data class TextPage(
    var index: Int = 0,
    var text: CharSequence = App.INSTANCE.getString(R.string.data_loading),
    var title: String = "",
    val textLines: ArrayList<TextLine> = arrayListOf(),
    var pageSize: Int = 0,
    var chapterSize: Int = 0,
    var chapterIndex: Int = 0
) {

    fun removePageAloudSpan(): TextPage {
        textLines.forEach { textLine ->
            textLine.isReadAloud = false
        }
        return this
    }

    fun upPageAloudSpan(pageStart: Int) {
        if (text is SpannableStringBuilder) {
            removePageAloudSpan()
            for (textLine in textLines) {

            }
        }
    }
}
