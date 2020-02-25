package io.legado.app.ui.book.read.page.content

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.book.read.page.ChapterProvider
import io.legado.app.ui.book.read.page.TextPageFactory
import io.legado.app.ui.book.read.page.entities.TextChar
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.utils.activity
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.getPrefBoolean

abstract class BaseContentTextView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    var selectAble = context.getPrefBoolean(PreferKey.textSelectAble)
    var upView: ((TextPage) -> Unit)? = null
    protected val selectedPaint by lazy {
        Paint().apply {
            color = context.getCompatColor(R.color.btn_bg_press_2)
            style = Paint.Style.FILL
        }
    }
    protected var callBack: CallBack
    protected var selectLineStart = 0
    protected var selectCharStart = 0
    protected var selectLineEnd = 0
    protected var selectCharEnd = 0
    protected var textPage: TextPage = TextPage()
    //滚动参数
    protected val pageFactory: TextPageFactory get() = callBack.pageFactory
    protected val maxScrollOffset = 100f
    protected var pageOffset = 0f
    protected var linePos = 0

    init {
        callBack = activity as CallBack
        contentDescription = textPage.text
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ChapterProvider.viewWidth = w
        ChapterProvider.viewHeight = h
        ChapterProvider.upSize()
        textPage.format()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.clipRect(
            ChapterProvider.paddingLeft,
            ChapterProvider.paddingTop,
            ChapterProvider.visibleRight,
            ChapterProvider.visibleBottom
        )
        if (ReadBookConfig.isScroll) {
            drawScrollPage(canvas)
        } else {
            drawHorizontalPage(canvas)
        }
    }

    abstract fun drawScrollPage(canvas: Canvas)

    abstract fun drawHorizontalPage(canvas: Canvas)

    protected fun drawChars(
        canvas: Canvas,
        textChars: List<TextChar>,
        lineTop: Float,
        lineBase: Float,
        lineBottom: Float,
        isTitle: Boolean,
        isReadAloud: Boolean
    ) {
        val textPaint = if (isTitle) ChapterProvider.titlePaint else ChapterProvider.contentPaint
        textPaint.color =
            if (isReadAloud) context.accentColor else ReadBookConfig.durConfig.textColor()
        textChars.forEach {
            canvas.drawText(it.charData, it.start, lineBase, textPaint)
            if (it.selected) {
                canvas.drawRect(it.start, lineTop, it.end, lineBottom, selectedPaint)
            }
        }
    }


    protected fun upSelectedStart(x: Float, y: Float) {
        callBack.upSelectedStart(x, y + callBack.headerHeight)
    }

    protected fun upSelectedEnd(x: Float, y: Float) {
        callBack.upSelectedEnd(x, y + callBack.headerHeight)
    }


    interface CallBack {
        fun upSelectedStart(x: Float, y: Float)
        fun upSelectedEnd(x: Float, y: Float)
        fun onCancelSelect()
        val headerHeight: Int
        val pageFactory: TextPageFactory
    }
}