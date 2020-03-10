package io.legado.app.ui.book.read.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import io.legado.app.help.ReadBookConfig
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.delegate.*
import io.legado.app.ui.book.read.page.entities.TextChapter
import io.legado.app.utils.activity

class PageView(context: Context, attrs: AttributeSet) :
    FrameLayout(context, attrs),
    DataSource {

    val callBack: CallBack get() = activity as CallBack
    var pageFactory: TextPageFactory = TextPageFactory(this)
    var pageDelegate: PageDelegate? = null

    var prevPage: ContentView = ContentView(context)
    var curPage: ContentView = ContentView(context)
    var nextPage: ContentView = ContentView(context)

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
            ReadBook.loadContent()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        pageDelegate?.onDraw(canvas)
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

    override fun upContent(relativePosition: Int) {
        if (ReadBookConfig.isScroll) {
            curPage.setContent(pageFactory.currentPage)
        } else {
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
        fun clickCenter()
        fun screenOffTimerStart()
        fun showTextActionMenu()
    }
}
