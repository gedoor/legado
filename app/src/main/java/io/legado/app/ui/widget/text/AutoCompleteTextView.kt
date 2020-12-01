package io.legado.app.ui.widget.text

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import io.legado.app.R
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.gone
import io.legado.app.utils.visible
import org.jetbrains.anko.sdk27.listeners.onClick

@Suppress("unused")
class AutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatAutoCompleteTextView(context, attrs) {

    var delCallBack: ((value: String) -> Unit)? = null

    init {
        ATH.applyAccentTint(this)
    }

    override fun enoughToFilter(): Boolean {
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            showDropDown()
        }
        return super.onTouchEvent(event)
    }

    fun setFilterValues(values: List<String>?) {
        values?.let {
            setAdapter(MyAdapter(context, values))
        }
    }

    fun setFilterValues(vararg value: String) {
        setAdapter(MyAdapter(context, value.toMutableList()))
    }

    inner class MyAdapter(context: Context, values: List<String>) :
        ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, values) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_1line_text_and_del, parent, false)
            val textView = view.findViewById<TextView>(R.id.text_view)
            textView.text = getItem(position)
            val ivDelete = view.findViewById<ImageView>(R.id.iv_delete)
            if (delCallBack != null) ivDelete.visible() else ivDelete.gone()
            ivDelete.onClick {
                getItem(position)?.let {
                    remove(it)
                    delCallBack?.invoke(it)
                    showDropDown()
                }
            }
            return view
        }
    }

}
