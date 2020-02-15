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

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        holder?.let {
            val view = it.findViewById(R.id.switchWidget)
            if (view is SwitchCompat) {
                ATH.setTint(view, context.accentColor)
            }
        }
    }

}
