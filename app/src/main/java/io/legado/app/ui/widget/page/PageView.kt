package io.legado.app.ui.widget.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import io.legado.app.help.ReadBookConfig
import io.legado.app.ui.widget.page.delegate.CoverPageDelegate
import io.legado.app.ui.widget.page.delegate.NoAnimPageDelegate
import io.legado.app.ui.widget.page.delegate.PageDelegate
import io.legado.app.ui.widget.page.delegate.SlidePageDelegate
import io.legado.app.utils.activity
import io.legado.app.utils.getPrefInt

class PageView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs),
    PageDelegate.PageInterface {

    private var callback: CallBack? = null
    private var pageDelegate: PageDelegate? = null
    private var pageFactory: TextPageFactory? = null

    var prevPage: ContentView? = null
    var curPage: ContentView? = null
    var nextPage: ContentView? = null

    init {
        callback = activity as? CallBack
        prevPage = ContentView(context)
        addView(prevPage)
        nextPage = ContentView(context)
        addView(nextPage)
        curPage = ContentView(context)
        addView(curPage)
        upBg()
        setWillNotDraw(false)

        upPageAnim()

        setPageFactory(TextPageFactory.create(object : DataSource {
            override fun pageIndex(): Int {
                return callback?.durChapterPos() ?: 0
            }

            override fun setPageIndex(pageIndex: Int) {
                callback?.setPageIndex(pageIndex)
            }

            override fun isPrepared(): Boolean {
                return true
            }

            override fun getChapterPosition(): Int {
                return callback?.durChapterIndex() ?: 0
            }

            override fun getChapter(position: Int): TextChapter? {
                return callback?.textChapter(position)
            }

            override fun getCurrentChapter(): TextChapter? {
                return callback?.textChapter(0)
            }

            override fun getNextChapter(): TextChapter? {
                return callback?.textChapter(1)
            }

            override fun getPreviousChapter(): TextChapter? {
                return callback?.textChapter(-1)
            }

            override fun hasNextChapter(): Boolean {
                callback?.let {
                    return it.durChapterIndex() < it.chapterSize() - 1
                }
                return false
            }

            override fun hasPrevChapter(): Boolean {
                callback?.let {
                    return it.durChapterIndex() > 0
                }
                return false
            }

            override fun moveToNextChapter() {
                callback?.moveToNextChapter()
            }

            override fun moveToPrevChapter() {
                callback?.moveToPrevChapter()
            }
        }))
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return pageDelegate?.onTouch(event) ?: super.onTouchEvent(event)
    }

    fun fillPage(direction: PageDelegate.Direction) {
        pageFactory?.let {
            when (direction) {
                PageDelegate.Direction.PREV -> {
                    it.moveToPrevious()
                }
                PageDelegate.Direction.NEXT -> {
                    it.moveToNext()
                }
                else -> return
            }
        }
        upContent()
    }

    private fun setPageFactory(factory: TextPageFactory) {
        this.pageFactory = factory

        //可做成异步回调
        upContent()
    }

    fun upPageAnim() {
        pageDelegate = when (context.getPrefInt("pageAnim")) {
            1 -> SlidePageDelegate(this)
            2 -> NoAnimPageDelegate(this)
            else -> CoverPageDelegate(this)
        }
    }

    fun upContent() {
        pageFactory?.let {
            prevPage?.setContent(it.previousPage())
            curPage?.setContent(it.currentPage())
            nextPage?.setContent(it.nextPage())
        }
    }

    fun moveToPrevPage(noAnim: Boolean = true) {
        if (noAnim) {
            fillPage(PageDelegate.Direction.PREV)
        }
    }

    fun moveToNextPage(noAnim: Boolean = true) {
        if (noAnim) {
            fillPage(PageDelegate.Direction.NEXT)
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

    override fun hasNext(): Boolean {
        return pageFactory?.hasNext() == true
    }

    override fun hasPrev(): Boolean {
        return pageFactory?.hasPrev() == true
    }

    override fun clickCenter() {
        callback?.clickCenter()
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
         * 下一章
         */
        fun moveToNextChapter(): Boolean

        /**
         * 上一章
         */
        fun moveToPrevChapter(last: Boolean = true): Boolean

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
