package io.legado.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import io.legado.app.ui.widget.text.AccentBgTextView
import io.legado.app.utils.dp


class LabelsBar(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {


    fun setLabels(labels: Array<String>) {
        removeAllViews()
        labels.forEach {
            addLabel(it)
        }
    }

    fun setLabels(labels: List<String>) {
        removeAllViews()
        labels.forEach {
            addLabel(it)
        }
    }

    fun clear() {
        removeAllViews()
    }

    fun addLabel(label: String) {
        addView(AccentBgTextView(context, null).apply {
            setPadding(3.dp, 0, 3.dp, 0)
            setRadios(2)
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 2.dp, 0)
            layoutParams = lp
            text = label
        })
    }

}