package io.legado.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.setPadding
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

    fun addLabel(label: String) {
        addView(AccentBgTextView(context, null).apply {
            setPadding(2.dp)
            setRadios(2)
            text = label
        })
    }

}