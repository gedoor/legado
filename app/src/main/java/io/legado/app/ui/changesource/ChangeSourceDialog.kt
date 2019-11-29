package io.legado.app.ui.changesource

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchBook
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.dialog_change_source.*


class ChangeSourceDialog : DialogFragment(),
    ChangeSourceViewModel.CallBack,
    ChangeSourceAdapter.CallBack {

    companion object {
        const val tag = "changeSourceDialog"

        fun show(manager: FragmentManager, name: String, author: String) {
            val fragment = (manager.findFragmentByTag(tag) as? ChangeSourceDialog)
                ?: ChangeSourceDialog().apply {
                    val bundle = Bundle()
                    bundle.putString("name", name)
                    bundle.putString("author", author)
                    arguments = bundle
                }
            fragment.show(manager, tag)
        }
    }

    private var callBack: CallBack? = null
    private lateinit var viewModel: ChangeSourceViewModel
    private lateinit var changeSourceAdapter: ChangeSourceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = getViewModel(ChangeSourceViewModel::class.java)
        return inflater.inflate(R.layout.dialog_change_source, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        callBack = activity as? CallBack
        viewModel.searchStateData.observe(viewLifecycleOwner, Observer {
            refresh_progress_bar.isAutoLoading = it
        })
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
        viewModel.initData()
        viewModel.search()
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
        changeSourceAdapter = ChangeSourceAdapter(requireContext(), this)
        recycler_view.layoutManager = LinearLayoutManager(context)
        recycler_view.addItemDecoration(
            DividerItemDecoration(requireContext(), LinearLayout.VERTICAL)
        )
        recycler_view.adapter = changeSourceAdapter
        viewModel.callBack = this
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
                viewModel.screen(newText)
                return false
            }

        })
    }

    override fun changeTo(searchBook: SearchBook) {
        val book = searchBook.toBook()
        callBack?.oldBook?.let { oldBook ->
            book.durChapterIndex = oldBook.durChapterIndex
            book.durChapterPos = oldBook.durChapterPos
            book.durChapterTitle = oldBook.durChapterTitle
            book.customCoverUrl = oldBook.customCoverUrl
            book.customIntro = oldBook.customIntro
            book.order = oldBook.order
            if (book.coverUrl.isNullOrEmpty()) {
                book.coverUrl = oldBook.getDisplayCover()
            }
            callBack?.changeTo(book)
        }
        dismiss()
    }

    override val bookUrl: String?
        get() = callBack?.oldBook?.bookUrl

    override fun adapter(): ChangeSourceAdapter {
        return changeSourceAdapter
    }

    interface CallBack {
        val oldBook: Book?
        fun changeTo(book: Book)
    }

}