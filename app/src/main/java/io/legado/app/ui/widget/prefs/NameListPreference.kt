package io.legado.app.ui.widget.prefs

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import io.legado.app.R


class NameListPreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {

    init {
        layoutResource = R.layout.view_preference
        widgetLayoutResource = R.layout.item_fillet_text
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        val v = Preference.bindView<TextView>(context, holder, icon, title, summary, widgetLayoutResource, R.id.text_view)
        if (v is TextView) {
            v.text = entry
        }
        super.onBindViewHolder(holder)
    }

}