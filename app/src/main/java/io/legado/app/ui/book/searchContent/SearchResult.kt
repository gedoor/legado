package io.legado.app.ui.book.searchContent

import android.util.Log
import io.legado.app.ui.book.read.page.entities.TextPage

data class SearchResult(
    var index: Int = 0,
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
            val sub1 = text.substring(0, newPosition)
            val sub2 = text.substring(newPosition + query.length, text.length)
            return "<font color=#000000>$sub1</font>" +
                    "<font color=#ff0000>$query</font>" +
                    "<font color=#000000>$sub2</font>" +
                    "<font color=#0000ff>($chapterTitle)</font>"
        }


}