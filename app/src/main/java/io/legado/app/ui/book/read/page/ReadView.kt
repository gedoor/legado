package io.legado.app.ui.book.read.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.widget.FrameLayout
import io.legado.app.constant.PageAnim
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.lib.theme.accentColor
import io.legado.app.model.ReadAloud
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.ContentEditDialog
import io.legado.app.ui.book.read.page.api.DataSource
import io.legado.app.ui.book.read.page.delegate.*
import io.legado.app.ui.book.read.page.entities.PageDirection
import io.legado.app.ui.book.read.page.entities.TextChapter
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.ui.book.read.page.entities.TextPos
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.ui.book.read.page.provider.TextPageFactory
import io.legado.app.utils.*
import java.text.BreakIterator
import java.util.*
import kotlin.math.abs

/**
 * 阅读视图
 */
class ReadView(context: Context, attrs: AttributeSet) :
    FrameLayout(context, attrs),
    DataSource {

    val callBack: CallBack get() = activity as CallBack
    var pageFactory: TextPageFactory = TextPageFactory(this)
    var pageDelegate: PageDelegate? = null
        private set(value) {
            field?.onDestroy()
            field = null
            field = value
            upContent()
        }
    var isScroll = false
    val prevPage by lazy { PageView(context) }
    val curPage by lazy { PageView(context) }
    val nextPage by lazy { PageView(context) }
    val defaultAnimationSpeed = 300
    private var pressDown = false
    private var isMove = false
    private var isPageMove = false

    //起始点
    var startX: Float = 0f
    var startY: Float = 0f

    //上一个触碰点
    var lastX: Float = 0f
    var lastY: Float = 0f

    //触碰点
    var touchX: Float = 0f
    var touchY: Float = 0f

    //是否停止动画动作
    var isAbortAnim = false

    //长按
    private var longPressed = false
    private val longPressTimeout = 600L
    private val longPressRunnable = Runnable {
        longPressed = true
        onLongPress()
    }
    var isTextSelected = false
    private var pressOnTextSelected = false
    private val initialTextPos = TextPos(0, 0, 0)

    val slopSquare by lazy { ViewConfiguration.get(context).scaledTouchSlop }
    private var pageSlopSquare: Int = slopSquare
    private val tlRect = RectF()
    private val tcRect = RectF()
    private val trRect = RectF()
    private val mlRect = RectF()
    private val mcRect = RectF()
    private val mrRect = RectF()
    private val blRect = RectF()
    private val bcRect = RectF()
    private val brRect = RectF()
    private val autoPageRect by lazy { Rect() }
    private val autoPagePint by lazy { Paint().apply { color = context.accentColor } }
    private val boundary by lazy { BreakIterator.getWordInstance(Locale.getDefault()) }

    init {
        addView(nextPage)
        addView(curPage)
        addView(prevPage)
        nextPage.invisible()
        prevPage.invisible()
        curPage.markAsMainView()
        if (!isInEditMode) {
            upBg()
            setWillNotDraw(false)
            upPageAnim()
            upPageSlopSquare()
        }
    }

    private fun setRect9x() {
        tlRect.set(0f, 0f, width * 0.33f, height * 0.33f)
        tcRect.set(width * 0.33f, 0f, width * 0.66f, height * 0.33f)
        trRect.set(width * 0.36f, 0f, width.toFloat(), height * 0.33f)
        mlRect.set(0f, height * 0.33f, width * 0.33f, height * 0.66f)
        mcRect.set(width * 0.33f, height * 0.33f, width * 0.66f, height * 0.66f)
        mrRect.set(width * 0.66f, height * 0.33f, width.toFloat(), height * 0.66f)
        blRect.set(0f, height * 0.66f, width * 0.33f, height.toFloat())
        bcRect.set(width * 0.33f, height * 0.66f, width * 0.66f, height.toFloat())
        brRect.set(width * 0.66f, height * 0.66f, width.toFloat(), height.toFloat())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setRect9x()
        prevPage.x = -w.toFloat()
        pageDelegate?.setViewSize(w, h)
        if (w > 0 && h > 0) {
            upBg()
            callBack.upSystemUiVisibility()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        pageDelegate?.onDraw(canvas)
        if (!isInEditMode && callBack.isAutoPage && !isScroll) {
            // 自动翻页
            nextPage.screenshot()?.let {
                val bottom = callBack.autoPageProgress
                autoPageRect.set(0, 0, width, bottom)
                canvas.drawBitmap(it, autoPageRect, autoPageRect, null)
                canvas.drawRect(
                    0f,
                    bottom.toFloat() - 1,
                    width.toFloat(),
                    bottom.toFloat(),
                    autoPagePint
                )
                it.recycle()
            }
        }
    }

    override fun computeScroll() {
        pageDelegate?.scroll()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    /**
     * 触摸事件
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets =
                this.rootWindowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.mandatorySystemGestures())
            val height = activity?.windowManager?.currentWindowMetrics?.bounds?.height()
            if (height != null) {
                if (event.y > height.minus(insets.bottom)) {
                    return true
                }
            }
        }

        //在多点触控时，事件不走ACTION_DOWN分支而产生的特殊事件处理
        if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_UP){
            pageDelegate?.onTouch(event)
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                callBack.screenOffTimerStart()
                if (isTextSelected) {
                    curPage.cancelSelect()
                    isTextSelected = false
                    pressOnTextSelected = true
                } else {
                    pressOnTextSelected = false
                }
                longPressed = false
                postDelayed(longPressRunnable, longPressTimeout)
                pressDown = true
                isMove = false
                isPageMove = false
                pageDelegate?.onTouch(event)
                pageDelegate?.onDown()
                setStartPoint(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isMove) {
                    isMove =
                        abs(startX - event.x) > slopSquare || abs(startY - event.y) > slopSquare
                }
                if (!isPageMove) {
                    isPageMove =
                        abs(startX - event.x) > pageSlopSquare || abs(startY - event.y) > pageSlopSquare
                }
                if (isMove) {
                    longPressed = false
                    removeCallbacks(longPressRunnable)
                    if (isTextSelected) {
                        selectText(event.x, event.y)
                    } else if (isPageMove) {
                        pageDelegate?.onTouch(event)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                callBack.screenOffTimerStart()
                removeCallbacks(longPressRunnable)
                if (!pressDown) return true
                pressDown = false
                if (!isPageMove) {
                    if (!longPressed && !pressOnTextSelected) {
                        if (!curPage.onClick(startX, startY)) {
                            onSingleTapUp()
                        }
                        return true
                    }
                }
                if (isTextSelected) {
                    callBack.showTextActionMenu()
                } else if (isPageMove) {
                    pageDelegate?.onTouch(event)
                }
                pressOnTextSelected = false
            }
            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPressRunnable)
                if (!pressDown) return true
                pressDown = false
                if (isTextSelected) {
                    callBack.showTextActionMenu()
                } else if (isPageMove) {
                    pageDelegate?.onTouch(event)
                }
                pressOnTextSelected = false
            }
        }
        return true
    }

    /**
     * 更新状态栏
     */
    fun upStatusBar() {
        curPage.upStatusBar()
        prevPage.upStatusBar()
        nextPage.upStatusBar()
    }

    /**
     * 保存开始位置
     */
    fun setStartPoint(x: Float, y: Float, invalidate: Boolean = true) {
        startX = x
        startY = y
        lastX = x
        lastY = y
        touchX = x
        touchY = y

        if (invalidate) {
            invalidate()
        }
    }

    /**
     * 保存当前位置
     */
    fun setTouchPoint(x: Float, y: Float, invalidate: Boolean = true) {
        lastX = touchX
        lastY = touchY
        touchX = x
        touchY = y
        if (invalidate) {
            invalidate()
        }
        pageDelegate?.onScroll()
    }

    /**
     * 长按选择
     */
    private fun onLongPress() {
        kotlin.runCatching {
            curPage.longPress(startX, startY) { textPos: TextPos ->
                isTextSelected = true
                pressOnTextSelected = true
                initialTextPos.upData(textPos)
                val startPos = textPos.copy()
                val endPos = textPos.copy()
                val page = curPage.relativePage(textPos.relativePagePos)
                val stringBuilder = StringBuilder()
                var cIndex = textPos.columnIndex
                var lineStart = textPos.lineIndex
                var lineEnd = textPos.lineIndex
                for (index in textPos.lineIndex - 1 downTo 0) {
                    val textLine = page.getLine(index)
                    if (textLine.isParagraphEnd) {
                        break
                    } else {
                        stringBuilder.insert(0, textLine.text)
                        lineStart -= 1
                        cIndex += textLine.charSize
                    }
                }
                for (index in textPos.lineIndex until page.lineSize) {
                    val textLine = page.getLine(index)
                    stringBuilder.append(textLine.text)
                    lineEnd += 1
                    if (textLine.isParagraphEnd) {
                        break
                    }
                }
                var start: Int
                var end: Int
                boundary.setText(stringBuilder.toString())
                start = boundary.first()
                end = boundary.next()
                while (end != BreakIterator.DONE) {
                    if (cIndex in start until end) {
                        break
                    }
                    start = end
                    end = boundary.next()
                }
                kotlin.run {
                    var ci = 0
                    for (index in lineStart..lineEnd) {
                        val textLine = page.getLine(index)
                        for (j in 0 until textLine.charSize) {
                            if (ci == start) {
                                startPos.lineIndex = index
                                startPos.columnIndex = j
                            } else if (ci == end - 1) {
                                endPos.lineIndex = index
                                endPos.columnIndex = j
                                return@run
                            }
                            ci++
                        }
                    }
                }
                curPage.selectStartMoveIndex(
                    startPos.relativePagePos,
                    startPos.lineIndex,
                    startPos.columnIndex
                )
                curPage.selectEndMoveIndex(
                    endPos.relativePagePos,
                    endPos.lineIndex,
                    endPos.columnIndex
                )
            }
        }
    }

    /**
     * 单击
     */
    private fun onSingleTapUp() {
        when {
            isTextSelected -> Unit
            mcRect.contains(startX, startY) -> if (!isAbortAnim) {
                click(AppConfig.clickActionMC)
            }
            bcRect.contains(startX, startY) -> {
                click(AppConfig.clickActionBC)
            }
            blRect.contains(startX, startY) -> {
                click(AppConfig.clickActionBL)
            }
            brRect.contains(startX, startY) -> {
                click(AppConfig.clickActionBR)
            }
            mlRect.contains(startX, startY) -> {
                click(AppConfig.clickActionML)
            }
            mrRect.contains(startX, startY) -> {
                click(AppConfig.clickActionMR)
            }
            tlRect.contains(startX, startY) -> {
                click(AppConfig.clickActionTL)
            }
            tcRect.contains(startX, startY) -> {
                click(AppConfig.clickActionTC)
            }
            trRect.contains(startX, startY) -> {
                click(AppConfig.clickActionTR)
            }
        }
    }

    /**
     * 点击
     */
    private fun click(action: Int) {
        when (action) {
            0 -> {
                pageDelegate?.dismissSnackBar()
                callBack.showActionMenu()
            }
            1 -> pageDelegate?.nextPageByAnim(defaultAnimationSpeed)
            2 -> pageDelegate?.prevPageByAnim(defaultAnimationSpeed)
            3 -> ReadBook.moveToNextChapter(true)
            4 -> ReadBook.moveToPrevChapter(upContent = true, toLast = false)
            5 -> ReadAloud.prevParagraph(context)
            6 -> ReadAloud.nextParagraph(context)
            7 -> callBack.addBookmark()
            8 -> activity?.showDialogFragment(ContentEditDialog())
            9 -> callBack.changeReplaceRuleState()
            10 -> callBack.openChapterList()
            11 -> callBack.openSearchActivity(null)
        }
    }

    /**
     * 选择文本
     */
    private fun selectText(x: Float, y: Float) {
        curPage.selectText(x, y) { textPos ->
            val compare = initialTextPos.compare(textPos)
            when {
                compare >= 0 -> {
                    curPage.selectStartMoveIndex(
                        textPos.relativePagePos,
                        textPos.lineIndex,
                        textPos.columnIndex
                    )
                    curPage.selectEndMoveIndex(
                        initialTextPos.relativePagePos,
                        initialTextPos.lineIndex,
                        initialTextPos.columnIndex
                    )
                }
                else -> {
                    curPage.selectStartMoveIndex(
                        initialTextPos.relativePagePos,
                        initialTextPos.lineIndex,
                        initialTextPos.columnIndex
                    )
                    curPage.selectEndMoveIndex(
                        textPos.relativePagePos,
                        textPos.lineIndex,
                        textPos.columnIndex
                    )
                }
            }
        }
    }

    /**
     * 销毁事件
     */
    fun onDestroy() {
        pageDelegate?.onDestroy()
        curPage.cancelSelect()
    }

    /**
     * 翻页动画完成后事件
     * @param direction 翻页翻页反向
     */
    fun fillPage(direction: PageDirection): Boolean {
        return when (direction) {
            PageDirection.PREV -> {
                pageFactory.moveToPrev(true)
            }
            PageDirection.NEXT -> {
                pageFactory.moveToNext(true)
            }
            else -> false
        }
    }

    /**
     * 更新翻页动画
     */
    fun upPageAnim() {
        isScroll = ReadBook.pageAnim() == 3
        ChapterProvider.upLayout()
        when (ReadBook.pageAnim()) {
            PageAnim.coverPageAnim -> if (pageDelegate !is CoverPageDelegate) {
                pageDelegate = CoverPageDelegate(this)
            }
            PageAnim.slidePageAnim -> if (pageDelegate !is SlidePageDelegate) {
                pageDelegate = SlidePageDelegate(this)
            }
            PageAnim.simulationPageAnim -> if (pageDelegate !is SimulationPageDelegate) {
                pageDelegate = SimulationPageDelegate(this)
            }
            PageAnim.scrollPageAnim -> if (pageDelegate !is ScrollPageDelegate) {
                pageDelegate = ScrollPageDelegate(this)
            }
            else -> if (pageDelegate !is NoAnimPageDelegate) {
                pageDelegate = NoAnimPageDelegate(this)
            }
        }
        (pageDelegate as? ScrollPageDelegate)?.noAnim = AppConfig.noAnimScrollPage
    }

    /**
     * 更新阅读内容
     * @param relativePosition 相对位置 -1 上一页 0 当前页 1 下一页
     * @param resetPageOffset 滚动阅读是是否重置位置
     */
    override fun upContent(relativePosition: Int, resetPageOffset: Boolean) {
        curPage.setContentDescription(pageFactory.curPage.text)
        if (isScroll && !callBack.isAutoPage) {
            curPage.setContent(pageFactory.curPage, resetPageOffset)
        } else {
            curPage.resetPageOffset()
            when (relativePosition) {
                -1 -> prevPage.setContent(pageFactory.prevPage)
                1 -> nextPage.setContent(pageFactory.nextPage)
                else -> {
                    curPage.setContent(pageFactory.curPage)
                    nextPage.setContent(pageFactory.nextPage)
                    prevPage.setContent(pageFactory.prevPage)
                }
            }
        }
        callBack.screenOffTimerStart()
    }

    /**
     * 更新滑动距离
     */
    fun upPageSlopSquare() {
        val pageTouchSlop = AppConfig.pageTouchSlop
        this.pageSlopSquare = if (pageTouchSlop == 0) slopSquare else pageTouchSlop
    }

    /**
     * 更新样式
     */
    fun upStyle() {
        ChapterProvider.upStyle()
        curPage.upStyle()
        prevPage.upStyle()
        nextPage.upStyle()
    }

    /**
     * 更新背景
     */
    fun upBg() {
        ReadBookConfig.upBg(width, height)
        curPage.upBg()
        prevPage.upBg()
        nextPage.upBg()
    }

    /**
     * 更新背景透明度
     */
    fun upBgAlpha() {
        curPage.upBgAlpha()
        prevPage.upBgAlpha()
        nextPage.upBgAlpha()
    }

    /**
     * 更新时间信息
     */
    fun upTime() {
        curPage.upTime()
        prevPage.upTime()
        nextPage.upTime()
    }

    /**
     * 更新电量信息
     */
    fun upBattery(battery: Int) {
        curPage.upBattery(battery)
        prevPage.upBattery(battery)
        nextPage.upBattery(battery)
    }

    /**
     * 从选择位置开始朗读
     */
    fun aloudStartSelect() {
        val selectStartPos = curPage.selectStartPos
        var pagePos = selectStartPos.relativePagePos
        val line = selectStartPos.lineIndex
        val column = selectStartPos.columnIndex
        while (pagePos > 0) {
            if (!ReadBook.moveToNextPage()) {
                ReadBook.moveToNextChapter(false)
            }
            pagePos--
        }
        val startPos = curPage.textPage.getPosByLineColumn(line, column)
        ReadAloud.play(context, startPos = startPos)
    }

    /**
     * @return 选择的文本
     */
    fun getSelectText(): String {
        return curPage.selectedText
    }

    fun getCurVisiblePage(): TextPage {
        return curPage.getCurVisiblePage()
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
        fun showActionMenu()
        fun screenOffTimerStart()
        fun showTextActionMenu()
        fun autoPageStop()
        fun openChapterList()
        fun addBookmark()
        fun changeReplaceRuleState()
        fun openSearchActivity(searchWord: String?)
        fun upSystemUiVisibility()
    }
}
