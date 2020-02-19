package io.legado.app.ui.book.read.page.delegate

import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import io.legado.app.ui.book.read.page.PageView
import io.legado.app.utils.screenshot
import kotlin.math.*

@Suppress("DEPRECATION")
class SimulationPageDelegate(pageView: PageView) : HorizontalPageDelegate(pageView) {
    //不让x,y为0,否则在点计算时会有问题
    private var mTouchX = 0.1f
    private var mTouchY = 0.1f
    // 拖拽点对应的页脚
    private var mCornerX = 1
    private var mCornerY = 1
    private val mPath0: Path = Path()
    private val mPath1: Path = Path()
    // 贝塞尔曲线起始点
    private val mBezierStart1 = PointF()
    // 贝塞尔曲线控制点
    private val mBezierControl1 = PointF()
    // 贝塞尔曲线顶点
    private val mBezierVertex1 = PointF()
    // 贝塞尔曲线结束点
    private var mBezierEnd1 = PointF()

    // 另一条贝塞尔曲线
    // 贝塞尔曲线起始点
    private val mBezierStart2 = PointF()
    // 贝塞尔曲线控制点
    private val mBezierControl2 = PointF()
    // 贝塞尔曲线顶点
    private val mBezierVertex2 = PointF()
    // 贝塞尔曲线结束点
    private var mBezierEnd2 = PointF()

