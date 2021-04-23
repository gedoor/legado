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
import io.legado.app.data.entities.Bookmark
import io.legado.app.databinding.ViewBookPageBinding
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.ReadTipConfig
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.ui.widget.BatteryView
import io.legado.app.utils.*
import java.util.*

/**
 * 阅读界面
 */
class PageView(context: Context) : FrameLayout(context) {
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
            upStyle()
        }
        binding.contentTextView.upView = {
            setProgress(it)
        }
    }

    fun upStyle() = with(binding) {
        upTipStyle()
        ReadBookConfig.let {
            val tipColor = with(ReadTipConfig) {
                if (tipColor == 0) it.textColor else tipColor
            }
            bvHeaderLeft.setColor(tipColor)
            tvHeaderLeft.setColor(tipColor)
            tvHeaderMiddle.setColor(tipColor)
            tvHeaderRight.setColor(tipColor)
            bvFooterLeft.setColor(tipColor)
            tvFooterLeft.setColor(tipColor)
            tvFooterMiddle.setColor(tipColor)
            tvFooterRight.setColor(tipColor)
            upStatusBar()
            llHeader.setPadding(
                it.headerPaddingLeft.dp,
                it.headerPaddingTop.dp,
                it.headerPaddingRight.dp,
                it.headerPaddingBottom.dp
            )
            llFooter.setPadding(
                it.footerPaddingLeft.dp,
                it.footerPaddingTop.dp,
                it.footerPaddingRight.dp,
                it.footerPaddingBottom.dp
            )
            vwTopDivider.visible(it.showHeaderLine)
            vwBottomDivider.visible(it.showFooterLine)
            pageNvBar.layoutParams = pageNvBar.layoutParams.apply {
                height = if (it.hideNavigationBar) 0 else App.navigationBarHeight
            }
        }
        contentTextView.upVisibleRect()
        upTime()
        upBattery(battery)
    }

    /**
     * 显示状态栏时隐藏header
     */
    fun upStatusBar() = with(binding.vwStatusBar) {
        setPadding(paddingLeft, context.statusBarHeight, paddingRight, paddingBottom)
        isGone =
            ReadBookConfig.hideStatusBar || (activity as? BaseActivity<*>)?.isInMultiWindow == true
    }

    private fun upTipStyle() = with(binding) {
        ReadTipConfig.apply {
            tvHeaderLeft.isInvisible = tipHeaderLeft != chapterTitle
            bvHeaderLeft.isInvisible =
                tipHeaderLeft == none || !tvHeaderLeft.isInvisible
            tvHeaderRight.isGone = tipHeaderRight == none
            tvHeaderMiddle.isGone = tipHeaderMiddle == none
            tvFooterLeft.isInvisible = tipFooterLeft != chapterTitle
            bvFooterLeft.isInvisible =
                tipFooterLeft == none || !tvFooterLeft.isInvisible
            tvFooterRight.isGone = tipFooterRight == none
            tvFooterMiddle.isGone = tipFooterMiddle == none
            llHeader.isGone = when (headerMode) {
                1 -> false
                2 -> true
                else -> !ReadBookConfig.hideStatusBar
            }
            llFooter.isGone = when (footerMode) {
                1 -> true
                else -> false
            }
        }
        tvTitle = getTipView(ReadTipConfig.chapterTitle)
        tvTitle?.apply {
            isBattery = false
            typeface = ChapterProvider.typeface
            textSize = 12f
        }
        tvTime = getTipView(ReadTipConfig.time)
        tvTime?.apply {
            isBattery = false
            typeface = ChapterProvider.typeface
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
            typeface = ChapterProvider.typeface
            textSize = 12f
        }
        tvTotalProgress = getTipView(ReadTipConfig.totalProgress)
        tvTotalProgress?.apply {
            isBattery = false
            typeface = ChapterProvider.typeface
            textSize = 12f
        }
        tvPageAndTotal = getTipView(ReadTipConfig.pageAndTotal)
        tvPageAndTotal?.apply {
            isBattery = false
            typeface = ChapterProvider.typeface
            textSize = 12f
        }
        tvBookName = getTipView(ReadTipConfig.bookName)
        tvBookName?.apply {
            isBattery = false
            typeface = ChapterProvider.typeface
            textSize = 12f
        }
        tvTimeBattery = getTipView(ReadTipConfig.timeBattery)
        tvTimeBattery?.apply {
            isBattery = false
            typeface = ChapterProvider.typeface
            textSize = 12f
        }
    }

    private fun getTipView(tip: Int): BatteryView? = with(binding) {
        return when (tip) {
            ReadTipConfig.tipHeaderLeft ->
                if (tip == ReadTipConfig.chapterTitle) tvHeaderLeft else bvHeaderLeft
            ReadTipConfig.tipHeaderMiddle -> tvHeaderMiddle
            ReadTipConfig.tipHeaderRight -> tvHeaderRight
            ReadTipConfig.tipFooterLeft ->
                if (tip == ReadTipConfig.chapterTitle) tvFooterLeft else bvFooterLeft
            ReadTipConfig.tipFooterMiddle -> tvFooterMiddle
            ReadTipConfig.tipFooterRight -> tvFooterRight
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

    fun setContent(textPage: TextPage, resetPageOffset: Boolean = true) {
        setProgress(textPage)
        if (resetPageOffset) {
            resetPageOffset()
        }
        binding.contentTextView.setContent(textPage)
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
        if (offset > 0) {
            for (i in 1..offset) {
                binding.contentTextView.scroll(1)
            }
        } else {
            for (i in offset..-1) {
                binding.contentTextView.scroll(-1)
            }
        }
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

    fun createBookmark(): Bookmark? {
        return binding.contentTextView.createBookmark()
    }

    val selectedText: String get() = binding.contentTextView.selectedText

    val textPage get() = binding.contentTextView.textPage
}