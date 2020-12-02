package io.legado.app.ui.book.read.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.constant.AppConst.timeFormat
import io.legado.app.databinding.ViewBookPageBinding
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.ReadTipConfig
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.entities.PageData
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.ui.widget.BatteryView
import io.legado.app.utils.*
import org.jetbrains.anko.topPadding
import java.util.*

/**
 * 阅读界面
 */
class ContentView(context: Context) : FrameLayout(context) {
    private val binding = ViewBookPageBinding.inflate(LayoutInflater.from(context), this, true)
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
            val h2 = if (binding.llHeader.isGone) 0 else binding.llHeader.height
            return h1 + h2
        }

    init {
        if (!isInEditMode) {
            //设置背景防止切换背景时文字重叠
            setBackgroundColor(context.getCompatColor(R.color.background))
            upTipStyle()
            upStyle()
            binding.contentTextView.upView = {
                setProgress(it)
            }
        }
    }

    fun upStyle() = ReadBookConfig.apply {
        binding.bvHeaderLeft.typeface = ChapterProvider.typeface
        binding.tvHeaderLeft.typeface = ChapterProvider.typeface
        binding.tvHeaderMiddle.typeface = ChapterProvider.typeface
        binding.tvHeaderRight.typeface = ChapterProvider.typeface
        binding.bvFooterLeft.typeface = ChapterProvider.typeface
        binding.tvFooterLeft.typeface = ChapterProvider.typeface
        binding.tvFooterMiddle.typeface = ChapterProvider.typeface
        binding.tvFooterRight.typeface = ChapterProvider.typeface
        val tipColor = if (ReadTipConfig.tipColor == 0) textColor else ReadTipConfig.tipColor
        binding.bvHeaderLeft.setColor(tipColor)
        binding.tvHeaderLeft.setColor(tipColor)
        binding.tvHeaderMiddle.setColor(tipColor)
        binding.tvHeaderRight.setColor(tipColor)
        binding.bvFooterLeft.setColor(tipColor)
        binding.tvFooterLeft.setColor(tipColor)
        binding.tvFooterMiddle.setColor(tipColor)
        binding.tvFooterRight.setColor(tipColor)
        upStatusBar()
        binding.llHeader.setPadding(
            headerPaddingLeft.dp,
            headerPaddingTop.dp,
            headerPaddingRight.dp,
            headerPaddingBottom.dp
        )
        binding.llFooter.setPadding(
            footerPaddingLeft.dp,
            footerPaddingTop.dp,
            footerPaddingRight.dp,
            footerPaddingBottom.dp
        )
        binding.vwTopDivider.visible(showHeaderLine)
        binding.vwBottomDivider.visible(showFooterLine)
        binding.pageNvBar.layoutParams = binding.pageNvBar.layoutParams.apply {
            height = if (hideNavigationBar) 0 else App.navigationBarHeight
        }
        binding.contentTextView.upVisibleRect()
        upTime()
        upBattery(battery)
    }

    /**
     * 显示状态栏时隐藏header
     */
    fun upStatusBar() {
        binding.vwStatusBar.topPadding = context.statusBarHeight
        binding.vwStatusBar.isGone =
            ReadBookConfig.hideStatusBar || (activity as? BaseActivity<*>)?.isInMultiWindow == true
    }

    fun upTipStyle() {
        ReadTipConfig.apply {
            binding.tvHeaderLeft.isInvisible = tipHeaderLeft != chapterTitle
            binding.bvHeaderLeft.isInvisible =
                tipHeaderLeft == none || !binding.tvHeaderLeft.isInvisible
            binding.tvHeaderRight.isGone = tipHeaderRight == none
            binding.tvHeaderMiddle.isGone = tipHeaderMiddle == none
            binding.tvFooterLeft.isInvisible = tipFooterLeft != chapterTitle
            binding.bvFooterLeft.isInvisible =
                tipFooterLeft == none || !binding.tvFooterLeft.isInvisible
            binding.tvFooterRight.isGone = tipFooterRight == none
            binding.tvFooterMiddle.isGone = tipFooterMiddle == none
            binding.llHeader.isGone = when (headerMode) {
                1 -> false
                2 -> true
                else -> !ReadBookConfig.hideStatusBar
            }
            binding.llFooter.isGone = when (footerMode) {
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
                if (tip == ReadTipConfig.chapterTitle) binding.tvHeaderLeft else binding.bvHeaderLeft
            ReadTipConfig.tipHeaderMiddle -> binding.tvHeaderMiddle
            ReadTipConfig.tipHeaderRight -> binding.tvHeaderRight
            ReadTipConfig.tipFooterLeft ->
                if (tip == ReadTipConfig.chapterTitle) binding.tvFooterLeft else binding.bvFooterLeft
            ReadTipConfig.tipFooterMiddle -> binding.tvFooterMiddle
            ReadTipConfig.tipFooterRight -> binding.tvFooterRight
            else -> null
        }
    }

    fun setBg(bg: Drawable?) {
        binding.pagePanel.background = bg
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
        binding.contentTextView.setContent(pageData)
    }

    fun setContentDescription(content: String) {
        binding.contentTextView.contentDescription = content
    }

    fun resetPageOffset() {
        binding.contentTextView.resetPageOffset()
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
        binding.contentTextView.scroll(offset)
    }

    fun upSelectAble(selectAble: Boolean) {
        binding.contentTextView.selectAble = selectAble
    }

    fun selectText(
        x: Float, y: Float,
        select: (relativePage: Int, lineIndex: Int, charIndex: Int) -> Unit,
    ) {
        return binding.contentTextView.selectText(x, y - headerHeight, select)
    }

    fun selectStartMove(x: Float, y: Float) {
        binding.contentTextView.selectStartMove(x, y - headerHeight)
    }

    fun selectStartMoveIndex(relativePage: Int, lineIndex: Int, charIndex: Int) {
        binding.contentTextView.selectStartMoveIndex(relativePage, lineIndex, charIndex)
    }

    fun selectEndMove(x: Float, y: Float) {
        binding.contentTextView.selectEndMove(x, y - headerHeight)
    }

    fun selectEndMoveIndex(relativePage: Int, lineIndex: Int, charIndex: Int) {
        binding.contentTextView.selectEndMoveIndex(relativePage, lineIndex, charIndex)
    }

    fun cancelSelect() {
        binding.contentTextView.cancelSelect()
    }

    val selectedText: String get() = binding.contentTextView.selectedText

}