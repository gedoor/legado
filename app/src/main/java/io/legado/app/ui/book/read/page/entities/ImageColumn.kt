package io.legado.app.ui.book.read.page.entities

data class ImageColumn(
    override var start: Float,
    override var end: Float,
    var src: String
) : BaseColumn