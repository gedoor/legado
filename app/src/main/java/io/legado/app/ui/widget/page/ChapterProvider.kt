package io.legado.app.ui.widget.page

import io.legado.app.data.entities.BookChapter


object ChapterProvider {

    @Synchronized
    fun getTextChapter(textView: ContentTextView, bookChapter: BookChapter, content: String): TextChapter {
        val textPages = arrayListOf<TextPage>()
        var surplusText = content
        var pageIndex = 0
        while (surplusText.isNotEmpty()) {
            textView.text = surplusText
            textPages.add(TextPage(pageIndex, surplusText.substring(0, textView.getCharNum())))
            surplusText = surplusText.substring(textView.getCharNum())
            pageIndex++
        }
        return TextChapter(bookChapter.index, textPages)
    }


}