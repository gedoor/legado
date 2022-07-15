package io.legado.app.ui.book.searchContent

import android.text.Spanned
import androidx.core.text.HtmlCompat

data class SearchResult(
    val resultCount: Int = 0,
    val resultCountWithinChapter: Int = 0,
    val resultText: String = "",
    val chapterTitle: String = "",
    val query: String = "",
    val pageSize: Int = 0,
    val chapterIndex: Int = 0,
    val pageIndex: Int = 0,
    val queryIndexInResult: Int = 0,
    val queryIndexInChapter: Int = 0
) {

    fun getHtmlCompat(textColor: String, accentColor: String): Spanned {
        return if (query.isNotBlank()) {
            val queryIndexInSurrounding = resultText.indexOf(query)
            val leftString = resultText.substring(0, queryIndexInSurrounding)
            val rightString =
                resultText.substring(queryIndexInSurrounding + query.length, resultText.length)
            val html = buildString {
                append(chapterTitle.colorTextForHtml(accentColor))
                append("<br>")
                append(leftString.colorTextForHtml(textColor))
                append(query.colorTextForHtml(accentColor))
                append(rightString.colorTextForHtml(textColor))
            }
            HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
        } else {
            HtmlCompat.fromHtml(
                resultText.colorTextForHtml(textColor),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }
    }

    private fun String.colorTextForHtml(textColor: String) =
        "<font color=#${textColor}>$this</font>"

}