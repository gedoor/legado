package io.legado.app.ui.widget.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import io.legado.app.ui.widget.page.delegate.CoverPageDelegate
import io.legado.app.ui.widget.page.delegate.PageDelegate
import kotlinx.android.synthetic.main.view_book_page.view.*
import org.jetbrains.anko.backgroundColor

class PageView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs), PageDelegate.PageInterface {

    var callBack: CallBack? = null
    private var pageDelegate: PageDelegate? = null

    var prevPage: ContentView? = null
    var curPage: ContentView? = null
    var nextPage: ContentView? = null

    init {
        prevPage = ContentView(context)
        addView(prevPage)
        nextPage = ContentView(context)
        addView(nextPage)
        curPage = ContentView(context)
        addView(curPage)

        setWillNotDraw(false)

        page_panel.backgroundColor = Color.WHITE

        pageDelegate = CoverPageDelegate(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        pageDelegate?.setViewSize(w, h)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

//        bringChildToFront(prevPage)

        pageDelegate?.onPerform(canvas)
    }

    override fun computeScroll() {
        pageDelegate?.scroll()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return pageDelegate?.onTouch(event) ?: super.onTouchEvent(event)
    }

    fun chapterLoadFinish(chapterOnDur: Int = 0) {
        callBack?.let { cb ->
            when (chapterOnDur) {
                0 -> {
                    cb.textChapter()?.let {
                        curPage?.setContent(it.page(cb.durChapterPos(it.pageSize()))?.text)
                        if (cb.durChapterPos(it.pageSize()) > 0) {
                            prevPage?.setContent(it.page(cb.durChapterPos(it.pageSize()) - 1)?.text)
                        }
                        if (cb.durChapterPos(it.pageSize()) < it.pageSize() - 1) {
                            nextPage?.setContent(it.page(cb.durChapterPos(it.pageSize()) + 1)?.text)
                        }
                    }
                }
                1 -> {
                    cb.textChapter()?.let {
                        if (cb.durChapterPos(it.pageSize()) == it.pageSize() - 1) {
                            nextPage?.setContent(cb.textChapter(1)?.page(0)?.text)
                        }
                    }
                }
                -1 -> {
                    cb.textChapter()?.let {
                        if (cb.durChapterPos(it.pageSize()) == 0) {
                            prevPage?.setContent(cb.textChapter(-1)?.lastPage()?.text)
                        }
                    }
                }
                else -> {
                }
            }
        }
    }

    fun setPageFactory(factory: PageFactory<*>) {

    }

    override fun hasNext(): Boolean {
        return true
    }

    override fun hasPrev(): Boolean {
        return true
    }

    fun upStyle() {
        curPage?.upStyle()
        prevPage?.upStyle()
        nextPage?.upStyle()
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

    interface CallBack {
        fun durChapterIndex(): Int
        fun durChapterPos(pageSize: Int): Int
        fun textChapter(chapterOnDur: Int = 0): TextChapter?
    }
}
