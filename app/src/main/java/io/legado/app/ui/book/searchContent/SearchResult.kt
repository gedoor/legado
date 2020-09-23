package io.legado.app.ui.book.searchContent

import android.text.Spanned
import androidx.core.text.HtmlCompat

data class SearchResult(
    var index: Int = 0,
    var indexWithinChapter: Int = 0,
    var text: String = "",
    var chapterTitle: String = "",
    val query: String,
    var pageSize: Int = 0,
    var chapterIndex: Int = 0,
    var pageIndex: Int = 0,
    var newPosition: Int = 0,
    var contentPosition: Int =0
) {

    fun getHtmlCompat(textColor: String, accentColor: String): Spanned {
        val html = colorPresentText(newPosition, query, text, textColor, accentColor) +
                "<font color=#${accentColor}>($chapterTitle)</font>"
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    private fun colorPresentText(
        position: Int,
        center: String,
        targetText: String,
        textColor: String,
        accentColor: String
    ): String {
        val sub1 = text.substring(0, position)
        val sub2 = text.substring(position + center.length, targetText.length)
        return "<font color=#${textColor}>$sub1</font>" +
                "<font color=#${accentColor}>$center</font>" +
                "<font color=#${textColor}>$sub2</font>"
    }

}