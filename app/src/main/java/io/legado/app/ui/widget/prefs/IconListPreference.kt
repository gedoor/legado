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
import io.legado.app.help.AppConfig
import io.legado.app.utils.getCompatDrawable
import kotlinx.android.synthetic.main.dialog_recycler_view.*
import kotlinx.android.synthetic.main.item_icon_preference.view.*
import org.jetbrains.anko.sdk27.listeners.onClick


class IconListPreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {
    private var iconNames: Array<CharSequence>
    private val mEntryDrawables = arrayListOf<Drawable?>()

    init {
        layoutResource = R.layout.view_preference
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
            var d: Drawable? = null
            kotlin.runCatching {
                d = context.getCompatDrawable(resId)
            }
            mEntryDrawables.add(d)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        val v = Preference.bindView<ImageView>(context, holder, icon, title, summary, widgetLayoutResource, R.id.preview, 50, 50)
        if (v is ImageView) {
            val selectedIndex = findIndexOfValue(value)
            if (selectedIndex >= 0) {
                val drawable = mEntryDrawables[selectedIndex]
                v.setImageDrawable(drawable)
            }
        }
    }

    override fun onClick() {
        getActivity()?.let {
            val dialog = IconDialog().apply {
                val args = Bundle()
                args.putString("value", value)
                args.putCharSequenceArray("entries", entries)
                args.putCharSequenceArray("entryValues", entryValues)
                args.putCharSequenceArray("iconNames", iconNames)
                arguments = args
                onChanged = { value ->
                    this@IconListPreference.value = value
                }
            }
            it.supportFragmentManager
                .beginTransaction()
                .add(dialog, getFragmentTag())
                .commitAllowingStateLoss()
        }
    }

    override fun onAttached() {
        super.onAttached()
        val fragment =
            getActivity()?.supportFragmentManager?.findFragmentByTag(getFragmentTag()) as IconDialog?
        fragment?.onChanged = { value ->
            this@IconListPreference.value = value
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

    private fun getFragmentTag(): String {
        return "icon_$key"
    }

    class IconDialog : DialogFragment() {

        var onChanged: ((value: String) -> Unit)? = null
        var dialogValue: String? = null
        var dialogEntries: Array<CharSequence>? = null
        var dialogEntryValues: Array<CharSequence>? = null
        var dialogIconNames: Array<CharSequence>? = null

        override fun onStart() {
            super.onStart()
            val dm = DisplayMetrics()
            activity?.windowManager?.defaultDisplay?.getMetrics(dm)
            dialog?.window?.setLayout(
                (dm.widthPixels * 0.8).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
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
            recycler_view.isEnableScroll = !AppConfig.isEInkMode
            recycler_view.layoutManager = LinearLayoutManager(requireContext())
            val adapter = Adapter(requireContext())
            recycler_view.adapter = adapter
            arguments?.let {
                dialogValue = it.getString("value")
                dialogEntries = it.getCharSequenceArray("entries")
                dialogEntryValues = it.getCharSequenceArray("entryValues")
                dialogIconNames = it.getCharSequenceArray("iconNames")
                dialogEntryValues?.let { values ->
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
                    dialogEntries?.let {
                        label.text = it[index]
                    }
                    dialogIconNames?.let {
                        val resId = context.resources
                            .getIdentifier(it[index].toString(), "mipmap", context.packageName)
                        val d = try {
                            context.getCompatDrawable(resId)
                        } catch (e: Exception) {
                            null
                        }
                        d?.let {
                            icon.setImageDrawable(d)
                        }
                    }
                    label.isChecked = item.toString() == dialogValue
                    onClick {
                        onChanged?.invoke(item.toString())
                        this@IconDialog.dismiss()
                    }
                }
            }

            override fun registerListener(holder: ItemViewHolder) {
                holder.itemView.onClick {
                    getItem(holder.layoutPosition)?.let {
                        onChanged?.invoke(it.toString())
                    }
                }
            }

            private fun findIndexOfValue(value: String?): Int {
                dialogEntryValues?.let { values ->
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
}
