package io.legado.app.ui.changesource

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchBook
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getViewModel
import io.legado.app.utils.putPrefBoolean
import kotlinx.android.synthetic.main.dialog_change_source.*


class ChangeSourceDialog : DialogFragment(),
    Toolbar.OnMenuItemClickListener,
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
    lateinit var adapter: ChangeSourceAdapter

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        callBack = activity as? CallBack
        viewModel = getViewModel(ChangeSourceViewModel::class.java)
        return inflater.inflate(R.layout.dialog_change_source, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initData(arguments)
        showTitle()
        tool_bar.inflateMenu(R.menu.change_source)
        tool_bar.setOnMenuItemClickListener(this)
        initRecyclerView()
        initMenu()
        initSearchView()
        initLiveData()
        viewModel.loadDbSearchBook()
        viewModel.search()
    }

    private fun showTitle() {
        tool_bar.title = viewModel.name
        tool_bar.subtitle = getString(R.string.author_show, viewModel.author)
    }

    private fun initMenu() {
        tool_bar.menu.findItem(R.id.menu_load_toc)?.isChecked =
            getPrefBoolean(PreferKey.changeSourceLoadToc)
    }

    private fun initRecyclerView() {
        adapter = ChangeSourceAdapter(requireContext(), this)
        recycler_view.layoutManager = LinearLayoutManager(context)
        recycler_view.addItemDecoration(VerticalDivider(requireContext()))
        recycler_view.adapter = adapter
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    recycler_view.scrollToPosition(0)
                }
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                if (toPosition == 0) {
                    recycler_view.scrollToPosition(0)
                }
            }
        })
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

    private fun initLiveData() {
        viewModel.searchStateData.observe(viewLifecycleOwner, Observer {
            refresh_progress_bar.isAutoLoading = it
        })
        viewModel.searchBooksLiveData.observe(viewLifecycleOwner, Observer {
            val diffResult = DiffUtil.calculateDiff(DiffCallBack(adapter.getItems(), it))
            adapter.setItems(it)
            diffResult.dispatchUpdatesTo(adapter)
        })
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_load_toc -> {
                putPrefBoolean(PreferKey.changeSourceLoadToc, !item.isChecked)
                item.isChecked = !item.isChecked
            }
        }
        return false
    }

    override fun changeTo(searchBook: SearchBook) {
        val book = searchBook.toBook()
        callBack?.oldBook?.let { oldBook ->
            book.group = oldBook.group
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

    interface CallBack {
        val oldBook: Book?
        fun changeTo(book: Book)
    }

}