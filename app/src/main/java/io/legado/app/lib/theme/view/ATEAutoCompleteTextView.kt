package io.legado.app.lib.theme.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import io.legado.app.R
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.item_1line_text_and_del.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class ATEAutoCompleteTextView : AppCompatAutoCompleteTextView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    var delCallBack: DelCallBack? = null
    var showDel: Boolean = false

    init {
        ATH.applyAccentTint(this)
    }

    override fun enoughToFilter(): Boolean {
        return true
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)

        showDropDown()
    }

    fun setFilterValues(values: List<String>, showDel: Boolean = false) {
        this.showDel = showDel
        setAdapter(MyAdapter(context, values))
    }

    fun setFilterValues(vararg value: String, showDel: Boolean = false) {
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
                    delCallBack?.delete(it)
                    showDropDown()
                }
            }
            return view
        }
    }

    interface DelCallBack {
        fun delete(value: String)
    }
}
