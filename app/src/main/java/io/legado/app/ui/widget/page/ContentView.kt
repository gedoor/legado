package io.legado.app.ui.widget.page

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import io.legado.app.R
import io.legado.app.constant.AppConst.TIME_FORMAT
import io.legado.app.help.ImageLoader
import io.legado.app.help.ReadBookConfig
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.view_book_page.view.*
import org.jetbrains.anko.matchParent
import java.util.*


class ContentView : FrameLayout {

    private val bgImage: AppCompatImageView = AppCompatImageView(context)
        .apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        addView(bgImage, LayoutParams(matchParent, matchParent))
        inflate(context, R.layout.view_book_page, this)
        upStyle()
        upTime()
    }

    fun upStyle() {
        ReadBookConfig.getConfig().apply {
            val pt = if (context.getPrefBoolean("hideStatusBar", false)) {
                top_bar.visible()
                paddingTop.dp
            } else {
                top_bar.gone()
                paddingTop.dp + context.getStatusBarHeight()
            }
            page_panel.setPadding(paddingLeft.dp, pt, paddingRight.dp, paddingBottom.dp)
            content_text_view.textSize = textSize.toFloat()
            content_text_view.setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
            content_text_view.letterSpacing = letterSpacing
            content_text_view.paint.isFakeBoldText = textBold
            textColor().let {
                content_text_view.setTextColor(it)
                tv_top_left.setTextColor(it)
                tv_top_right.setTextColor(it)
                tv_bottom_left.setTextColor(it)
                tv_bottom_right.setTextColor(it)
            }
        }
    }

    fun setBg(bg: Drawable?) {
        //all supported
        ImageLoader.load(context, bg)
            .centerCrop()
            .setAsDrawable(bgImage)
    }

    fun upTime() {
        tv_top_left.text = TIME_FORMAT.format(Date(System.currentTimeMillis()))
    }

    fun upBattery(battery: Int) {
        tv_top_right.text = context.getString(R.string.battery_show, battery)
    }

    fun setChapterTile(tile: String) {
        tv_bottom_left.text = tile
    }

    fun setContent(page: TextPage?) {
        content_text_view.text = page?.text

        tv_bottom_right.text = page?.index?.toString()
    }
}