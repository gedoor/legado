package io.legado.app.ui.widget.prefs

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.preference.PreferenceViewHolder
import io.legado.app.R
import io.legado.app.lib.theme.accentColor
import org.jetbrains.anko.layoutInflater
import kotlin.math.roundToInt

class Preference(context: Context, attrs: AttributeSet) :
    androidx.preference.Preference(context, attrs) {

    init {
        // isPersistent = true
        layoutResource = R.layout.view_preference
    }

    companion object {

        fun <T : View> bindView(
            context: Context,
            it: PreferenceViewHolder?,
            icon: Drawable?,
            title: CharSequence?,
            summary: CharSequence?,
            weightLayoutRes: Int?,
            viewId: Int?,
            weightWidth: Int = 0,
            weightHeight: Int = 0
        ): T? {
            if (it == null) return null
            val view = it.findViewById(R.id.preference_title)
            if (view is TextView) {
                view.text = title
                view.isVisible = title != null && title.isNotEmpty()

                val tvSummary = it.findViewById(R.id.preference_desc)
                if (tvSummary is TextView) {
                    tvSummary.text = summary
                    tvSummary.isGone = summary.isNullOrEmpty()
                }

                val iconView = it.findViewById(R.id.preference_icon)
                if (iconView is ImageView) {
                    iconView.isVisible = icon != null && icon.isVisible
                    iconView.setImageDrawable(icon)
                    iconView.setColorFilter(context.accentColor)
                }

            }

            if (weightLayoutRes != null && weightLayoutRes != 0 && viewId != null && viewId != 0) {
                val lay = it.findViewById(R.id.preference_widget)
                if (lay is FrameLayout) {
                    var needRequestLayout = false
                    var v = it.itemView.findViewById<T>(viewId)
                    if (v == null) {
                        val inflater: LayoutInflater = context.layoutInflater
                        val childView = inflater.inflate(weightLayoutRes, null)
                        lay.removeAllViews()
                        lay.addView(childView)
                        lay.isVisible = true
                        v = lay.findViewById(viewId)
                    } else
                        needRequestLayout = true

                    if (weightWidth > 0 || weightHeight > 0) {
                        val lp = lay.layoutParams
                        if (weightHeight > 0)
                            lp.height =
                                (context.resources.displayMetrics.density * weightHeight).roundToInt()
                        if (weightWidth > 0)
                            lp.width =
                                (context.resources.displayMetrics.density * weightWidth).roundToInt()
                        lay.layoutParams = lp
                    } else if (needRequestLayout)
                        v.requestLayout()

                    return v
                }
            }

            return null
        }

    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        bindView<View>(context, holder, icon, title, summary, null, null)
        super.onBindViewHolder(holder)
    }

}
