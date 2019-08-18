package io.legado.app.ui.widget.page

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import io.legado.app.R
import io.legado.app.utils.dp
import kotlinx.android.synthetic.main.view_book_page.view.*
import org.jetbrains.anko.horizontalPadding


class ContentView : FrameLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        inflate(context, R.layout.view_book_page, this)

        upStyle()
    }

    fun upStyle() {
        page_panel.horizontalPadding = 16.dp
    }

    fun upTime() {

    }

    fun upBattery(battery: Int) {

    }

    fun setContent(text: CharSequence?) {
        content_text_view.text = text
    }
}