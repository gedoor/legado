package io.legado.app.ui.book.read.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.theme.accentColor
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.delegate.*
import io.legado.app.ui.book.read.page.entities.TextChapter
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.utils.activity
import io.legado.app.utils.screenshot
import kotlinx.android.synthetic.main.activity_book_read.view.*

class PageView(context: Context, attrs: AttributeSet) :
    FrameLayout(context, attrs),
    DataSource {

    val callBack: CallBack get() = activity as CallBack
    var pageFactory: TextPageFactory = TextPageFactory(this)
    var pageDelegate: PageDelegate? = null

    var prevPage: ContentView = ContentView(context)
    var curPage: ContentView = ContentView(context)
    var nextPage: ContentView = ContentView(context)
    private val autoPageRect by lazy { Rect() }
    private val autoPagePint by lazy {
        Paint().apply {
            color = context.accentColor
        }
    }

    init {
        addView(nextPage)
        addView(curPage)
        addView(prevPage)
        upBg()
        setWillNotDraw(false)
        upPageAnim()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        prevPage.x = -w.toFloat()
        pageDelegate?.setViewSize(w, h)
        if (oldw != 0 && oldh != 0) {
            ReadBook.loadContent(resetPageOffset = false)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        pageDelegate?.onDraw(canvas)
        if (callBack.isAutoPage) {
            nextPage.screenshot()?.let {
                val bottom =
                    page_view.height * callBack.autoPageProgress / (ReadBookConfig.autoReadSpeed * 10)
                autoPageRect.set(0, 0, page_view.width, bottom)
                canvas.drawBitmap(it, autoPageRect, autoPageRect, null)
                canvas.drawRect(
                    0f,
                    bottom.toFloat() - 1,
                    page_view.width.toFloat(),
                    bottom.toFloat(),
                    autoPagePint
                )
            }
        }
    }

    override fun computeScroll() {
        pageDelegate?.scroll()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        pageDelegate?.onTouch(event)
        callBack.screenOffTimerStart()
        return true
    }

    fun onDestroy() {
        pageDelegate?.onDestroy()
        curPage.cancelSelect()
    }

    fun fillPage(direction: PageDelegate.Direction) {
        when (direction) {
            PageDelegate.Direction.PREV -> {
                pageFactory.moveToPrev(true)
            }
            PageDelegate.Direction.NEXT -> {
                pageFactory.moveToNext(true)
            }
            else -> Unit
        }
    }

    fun upPageAnim() {
        pageDelegate?.onDestroy()
        pageDelegate = null
        pageDelegate = when (ReadBookConfig.pageAnim) {
            0 -> CoverPageDelegate(this)
            1 -> SlidePageDelegate(this)
            2 -> SimulationPageDelegate(this)
            3 -> ScrollPageDelegate(this)
            else -> NoAnimPageDelegate(this)
        }
        upContent()
    }

    override fun upContent(relativePosition: Int, resetPageOffset: Boolean) {
        if (ReadBookConfig.isScroll && !callBack.isAutoPage) {
            curPage.setContent(pageFactory.currentPage, resetPageOffset)
        } else {
            curPage.resetPageOffset()
            when (relativePosition) {
                -1 -> prevPage.setContent(pageFactory.prevPage)
                1 -> nextPage.setContent(pageFactory.nextPage)
                else -> {
                    curPage.setContent(pageFactory.currentPage)
                    nextPage.setContent(pageFactory.nextPage)
                    prevPage.setContent(pageFactory.prevPage)
                }
            }
        }
        callBack.screenOffTimerStart()
    }

    fun upTipStyle() {
        curPage.upTipStyle()
        prevPage.upTipStyle()
        nextPage.upTipStyle()
    }

    fun upStyle() {
        ChapterProvider.upStyle()
        curPage.upStyle()
        prevPage.upStyle()
        nextPage.upStyle()
    }

    fun upBg() {
        ReadBookConfig.bg ?: let {
            ReadBookConfig.upBg()
        }
        curPage.setBg(ReadBookConfig.bg)
        prevPage.setBg(ReadBookConfig.bg)
        nextPage.setBg(ReadBookConfig.bg)
    }

    fun upTime() {
        curPage.upTime()
        prevPage.upTime()
        nextPage.upTime()
    }

    fun upBattery(battery: Int) {
        curPage.upBattery(battery)
        prevPage.upBattery(battery)
        nextPage.upBattery(battery)
    }

    override val currentChapter: TextChapter?
        get() {
            return if (callBack.isInitFinish) ReadBook.textChapter(0) else null
        }

    override val nextChapter: TextChapter?
        get() {
            return if (callBack.isInitFinish) ReadBook.textChapter(1) else null
        }

    override val prevChapter: TextChapter?
        get() {
            return if (callBack.isInitFinish) ReadBook.textChapter(-1) else null
        }

    override fun hasNextChapter(): Boolean {
        return ReadBook.durChapterIndex < ReadBook.chapterSize - 1
    }

    override fun hasPrevChapter(): Boolean {
        return ReadBook.durChapterIndex > 0
    }

    interface CallBack {
        val isInitFinish: Boolean
        val isAutoPage: Boolean
        val autoPageProgress: Int
        fun clickCenter()
        fun screenOffTimerStart()
        fun showTextActionMenu()
    }
}
