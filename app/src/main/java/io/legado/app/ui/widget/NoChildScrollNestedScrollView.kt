package io.legado.app.ui.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.widget.NestedScrollView

class NoChildScrollNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : NestedScrollView(context, attrs) {

    override fun requestChildRectangleOnScreen(
        child: View,
        rectangle: Rect?,
        immediate: Boolean
    ): Boolean {
        return false
    }

}
