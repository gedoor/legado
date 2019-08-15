package io.legado.app.ui.changesource

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.dialog_change_source.*


class ChangeSourceDialog : DialogFragment() {

    companion object {
        const val tag = "changeSourceDialog"

        fun newInstance(name: String, author: String): ChangeSourceDialog {
            val changeSourceDialog = ChangeSourceDialog()
            val bundle = Bundle()
            bundle.putString("name", name)
            bundle.putString("author", author)
            changeSourceDialog.arguments = bundle
            return changeSourceDialog
        }
    }

    private lateinit var viewModel: ChangeSourceViewModel
    private lateinit var changeSourceAdapter: ChangeSourceAdapter
    var callBack: CallBack? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = getViewModel(ChangeSourceViewModel::class.java)
        return inflater.inflate(R.layout.dialog_change_source, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let { bundle ->
            bundle.getString("name")?.let {
                viewModel.name = it
            }
            bundle.getString("author")?.let {
                viewModel.author = it
            }
        }
        tool_bar.inflateMenu(R.menu.search_view)
        showTitle()
        initRecyclerView()
        initSearchView()
    }

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }

    private fun showTitle() {
        tool_bar.title = viewModel.name
        tool_bar.subtitle = getString(R.string.author_show, viewModel.author)
    }

    private fun initRecyclerView() {
        changeSourceAdapter = ChangeSourceAdapter(requireContext())
        recycler_view.layoutManager = LinearLayoutManager(context)
        recycler_view.adapter = changeSourceAdapter
    }

    private fun initSearchView() {
        val searchView = tool_bar.menu.findItem(R.id.menu_search).actionView as SearchView
        searchView.setOnCloseListener {
            showTitle()
            false
        }
        searchView.setOnSearchClickListener {
            tool_bar.title = ""
            tool_bar.subtitle = ""
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

        })
    }


    interface CallBack {
        fun changeTo(book: Book)
    }

}