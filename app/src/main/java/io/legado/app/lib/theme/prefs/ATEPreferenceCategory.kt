package io.legado.app.lib.theme.prefs

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import io.legado.app.lib.theme.ThemeStore


class ATEPreferenceCategory(context: Context, attrs: AttributeSet) :
    PreferenceCategory(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        holder?.let {
            val view = it.findViewById(android.R.id.title)
            if (view is TextView) {
                view.setTextColor(ThemeStore.accentColor(view.getContext()))//设置title文本的颜色
            }
        }
    }

}
