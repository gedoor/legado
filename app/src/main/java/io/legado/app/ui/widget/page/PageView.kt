package io.legado.app.ui.widget.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import io.legado.app.help.ReadBookConfig
import io.legado.app.ui.widget.page.curl.CurlView
import io.legado.app.ui.widget.page.delegate.*
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
    var curlView: CurlView? = null

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
        upPageAnim()
        curPage?.callBack = this
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        pageDelegate?.setViewSize(w, h)
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

    fun upPageAnim() {
        if (curlView != null) {
            removeView(curlView)
            curlView = null
        }
        pageDelegate = null
        pageDelegate = when (context.getPrefInt("pageAnim")) {
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
                    callBack?.let { callback ->
                        if (isScrollDelegate) {
                            curPage?.scrollTo(callback.textChapter()?.getStartLine(callback.durChapterPos()))
                        }
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
                callBack?.textChapter()?.let {
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
                callBack?.textChapter()?.let {
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
        get() = callBack?.durChapterPos() ?: 0

    override fun setPageIndex(pageIndex: Int) {
        callBack?.setPageIndex(pageIndex)
    }

    override fun getChapterPosition(): Int {
        return callBack?.durChapterIndex() ?: 0
    }

    override fun getChapter(position: Int): TextChapter? {
        return callBack?.textChapter(position)
    }

    override fun getCurrentChapter(): TextChapter? {
        return callBack?.textChapter(0)
    }

    override fun getNextChapter(): TextChapter? {
        return callBack?.textChapter(1)
    }

    override fun getPreviousChapter(): TextChapter? {
        return callBack?.textChapter(-1)
    }

    override fun hasNextChapter(): Boolean {
        callBack?.let {
            return it.durChapterIndex() < it.chapterSize() - 1
        }
        return false
    }

    override fun hasPrevChapter(): Boolean {
        callBack?.let {
            return it.durChapterIndex() > 0
        }
        return false
    }

    override fun scrollToLine(line: Int) {
        if (isScrollDelegate) {
            callBack?.textChapter()?.let {
                val pageIndex = it.getPageIndex(line)
                curPage?.setPageIndex(pageIndex)
                callBack?.setPageIndex(pageIndex)
            }
        }
    }

    override fun scrollToLast() {
        if (isScrollDelegate) {
            callBack?.textChapter()?.let {
                callBack?.setPageIndex(it.lastIndex())
                curPage?.setPageIndex(it.lastIndex())
            }
        }
    }

    interface CallBack {
        fun chapterSize(): Int
        fun durChapterIndex(): Int
        fun durChapterPos(): Int

        /**
         * chapterOnDur: 0为当前页,1为下一页,-1为上一页
         */
        fun textChapter(chapterOnDur: Int = 0): TextChapter?

        /**
         * 加载章节内容, index章节序号
         */
        fun loadContent(index: Int)

        /**
         * 保存页数
         */
        fun setPageIndex(pageIndex: Int)

        /**
         * 点击屏幕中间
         */
        fun clickCenter()
    }
}
