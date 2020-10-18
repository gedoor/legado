package io.legado.app.ui.widget.image.photo

import android.graphics.PointF

import android.graphics.RectF
import android.widget.ImageView


@Suppress("MemberVisibilityCanBePrivate")
class Info(
    rect: RectF,
    img: RectF,
    widget: RectF,
    base: RectF,
    screenCenter: PointF,
    scale: Float,
    degrees: Float,
    scaleType: ImageView.ScaleType?
) {
    // 内部图片在整个手机界面的位置
    var mRect = RectF()

    // 控件在窗口的位置
    var mImgRect = RectF()

    var mWidgetRect = RectF()

    var mBaseRect = RectF()

    var mScreenCenter = PointF()

    var mScale = 0f

    var mDegrees = 0f

    var mScaleType: ImageView.ScaleType? = null

    init {
        mRect.set(rect)
        mImgRect.set(img)
        mWidgetRect.set(widget)
        mScale = scale
        mScaleType = scaleType
        mDegrees = degrees
        mBaseRect.set(base)
        mScreenCenter.set(screenCenter)
    }

}