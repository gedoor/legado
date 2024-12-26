package io.legado.app.data.entities.rule

import android.view.View
import com.google.android.flexbox.FlexboxLayout

data class FlexChildStyle(
    val layout_flexGrow: Float = 0F,
    val layout_flexShrink: Float = 1F,
    val layout_alignSelf: String = "auto",
    val layout_flexBasisPercent: Float = -1F,
    val layout_wrapBefore: Boolean = false,
) {

    fun alignSelf(): Int {
        return when (layout_alignSelf) {
            "auto" -> -1
            "flex_start" -> 0
            "flex_end" -> 1
            "center" -> 2
            "baseline" -> 3
            "stretch" -> 4
            else -> -1
        }
    }

    fun apply(view: View) {
        val lp = view.layoutParams as FlexboxLayout.LayoutParams
        lp.flexGrow = layout_flexGrow
        lp.flexShrink = layout_flexShrink
        lp.alignSelf = alignSelf()
        lp.flexBasisPercent = layout_flexBasisPercent
        lp.isWrapBefore = layout_wrapBefore
    }

    companion object {
        val defaultStyle = FlexChildStyle()
    }

}
