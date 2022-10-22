package io.legado.app.ui.book.read.page.entities.column

import com.android.tools.r8.Keep

/**
 * 图片列
 */
@Keep
data class ImageColumn(
    override var start: Float,
    override var end: Float,
    var src: String
) : BaseColumn