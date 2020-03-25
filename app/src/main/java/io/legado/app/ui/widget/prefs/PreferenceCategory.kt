package io.legado.app.ui.widget.prefs

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import io.legado.app.R
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.utils.getCompatColor


class PreferenceCategory(context: Context, attrs: AttributeSet) : PreferenceCategory(context, attrs) {

    init {
        isPersistent = true
        layoutResource = R.layout.view_preference_category
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        holder?.let {
            val view = it.findViewById(R.id.preference_title)
            if (view is TextView) {  //  && !view.isInEditMode
                view.text = title
                if (view.isInEditMode) {
                    view.setTextColor(context.getCompatColor(R.color.colorAccent))
                } else {
                    view.setBackgroundColor(context.backgroundColor)
                    view.setTextColor(context.accentColor)
                }
                view.isVisible = title != null && title.isNotEmpty()

                val da = it.findViewById(R.id.preference_divider_above)
                if (da is View) {
                    da.setBackgroundColor(ColorUtils.darkenColor(context.backgroundColor))
                    da.isVisible = it.isDividerAllowedAbove
                }
                val db = it.findViewById(R.id.preference_divider_below)
                if (db is View) {
                    db.setBackgroundColor(ColorUtils.darkenColor(context.backgroundColor))
                    db.isVisible = it.isDividerAllowedBelow
                }
            }
        }
    }

}
