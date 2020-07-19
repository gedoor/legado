package io.legado.app.ui.book.read.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import io.legado.app.R
import io.legado.app.constant.AppConst.timeFormat
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.ReadTipConfig
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.ui.widget.BatteryView
import io.legado.app.utils.dp
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.statusBarHeight
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.view_book_page.view.*
import java.util.*


class ContentView(context: Context) : FrameLayout(context) {

    private var battery = 100
    private var tvTitle: BatteryView? = null
    private var tvTime: BatteryView? = null
    private var tvBattery: BatteryView? = null
    private var tvPage: BatteryView? = null
    private var tvTotalProgress: BatteryView? = null
    private var tvPageAndTotal: BatteryView? = null

    val headerHeight: Int
        get() = if (ReadBookConfig.hideStatusBar) ll_header.height else context.statusBarHeight

    init {
        //设置背景防止切换背景时文字重叠
        setBackgroundColor(context.getCompatColor(R.color.background))
        inflate(context, R.layout.view_book_page, this)
        upTipStyle()
        upStyle()
        content_text_view.upView = {
            setProgress(it)
        }
    }

    fun upStyle() {
        ReadBookConfig.apply {
            bv_header_left.typeface = ChapterProvider.typeface
            tv_header_left.typeface = ChapterProvider.typeface
            tv_header_middle.typeface = ChapterProvider.typeface
            tv_header_right.typeface = ChapterProvider.typeface
            bv_footer_left.typeface = ChapterProvider.typeface
            tv_footer_left.typeface = ChapterProvider.typeface
            tv_footer_middle.typeface = ChapterProvider.typeface
            tv_footer_right.typeface = ChapterProvider.typeface
            bv_header_left.setColor(textColor)
            tv_header_left.setColor(textColor)
            tv_header_middle.setColor(textColor)
            tv_header_right.setColor(textColor)
            bv_footer_left.setColor(textColor)
            tv_footer_left.setColor(textColor)
            tv_footer_middle.setColor(textColor)
            tv_footer_right.setColor(textColor)
            //显示状态栏时隐藏header
            vw_status_bar.setPadding(0, context.statusBarHeight, 0, 0)
            vw_status_bar.isGone = hideStatusBar
            ll_header.setPadding(
                headerPaddingLeft.dp,
                headerPaddingTop.dp,
                headerPaddingRight.dp,
                headerPaddingBottom.dp
            )
            ll_footer.setPadding(
                footerPaddingLeft.dp,
                footerPaddingTop.dp,
                footerPaddingRight.dp,
                footerPaddingBottom.dp
            )
            vw_top_divider.visible(showHeaderLine)
            vw_bottom_divider.visible(showFooterLine)
            content_text_view.upVisibleRect()
        }
        upTime()
        upBattery(battery)
    }

    fun upTipStyle() {
        ReadTipConfig.apply {
            tv_header_left.isInvisible = tipHeaderLeft != chapterTitle
            bv_header_left.isInvisible = tipHeaderLeft == none || !tv_header_left.isInvisible
            tv_header_right.isGone = tipHeaderRight == none
            tv_header_middle.isGone = tipHeaderMiddle == none
            tv_footer_left.isInvisible = tipFooterLeft != chapterTitle
            bv_footer_left.isInvisible = tipFooterLeft == none || !tv_footer_left.isInvisible
            tv_footer_right.isGone = tipFooterRight == none
            tv_footer_middle.isGone = tipFooterMiddle == none
            ll_header.isGone = hideHeader
            ll_footer.isGone = hideFooter
        }
        tvTitle = when (ReadTipConfig.chapterTitle) {
            ReadTipConfig.tipHeaderLeft -> tv_header_left
            ReadTipConfig.tipHeaderMiddle -> tv_header_middle
            ReadTipConfig.tipHeaderRight -> tv_header_right
            ReadTipConfig.tipFooterLeft -> tv_footer_left
            ReadTipConfig.tipFooterMiddle -> tv_footer_middle
            ReadTipConfig.tipFooterRight -> tv_footer_right
            else -> null
        }
        tvTitle?.apply {
            isBattery = false
            textSize = 12f
        }
        tvTime = when (ReadTipConfig.time) {
            ReadTipConfig.tipHeaderLeft -> bv_header_left
            ReadTipConfig.tipHeaderMiddle -> tv_header_middle
            ReadTipConfig.tipHeaderRight -> tv_header_right
            ReadTipConfig.tipFooterLeft -> bv_footer_left
            ReadTipConfig.tipFooterMiddle -> tv_footer_middle
            ReadTipConfig.tipFooterRight -> tv_footer_right
            else -> null
        }
        tvTime?.apply {
            isBattery = false
            textSize = 12f
        }
        tvBattery = when (ReadTipConfig.battery) {
            ReadTipConfig.tipHeaderLeft -> bv_header_left
            ReadTipConfig.tipHeaderMiddle -> tv_header_middle
            ReadTipConfig.tipHeaderRight -> tv_header_right
            ReadTipConfig.tipFooterLeft -> bv_footer_left
            ReadTipConfig.tipFooterMiddle -> tv_footer_middle
            ReadTipConfig.tipFooterRight -> tv_footer_right
            else -> null
        }
        tvBattery?.apply {
            isBattery = true
            textSize = 10f
        }
        tvPage = when (ReadTipConfig.page) {
            ReadTipConfig.tipHeaderLeft -> bv_header_left
            ReadTipConfig.tipHeaderMiddle -> tv_header_middle
            ReadTipConfig.tipHeaderRight -> tv_header_right
            ReadTipConfig.tipFooterLeft -> bv_footer_left
            ReadTipConfig.tipFooterMiddle -> tv_footer_middle
            ReadTipConfig.tipFooterRight -> tv_footer_right
            else -> null
        }
        tvPage?.apply {
            isBattery = false
            textSize = 12f
        }
        tvTotalProgress = when (ReadTipConfig.totalProgress) {
            ReadTipConfig.tipHeaderLeft -> bv_header_left
            ReadTipConfig.tipHeaderMiddle -> tv_header_middle
            ReadTipConfig.tipHeaderRight -> tv_header_right
            ReadTipConfig.tipFooterLeft -> bv_footer_left
            ReadTipConfig.tipFooterMiddle -> tv_footer_middle
            ReadTipConfig.tipFooterRight -> tv_footer_right
            else -> null
        }
        tvTotalProgress?.apply {
            isBattery = false
            textSize = 12f
        }
        tvPageAndTotal = when (ReadTipConfig.pageAndTotal) {
            ReadTipConfig.tipHeaderLeft -> bv_header_left
            ReadTipConfig.tipHeaderMiddle -> tv_header_middle
            ReadTipConfig.tipHeaderRight -> tv_header_right
            ReadTipConfig.tipFooterLeft -> bv_footer_left
            ReadTipConfig.tipFooterMiddle -> tv_footer_middle
            ReadTipConfig.tipFooterRight -> tv_footer_right
            else -> null
        }
        tvPageAndTotal?.apply {
            isBattery = false
            textSize = 12f
        }
    }

    fun setBg(bg: Drawable?) {
        page_panel.background = bg
    }

    fun upTime() {
        tvTime?.text = timeFormat.format(Date(System.currentTimeMillis()))
    }

    fun upBattery(battery: Int) {
        this.battery = battery
        tvBattery?.setBattery(battery)
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
        val title = textPage.title
        tvTitle?.text = title
        tvPage?.text = "${index.plus(1)}/$pageSize"
        tvTotalProgress?.text = readProgress
        tvPageAndTotal?.text = "${index.plus(1)}/$pageSize  $readProgress"
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