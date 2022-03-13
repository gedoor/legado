package io.legado.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import io.legado.app.ui.widget.text.AccentBgTextView
import io.legado.app.utils.dpToPx

@Suppress("unused", "MemberVisibilityCanBePrivate")
class LabelsBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val unUsedViews = arrayListOf<TextView>()
    private val usedViews = arrayListOf<TextView>()
    var textSize = 12f

    fun setLabels(labels: Array<String>) {
        clear()
        labels.forEach {
            addLabel(it)
        }
    }

    fun setLabels(labels: List<String>) {
        clear()
        labels.forEach {
            addLabel(it)
        }
    }

    fun clear() {
        unUsedViews.addAll(usedViews)
        usedViews.clear()
        removeAllViews()
    }

    fun addLabel(label: String) {
        val tv = if (unUsedViews.isEmpty()) {
            AccentBgTextView(context, null).apply {
                setPadding(3.dpToPx(), 0, 3.dpToPx(), 0)
                setRadius(2)
                val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, 2.dpToPx(), 0)
                layoutParams = lp
                text = label
                maxLines = 1
                usedViews.add(this)
            }
        } else {
            unUsedViews.last().apply {
                usedViews.add(this)
                unUsedViews.removeAt(unUsedViews.lastIndex)
            }
        }
        tv.textSize = textSize
        tv.text = label
        addView(tv)
    }
}