package io.legado.app.ui.book.read.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import io.legado.app.constant.PreferKey
import io.legado.app.help.ReadBookConfig
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.delegate.*
import io.legado.app.utils.activity
import io.legado.app.utils.getPrefInt

class PageView(context: Context, attrs: AttributeSet) :
    FrameLayout(context, attrs),
    ContentView.CallBack,
    DataSource {

    var callBack: CallBack? = null
    var pageFactory: TextPageFactory? = null
    private var pageDelegate: PageDelegate? = null

    var prevPage: ContentView? = null
    var curPage: ContentView? = null
    var nextPage: ContentView? = null

    init {
        callBack = activity as? CallBack
        prevPage = ContentView(context)
        addView(prevPage)
        nextPage = ContentView(context)
        addView(nextPage)
        curPage = ContentView(context)
        addView(curPage)
        upBg()
        setWillNotDraw(false)
        pageFactory = TextPageFactory(this)
        upPageAnim(context.getPrefInt(PreferKey.pageAnim))
        curPage?.callBack = this
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
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
        callBack?.screenOffTimerStart()
        return pageDelegate?.onTouch(event) ?: super.onTouchEvent(event)
    }

    fun fillPage(direction: PageDelegate.Direction) {
        when (direction) {
            PageDelegate.Direction.PREV -> {
                pageFactory?.moveToPrevious()
                upContent()
                if (isScrollDelegate) {
                    curPage?.scrollToBottom()
                }
            }
            PageDelegate.Direction.NEXT -> {
                pageFactory?.moveToNext()
                upContent()
                if (isScrollDelegate) {
                    curPage?.scrollTo(0)
                }
            }
            else -> Unit
        }
    }

    fun upPageAnim(pageAnim: Int) {
        pageDelegate = null
        pageDelegate = when (pageAnim) {
            0 -> CoverPageDelegate(this)
            1 -> SlidePageDelegate(this)
            2 -> SimulationPageDelegate(this)
            3 -> ScrollPageDelegate(this)
            else -> NoAnimPageDelegate(this)
        }
        upContent()
    }

    fun upContent(position: Int = 0) {
        pageFactory?.let {
            when (position) {
                -1 -> prevPage?.setContent(it.previousPage())
                1 -> nextPage?.setContent(it.nextPage())
                else -> {
                    curPage?.setContent(it.currentPage())
                    nextPage?.setContent(it.nextPage())
                    prevPage?.setContent(it.previousPage())
                    if (isScrollDelegate) {
                        curPage?.scrollTo(ReadBook.textChapter()?.getStartLine(ReadBook.durChapterPos()))
                    }
                }
            }
            if (isScrollDelegate) {
                prevPage?.scrollToBottom()
            }
        }
        pageDelegate?.onPageUp()
    }

    fun moveToPrevPage(noAnim: Boolean = true) {
        if (noAnim) {
            if (isScrollDelegate) {
                ReadBook.textChapter()?.let {
                    curPage?.scrollTo(it.getStartLine(pageIndex - 1))
                }
            } else {
                fillPage(PageDelegate.Direction.PREV)
            }
        }
    }

    fun moveToNextPage(noAnim: Boolean = true) {
        if (noAnim) {
            if (isScrollDelegate) {
                ReadBook.textChapter()?.let {
                    curPage?.scrollTo(it.getStartLine(pageIndex + 1))
                }
            } else {
                fillPage(PageDelegate.Direction.NEXT)
            }
        }
    }

    fun upStyle() {
        curPage?.upStyle()
        prevPage?.upStyle()
        nextPage?.upStyle()
    }

    fun upBg() {
        ReadBookConfig.bg ?: let {
            ReadBookConfig.upBg()
        }
        curPage?.setBg(ReadBookConfig.bg)
        prevPage?.setBg(ReadBookConfig.bg)
        nextPage?.setBg(ReadBookConfig.bg)
    }

    fun upTime() {
        curPage?.upTime()
        prevPage?.upTime()
        nextPage?.upTime()
    }

    fun upBattery(battery: Int) {
        curPage?.upBattery(battery)
        prevPage?.upBattery(battery)
        nextPage?.upBattery(battery)
    }

    override val isScrollDelegate: Boolean
        get() = pageDelegate is ScrollPageDelegate

    override val pageIndex: Int
        get() = ReadBook.durChapterPos()

    override fun setPageIndex(pageIndex: Int) {
        callBack?.setPageIndex(pageIndex)
    }

    override fun getChapterPosition(): Int {
        return ReadBook.durChapterIndex
    }

    override fun getCurrentChapter(): TextChapter? {
        return if (callBack?.isInitFinish == true) ReadBook.textChapter(0) else null
    }

    override fun getNextChapter(): TextChapter? {
        return if (callBack?.isInitFinish == true) ReadBook.textChapter(1) else null
    }

    override fun getPreviousChapter(): TextChapter? {
        return if (callBack?.isInitFinish == true) ReadBook.textChapter(-1) else null
    }

    override fun hasNextChapter(): Boolean {
        return ReadBook.durChapterIndex < ReadBook.chapterSize - 1
    }

    override fun hasPrevChapter(): Boolean {
        callBack?.let {
            return ReadBook.durChapterIndex > 0
        }
        return false
    }

    override fun scrollToLine(line: Int) {
        if (isScrollDelegate) {
            ReadBook.textChapter()?.let {
                val pageIndex = it.getPageIndex(line)
                curPage?.setPageIndex(pageIndex)
                callBack?.setPageIndex(pageIndex)
            }
        }
    }

    override fun scrollToLast() {
        if (isScrollDelegate) {
            ReadBook.textChapter()?.let {
                callBack?.setPageIndex(it.lastIndex())
                curPage?.setPageIndex(it.lastIndex())
            }
        }
    }

    interface CallBack {

        /**
         * 保存页数
         */
        fun setPageIndex(pageIndex: Int)

        /**
         * 点击屏幕中间
         */
        fun clickCenter()

        val isInitFinish: Boolean

        fun screenOffTimerStart()
    }
}
