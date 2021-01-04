package io.legado.app.ui.widget.text

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import androidx.appcompat.widget.AppCompatTextView
import io.legado.app.R
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.gone
import io.legado.app.utils.visible


/**
 * Created by milad heydari on 5/6/2016.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class BadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    var isHideOnNull = true
        set(hideOnNull) {
            field = hideOnNull
            text = text
        }
    private var radius: Float = 0.toFloat()
    private var flatangle: Boolean

    val badgeCount: Int?
        get() {
            if (text == null) {
                return null
            }
            val text = text.toString()
            return kotlin.runCatching {
                Integer.parseInt(text)
            }.getOrNull()
        }

    var badgeGravity: Int
        get() {
            val params = layoutParams as LayoutParams
            return params.gravity
        }
        set(gravity) {
            val params = layoutParams as LayoutParams
            params.gravity = gravity
            layoutParams = params
        }

    val badgeMargin: IntArray
        get() {
            val params = layoutParams as LayoutParams
            return intArrayOf(
                params.leftMargin,
                params.topMargin,
                params.rightMargin,
                params.bottomMargin
            )
        }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BadgeView)
        val radios =
            typedArray.getDimensionPixelOffset(R.styleable.BadgeView_radius, 8)
        flatangle =
            typedArray.getBoolean(R.styleable.BadgeView_up_flat_angle, false)
        typedArray.recycle()

        if (layoutParams !is LayoutParams) {
            val layoutParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            setLayoutParams(layoutParams)
        }

        // set default font
        setTextColor(Color.WHITE)
        //setTypeface(Typeface.DEFAULT_BOLD);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setPadding(dip2Px(5f), dip2Px(1f), dip2Px(5f), dip2Px(1f))
        radius = radios.toFloat()

        // set default background
        setBackground(radius, context.accentColor)

        gravity = Gravity.CENTER

        // default values
        isHideOnNull = true
        setBadgeCount(0)
        minWidth = dip2Px(16f)
        minHeight = dip2Px(16f)
    }

    fun setBackground(dipRadius: Float, badgeColor: Int) {
        val radius = dip2Px(dipRadius).toFloat()
        val radiusArray =
            floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)
        if (flatangle) { radiusArray.fill(0f, 0, 3) }

        val roundRect = RoundRectShape(radiusArray, null, null)
        val bgDrawable = ShapeDrawable(roundRect)
        bgDrawable.paint.color = badgeColor
        background = bgDrawable
    }

    fun setBackground(badgeColor: Int) {
        setBackground(radius, badgeColor)
    }

    /**
     * @see android.widget.TextView.setText
     */
    override fun setText(text: CharSequence, type: BufferType) {
        if (isHideOnNull && TextUtils.isEmpty(text)) {
            gone()
        } else {
            visible()
        }
        super.setText(text, type)
    }

    fun setBadgeCount(count: Int) {
        text = count.toString()
        if (count == 0) {
            gone()
        } else {
            visible()
        }
    }

    fun setHighlight(highlight: Boolean) {
        if (highlight) {
            setBackground(context.accentColor)
        } else {
            setBackground(context.getCompatColor(R.color.darker_gray))
        }
    }

    fun setBadgeMargin(dipMargin: Int) {
        setBadgeMargin(dipMargin, dipMargin, dipMargin, dipMargin)
    }

    fun setBadgeMargin(
        leftDipMargin: Int,
        topDipMargin: Int,
        rightDipMargin: Int,
        bottomDipMargin: Int
    ) {
        val params = layoutParams as LayoutParams
        params.leftMargin = dip2Px(leftDipMargin.toFloat())
        params.topMargin = dip2Px(topDipMargin.toFloat())
        params.rightMargin = dip2Px(rightDipMargin.toFloat())
        params.bottomMargin = dip2Px(bottomDipMargin.toFloat())
        layoutParams = params
    }

    fun incrementBadgeCount(increment: Int) {
        val count = badgeCount
        if (count == null) {
            setBadgeCount(increment)
        } else {
            setBadgeCount(increment + count)
        }
    }

    fun decrementBadgeCount(decrement: Int) {
        incrementBadgeCount(-decrement)
    }

    /**
     * Attach the BadgeView to the target view
     * @param target the view to attach the BadgeView
     */
    fun setTargetView(target: View?) {
        if (parent != null) {
            (parent as ViewGroup).removeView(this)
        }

        if (target == null) {
            return
        }

        if (target.parent is FrameLayout) {
            (target.parent as FrameLayout).addView(this)

        } else if (target.parent is ViewGroup) {
            // use a new FrameLayout container for adding badge
            val parentContainer = target.parent as ViewGroup
            val groupIndex = parentContainer.indexOfChild(target)
            parentContainer.removeView(target)

            val badgeContainer = FrameLayout(context)
            val parentLayoutParams = target.layoutParams

            badgeContainer.layoutParams = parentLayoutParams
            target.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )

            parentContainer.addView(badgeContainer, groupIndex, parentLayoutParams)
            badgeContainer.addView(target)

            badgeContainer.addView(this)
        }

    }

    /**
     * converts dip to px
     */
    private fun dip2Px(dip: Float): Int {
        return (dip * context.resources.displayMetrics.density + 0.5f).toInt()
    }
}
