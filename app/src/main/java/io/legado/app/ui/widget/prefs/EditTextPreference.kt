package io.legado.app.ui.widget.prefs

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import io.legado.app.R

class EditTextPreference(context: Context, attrs: AttributeSet) : androidx.preference.EditTextPreference(context, attrs) {

    init {
        // isPersistent = true
        layoutResource = R.layout.view_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        Preference.bindView<TextView>(context, holder, icon, title, summary, null, null)
        super.onBindViewHolder(holder)
    }

}
