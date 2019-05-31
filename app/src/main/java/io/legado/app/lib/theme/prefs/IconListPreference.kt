package io.legado.app.lib.theme.prefs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog.Builder
import android.content.Context
import android.graphics.drawable.Drawable
import android.preference.ListPreference
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.ListAdapter
import io.legado.app.R
import java.util.*


class IconListPreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {

    private val mEntryDrawables = ArrayList<Drawable>()

    init {

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.IconListPreference, 0, 0)

        val drawables: Array<CharSequence>

        try {
            drawables = a.getTextArray(R.styleable.IconListPreference_icons)
        } finally {
            a.recycle()
        }

        for (drawable in drawables) {
            val resId = context.resources.getIdentifier(drawable.toString(), "mipmap", context.packageName)

            val d = context.resources.getDrawable(resId)

            mEntryDrawables.add(d)
        }

        widgetLayoutResource = R.layout.view_icon
    }

    protected fun createListAdapter(): ListAdapter {
        val selectedValue = value
        val selectedIndex = findIndexOfValue(selectedValue)
        return AppArrayAdapter(context, R.layout.item_icon_preference, entries, mEntryDrawables, selectedIndex)
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        val selectedValue = value
        val selectedIndex = findIndexOfValue(selectedValue)

        val drawable = mEntryDrawables[selectedIndex]

        (view.findViewById<View>(R.id.preview) as ImageView).setImageDrawable(drawable)
    }

    override fun onPrepareDialogBuilder(builder: Builder) {
        builder.setAdapter(createListAdapter(), this)
        super.onPrepareDialogBuilder(builder)
    }

    inner class AppArrayAdapter(
        context: Context, textViewResourceId: Int,
        objects: Array<CharSequence>, imageDrawables: List<Drawable>,
        selectedIndex: Int
    ) : ArrayAdapter<CharSequence>(context, textViewResourceId, objects) {
        private var mImageDrawables: List<Drawable>? = null
        private var mSelectedIndex = 0

        init {
            mSelectedIndex = selectedIndex
            mImageDrawables = imageDrawables
        }

        @SuppressLint("ViewHolder")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val inflater = (context as Activity).layoutInflater
            val view = inflater.inflate(R.layout.item_icon_preference, parent, false)
            val textView = view.findViewById<View>(R.id.label) as CheckedTextView
            textView.text = getItem(position)
            textView.isChecked = position == mSelectedIndex

            val imageView = view.findViewById<View>(R.id.icon) as ImageView
            imageView.setImageDrawable(mImageDrawables!![position])
            return view
        }
    }
}
