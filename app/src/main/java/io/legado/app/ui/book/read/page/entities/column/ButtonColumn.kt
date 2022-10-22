package io.legado.app.ui.book.read.page.entities.column

import com.android.tools.r8.Keep


/**
 * 按钮列
 */
@Keep
data class ButtonColumn(
    override var start: Float,
    override var end: Float
) : BaseColumn