package io.legado.app.ui.widget.prefs

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import io.legado.app.lib.theme.accentColor


class PreferenceCategory(context: Context, attrs: AttributeSet) :
    PreferenceCategory(context, attrs) {

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        holder?.let {
            val view = it.findViewById(android.R.id.title)
            if (view is TextView && !view.isInEditMode) {
                view.setTextColor(context.accentColor)//设置title文本的颜色
            }
        }
    }

}
