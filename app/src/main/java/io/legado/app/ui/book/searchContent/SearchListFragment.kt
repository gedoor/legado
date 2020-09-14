package io.legado.app.ui.book.searchContent

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.EventBus
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.ui.widget.recycler.UpLinearLayoutManager
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.getViewModelOfActivity
import io.legado.app.utils.observeEvent
import kotlinx.android.synthetic.main.fragment_search_list.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.jetbrains.anko.sdk27.listeners.onClick
import java.util.regex.Pattern

class SearchListFragment : VMBaseFragment<SearchListViewModel>(R.layout.fragment_search_list),
    SearchListAdapter.Callback,
    SearchListViewModel.SearchListCallBack{
    override val viewModel: SearchListViewModel
        get() = getViewModelOfActivity(SearchListViewModel::class.java)

    lateinit var adapter: SearchListAdapter
    private lateinit var mLayoutManager: UpLinearLayoutManager
    private var tocLiveData: LiveData<List<BookChapter>>? = null
    private var searchResultCounts = 0

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.searchCallBack = this

        /* set color for the bottom bar
        val bbg = bottomBackground
        val btc = requireContext().getPrimaryTextColor(ColorUtils.isColorLight(bbg))
        ll_chapter_base_info.setBackgroundColor(bbg)
        tv_current_chapter_info.setTextColor(btc)
        iv_chapter_top.setColorFilter(btc)
        iv_chapter_bottom.setColorFilter(btc)

         */
        initRecyclerView()
        initView()
        initBook()
    }

    private fun initRecyclerView() {
        adapter = SearchListAdapter(requireContext(), this)
        mLayoutManager = UpLinearLayoutManager(requireContext())
        recycler_view.layoutManager = mLayoutManager
        recycler_view.addItemDecoration(VerticalDivider(requireContext()))
        recycler_view.adapter = adapter
    }

    private fun initView() {
        iv_search_content_top.onClick { mLayoutManager.scrollToPositionWithOffset(0, 0) }
        iv_search_content_bottom.onClick {
            if (adapter.itemCount > 0) {
                mLayoutManager.scrollToPositionWithOffset(adapter.itemCount - 1, 0)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initBook() {
        launch {
            viewModel.book?.let {
                tv_current_search_info.text =
                    "搜索结果：$searchResultCounts"
                initCacheFileNames(it)
            }
        }
    }

    /*
    private fun initDoc() {
        tocLiveData?.removeObservers(this@SearchListFragment)
        tocLiveData = App.db.bookChapterDao().observeByBook(viewModel.bookUrl)
        tocLiveData?.observe(viewLifecycleOwner, {
            adapter.setItems(it)
        })
    }
    */

    private fun initCacheFileNames(book: Book) {
        launch(IO) {
            adapter.cacheFileNames.addAll(BookHelp.getChapterFiles(book))
            withContext(Main) {
                adapter.notifyItemRangeChanged(0, adapter.getActualItemCount(), true)
            }
        }
    }

    override fun observeLiveBus() {
        observeEvent<BookChapter>(EventBus.SAVE_CONTENT) { chapter ->
            viewModel.book?.bookUrl?.let { bookUrl ->
                if (chapter.bookUrl == bookUrl) {
                    adapter.cacheFileNames.add(BookHelp.formatChapterName(chapter))
                    adapter.notifyItemChanged(chapter.index, true)
                }
            }
        }
    }

    override fun startContentSearch(newText: String?) {
        if (!newText.isNullOrBlank()) {
            App.db.bookChapterDao().getChapterList(viewModel.bookUrl).map{
                launch(IO) {
                    val beginTime = System.currentTimeMillis()
                    if (isLocalBook ||
                        adapter.cacheFileNames.contains(BookHelp.formatChapterName(it))
                    ) {
                        val value = searchChapter(newText, it)
                        searchResultCounts += value
                    }
                    val finishedTime = System.currentTimeMillis() - beginTime
                    Log.d("Jason", "Search finished, the total time cost is $finishedTime")
                    Log.d("Jason", "Search finished, the total count is $searchResultCounts")
                }
            }
        }

    }

    private suspend fun searchChapter(query: String, chapter: BookChapter?): Int  {
        val startTime = System.currentTimeMillis()
        val searchResult: MutableList<SearchResult> = mutableListOf()
        var positions : List<Int>? = listOf()
        if (chapter != null){
            Log.d("Jason", "Search ${chapter.title}")
            viewModel.book?.let { bookSource ->
                val bookContent = BookHelp.getContent(bookSource, chapter)
                if (bookContent != null){
                /* replace content, let's focus on original content first
                chapter.title = when (AppConfig.chineseConverterType) {
                    1 -> HanLP.convertToSimplifiedChinese(chapter.title)
                    2 -> HanLP.convertToTraditionalChinese(chapter.title)
                    else -> chapter.title
                }
                var replaceContents: List<String>? = null
                bookContent?.let {
                    replaceContents = BookHelp.disposeContent(
                        chapter.title,
                        bookSource.name,
                        bookSource.bookUrl,
                        it,
                        bookSource.useReplaceRule
                    )
                }

                replaceContents?.map {
                    async(IO) {
                        if (it.contains(query)) {
                            Log.d("targetd contents", it)
                            searchResult.add(it)
                        }
                    }
                }?.awaitAll()
                */
                    positions = countMatches(bookContent, query)
                    positions?.map{
                        searchResult.add(
                            SearchResult(index = 0,
                                text = constructText(bookContent, it),
                                chapterTitle = chapter.title,
                                query = query,
                                pageSize = 0, // to be finished
                                chapterIndex = chapter.index,   // to be finished
                                pageIndex = 0,  // to be finished
                                )
                        )
                    }
                    adapter.addItems(searchResult)
                    Log.d("Jason", "Search ${chapter.title} finished, the appeared count is ${positions!!.size}")
                }
            }
            val endTime = System.currentTimeMillis() - startTime
            Log.d("Jason", "Search ${chapter.title} finished, the time cost is $endTime")
        }
        return positions!!.size
    }

    private fun countMatches(content: String, pattern: String): List<Int> {
        val position : MutableList<Int> = mutableListOf()
        var count = 0
        var index = content.indexOf(pattern)
        while(index >= 0){
            position.add(index)
            index = content.indexOf(pattern, index + 1);
        }
        return position
    }

    private fun constructText(content: String, position: Int): String{

        val length = 10
        var po1 = position - length
        var po2 = position + length
        if (po1 <0) {
            po1 = 0
        }
        if (po2 > content.length){
            po2 = content.length
        }
        return content.substring(po1, po2)
        /*
        if (position >= length && position <= content.length - length){
            return content.substring(position - length, position + length)
        }
        else if (position <= length){
            return content.substring(0, position + length)
        }
        else if (position >= content.length - length){
            return content.substring(position - length, content.length)
        }
         */
    }

    val isLocalBook: Boolean
        get() = viewModel.book?.isLocalBook() == true

    override fun openSearchResult(searchResult: SearchResult) {
        val searchData = Intent()
        searchData.putExtra("index", searchResult.chapterIndex)
        searchData.putExtra("pageIndex", searchResult.pageIndex)
        activity?.setResult(RESULT_OK, searchData)
        activity?.finish()
    }

}