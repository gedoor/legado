package io.legado.app.ui.book.read.page.delegate

import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Trace
import android.view.MotionEvent
import io.legado.app.ui.book.read.page.PageView
import io.legado.app.utils.screenshot
import kotlin.math.hypot

class TestDelegate(pageView: PageView) : HorizontalPageDelegate(pageView) {
    private var pointPaint //绘制各标识点的画笔
            : Paint? = null
    private var bgPaint //背景画笔
            : Paint? = null
    private var pathAPaint //绘制A区域画笔
            : Paint? = null
    private var pathBPaint //绘制B区域画笔
            : Paint? = null
    private var pathCPaint //绘制C区域画笔
            : Paint? = null
    private var textPaint //绘制文字画笔
            : Paint? = null
    private var pathCContentPaint //绘制C区域内容画笔
            : Paint? = null
    private var a: MyPoint? = null
    private var f: MyPoint? = null
    private var g: MyPoint? = null
    private var e: MyPoint? = null
    private var h: MyPoint? = null
    private var c: MyPoint? = null
    private var j: MyPoint? = null
    private var b: MyPoint? = null
    private var k: MyPoint? = null
    private var d: MyPoint? = null
    private var i: MyPoint? = null
    private var pathA: Path? = null
    private var pathB: Path? = null
    private var pathC: Path? = null
    private var defaultWidth = 0 //默认宽度 = 0
    private var defaultHeight = 0 //默认高度 = 0
    var lPathAShadowDis = 0f //A区域左阴影矩形短边长度参考值
    var rPathAShadowDis = 0f //A区域右阴影矩形短边长度参考值
    private val mMatrixArray = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1.0f)
    private var mMatrix: Matrix? = null
    private var style: String? = null
    private var drawableLeftTopRight: GradientDrawable? = null
    private var drawableLeftLowerRight: GradientDrawable? = null
    private var drawableRightTopRight: GradientDrawable? = null
    private var drawableRightLowerRight: GradientDrawable? = null
    private var drawableHorizontalLowerRight: GradientDrawable? = null
    private var drawableBTopRight: GradientDrawable? = null
    private var drawableBLowerRight: GradientDrawable? = null
    private var drawableCTopRight: GradientDrawable? = null
    private var drawableCLowerRight: GradientDrawable? = null
    private val pathAContentBitmap
        get() = pageView.prevPage?.screenshot()
    private val pathBContentBitmap //B区域内容Bitmap
        get() = pageView.curPage?.screenshot()
    private val pathCContentBitmap //C区域内容Bitmap
        get() = pageView.nextPage?.screenshot()

    init {
        defaultWidth = 600
        defaultHeight = 1000
        a = MyPoint()
        f = MyPoint()
        g = MyPoint()
        e = MyPoint()
        h = MyPoint()
        c = MyPoint()
        j = MyPoint()
        b = MyPoint()
        k = MyPoint()
        d = MyPoint()
        i = MyPoint()
        pointPaint = Paint()
        pointPaint!!.color = Color.RED
        pointPaint!!.textSize = 25f
        pointPaint!!.style = Paint.Style.STROKE
        bgPaint = Paint()
        bgPaint!!.color = Color.GREEN
        pathAPaint = Paint()
        pathAPaint!!.color = Color.GREEN
        pathAPaint!!.isAntiAlias = true //设置抗锯齿
        pathBPaint = Paint()
        pathBPaint!!.color = Color.GREEN
        pathBPaint!!.isAntiAlias = true //设置抗锯齿
        //        pathBPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));我们不需要单独绘制path了，记得注释掉
        pathCPaint = Paint()
        pathCPaint!!.color = Color.YELLOW
        pathCPaint!!.isAntiAlias = true //设置抗锯齿
        //        pathCPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
//        pathCPaint.setStyle(Paint.Style.STROKE);
        pathCContentPaint = Paint()
        pathCContentPaint!!.color = Color.YELLOW
        pathCContentPaint!!.isAntiAlias = true //设置抗锯齿
        textPaint = Paint()
        textPaint!!.color = Color.BLACK
        textPaint!!.textAlign = Paint.Align.CENTER
        textPaint!!.isSubpixelText = true //设置自像素。如果该项为true，将有助于文本在LCD屏幕上的显示效果。
        textPaint!!.textSize = 30f
        pathA = Path()
        pathB = Path()
        pathC = Path()
        style = STYLE_LOWER_RIGHT
        mMatrix = Matrix()
        createGradientDrawable()
    }

    private fun drawPathAContentBitmap(
        bitmap: Bitmap?,
        pathPaint: Paint?
    ) {
        val mCanvas = Canvas(bitmap!!)
        //下面开始绘制区域内的内容...
        mCanvas.drawPath(pathDefault!!, pathPaint!!)
        mCanvas.drawText(
            "这是在A区域的内容...AAAA",
            viewWidth - 260.toFloat(),
            viewHeight - 100.toFloat(),
            textPaint!!
        )
        //结束绘制区域内的内容...
    }

    private fun drawPathBContentBitmap(
        bitmap: Bitmap?,
        pathPaint: Paint?
    ) {
        val mCanvas = Canvas(bitmap!!)
        //下面开始绘制区域内的内容...
        mCanvas.drawPath(pathDefault!!, pathPaint!!)
        mCanvas.drawText(
            "这是在B区域的内容...BBBB",
            viewWidth - 260.toFloat(),
            viewHeight - 100.toFloat(),
            textPaint!!
        )
        //结束绘制区域内的内容...
    }


    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.YELLOW)
        if (a!!.x == -1f && a!!.y == -1f) {
            drawPathAContent(canvas, pathDefault)
        } else {
            if (f!!.x == viewWidth.toFloat() && f!!.y == 0f) {
                drawPathAContent(canvas, pathAFromTopRight)
                drawPathCContent(canvas, pathAFromTopRight)
                drawPathBContent(canvas, pathAFromTopRight)
            } else if (f!!.x == viewWidth.toFloat() && f!!.y == viewHeight.toFloat()) {
                beginTrace("drawPathA")
                drawPathAContent(canvas, pathAFromLowerRight)
                endTrace()
                beginTrace("drawPathC")
                drawPathCContent(canvas, pathAFromLowerRight)
                endTrace()
                beginTrace("drawPathB")
                drawPathBContent(canvas, pathAFromLowerRight)
                endTrace()
            }
        }
    }

    private fun beginTrace(tag: String) {
        Trace.beginSection(tag)
    }

    private fun endTrace() {
        Trace.endSection()
    }

    override fun onScroll() {
        if (style == STYLE_TOP_RIGHT) {
            setTouchPoint(touchX, touchY, STYLE_TOP_RIGHT)
        } else {
            setTouchPoint(touchX, touchY, STYLE_LOWER_RIGHT)
        }
    }

    override fun onTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x: Float = event.x
                val y: Float = event.y
                if (x <= viewWidth / 3) { //左
                    style = STYLE_LEFT
                    setTouchPoint(x, y, style)
                } else if (x > viewWidth / 3 && y <= viewHeight / 3) { //上
                    style = STYLE_TOP_RIGHT
                    setTouchPoint(x, y, style)
                } else if (x > viewWidth * 2 / 3 && y > viewHeight / 3 && y <= viewHeight * 2 / 3) { //右
                    style = STYLE_RIGHT
                    setTouchPoint(x, y, style)
                } else if (x > viewWidth / 3 && y > viewHeight * 2 / 3) { //下
                    style = STYLE_LOWER_RIGHT
                    setTouchPoint(x, y, style)
                } else if (x > viewWidth / 3 && x < viewWidth * 2 / 3 && y > viewHeight / 3 && y < viewHeight * 2 / 3) { //中
                    style = STYLE_MIDDLE
                }
            }
            MotionEvent.ACTION_MOVE -> setTouchPoint(event.x, event.y, style)
            MotionEvent.ACTION_UP -> startCancelAnim()
        }
        return super.onTouch(event)
    }

    /**
     * 取消翻页动画,计算滑动位置与时间
     */
    fun startCancelAnim() {
        val dx: Int
        val dy: Int
        //让a滑动到f点所在位置，留出1像素是为了防止当a和f重叠时出现View闪烁的情况
        if (style == STYLE_TOP_RIGHT) {
            dx = (viewWidth - 1 - a!!.x).toInt()
            dy = (1 - a!!.y).toInt()
        } else {
            dx = (viewWidth - 1 - a!!.x).toInt()
            dy = (viewHeight - 1 - a!!.y).toInt()
        }
    }

    override fun onScrollStart() {
        val distanceX: Float
        when (direction) {
            Direction.NEXT -> distanceX =
                if (isCancel) {
                    var dis = viewWidth - startX + touchX
                    if (dis > viewWidth) {
                        dis = viewWidth.toFloat()
                    }
                    viewWidth - dis
                } else {
                    -(touchX + (viewWidth - startX))
                }
            else -> distanceX =
                if (isCancel) {
                    -(touchX - startX)
                } else {
                    viewWidth - (touchX - startX)
                }
        }

        startScroll(touchX.toInt(), 0, distanceX.toInt(), 0)
    }

    override fun onScrollStop() {

    }

    /**
     * 设置触摸点
     * @param x
     * @param y
     * @param style
     */
    fun setTouchPoint(x: Float, y: Float, style: String?) {
        val touchPoint: MyPoint
        a!!.x = x
        a!!.y = y
        this.style = style
        when (style) {
            STYLE_TOP_RIGHT -> {
                f!!.x = viewWidth.toFloat()
                f!!.y = 0f
                calcPointsXY(a, f)
                touchPoint = MyPoint(x, y)
                if (calcPointCX(touchPoint, f) < 0) { //如果c点x坐标小于0则重新测量a点坐标
                    calcPointAByTouchPoint()
                    calcPointsXY(a, f)
                }
                pageView.postInvalidate()
            }
            STYLE_LEFT, STYLE_RIGHT -> {
                a!!.y = viewHeight - 1.toFloat()
                f!!.x = viewWidth.toFloat()
                f!!.y = viewHeight.toFloat()
                calcPointsXY(a, f)
                pageView.postInvalidate()
            }
            STYLE_LOWER_RIGHT -> {
                f!!.x = viewWidth.toFloat()
                f!!.y = viewHeight.toFloat()
                calcPointsXY(a, f)
                touchPoint = MyPoint(x, y)
                if (calcPointCX(touchPoint, f) < 0) { //如果c点x坐标小于0则重新测量a点坐标
                    calcPointAByTouchPoint()
                    calcPointsXY(a, f)
                }
                pageView.postInvalidate()
            }
            else -> {
            }
        }
    }

    /**
     * 如果c点x坐标小于0,根据触摸点重新测量a点坐标
     */
    private fun calcPointAByTouchPoint() {
        val w0 = viewWidth - c!!.x
        val w1 = Math.abs(f!!.x - a!!.x)
        val w2 = viewWidth * w1 / w0
        a!!.x = Math.abs(f!!.x - w2)
        val h1 = Math.abs(f!!.y - a!!.y)
        val h2 = w2 * h1 / w1
        a!!.y = Math.abs(f!!.y - h2)
    }

    /**
     * 回到默认状态
     */
    fun setDefaultPath() {
        a!!.x = -1f
        a!!.y = -1f
        pageView.postInvalidate()
    }

    /**
     * 初始化各区域阴影GradientDrawable
     */
    private fun createGradientDrawable() {
        var deepColor = 0x33333333
        var lightColor = 0x01333333
        var gradientColors = intArrayOf(lightColor, deepColor) //渐变颜色数组
        drawableLeftTopRight =
            GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors)
        drawableLeftTopRight!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        drawableLeftLowerRight =
            GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, gradientColors)
        drawableLeftLowerRight!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        deepColor = 0x22333333
        lightColor = 0x01333333
        gradientColors = intArrayOf(deepColor, lightColor, lightColor)
        drawableRightTopRight =
            GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, gradientColors)
        drawableRightTopRight!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        drawableRightLowerRight =
            GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, gradientColors)
        drawableRightLowerRight!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        deepColor = 0x44333333
        lightColor = 0x01333333
        gradientColors = intArrayOf(lightColor, deepColor) //渐变颜色数组
        drawableHorizontalLowerRight =
            GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors)
        drawableHorizontalLowerRight!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        deepColor = 0x55111111
        lightColor = 0x00111111
        gradientColors = intArrayOf(deepColor, lightColor) //渐变颜色数组
        drawableBTopRight =
            GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors)
        drawableBTopRight!!.gradientType = GradientDrawable.LINEAR_GRADIENT //线性渐变
        drawableBLowerRight =
            GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, gradientColors)
        drawableBLowerRight!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        deepColor = 0x55333333
        lightColor = 0x00333333
        gradientColors = intArrayOf(lightColor, deepColor) //渐变颜色数组
        drawableCTopRight =
            GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradientColors)
        drawableCTopRight!!.gradientType = GradientDrawable.LINEAR_GRADIENT
        drawableCLowerRight =
            GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, gradientColors)
        drawableCLowerRight!!.gradientType = GradientDrawable.LINEAR_GRADIENT
    }

    /**
     * 绘制A区域内容
     * @param canvas
     * @param pathA
     */
    private fun drawPathAContent(
        canvas: Canvas,
        pathA: Path?
    ) {
        canvas.save()
        canvas.clipPath(pathA!!, Region.Op.INTERSECT) //对绘制内容进行裁剪，取和A区域的交集
        canvas.drawBitmap(pathAContentBitmap!!, 0f, 0f, null)
        if (style == STYLE_LEFT || style == STYLE_RIGHT) {
            drawPathAHorizontalShadow(canvas, pathA)
        } else {
            drawPathALeftShadow(canvas, pathA)
            drawPathARightShadow(canvas, pathA)
        }
        canvas.restore()
    }

    /**
     * 绘制A区域左阴影
     * @param canvas
     */
    private fun drawPathALeftShadow(
        canvas: Canvas,
        pathA: Path?
    ) {
        canvas.restore()
        canvas.save()
        val left: Int
        val right: Int
        val top = e!!.y.toInt()
        val bottom = (e!!.y + viewHeight).toInt()
        val gradientDrawable: GradientDrawable?
        if (style == STYLE_TOP_RIGHT) {
            gradientDrawable = drawableLeftTopRight
            left = (e!!.x - lPathAShadowDis / 2).toInt()
            right = e!!.x.toInt()
        } else {
            gradientDrawable = drawableLeftLowerRight
            left = e!!.x.toInt()
            right = (e!!.x + lPathAShadowDis / 2).toInt()
        }
        val mPath = Path()
        mPath.moveTo(a!!.x - Math.max(rPathAShadowDis, lPathAShadowDis) / 2, a!!.y)
        mPath.lineTo(d!!.x, d!!.y)
        mPath.lineTo(e!!.x, e!!.y)
        mPath.lineTo(a!!.x, a!!.y)
        mPath.close()
        canvas.clipPath(pathA!!)
        canvas.clipPath(mPath, Region.Op.INTERSECT)
        val mDegrees = Math.toDegrees(
            Math.atan2(
                e!!.x - a!!.x.toDouble(),
                a!!.y - e!!.y.toDouble()
            )
        ).toFloat()
        canvas.rotate(mDegrees, e!!.x, e!!.y)
        gradientDrawable!!.setBounds(left, top, right, bottom)
        gradientDrawable.draw(canvas)
    }

    /**
     * 绘制A区域右阴影
     * @param canvas
     */
    private fun drawPathARightShadow(
        canvas: Canvas,
        pathA: Path?
    ) {
        canvas.restore()
        canvas.save()
        val viewDiagonalLength = Math.hypot(
            viewWidth.toDouble(),
            viewHeight.toDouble()
        ).toFloat() //view对角线长度
        val left = h!!.x.toInt()
        val right = (h!!.x + viewDiagonalLength * 10).toInt() //需要足够长的长度
        val top: Int
        val bottom: Int
        val gradientDrawable: GradientDrawable?
        if (style == STYLE_TOP_RIGHT) {
            gradientDrawable = drawableRightTopRight
            top = (h!!.y - rPathAShadowDis / 2).toInt()
            bottom = h!!.y.toInt()
        } else {
            gradientDrawable = drawableRightLowerRight
            top = h!!.y.toInt()
            bottom = (h!!.y + rPathAShadowDis / 2).toInt()
        }
        gradientDrawable!!.setBounds(left, top, right, bottom)
        val mPath = Path()
        mPath.moveTo(a!!.x - Math.max(rPathAShadowDis, lPathAShadowDis) / 2, a!!.y)
        //        mPath.lineTo(i.x,i.y);
        mPath.lineTo(h!!.x, h!!.y)
        mPath.lineTo(a!!.x, a!!.y)
        mPath.close()
        canvas.clipPath(pathA!!)
        canvas.clipPath(mPath, Region.Op.INTERSECT)
        val mDegrees = Math.toDegrees(
            Math.atan2(
                a!!.y - h!!.y.toDouble(),
                a!!.x - h!!.x.toDouble()
            )
        ).toFloat()
        canvas.rotate(mDegrees, h!!.x, h!!.y)
        gradientDrawable.draw(canvas)
    }

    /**
     * 绘制A区域水平翻页阴影
     * @param canvas
     */
    private fun drawPathAHorizontalShadow(
        canvas: Canvas,
        pathA: Path?
    ) {
        canvas.restore()
        canvas.save()
        canvas.clipPath(pathA!!, Region.Op.INTERSECT)
        val maxShadowWidth = 30 //阴影矩形最大的宽度
        val left =
            (a!!.x - Math.min(maxShadowWidth.toFloat(), rPathAShadowDis / 2)).toInt()
        val right = a!!.x.toInt()
        val top = 0
        val bottom = viewHeight
        val gradientDrawable = drawableHorizontalLowerRight
        gradientDrawable!!.setBounds(left, top, right, bottom)
        val mDegrees = Math.toDegrees(
            Math.atan2(
                f!!.x - a!!.x.toDouble(),
                f!!.y - h!!.y.toDouble()
            )
        ).toFloat()
        canvas.rotate(mDegrees, a!!.x, a!!.y)
        gradientDrawable.draw(canvas)
    }

    /**
     * 绘制默认的界面
     * @return
     */
    private val pathDefault: Path?
        private get() {
            pathA!!.reset()
            pathA!!.lineTo(0f, viewHeight.toFloat())
            pathA!!.lineTo(viewWidth.toFloat(), viewHeight.toFloat())
            pathA!!.lineTo(viewWidth.toFloat(), 0f)
            pathA!!.close()
            return pathA
        }//移动到c点
    //从c到b画贝塞尔曲线，控制点为e
    //移动到a点
    //移动到k点
    //从k到j画贝塞尔曲线，控制点为h
    //移动到右下角
    //移动到左下角

    /**
     * 获取f点在右上角的pathA
     * @return
     */
    private val pathAFromTopRight: Path?
        private get() {
            pathA!!.reset()
            pathA!!.lineTo(c!!.x, c!!.y) //移动到c点
            pathA!!.quadTo(e!!.x, e!!.y, b!!.x, b!!.y) //从c到b画贝塞尔曲线，控制点为e
            pathA!!.lineTo(a!!.x, a!!.y) //移动到a点
            pathA!!.lineTo(k!!.x, k!!.y) //移动到k点
            pathA!!.quadTo(h!!.x, h!!.y, j!!.x, j!!.y) //从k到j画贝塞尔曲线，控制点为h
            pathA!!.lineTo(viewWidth.toFloat(), viewHeight.toFloat()) //移动到右下角
            pathA!!.lineTo(0f, viewHeight.toFloat()) //移动到左下角
            pathA!!.close()
            return pathA
        }//移动到左下角
    //移动到c点
    //从c到b画贝塞尔曲线，控制点为e
    //移动到a点
    //移动到k点
    //从k到j画贝塞尔曲线，控制点为h
    //移动到右上角
    //闭合区域

    /**
     * 获取f点在右下角的pathA
     * @return
     */
    private val pathAFromLowerRight: Path?
        private get() {
            pathA!!.reset()
            pathA!!.lineTo(0f, viewHeight.toFloat()) //移动到左下角
            pathA!!.lineTo(c!!.x, c!!.y) //移动到c点
            pathA!!.quadTo(e!!.x, e!!.y, b!!.x, b!!.y) //从c到b画贝塞尔曲线，控制点为e
            pathA!!.lineTo(a!!.x, a!!.y) //移动到a点
            pathA!!.lineTo(k!!.x, k!!.y) //移动到k点
            pathA!!.quadTo(h!!.x, h!!.y, j!!.x, j!!.y) //从k到j画贝塞尔曲线，控制点为h
            pathA!!.lineTo(viewWidth.toFloat(), 0f) //移动到右上角
            pathA!!.close() //闭合区域
            return pathA
        }

    /**
     * 绘制B区域内容
     * @param canvas
     * @param pathA
     */
    private fun drawPathBContent(
        canvas: Canvas,
        pathA: Path?
    ) {
        canvas.save()
        canvas.clipPath(pathA!!) //裁剪出A区域
        canvas.clipPath(getPathC()!!, Region.Op.INTERSECT) //裁剪出A和C区域的全集
        canvas.clipPath(
            getPathB()!!,
            Region.Op.DIFFERENCE
        ) //裁剪出B区域中不同于与AC区域的部分
        canvas.drawBitmap(pathBContentBitmap!!, 0f, 0f, null)
        drawPathBShadow(canvas)
        canvas.restore()
    }

    /**
     * 绘制B区域阴影，阴影左深右浅
     * @param canvas
     */
    private fun drawPathBShadow(canvas: Canvas) {
        val deepOffset = 0 //深色端的偏移值
        val lightOffset = 0 //浅色端的偏移值
        val aTof = Math.hypot(
            (a!!.x - f!!.x).toDouble(),
            (a!!.y - f!!.y).toDouble()
        ).toFloat() //a到f的距离
        val viewDiagonalLength = Math.hypot(
            viewWidth.toDouble(),
            viewHeight.toDouble()
        ).toFloat() //对角线长度
        val left: Int
        val right: Int
        val top = c!!.y.toInt()
        val bottom = (viewDiagonalLength + c!!.y).toInt()
        val gradientDrawable: GradientDrawable?
        if (style == STYLE_TOP_RIGHT) { //f点在右上角
//从左向右线性渐变
            gradientDrawable = drawableBTopRight
            left = (c!!.x - deepOffset).toInt() //c点位于左上角
            right = (c!!.x + aTof / 4 + lightOffset).toInt()
        } else { //从右向左线性渐变
            gradientDrawable = drawableBLowerRight
            left = (c!!.x - aTof / 4 - lightOffset).toInt() //c点位于左下角
            right = (c!!.x + deepOffset).toInt()
        }
        gradientDrawable!!.setBounds(left, top, right, bottom) //设置阴影矩形
        val rotateDegrees = Math.toDegrees(
            Math.atan2(
                e!!.x - f!!.x.toDouble(),
                h!!.y - f!!.y.toDouble()
            )
        ).toFloat() //旋转角度
        canvas.rotate(rotateDegrees, c!!.x, c!!.y) //以c为中心点旋转
        gradientDrawable.draw(canvas)
    }

    /**
     * 绘制区域B
     * @return
     */
    private fun getPathB(): Path? {
        pathB!!.reset()
        pathB!!.lineTo(0f, viewHeight.toFloat()) //移动到左下角
        pathB!!.lineTo(viewWidth.toFloat(), viewHeight.toFloat()) //移动到右下角
        pathB!!.lineTo(viewWidth.toFloat(), 0f) //移动到右上角
        pathB!!.close() //闭合区域
        return pathB
    }

    /**
     * 绘制C区域内容
     * @param canvas
     * @param pathA
     */
    private fun drawPathCContent(
        canvas: Canvas,
        pathA: Path?
    ) {
        canvas.save()
        canvas.clipPath(pathA!!)
        canvas.clipPath(getPathC()!!, Region.Op.DIFFERENCE) //裁剪出C区域不同于A区域的部分
        //        canvas.drawPath(getPathC(),pathCPaint);
        val eh =
            hypot(f!!.x - e!!.x.toDouble(), h!!.y - f!!.y.toDouble()).toFloat()
        val sin0 = (f!!.x - e!!.x) / eh
        val cos0 = (h!!.y - f!!.y) / eh
        //设置翻转和旋转矩阵
        mMatrixArray[0] = -(1 - 2 * sin0 * sin0)
        mMatrixArray[1] = 2 * sin0 * cos0
        mMatrixArray[3] = 2 * sin0 * cos0
        mMatrixArray[4] = 1 - 2 * sin0 * sin0
        mMatrix!!.reset()
        mMatrix!!.setValues(mMatrixArray) //翻转和旋转
        mMatrix!!.preTranslate(-e!!.x, -e!!.y) //沿当前XY轴负方向位移得到 矩形A₃B₃C₃D₃
        mMatrix!!.postTranslate(e!!.x, e!!.y) //沿原XY轴方向位移得到 矩形A4 B4 C4 D4
        canvas.drawBitmap(pathCContentBitmap!!, mMatrix!!, null)
        drawPathCShadow(canvas)
        canvas.restore()
    }

    /**
     * 绘制C区域阴影，阴影左浅右深
     * @param canvas
     */
    private fun drawPathCShadow(canvas: Canvas) {
        val deepOffset = 1 //深色端的偏移值
        val lightOffset = -30 //浅色端的偏移值
        val viewDiagonalLength = Math.hypot(
            viewWidth.toDouble(),
            viewHeight.toDouble()
        ).toFloat() //view对角线长度
        val midpoint_ce = (c!!.x + e!!.x).toInt() / 2 //ce中点
        val midpoint_jh = (j!!.y + h!!.y).toInt() / 2 //jh中点
        val minDisToControlPoint = Math.min(
            Math.abs(midpoint_ce - e!!.x),
            Math.abs(midpoint_jh - h!!.y)
        ) //中点到控制点的最小值
        val left: Int
        val right: Int
        val top = c!!.y.toInt()
        val bottom = (viewDiagonalLength + c!!.y).toInt()
        val gradientDrawable: GradientDrawable?
        if (style == STYLE_TOP_RIGHT) {
            gradientDrawable = drawableCTopRight
            left = (c!!.x - lightOffset).toInt()
            right = (c!!.x + minDisToControlPoint + deepOffset).toInt()
        } else {
            gradientDrawable = drawableCLowerRight
            left = (c!!.x - minDisToControlPoint - deepOffset).toInt()
            right = (c!!.x + lightOffset).toInt()
        }
        gradientDrawable!!.setBounds(left, top, right, bottom)
        val mDegrees = Math.toDegrees(
            Math.atan2(
                e!!.x - f!!.x.toDouble(),
                h!!.y - f!!.y.toDouble()
            )
        ).toFloat()
        canvas.rotate(mDegrees, c!!.x, c!!.y)
        gradientDrawable.draw(canvas)
    }

    /**
     * 绘制区域C
     * @return
     */
    private fun getPathC(): Path? {
        pathC!!.reset()
        pathC!!.moveTo(i!!.x, i!!.y) //移动到i点
        pathC!!.lineTo(d!!.x, d!!.y) //移动到d点
        pathC!!.lineTo(b!!.x, b!!.y) //移动到b点
        pathC!!.lineTo(a!!.x, a!!.y) //移动到a点
        pathC!!.lineTo(k!!.x, k!!.y) //移动到k点
        pathC!!.close() //闭合区域
        return pathC
    }

    /**
     * 计算各点坐标
     * @param a
     * @param f
     */
    private fun calcPointsXY(a: MyPoint?, f: MyPoint?) {
        g!!.x = (a!!.x + f!!.x) / 2
        g!!.y = (a.y + f.y) / 2
        e!!.x = g!!.x - (f.y - g!!.y) * (f.y - g!!.y) / (f.x - g!!.x)
        e!!.y = f.y
        h!!.x = f.x
        h!!.y = g!!.y - (f.x - g!!.x) * (f.x - g!!.x) / (f.y - g!!.y)
        c!!.x = e!!.x - (f.x - e!!.x) / 2
        c!!.y = f.y
        j!!.x = f.x
        j!!.y = h!!.y - (f.y - h!!.y) / 2
        b = getIntersectionPoint(a, e, c, j)
        k = getIntersectionPoint(a, h, c, j)
        d!!.x = (c!!.x + 2 * e!!.x + b!!.x) / 4
        d!!.y = (2 * e!!.y + c!!.y + b!!.y) / 4
        i!!.x = (j!!.x + 2 * h!!.x + k!!.x) / 4
        i!!.y = (2 * h!!.y + j!!.y + k!!.y) / 4
        //计算d点到ae的距离
        val lA = a.y - e!!.y
        val lB = e!!.x - a.x
        val lC = a.x * e!!.y - e!!.x * a.y
        lPathAShadowDis = Math.abs(
            (lA * d!!.x + lB * d!!.y + lC) / Math.hypot(
                lA.toDouble(),
                lB.toDouble()
            ).toFloat()
        )
        //计算i点到ah的距离
        val rA = a.y - h!!.y
        val rB = h!!.x - a.x
        val rC = a.x * h!!.y - h!!.x * a.y
        rPathAShadowDis = Math.abs(
            (rA * i!!.x + rB * i!!.y + rC) / Math.hypot(
                rA.toDouble(),
                rB.toDouble()
            ).toFloat()
        )
    }

    /**
     * 计算两线段相交点坐标
     * @param lineOne_My_pointOne
     * @param lineOne_My_pointTwo
     * @param lineTwo_My_pointOne
     * @param lineTwo_My_pointTwo
     * @return 返回该点
     */
    private fun getIntersectionPoint(
        lineOne_My_pointOne: MyPoint?,
        lineOne_My_pointTwo: MyPoint?,
        lineTwo_My_pointOne: MyPoint?,
        lineTwo_My_pointTwo: MyPoint?
    ): MyPoint {
        val x1: Float
        val y1: Float
        val x2: Float
        val y2: Float
        val x3: Float
        val y3: Float
        val x4: Float
        val y4: Float
        x1 = lineOne_My_pointOne!!.x
        y1 = lineOne_My_pointOne.y
        x2 = lineOne_My_pointTwo!!.x
        y2 = lineOne_My_pointTwo.y
        x3 = lineTwo_My_pointOne!!.x
        y3 = lineTwo_My_pointOne.y
        x4 = lineTwo_My_pointTwo!!.x
        y4 = lineTwo_My_pointTwo.y
        val pointX =
            (((x1 - x2) * (x3 * y4 - x4 * y3) - (x3 - x4) * (x1 * y2 - x2 * y1))
                    / ((x3 - x4) * (y1 - y2) - (x1 - x2) * (y3 - y4)))
        val pointY =
            (((y1 - y2) * (x3 * y4 - x4 * y3) - (x1 * y2 - x2 * y1) * (y3 - y4))
                    / ((y1 - y2) * (x3 - x4) - (x1 - x2) * (y3 - y4)))
        return MyPoint(pointX, pointY)
    }

    /**
     * 计算C点的X值
     * @param a
     * @param f
     * @return
     */
    private fun calcPointCX(a: MyPoint, f: MyPoint?): Float {
        val g: MyPoint
        val e: MyPoint
        g = MyPoint()
        e = MyPoint()
        g.x = (a.x + f!!.x) / 2
        g.y = (a.y + f.y) / 2
        e.x = g.x - (f.y - g.y) * (f.y - g.y) / (f.x - g.x)
        e.y = f.y
        return e.x - (f.x - e.x) / 2
    }

    fun getViewWidth(): Float {
        return viewWidth.toFloat()
    }

    fun getViewHeight(): Float {
        return viewHeight.toFloat()
    }

    internal inner class MyPoint {
        var x = 0f
        var y = 0f

        constructor() {}
        constructor(x: Float, y: Float) {
            this.x = x
            this.y = y
        }
    }

    companion object {
        const val STYLE_LEFT = "STYLE_LEFT" //点击左边区域
        const val STYLE_RIGHT = "STYLE_RIGHT" //点击右边区域
        const val STYLE_MIDDLE = "STYLE_MIDDLE" //点击中间区域
        const val STYLE_TOP_RIGHT = "STYLE_TOP_RIGHT" //f点在右上角
        const val STYLE_LOWER_RIGHT = "STYLE_LOWER_RIGHT" //f点在右下角
    }
}