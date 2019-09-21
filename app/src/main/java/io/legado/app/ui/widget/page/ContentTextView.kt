package io.legado.app.ui.widget.page

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView


class ContentTextView : AppCompatTextView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    /**
     * 获取当前页总字数
     */
    fun getCharNum(lineNum: Int = getLineNum()): Int {
        return layout?.getLineEnd(lineNum) ?: 0
    }

    /**
     * 获取当前页总行数
     */
    fun getLineNum(): Int {
        val topOfLastLine = height - paddingTop - paddingBottom - lineHeight
        return layout?.getLineForVertical(topOfLastLine) ?: 0
    }
}
