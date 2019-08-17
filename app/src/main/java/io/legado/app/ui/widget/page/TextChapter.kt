package io.legado.app.ui.widget.page

data class TextChapter(
    val position: Int,
    val title: String,
    val pages: List<TextPage>
) {
    fun page(index: Int): TextPage? {
        if (index >= 0 && index < pages.size) {
            return pages[index]
        }
        return null
    }

    fun pageSize(): Int {
        return pages.size
    }
}

