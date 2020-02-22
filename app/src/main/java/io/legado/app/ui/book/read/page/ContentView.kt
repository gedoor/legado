package io.legado.app.ui.book.read.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.widget.FrameLayout
import io.legado.app.R
import io.legado.app.constant.AppConst.TIME_FORMAT
import io.legado.app.constant.PreferKey
import io.legado.app.help.ReadBookConfig
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.view_book_page.view.*
import java.util.*


class ContentView(context: Context) : FrameLayout(context) {
    var callBack: CallBack? = null
    private var pageSize: Int = 0

    init {
        //设置背景防止切换背景时文字重叠
        setBackgroundColor(context.getCompatColor(R.color.background))
        inflate(context, R.layout.view_book_page, this)
        upStyle()
        upTime()
    }

    fun upStyle() {
        ReadBookConfig.durConfig.apply {
            tv_top_left.typeface = ChapterProvider.typeface
            tv_top_right.typeface = ChapterProvider.typeface
            tv_bottom_left.typeface = ChapterProvider.typeface
            tv_bottom_right.typeface = ChapterProvider.typeface
            if (context.getPrefBoolean(PreferKey.hideStatusBar, false)) {
                //显示状态栏时隐藏header
                ll_header.visible()
                ll_header.layoutParams =
                    ll_header.layoutParams.apply { height = context.getStatusBarHeight() }
                ll_header.setPadding(
                    headerPaddingLeft.dp,
                    headerPaddingTop.dp,
                    headerPaddingRight.dp,
                    headerPaddingBottom.dp
                )
                content_text_view.headerHeight = ll_header.height
                page_panel.setPadding(0, 0, 0, 0)
            } else {
                ll_header.gone()
                content_text_view.headerHeight = context.getStatusBarHeight()
                page_panel.setPadding(0, content_text_view.headerHeight, 0, 0)
            }
            content_text_view.setPadding(
                paddingLeft.dp,
                paddingTop.dp,
                paddingRight.dp,
                paddingBottom.dp
            )
            ll_footer.setPadding(
                footerPaddingLeft.dp,
                footerPaddingTop.dp,
                footerPaddingRight.dp,
                footerPaddingBottom.dp
            )
            textColor().let {
                tv_top_left.setTextColor(it)
                tv_top_right.setTextColor(it)
                tv_bottom_left.setTextColor(it)
                tv_bottom_right.setTextColor(it)
            }
        }
    }

    fun setBg(bg: Drawable?) {
        page_panel.background = bg
    }

    fun upTime() {
        tv_top_left.text = TIME_FORMAT.format(Date(System.currentTimeMillis()))
    }

    fun upBattery(battery: Int) {
        tv_top_right.text = context.getString(R.string.battery_show, battery)
    }

    fun setContent(textPage: TextPage?) {
        content_text_view.setContent(textPage)
        if (textPage != null) {
            tv_bottom_left.text = textPage.title
            pageSize = textPage.pageSize
            setPageIndex(textPage.index)
        }
    }

    @SuppressLint("SetTextI18n")
    fun setPageIndex(pageIndex: Int?) {
        pageIndex?.let {
            tv_bottom_right.text = "${pageIndex.plus(1)}/${pageSize}"
        }
    }

    fun upSelectAble(selectAble: Boolean) {
        content_text_view.selectAble = selectAble
    }

    fun selectText(e: MotionEvent): Boolean {
        val y = e.y - content_text_view.headerHeight
        return content_text_view.selectText(e.x, y)
    }

    fun selectStartMove(x: Float, y: Float) {
        content_text_view.selectStartMove(x, y - content_text_view.headerHeight)
    }

    fun selectEndMove(x: Float, y: Float) {
        content_text_view.selectEndMove(x, y - content_text_view.headerHeight)
    }

    fun cancelSelect() {
        content_text_view.cancelSelect()
    }

    val selectedText: String get() = content_text_view.selectedText

    fun scrollTo(pos: Int?) {
        if (pos != null) {
            content_text_view.post {

            }
        }
    }

    fun scrollToBottom() {
        content_text_view.post {

        }
    }

    interface CallBack {
        fun scrollToLine(line: Int)
        fun scrollToLast()
    }
}