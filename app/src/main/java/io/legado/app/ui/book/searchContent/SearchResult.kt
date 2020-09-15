package io.legado.app.ui.book.searchContent

import android.text.Spanned
import androidx.core.text.HtmlCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.hexString

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
    val presentText: String
        get(){
            return colorPresentText(newPosition, query, text) +
                    "<font color=#0000ff>($chapterTitle)</font>"
        }

    fun colorPresentText(position: Int, center: String, targetText: String): String {
        val sub1 = text.substring(0, position)
        val sub2 = text.substring(position + center.length, targetText.length)
        val textColor = App.INSTANCE.getCompatColor(R.color.primaryText).hexString
        return "<font color=#${textColor}>$sub1</font>" +
                "<font color=#ff0000>$center</font>" +
                "<font color=#${textColor}>$sub2</font>"
    }

    fun parseText(targetText: String): Spanned {
        return HtmlCompat.fromHtml(targetText, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }



}