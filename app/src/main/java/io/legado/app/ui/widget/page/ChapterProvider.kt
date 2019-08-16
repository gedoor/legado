package io.legado.app.ui.widget.page

import android.widget.TextView


class ChapterProvider {


    fun getTextChapter(textView: TextView, content: String) {

        textView.text = content
        val layout = textView.layout
        val topOfLastLine = textView.height - textView.paddingTop - textView.paddingBottom - textView.lineHeight
        val lineNum = layout.getLineForVertical(topOfLastLine)
        val lastCharNum = layout.getLineEnd(lineNum)
    }


}