    private var mMiddleX = 0f
    private var mMiddleY = 0f
    private var mDegrees = 0f
    private var mTouchToCornerDis = 0f
    private var mColorMatrixFilter: ColorMatrixColorFilter? = null
    private val mMatrix: Matrix = Matrix()
    private val mMatrixArray = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f)

    // 是否属于右上左下
    private var mIsRtOrLb = false
    private var mMaxLength = 0f
    // 背面颜色组
    private var mBackShadowColors: IntArray? = null
    // 前面颜色组
    private var mFrontShadowColors: IntArray? = null
    // 有阴影的GradientDrawable
    private var mBackShadowDrawableLR: GradientDrawable? = null
    private var mBackShadowDrawableRL: GradientDrawable? = null
    private var mFolderShadowDrawableLR: GradientDrawable? = null
    private var mFolderShadowDrawableRL: GradientDrawable? = null

    private var mFrontShadowDrawableHBT: GradientDrawable? = null
    private var mFrontShadowDrawableHTB: GradientDrawable? = null
    private var mFrontShadowDrawableVLR: GradientDrawable? = null
    private var mFrontShadowDrawableVRL: GradientDrawable? = null

    private val mPaint: Paint = Paint()

    private var curBitmap: Bitmap? = null
    private var prevBitmap: Bitmap? = null
    private var nextBitmap: Bitmap? = null

    init {
        mMaxLength = hypot(viewWidth.toDouble(), viewWidth.toDouble()).toFloat()
        mPaint.style = Paint.Style.FILL
        //设置颜色数组
        createDrawable()
        val cm = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        mColorMatrixFilter = ColorMatrixColorFilter(cm)
    }

    override fun setStartPoint(x: Float, y: Float, invalidate: Boolean) {
        super.setStartPoint(x, y, invalidate)
        calcCornerXY(x, y)
    }

    override fun setTouchPoint(x: Float, y: Float, invalidate: Boolean) {
        super.setTouchPoint(x, y, invalidate)
        //触摸y中间位置吧y变成屏幕高度
        if ((startY > viewHeight * 0.33 && startY < viewHeight * 0.66)
            || mDirection == Direction.PREV
        ) {
            touchY = viewHeight.toFloat()
        }

        if (startY > viewHeight * 0.33 && startY < viewHeight / 2.0
            && mDirection == Direction.NEXT
        ) {
            touchY = 1f
        }
    }

    override fun setDirection(direction: Direction) {
        super.setDirection(direction)
        when (direction) {
            Direction.PREV ->
                //上一页滑动不出现对角
                if (startX > viewWidth / 2.0) {
                    calcCornerXY(startX, viewHeight.toFloat())
                } else {
                    calcCornerXY(viewWidth - startX, viewHeight.toFloat())
                }
            Direction.NEXT ->
                if (viewWidth / 2.0 > startX) {
                    calcCornerXY(viewWidth - startX, startY)
                }
            else -> Unit
        }
    }

    override fun setBitmap() {
        when (mDirection) {
            Direction.PREV -> {
                prevBitmap = prevPage.screenshot()
                curBitmap = curPage.screenshot()
            }
            Direction.NEXT -> {
                nextBitmap = nextPage.screenshot()
                curBitmap = curPage.screenshot()
            }
            else -> Unit
        }
    }

    override fun onScrollStart() {
        var dx: Float
        val dy: Float
        // dx 水平方向滑动的距离，负值会使滚动向左滚动
        // dy 垂直方向滑动的距离，负值会使滚动向上滚动
        if (isCancel) {
            dx = if (mCornerX > 0 && mDirection == Direction.NEXT) {
                (viewWidth - touchX)
            } else {
                -touchX
            }
            if (mDirection != Direction.NEXT) {
                dx = -(viewWidth + touchX)
            }
            dy = if (mCornerY > 0) {
                (viewHeight - touchY)
            } else {
                -touchY // 防止mTouchY最终变为0
            }
        } else {
            dx = if (mCornerX > 0 && mDirection == Direction.NEXT) {
                -(viewWidth + touchX)
            } else {
                (viewWidth - touchX + viewWidth)
            }
            dy = if (mCornerY > 0) {
                (viewHeight - touchY)
            } else {
                (1 - touchY) // 防止mTouchY最终变为0
            }
        }
        startScroll(touchX.toInt(), touchY.toInt(), dx.toInt(), dy.toInt())
    }

    override fun onScrollStop() {
        if (!isCancel) {
            pageView.fillPage(mDirection)
        }
        prevBitmap?.recycle()
        prevBitmap = null
        nextBitmap?.recycle()
        nextBitmap = null
        curBitmap?.recycle()
        curBitmap = null
    }

    override fun onDraw(canvas: Canvas) {
        if (mDirection === Direction.NEXT) {
            calcPoints()
            drawCurrentPageArea(canvas, curBitmap, mPath0)
            drawNextPageAreaAndShadow(canvas, nextBitmap)
            drawCurrentPageShadow(canvas)
            drawCurrentBackArea(canvas, curBitmap)
        } else {
            calcPoints()
            drawCurrentPageArea(canvas, prevBitmap, mPath0)
            drawNextPageAreaAndShadow(canvas, curBitmap)
            drawCurrentPageShadow(canvas)
            drawCurrentBackArea(canvas, prevBitmap)
        }
    }

    /**
     * 创建阴影的GradientDrawable
     */
    private fun createDrawable() {
        val color = intArrayOf(0x333333, -0x4fcccccd)
        mFolderShadowDrawableRL = GradientDrawable(
            GradientDrawable.Orientation.RIGHT_LEFT, color
        ).apply { gradientType = GradientDrawable.LINEAR_GRADIENT }

        mFolderShadowDrawableLR = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, color
        ).apply { gradientType = GradientDrawable.LINEAR_GRADIENT }

        mBackShadowColors = intArrayOf(-0xeeeeef, 0x111111)
        mBackShadowDrawableRL = GradientDrawable(
            GradientDrawable.Orientation.RIGHT_LEFT, mBackShadowColors
        ).apply { gradientType = GradientDrawable.LINEAR_GRADIENT }

        mBackShadowDrawableLR = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, mBackShadowColors
        ).apply { gradientType = GradientDrawable.LINEAR_GRADIENT }

        mFrontShadowColors = intArrayOf(-0x7feeeeef, 0x111111)
        mFrontShadowDrawableVLR = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, mFrontShadowColors
        ).apply { gradientType = GradientDrawable.LINEAR_GRADIENT }

        mFrontShadowDrawableVRL = GradientDrawable(
            GradientDrawable.Orientation.RIGHT_LEFT, mFrontShadowColors
        ).apply { gradientType = GradientDrawable.LINEAR_GRADIENT }

        mFrontShadowDrawableHTB = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, mFrontShadowColors
        ).apply { gradientType = GradientDrawable.LINEAR_GRADIENT }

        mFrontShadowDrawableHBT = GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP, mFrontShadowColors
        ).apply { gradientType = GradientDrawable.LINEAR_GRADIENT }
    }

    /**
     * 绘制翻起页背面
     */
    private fun drawCurrentBackArea(
        canvas: Canvas,
        bitmap: Bitmap?
    ) {
        bitmap ?: return
        val i = ((mBezierStart1.x + mBezierControl1.x) / 2).toInt()
        val f1 = abs(i - mBezierControl1.x)
        val i1 = ((mBezierStart2.y + mBezierControl2.y) / 2).toInt()
        val f2 = abs(i1 - mBezierControl2.y)
        val f3 = min(f1, f2)
        mPath1.reset()
        mPath1.moveTo(mBezierVertex2.x, mBezierVertex2.y)
        mPath1.lineTo(mBezierVertex1.x, mBezierVertex1.y)
        mPath1.lineTo(mBezierEnd1.x, mBezierEnd1.y)
        mPath1.lineTo(mTouchX, mTouchY)
        mPath1.lineTo(mBezierEnd2.x, mBezierEnd2.y)
        mPath1.close()
        val mFolderShadowDrawable: GradientDrawable
        val left: Int
        val right: Int
        if (mIsRtOrLb) {
            left = mBezierStart1.x.toInt() - 1
            right = (mBezierStart1.x + f3 + 1).toInt()
            mFolderShadowDrawable = mFolderShadowDrawableLR!!
        } else {
            left = (mBezierStart1.x - f3 - 1).toInt()
            right = (mBezierStart1.x + 1).toInt()
            mFolderShadowDrawable = mFolderShadowDrawableRL!!
        }
        canvas.save()
        kotlin.runCatching {
            canvas.clipPath(mPath0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipPath(mPath1)
            } else {
                canvas.clipPath(mPath1, Region.Op.INTERSECT)
            }
        }
        mPaint.colorFilter = mColorMatrixFilter
        val dis = hypot(
            mCornerX - mBezierControl1.x.toDouble(),
            mBezierControl2.y - mCornerY.toDouble()
        ).toFloat()
        val f8 = (mCornerX - mBezierControl1.x) / dis
        val f9 = (mBezierControl2.y - mCornerY) / dis
        mMatrixArray[0] = 1 - 2 * f9 * f9
        mMatrixArray[1] = 2 * f8 * f9
        mMatrixArray[3] = mMatrixArray[1]
        mMatrixArray[4] = 1 - 2 * f8 * f8
        mMatrix.reset()
        mMatrix.setValues(mMatrixArray)
        mMatrix.preTranslate(-mBezierControl1.x, -mBezierControl1.y)
        mMatrix.postTranslate(mBezierControl1.x, mBezierControl1.y)
        canvas.drawBitmap(bitmap, mMatrix, mPaint)
        mPaint.colorFilter = null
        canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y)
        mFolderShadowDrawable.setBounds(
            left, mBezierStart1.y.toInt(),
            right, (mBezierStart1.y + mMaxLength).toInt()
        )
        mFolderShadowDrawable.draw(canvas)
        canvas.restore()
    }

    /**
     * 绘制翻起页的阴影
     */
    private fun drawCurrentPageShadow(canvas: Canvas) {
        val degree: Double = if (mIsRtOrLb) {
            (Math.PI / 4 - atan2(mBezierControl1.y - mTouchX, mTouchX - mBezierControl1.x))
        } else {
            (Math.PI / 4 - atan2(mTouchY - mBezierControl1.y, mTouchX - mBezierControl1.x))
        }
        // 翻起页阴影顶点与touch点的距离
        val d1 = 25.toFloat() * 1.414 * cos(degree)
        val d2 = 25.toFloat() * 1.414 * sin(degree)
        val x = (mTouchX + d1).toFloat()
        val y: Float = if (mIsRtOrLb) {
            (mTouchY + d2).toFloat()
        } else {
            (mTouchY - d2).toFloat()
        }
        mPath1.reset()
        mPath1.moveTo(x, y)
        mPath1.lineTo(mTouchX, mTouchY)
        mPath1.lineTo(mBezierControl1.x, mBezierControl1.y)
        mPath1.lineTo(mBezierStart1.x, mBezierStart1.y)
        mPath1.close()
        canvas.save()
        kotlin.runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(mPath0)
            } else {
                canvas.clipPath(mPath0, Region.Op.XOR)
            }
            canvas.clipPath(mPath1, Region.Op.INTERSECT)
        }
        var leftX: Int
        var rightX: Int
        var mCurrentPageShadow: GradientDrawable
        if (mIsRtOrLb) {
            leftX = mBezierControl1.x.toInt()
            rightX = mBezierControl1.x.toInt() + 25
            mCurrentPageShadow = mFrontShadowDrawableVLR!!
        } else {
            leftX = mBezierControl1.x.toInt() - 25
            rightX = mBezierControl1.x.toInt() + 1
            mCurrentPageShadow = mFrontShadowDrawableVRL!!
        }
        var rotateDegrees: Float =
            Math.toDegrees(
                atan2(mTouchX - mBezierControl1.x, mBezierControl1.y - mTouchY).toDouble()
            )
                .toFloat()
        canvas.rotate(rotateDegrees, mBezierControl1.x, mBezierControl1.y)
        mCurrentPageShadow.setBounds(
            leftX, (mBezierControl1.y - mMaxLength).toInt(),
            rightX, mBezierControl1.y.toInt()
        )
        mCurrentPageShadow.draw(canvas)
        canvas.restore()

        mPath1.reset()
        mPath1.moveTo(x, y)
        mPath1.lineTo(mTouchX, mTouchY)
        mPath1.lineTo(mBezierControl2.x, mBezierControl2.y)
        mPath1.lineTo(mBezierStart2.x, mBezierStart2.y)
        mPath1.close()
        canvas.save()
        kotlin.runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(mPath0)
            } else {
                canvas.clipPath(mPath0, Region.Op.XOR)
            }
            canvas.clipPath(mPath1)
        }
        if (mIsRtOrLb) {
            leftX = mBezierControl2.y.toInt()
            rightX = mBezierControl2.y.toInt() + 25
            mCurrentPageShadow = mFrontShadowDrawableHTB!!
        } else {
            leftX = mBezierControl2.y.toInt() - 25
            rightX = mBezierControl2.y.toInt() + 1
            mCurrentPageShadow = mFrontShadowDrawableHBT!!
        }
        rotateDegrees = Math.toDegrees(
            atan2(mBezierControl2.y - mTouchY, mBezierControl2.x - mTouchX).toDouble()
        ).toFloat()
        canvas.rotate(rotateDegrees, mBezierControl2.x, mBezierControl2.y)
        val temp: Float =
            if (mBezierControl2.y < 0) mBezierControl2.y - viewHeight else mBezierControl2.y
        val hmg = hypot(mBezierControl2.x.toDouble(), temp.toDouble()).toInt()
        if (hmg > mMaxLength)
            mCurrentPageShadow.setBounds(
                (mBezierControl2.x - 25).toInt() - hmg, leftX,
                (mBezierControl2.x + mMaxLength).toInt() - hmg, rightX
            )
        else
            mCurrentPageShadow.setBounds(
                (mBezierControl2.x - mMaxLength).toInt(), leftX,
                mBezierControl2.x.toInt(), rightX
            )
        mCurrentPageShadow.draw(canvas)
        canvas.restore()
    }

    private fun drawNextPageAreaAndShadow(
        canvas: Canvas,
        bitmap: Bitmap?
    ) {
        bitmap ?: return
        mPath1.reset()
        mPath1.moveTo(mBezierStart1.x, mBezierStart1.y)
        mPath1.lineTo(mBezierVertex1.x, mBezierVertex1.y)
        mPath1.lineTo(mBezierVertex2.x, mBezierVertex2.y)
        mPath1.lineTo(mBezierStart2.x, mBezierStart2.y)
        mPath1.lineTo(mCornerX.toFloat(), mCornerY.toFloat())
        mPath1.close()
        mDegrees = Math.toDegrees(
            atan2(
                (mBezierControl1.x - mCornerX).toDouble(),
                mBezierControl2.y - mCornerY.toDouble()
            )
        ).toFloat()
        val leftX: Int
        val rightX: Int
        val mBackShadowDrawable: GradientDrawable
        if (mIsRtOrLb) { //左下及右上
            leftX = mBezierStart1.x.toInt()
            rightX = (mBezierStart1.x + mTouchToCornerDis / 4).toInt()
            mBackShadowDrawable = mBackShadowDrawableLR!!
        } else {
            leftX = (mBezierStart1.x - mTouchToCornerDis / 4).toInt()
            rightX = mBezierStart1.x.toInt()
            mBackShadowDrawable = mBackShadowDrawableRL!!
        }
        canvas.save()
        kotlin.runCatching {
            canvas.clipPath(mPath0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipPath(mPath1)
            } else {
                canvas.clipPath(mPath1, Region.Op.INTERSECT)
            }
        }

        canvas.drawBitmap(bitmap, 0f, 0f, null)
        canvas.rotate(mDegrees, mBezierStart1.x, mBezierStart1.y)
        mBackShadowDrawable.setBounds(
            leftX, mBezierStart1.y.toInt(),
            rightX, (mMaxLength + mBezierStart1.y).toInt()
        ) //左上及右下角的xy坐标值,构成一个矩形
        mBackShadowDrawable.draw(canvas)
        canvas.restore()
    }

    private fun drawCurrentPageArea(
        canvas: Canvas,
        bitmap: Bitmap?,
        path: Path
    ) {
        bitmap ?: return
        mPath0.reset()
        mPath0.moveTo(mBezierStart1.x, mBezierStart1.y)
        mPath0.quadTo(mBezierControl1.x, mBezierControl1.y, mBezierEnd1.x, mBezierEnd1.y)
        mPath0.lineTo(mTouchX, mTouchY)
        mPath0.lineTo(mBezierEnd2.x, mBezierEnd2.y)
        mPath0.quadTo(mBezierControl2.x, mBezierControl2.y, mBezierStart2.x, mBezierStart2.y)
        mPath0.lineTo(mCornerX.toFloat(), mCornerY.toFloat())
        mPath0.close()
        canvas.save()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutPath(path)
        } else {
            canvas.clipPath(path, Region.Op.XOR)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        kotlin.runCatching {
            canvas.restore()
        }
    }

    /**
     * 计算拖拽点对应的拖拽脚
     */
    private fun calcCornerXY(x: Float, y: Float) {
        mCornerX = if (x <= viewWidth / 2.0) 0 else viewWidth
        mCornerY = if (y <= viewHeight / 2.0) 0 else viewHeight
        mIsRtOrLb = (mCornerX == 0 && mCornerY == viewHeight)
                || (mCornerY == 0 && mCornerX == viewWidth)
    }

    private fun calcPoints() {
        mTouchX = touchX
        mTouchY = touchY
        mMiddleX = (mTouchX + mCornerX) / 2
        mMiddleY = (mTouchY + mCornerY) / 2
        mBezierControl1.x =
            mMiddleX - (mCornerY - mMiddleY) * (mCornerY - mMiddleY) / (mCornerX - mMiddleX)
        mBezierControl1.y = mCornerY.toFloat()

        mBezierControl2.x = mCornerX.toFloat()
        mBezierControl2.y = if ((mCornerY - mMiddleY).toInt() == 0) {
            mMiddleY - (mCornerX - mMiddleX) * (mCornerX - mMiddleX) / 0.1f
        } else {
            mMiddleY - (mCornerX - mMiddleX) * (mCornerX - mMiddleX) / (mCornerY - mMiddleY)
        }
        mBezierStart1.x = mBezierControl1.x - (mCornerX - mBezierControl1.x) / 2
        mBezierStart1.y = mCornerY.toFloat()
        //固定左边上下两个点
        if (mTouchX > 0 && mTouchX < viewWidth) {
            if (mBezierStart1.x < 0 || mBezierStart1.x > viewWidth) {
                if (mBezierStart1.x < 0)
                    mBezierStart1.x = viewWidth - mBezierStart1.x

                val f1: Float = abs(mCornerX - mTouchX)
                val f2: Float = viewWidth * f1 / mBezierStart1.x
                mTouchX = abs(mCornerX - f2)
                val f3: Float = abs(mCornerX - mTouchX) * abs(mCornerY - mTouchY) / f1
                mTouchY = abs(mCornerY - f3)

                mMiddleX = (mTouchX + mCornerX) / 2
                mMiddleY = (mTouchY + mCornerY) / 2
                mBezierControl1.x =
                    mMiddleX - (mCornerY - mMiddleY) * (mCornerY - mMiddleY) / (mCornerX - mMiddleX)
                mBezierControl1.y = mCornerY.toFloat()
                mBezierControl2.x = mCornerX.toFloat()
                mBezierControl2.y = if ((mCornerY - mMiddleY).toInt() == 0) {
                    mMiddleY - (mCornerX - mMiddleX) * (mCornerX - mMiddleX) / 0.1f
                } else {
                    mMiddleY - (mCornerX - mMiddleX) * (mCornerX - mMiddleX) / (mCornerY - mMiddleY)
                }
                mBezierStart1.x = (mBezierControl1.x - (mCornerX - mBezierControl1.x) / 2)
            }
        }
        mBezierStart2.x = mCornerX.toFloat()
        mBezierStart2.y = mBezierControl2.y - (mCornerY - mBezierControl2.y) / 2

        mTouchToCornerDis = hypot(mTouchX - mCornerX, touchY - mCornerY)

        mBezierEnd1 =
            getCross(PointF(mTouchX, mTouchY), mBezierControl1, mBezierStart1, mBezierStart2)
        mBezierEnd2 =
            getCross(PointF(mTouchX, mTouchY), mBezierControl2, mBezierStart1, mBezierStart2)

        mBezierVertex1.x = (mBezierStart1.x + 2 * mBezierControl1.x + mBezierEnd1.x) / 4
        mBezierVertex1.y = (2 * mBezierControl1.y + mBezierStart1.y + mBezierEnd1.y) / 4
        mBezierVertex2.x = (mBezierStart2.x + 2 * mBezierControl2.x + mBezierEnd2.x) / 4
        mBezierVertex2.y = (2 * mBezierControl2.y + mBezierStart2.y + mBezierEnd2.y) / 4
    }

    /**
     * 求解直线P1P2和直线P3P4的交点坐标
     */
    private fun getCross(P1: PointF, P2: PointF, P3: PointF, P4: PointF): PointF {
        val crossP = PointF()
        // 二元函数通式： y=ax+b
        val a1 = (P2.y - P1.y) / (P2.x - P1.x)
        val b1 = (P1.x * P2.y - P2.x * P1.y) / (P1.x - P2.x)
        val a2 = (P4.y - P3.y) / (P4.x - P3.x)
        val b2 = (P3.x * P4.y - P4.x * P3.y) / (P3.x - P4.x)
        crossP.x = (b2 - b1) / (a1 - a2)
        crossP.y = a1 * crossP.x + b1
        return crossP
    }
}