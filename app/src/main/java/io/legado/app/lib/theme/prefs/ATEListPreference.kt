package io.legado.app.lib.theme.prefs

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import io.legado.app.R


class ATEListPreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {

    init {
        widgetLayoutResource = R.layout.item_text
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        val textView = holder?.itemView?.findViewById<TextView>(R.id.text_view)
        textView?.text = entry
    }
}