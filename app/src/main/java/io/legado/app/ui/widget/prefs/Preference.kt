package io.legado.app.ui.widget.prefs

import android.content.Context
import android.content.ContextWrapper
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.Image
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.jaredrummler.android.colorpicker.*
import io.legado.app.lib.theme.ATH
import io.legado.app.R
import io.legado.app.lib.theme.accentColor
import org.jetbrains.anko.imageBitmap
import org.jetbrains.anko.layoutInflater
import kotlin.math.roundToInt

class Preference(context: Context, attrs: AttributeSet) : androidx.preference.Preference(context, attrs) {

    init {
        // isPersistent = true
        layoutResource = R.layout.view_preference
    }

    companion object {

        fun <T: View> bindView(context: Context, it: PreferenceViewHolder?, icon: Drawable?, title: CharSequence?, summary: CharSequence?, weightLayoutRes: Int?,  viewId: Int?,
                               weightWidth: Int = 0, weightHeight: Int = 0): T? {
            if (it == null) return null
            val view = it.findViewById(R.id.preference_title)
            if (view is TextView) {  //  && !view.isInEditMode
                view.text = title
                view.isVisible = title != null && title.isNotEmpty()

                val tv_summary = it.findViewById(R.id.preference_desc)
                if(tv_summary is TextView) {
                    tv_summary.text = summary
                    tv_summary.isVisible = summary != null && summary.isNotEmpty()
                }

                val _icon = it.findViewById(R.id.preference_icon)
                if (_icon is ImageView) {
                    _icon.isVisible = icon != null && icon.isVisible
                    _icon.setImageDrawable(icon)
                    _icon.setColorFilter(context.accentColor)
                }
            }

            if (weightLayoutRes != null && weightLayoutRes != 0 && viewId != null && viewId != 0) {
                val lay = it.findViewById(R.id.preference_widget)
                if (lay is FrameLayout) {
                    var v = it.itemView.findViewById<T>(viewId)
                    if (v == null) {
                        val inflater: LayoutInflater = context.layoutInflater
                        val childView = inflater.inflate(weightLayoutRes, null)
                        lay.removeAllViews()
                        lay.addView(childView)
                        lay.isVisible = true
                        v = lay.findViewById<T>(viewId)
                    }

                    if (weightWidth > 0 || weightHeight > 0) {
                        val lp = lay.layoutParams
                        if (weightHeight > 0)
                            lp.height = (context.resources.displayMetrics.density * weightHeight).roundToInt()
                        if (weightWidth > 0)
                            lp.width = (context.resources.displayMetrics.density * weightWidth).roundToInt()
                        lay.layoutParams = lp
                    }

                    return v
                }
            }

            return null
        }

    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        bindView<View>(context, holder, icon, title, summary, null, null)
    }

}
