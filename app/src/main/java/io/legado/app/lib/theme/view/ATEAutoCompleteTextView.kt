package io.legado.app.lib.theme.view

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import io.legado.app.R
import io.legado.app.lib.theme.Selector
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_1line_text_and_del.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class ATEAutoCompleteTextView(context: Context, attrs: AttributeSet) :
    AppCompatAutoCompleteTextView(context, attrs) {

    var callBack: CallBack? = null
    var showDel: Boolean = false

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            backgroundTintList = Selector.colorBuild()
                .setFocusedColor(ThemeStore.accentColor(context))
                .setDefaultColor(ThemeStore.textColorPrimary(context))
                .create()
        }
    }

    override fun enoughToFilter(): Boolean {
        return true
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)

        performFiltering()
    }

    fun performFiltering() {
        performFiltering(text, KeyEvent.KEYCODE_UNKNOWN)
    }

    fun setSelectValues(values: List<String>, showDel: Boolean = false) {
        this.showDel = showDel
        setAdapter(MyAdapter(context, values))
    }

    fun setSelectValues(vararg value: String, showDel: Boolean = false) {
        this.showDel = showDel
        setAdapter(MyAdapter(context, value.toMutableList()))
    }

    inner class MyAdapter(context: Context, values: List<String>) :
        ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, values) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_1line_text_and_del, parent, false)
            view.text_view.text = getItem(position)
            if (showDel) view.iv_delete.visible() else view.iv_delete.gone()
            view.iv_delete.onClick {
                getItem(position)?.let {
                    remove(it)
                    callBack?.delete(it)
                    performFiltering()
                }
            }
            return view
        }
    }

    interface CallBack {
        fun delete(value: String)
    }
}
