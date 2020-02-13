package io.legado.app.ui.widget.prefs

import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.utils.getCompatDrawable
import kotlinx.android.synthetic.main.dialog_recycler_view.*
import kotlinx.android.synthetic.main.item_icon_preference.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class IconListPreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {
    private var iconNames: Array<CharSequence>
    private val mEntryDrawables = arrayListOf<Drawable>()

    init {
        widgetLayoutResource = R.layout.view_icon

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.IconListPreference, 0, 0)

        iconNames = try {
            a.getTextArray(R.styleable.IconListPreference_icons)
        } finally {
            a.recycle()
        }

        for (iconName in iconNames) {
            val resId = context.resources
                .getIdentifier(iconName.toString(), "mipmap", context.packageName)
            val d = context.getCompatDrawable(resId)
            mEntryDrawables.add(d!!)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        holder?.itemView?.findViewById<ImageView>(R.id.preview)?.let {
            val selectedIndex = findIndexOfValue(value)
            if (selectedIndex > 0) {
                val drawable = mEntryDrawables[selectedIndex]
                it.setImageDrawable(drawable)
            }
        }
    }

    override fun onClick() {
        getActivity()?.let {
            IconDialog().apply {
                val args = Bundle()
                args.putCharSequenceArray("entries", entries)
                args.putCharSequenceArray("entryValues", entryValues)
                args.putCharSequenceArray("iconNames", iconNames)
                arguments = args
                onChanged = { value ->
                    this@IconListPreference.value = value
                }
            }.show(it.supportFragmentManager, "iconDialog")
        }
    }

    private fun getActivity(): FragmentActivity? {
        val context = context
        if (context is FragmentActivity) {
            return context
        } else if (context is ContextWrapper) {
            val baseContext = context.baseContext
            if (baseContext is FragmentActivity) {
                return baseContext
            }
        }
        return null
    }
}

class IconDialog : DialogFragment() {

    var onChanged: ((value: String) -> Unit)? = null
    var entries: Array<CharSequence>? = null
    var entryValues: Array<CharSequence>? = null
    var iconNames: Array<CharSequence>? = null

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.setLayout(
            (dm.widthPixels * 0.9).toInt(),
            (dm.heightPixels * 0.9).toInt()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_recycler_view, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tool_bar.setTitle(R.string.change_icon)
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        val adapter = Adapter(requireContext())
        recycler_view.adapter = adapter
        arguments?.let {
            entries = it.getCharSequenceArray("entries")
            entryValues = it.getCharSequenceArray("entryValues")
            iconNames = it.getCharSequenceArray("iconNames")
            entryValues?.let { values ->
                adapter.setItems(values.toList())
            }
        }
    }


    inner class Adapter(context: Context) :
        SimpleRecyclerAdapter<CharSequence>(context, R.layout.item_icon_preference) {

        override fun convert(
            holder: ItemViewHolder,
            item: CharSequence,
            payloads: MutableList<Any>
        ) {
            with(holder.itemView) {
                val index = findIndexOfValue(item.toString())
                entries?.let {
                    label.text = it[index]
                }
                iconNames?.let {
                    val resId = context.resources
                        .getIdentifier(it[index].toString(), "mipmap", context.packageName)
                    val d = context.getCompatDrawable(resId)
                    icon.setImageDrawable(d)
                }
                onClick {
                    onChanged?.invoke(item.toString())
                }
            }
        }

        private fun findIndexOfValue(value: String?): Int {
            entryValues?.let { values ->
                for (i in values.indices.reversed()) {
                    if (values[i] == value) {
                        return i
                    }
                }
            }
            return -1
        }
    }
}