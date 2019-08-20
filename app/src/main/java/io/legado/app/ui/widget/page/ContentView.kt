package io.legado.app.ui.widget.page

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import io.legado.app.R
import io.legado.app.help.ImageLoader
import io.legado.app.utils.dp
import kotlinx.android.synthetic.main.view_book_page.view.*
import org.jetbrains.anko.horizontalPadding
import org.jetbrains.anko.matchParent


class ContentView : FrameLayout {

    private val bgImage: AppCompatImageView = AppCompatImageView(context)

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        addView(bgImage, LayoutParams(matchParent, matchParent))
        inflate(context, R.layout.view_book_page, this)
        upStyle()
    }

    fun upStyle() {
        page_panel.horizontalPadding = 16.dp
        ImageLoader.load(context, R.drawable.bg1)
            .centerCrop()
            .setAsDrawable(bgImage)
    }

    fun upTime() {

    }

    fun upBattery(battery: Int) {

    }

    fun setContent(text: CharSequence?) {
        content_text_view.text = text
    }
}