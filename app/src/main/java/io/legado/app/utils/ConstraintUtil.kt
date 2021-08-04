package io.legado.app.utils

import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.TransitionManager


@Suppress("MemberVisibilityCanBePrivate", "unused")
class ConstraintUtil(private val constraintLayout: ConstraintLayout) {

    private var begin: ConstraintBegin? = null
    private val applyConstraintSet = ConstraintSet()
    private val resetConstraintSet = ConstraintSet()

    init {
        resetConstraintSet.clone(constraintLayout)
    }

    /**
     * 开始修改
     */
    fun begin(): ConstraintBegin {
        synchronized(ConstraintBegin::class.java) {
            if (begin == null) {
                begin = ConstraintBegin(constraintLayout, applyConstraintSet)
            }
        }
        applyConstraintSet.clone(constraintLayout)
        return begin!!
    }

    /**
     * 带动画的修改
     * @return
     */
    fun beginWithAnim(): ConstraintBegin? {
        TransitionManager.beginDelayedTransition(constraintLayout)
        return begin()
    }

    /**
     * 重置
     */
    fun reSet() {
        resetConstraintSet.applyTo(constraintLayout)
    }

    /**
     * 带动画的重置
     */
    fun reSetWidthAnim() {
        TransitionManager.beginDelayedTransition(constraintLayout)
        resetConstraintSet.applyTo(constraintLayout)
    }

}

@Suppress("unused", "MemberVisibilityCanBePrivate")
class ConstraintBegin(
    private val constraintLayout: ConstraintLayout,
    private val applyConstraintSet: ConstraintSet
) {

    /**
     * 清除关系<br></br>
     * 注意：这里不仅仅会清除关系，还会清除对应控件的宽高为 w:0,h:0
     * @param viewIds
     * @return
     */
    fun clear(@IdRes vararg viewIds: Int): ConstraintBegin {
        for (viewId in viewIds) {
            applyConstraintSet.clear(viewId)
        }
        return this
    }

    /**
     * 清除某个控件的，某个关系
     * @param viewId
     * @param anchor
     * @return
     */
    fun clear(viewId: Int, anchor: Int): ConstraintBegin {
        applyConstraintSet.clear(viewId, anchor)
        return this
    }

    fun setHorizontalWeight(viewId: Int, weight: Float): ConstraintBegin {
        applyConstraintSet.setHorizontalWeight(viewId, weight)
        return this
    }

    fun setVerticalWeight(viewId: Int, weight: Float): ConstraintBegin {
        applyConstraintSet.setVerticalWeight(viewId, weight)
        return this
    }

    /**
     * 为某个控件设置 margin
     * @param viewId 某个控件ID
     * @param left marginLeft
     * @param top   marginTop
     * @param right marginRight
     * @param bottom marginBottom
     * @return
     */
    fun setMargin(
        @IdRes viewId: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): ConstraintBegin {
        setMarginLeft(viewId, left)
        setMarginTop(viewId, top)
        setMarginRight(viewId, right)
        setMarginBottom(viewId, bottom)
        return this
    }

    /**
     * 为某个控件设置 marginLeft
     * @param viewId 某个控件ID
     * @param left marginLeft
     * @return
     */
    fun setMarginLeft(@IdRes viewId: Int, left: Int): ConstraintBegin {
        applyConstraintSet.setMargin(viewId, ConstraintSet.LEFT, left)
        return this
    }

    /**
     * 为某个控件设置 marginRight
     * @param viewId 某个控件ID
     * @param right marginRight
     * @return
     */
    fun setMarginRight(@IdRes viewId: Int, right: Int): ConstraintBegin {
        applyConstraintSet.setMargin(viewId, ConstraintSet.RIGHT, right)
        return this
    }

    /**
     * 为某个控件设置 marginTop
     * @param viewId 某个控件ID
     * @param top marginTop
     * @return
     */
    fun setMarginTop(@IdRes viewId: Int, top: Int): ConstraintBegin {
        applyConstraintSet.setMargin(viewId, ConstraintSet.TOP, top)
        return this
    }

    /**
     * 为某个控件设置marginBottom
     * @param viewId 某个控件ID
     * @param bottom marginBottom
     * @return
     */
    fun setMarginBottom(@IdRes viewId: Int, bottom: Int): ConstraintBegin {
        applyConstraintSet.setMargin(viewId, ConstraintSet.BOTTOM, bottom)
        return this
    }

    /**
     * 为某个控件设置关联关系 left_to_left_of
     * @param startId
     * @param endId
     * @return
     */
    fun leftToLeftOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
        applyConstraintSet.connect(startId, ConstraintSet.LEFT, endId, ConstraintSet.LEFT)
        return this
    }

    /**
     * 为某个控件设置关联关系 left_to_right_of
     * @param startId
     * @param endId
     * @return
     */
    fun leftToRightOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
        applyConstraintSet.connect(startId, ConstraintSet.LEFT, endId, ConstraintSet.RIGHT)
        return this
    }

    /**
     * 为某个控件设置关联关系 top_to_top_of
     * @param startId
     * @param endId
     * @return
     */
    fun topToTopOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
        applyConstraintSet.connect(startId, ConstraintSet.TOP, endId, ConstraintSet.TOP)
        return this
    }

    /**
     * 为某个控件设置关联关系 top_to_bottom_of
     * @param startId
     * @param endId
     * @return
     */
    fun topToBottomOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
        applyConstraintSet.connect(startId, ConstraintSet.TOP, endId, ConstraintSet.BOTTOM)
        return this
    }

    /**
     * 为某个控件设置关联关系 right_to_left_of
     * @param startId
     * @param endId
     * @return
     */
    fun rightToLeftOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
        applyConstraintSet.connect(startId, ConstraintSet.RIGHT, endId, ConstraintSet.LEFT)
        return this
    }

    /**
     * 为某个控件设置关联关系 right_to_right_of
     * @param startId
     * @param endId
     * @return
     */
    fun rightToRightOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
        applyConstraintSet.connect(startId, ConstraintSet.RIGHT, endId, ConstraintSet.RIGHT)
        return this
    }

    /**
     * 为某个控件设置关联关系 bottom_to_bottom_of
     * @param startId
     * @param endId
     * @return
     */
    fun bottomToBottomOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
        applyConstraintSet.connect(startId, ConstraintSet.BOTTOM, endId, ConstraintSet.BOTTOM)
        return this
    }

    /**
     * 为某个控件设置关联关系 bottom_to_top_of
     * @param startId
     * @param endId
     * @return
     */
    fun bottomToTopOf(@IdRes startId: Int, @IdRes endId: Int): ConstraintBegin {
        applyConstraintSet.connect(startId, ConstraintSet.BOTTOM, endId, ConstraintSet.TOP)
        return this
    }

    /**
     * 为某个控件设置宽度
     * @param viewId
     * @param width
     * @return
     */
    fun setWidth(@IdRes viewId: Int, width: Int): ConstraintBegin {
        applyConstraintSet.constrainWidth(viewId, width)
        return this
    }

    /**
     * 某个控件设置高度
     * @param viewId
     * @param height
     * @return
     */
    fun setHeight(@IdRes viewId: Int, height: Int): ConstraintBegin {
        applyConstraintSet.constrainHeight(viewId, height)
        return this
    }

    /**
     * 提交应用生效
     */
    fun commit() {
        applyConstraintSet.applyTo(constraintLayout)
    }
}
