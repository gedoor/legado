package io.legado.app.ui.widget.prefs

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import io.legado.app.R


class IconListPreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {

    private val mEntryDrawables = arrayListOf<Drawable>()

    init {
        widgetLayoutResource = R.layout.view_icon

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.IconListPreference, 0, 0)

        val drawables: Array<CharSequence>

        drawables = try {
            a.getTextArray(R.styleable.IconListPreference_icons)
        } finally {
            a.recycle()
        }

        for (drawable in drawables) {
            val resId = context.resources
                .getIdentifier(drawable.toString(), "mipmap", context.packageName)
            val d = context.resources.getDrawable(resId)
            mEntryDrawables.add(d)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        holder?.itemView?.findViewById<ImageView>(R.id.preview)?.let {
            val selectedIndex = findIndexOfValue(value)
            val drawable = mEntryDrawables[selectedIndex]
            it.setImageDrawable(drawable)
        }
    }


}