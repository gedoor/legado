package io.legado.app.ui.widget.page

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import io.legado.app.R


class ContentView : FrameLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        inflate(context, R.layout.view_book_page, this)
    }
}