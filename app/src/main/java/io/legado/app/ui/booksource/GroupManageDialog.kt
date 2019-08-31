package io.legado.app.ui.booksource

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.utils.splitNotBlank
import kotlinx.android.synthetic.main.dialog_recycler_view.*

class GroupManageDialog : DialogFragment() {

    private lateinit var adapter: GroupAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_recycler_view, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    private fun initData() {
        tool_bar.title = getString(R.string.group_manage)
        adapter = GroupAdapter(requireContext())
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.adapter = adapter
        App.db.bookSourceDao().observeGroup().observe(viewLifecycleOwner, Observer {
            val groups = linkedSetOf<String>()
            it.map { group ->
                groups.addAll(group.splitNotBlank(",", ";"))
            }
            adapter.setItems(groups.toList())
        })
    }


    class GroupAdapter(context: Context) :
        SimpleRecyclerAdapter<String>(context, R.layout.item_group_manage) {

        override fun convert(holder: ItemViewHolder, item: String, payloads: MutableList<Any>) {

        }

    }
}