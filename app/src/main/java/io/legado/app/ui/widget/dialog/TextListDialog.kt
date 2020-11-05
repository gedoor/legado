package io.legado.app.ui.widget.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.utils.getSize
import kotlinx.android.synthetic.main.dialog_recycler_view.*
import kotlinx.android.synthetic.main.item_log.view.*

class TextListDialog : BaseDialogFragment() {

    companion object {
        fun show(fragmentManager: FragmentManager, title: String, values: ArrayList<String>) {
            TextListDialog().apply {
                val bundle = Bundle()
                bundle.putString("title", title)
                bundle.putStringArrayList("values", values)
                arguments = bundle
            }.show(fragmentManager, "textListDialog")
        }
    }

    lateinit var adapter: TextAdapter
    var values: ArrayList<String>? = null

    override fun onStart() {
        super.onStart()
        val dm = requireActivity().getSize()
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_recycler_view, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let {
            tool_bar.title = it.getString("title")
            values = it.getStringArrayList("values")
        }
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        adapter = TextAdapter(requireContext())
        recycler_view.adapter = adapter
        adapter.setItems(values)
    }

    class TextAdapter(context: Context) :
        SimpleRecyclerAdapter<String>(context, R.layout.item_log) {
        override fun convert(holder: ItemViewHolder, item: String, payloads: MutableList<Any>) {
            holder.itemView.apply {
                if (text_view.getTag(R.id.tag1) == null) {
                    val listener = object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) {
                            text_view.isCursorVisible = false
                            text_view.isCursorVisible = true
                        }

                        override fun onViewDetachedFromWindow(v: View) {}
                    }
                    text_view.addOnAttachStateChangeListener(listener)
                    text_view.setTag(R.id.tag1, listener)
                }
                text_view.text = item
            }
        }

        override fun registerListener(holder: ItemViewHolder) {
            //nothing
        }
    }

}