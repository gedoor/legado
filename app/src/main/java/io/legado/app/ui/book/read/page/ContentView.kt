package io.legado.app.ui.book.read.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.constant.AppConst.timeFormat
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.ReadTipConfig
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.entities.PageData
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.ui.widget.BatteryView
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.view_book_page.view.*
import org.jetbrains.anko.topPadding
import java.util.*


class ContentView(context: Context) : FrameLayout(context) {

    private var battery = 100
    private var tvTitle: BatteryView? = null
    private var tvTime: BatteryView? = null
    private var tvBattery: BatteryView? = null
    private var tvPage: BatteryView? = null
    private var tvTotalProgress: BatteryView? = null
    private var tvPageAndTotal: BatteryView? = null
    private var tvBookName: BatteryView? = null
    private var tvTimeBattery: BatteryView? = null

    val headerHeight: Int
        get() {
            val h1 = if (ReadBookConfig.hideStatusBar) 0 else context.statusBarHeight
            val h2 = if (ll_header.isGone) 0 else ll_header.height
            return h1 + h2
        }

    init {
        if (!isInEditMode) {
            //设置背景防止切换背景时文字重叠
            setBackgroundColor(context.getCompatColor(R.color.background))
            inflate(context, R.layout.view_book_page, this)
            upTipStyle()
            upStyle()
            content_text_view.upView = {
                setProgress(it)
            }
        }
    }

    fun upStyle() = ReadBookConfig.apply {
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
        upStatusBar()
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
        page_nv_bar.layoutParams = page_nv_bar.layoutParams.apply {
            height = if (hideNavigationBar) 0 else App.navigationBarHeight
        }
        content_text_view.upVisibleRect()
        upTime()
        upBattery(battery)
    }

    /**
     * 显示状态栏时隐藏header
     */
    fun upStatusBar() {
        vw_status_bar.topPadding = context.statusBarHeight
        vw_status_bar.isGone =
            ReadBookConfig.hideStatusBar || (activity as? BaseActivity)?.isInMultiWindow == true
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
            ll_header.isGone = when (headerMode) {
                1 -> false
                2 -> true
                else -> !ReadBookConfig.hideStatusBar
            }
            ll_footer.isGone = when (footerMode) {
                1 -> true
                else -> false
            }
        }
        tvTitle = getTipView(ReadTipConfig.chapterTitle)
        tvTitle?.apply {
            isBattery = false
            textSize = 12f
        }
        tvTime = getTipView(ReadTipConfig.time)
        tvTime?.apply {
            isBattery = false
            textSize = 12f
        }
        tvBattery = getTipView(ReadTipConfig.battery)
        tvBattery?.apply {
            isBattery = true
            textSize = 10f
        }
        tvPage = getTipView(ReadTipConfig.page)
        tvPage?.apply {
            isBattery = false
            textSize = 12f
        }
        tvTotalProgress = getTipView(ReadTipConfig.totalProgress)
        tvTotalProgress?.apply {
            isBattery = false
            textSize = 12f
        }
        tvPageAndTotal = getTipView(ReadTipConfig.pageAndTotal)
        tvPageAndTotal?.apply {
            isBattery = false
            textSize = 12f
        }
        tvBookName = getTipView(ReadTipConfig.bookName)
        tvBookName?.apply {
            isBattery = false
            textSize = 12f
        }
        tvTimeBattery = getTipView(ReadTipConfig.timeBattery)
        tvTimeBattery?.apply {
            isBattery = false
            textSize = 12f
        }
    }

    private fun getTipView(tip: Int): BatteryView? {
        return when (tip) {
            ReadTipConfig.tipHeaderLeft ->
                if (tip == ReadTipConfig.chapterTitle) tv_header_left else bv_header_left
            ReadTipConfig.tipHeaderMiddle -> tv_header_middle
            ReadTipConfig.tipHeaderRight -> tv_header_right
            ReadTipConfig.tipFooterLeft ->
                if (tip == ReadTipConfig.chapterTitle) tv_footer_left else bv_footer_left
            ReadTipConfig.tipFooterMiddle -> tv_footer_middle
            ReadTipConfig.tipFooterRight -> tv_footer_right
            else -> null
        }
    }

    fun setBg(bg: Drawable?) {
        page_panel.background = bg
    }

    fun upTime() {
        tvTime?.text = timeFormat.format(Date(System.currentTimeMillis()))
        upTimeBattery()
    }

    fun upBattery(battery: Int) {
        this.battery = battery
        tvBattery?.setBattery(battery)
        upTimeBattery()
    }

    @SuppressLint("SetTextI18n")
    private fun upTimeBattery() {
        tvTimeBattery?.let {
            val time = timeFormat.format(Date(System.currentTimeMillis()))
            it.text = "$time $battery%"
        }
    }

    fun setContent(pageData: PageData, resetPageOffset: Boolean = true) {
        setProgress(pageData.textPage)
        if (resetPageOffset) {
            resetPageOffset()
        }
        content_text_view.setContent(pageData)
    }

    fun setContentDescription(content: String) {
        content_text_view.contentDescription = content
    }

    fun resetPageOffset() {
        content_text_view.resetPageOffset()
    }

    @SuppressLint("SetTextI18n")
    fun setProgress(textPage: TextPage) = textPage.apply {
        tvBookName?.text = ReadBook.book?.name
        tvTitle?.text = textPage.title
        tvPage?.text = "${index.plus(1)}/$pageSize"
        tvTotalProgress?.text = readProgress
        tvPageAndTotal?.text = "${index.plus(1)}/$pageSize  $readProgress"
    }

    fun scroll(offset: Int) {
        content_text_view.scroll(offset)
    }

    fun upSelectAble(selectAble: Boolean) {
        content_text_view.selectAble = selectAble
    }

    fun selectText(
        x: Float, y: Float,
        select: (relativePage: Int, lineIndex: Int, charIndex: Int) -> Unit,
    ) {
        return content_text_view.selectText(x, y - headerHeight, select)
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