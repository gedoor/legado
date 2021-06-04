package io.legado.app.ui.book.read.page.entities

import io.legado.app.data.entities.DrawLineEntity
import io.legado.app.data.entities.IdealEntity

data class TextChar(
    val charData: String,
    var start: Float,
    var end: Float,
    var selected: Boolean = false,
    var isImage: Boolean = false,
    // 是否是首行缩进字符
    var isIndent: Boolean = false,
    // 划线
    var drawLineEntity: DrawLineEntity? = null,
    // 想法
    var idealEntity: IdealEntity? = null
)