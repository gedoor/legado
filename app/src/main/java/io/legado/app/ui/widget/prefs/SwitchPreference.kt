package io.legado.app.ui.widget.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import io.legado.app.R
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor

class SwitchPreference(context: Context, attrs: AttributeSet) :
    SwitchPreferenceCompat(context, attrs) {

    init {
        layoutResource = R.layout.view_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        val v = Preference.bindView<SwitchCompat>(
            context,
            holder,
            icon,
            title,
            summary,
            widgetLayoutResource,
            R.id.switchWidget
        )
        if (v is SwitchCompat && !v.isInEditMode) {
            ATH.setTint(v, context.accentColor)
        }
        super.onBindViewHolder(holder)
    }

}
