package io.legado.app.ui.book.searchContent

import android.text.Spanned
import androidx.core.text.HtmlCompat

data class SearchResult(
    val resultCount: Int = 0,
    val resultCountWithinChapter: Int = 0,
    val resultText: String = "",
    val chapterTitle: String = "",
    val query: String,
    val pageSize: Int = 0,
    val chapterIndex: Int = 0,
    val pageIndex: Int = 0,
    val queryIndexInResult: Int = 0,
    val contentPosition: Int = 0
) {

    fun getHtmlCompat(textColor: String, accentColor: String): Spanned {
        val queryIndexInSurrounding = resultText.indexOf(query)
        val leftString = resultText.substring(0, queryIndexInSurrounding)
        val rightString = resultText.substring(queryIndexInSurrounding + query.length, resultText.length)
        val html = leftString.colorTextForHtml(textColor) +
                query.colorTextForHtml(accentColor) +
                rightString.colorTextForHtml(textColor) +
                chapterTitle.colorTextForHtml(accentColor)
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    private fun String.colorTextForHtml(textColor: String) = "<font color=#${textColor}>$this</font>"

}