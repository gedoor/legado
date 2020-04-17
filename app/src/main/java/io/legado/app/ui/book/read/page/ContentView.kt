package io.legado.app.ui.book.read.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.widget.FrameLayout
import com.hankcs.hanlp.HanLP
import io.legado.app.R
import io.legado.app.constant.AppConst.timeFormat
import io.legado.app.help.AppConfig
import io.legado.app.help.ReadBookConfig
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.view_book_page.view.*
import java.util.*


class ContentView(context: Context) : FrameLayout(context) {

    private var battery = 100

    init {
        //设置背景防止切换背景时文字重叠
        setBackgroundColor(context.getCompatColor(R.color.background))
        inflate(context, R.layout.view_book_page, this)

        upStyle()
        upTime()
        content_text_view.upView = {
            setProgress(it)
        }
    }

    fun upStyle() {
        ReadBookConfig.apply {
            tv_top_left.typeface = ChapterProvider.typeface
            tv_top_right.typeface = ChapterProvider.typeface
            tv_bottom_left.typeface = ChapterProvider.typeface
            tv_bottom_right.typeface = ChapterProvider.typeface
            battery_view.typeface = ChapterProvider.typeface
            //显示状态栏时隐藏header
            if (hideStatusBar) {
                ll_header.setPadding(
                    headerPaddingLeft.dp,
                    headerPaddingTop.dp,
                    headerPaddingRight.dp,
                    headerPaddingBottom.dp
                )
                ll_header.visible()
                page_panel.setPadding(0, 0, 0, 0)
            } else {
                ll_header.gone()
                page_panel.setPadding(0, context.statusBarHeight, 0, 0)
            }
            ll_footer.setPadding(
                footerPaddingLeft.dp,
                footerPaddingTop.dp,
                footerPaddingRight.dp,
                footerPaddingBottom.dp
            )
            vw_top_divider.visible(showHeaderLine)
            vw_bottom_divider.visible(showFooterLine)
            content_text_view.upVisibleRect()
            durConfig.textColor().let {
                tv_top_left.setTextColor(it)
                tv_top_right.setTextColor(it)
                tv_bottom_left.setTextColor(it)
                tv_bottom_right.setTextColor(it)
                battery_view.setColor(it)
            }
            if (hideStatusBar) {
                tv_bottom_left.text = timeFormat.format(Date(System.currentTimeMillis()))
                battery_view.visible()
                battery_view.setBattery(battery)
            } else {
                battery_view.gone()
            }
        }
    }

    val headerHeight: Int
        get() {
            return if (ReadBookConfig.hideStatusBar) {
                ll_header.height
            } else {
                context.statusBarHeight
            }
        }

    fun setBg(bg: Drawable?) {
        page_panel.background = bg
    }

    fun upTime() {
        if (ReadBookConfig.hideStatusBar) {
            tv_bottom_right.text = timeFormat.format(Date(System.currentTimeMillis()))
        }
    }

    fun upBattery(battery: Int) {
        this.battery = battery
        if (ReadBookConfig.hideStatusBar) {
            battery_view.setBattery(battery)
        }
    }

    fun setContent(textPage: TextPage, resetPageOffset: Boolean = true) {
        setProgress(textPage)
        if (resetPageOffset)
            resetPageOffset()
        content_text_view.setContent(textPage)
    }

    fun resetPageOffset() {
        content_text_view.resetPageOffset()
    }

    @SuppressLint("SetTextI18n")
    fun setProgress(textPage: TextPage) = textPage.apply {
        val title = when (AppConfig.chineseConverterType) {
            1 -> HanLP.convertToSimplifiedChinese(textPage.title)
            2 -> HanLP.convertToTraditionalChinese(textPage.title)
            else -> textPage.title
        }
        if (ReadBookConfig.hideStatusBar) {
            tv_top_left.text = title
            tv_top_right.text = readProgress
            tv_bottom_left.text = "${index.plus(1)}/$pageSize"
        } else {
            tv_bottom_left.text = title
            tv_bottom_right.text = "${index.plus(1)}/$pageSize  $readProgress"
        }
    }

    fun onScroll(offset: Float) {
        content_text_view.onScroll(offset)
    }

    fun upSelectAble(selectAble: Boolean) {
        content_text_view.selectAble = selectAble
    }

    fun selectText(
        e: MotionEvent,
        select: (relativePage: Int, lineIndex: Int, charIndex: Int) -> Unit
    ) {
        val y = e.y - headerHeight
        return content_text_view.selectText(e.x, y, select)
    }

    fun selectStartMove(x: Float, y: Float) {
        content_text_view.selectStartMove(x, y - headerHeight)
    }

    fun selectStartMoveIndex(relativePage: Int, lineIndex: Int, charIndex: Int) {
        content_text_view.selectStartMoveIndex(relativePage, lineIndex, charIndex)
    }

    fun selectEndMove(x: Float, y: Float) {
        content_text_view.selectEndMove(x, y - headerHeight)
    }

    fun selectEndMoveIndex(relativePage: Int, lineIndex: Int, charIndex: Int) {
        content_text_view.selectEndMoveIndex(relativePage, lineIndex, charIndex)
    }

    fun cancelSelect() {
        content_text_view.cancelSelect()
    }

    val selectedText: String get() = content_text_view.selectedText

}