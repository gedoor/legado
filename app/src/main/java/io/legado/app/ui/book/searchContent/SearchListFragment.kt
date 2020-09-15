package io.legado.app.ui.book.searchContent

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import com.hankcs.hanlp.HanLP
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.EventBus
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.ui.widget.recycler.UpLinearLayoutManager
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.ColorUtils
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
    private var searchResultCounts = 0
    private var durChapterIndex = 0
    private var searchResultList: MutableList<SearchResult> = mutableListOf()

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.searchCallBack = this
        val bbg = bottomBackground
        val btc = requireContext().getPrimaryTextColor(ColorUtils.isColorLight(bbg))
        ll_search_base_info.setBackgroundColor(bbg)
        tv_current_search_info.setTextColor(btc)
        iv_search_content_top.setColorFilter(btc)
        iv_search_content_bottom.setColorFilter(btc)
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

            tv_current_search_info.text = "搜索结果：$searchResultCounts"
            viewModel.book?.let {
                initCacheFileNames(it)
                durChapterIndex = it.durChapterIndex
            }
        }
    }

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

    @SuppressLint("SetTextI18n")
    override fun startContentSearch(newText: String) {
        // 按章节搜索内容
        if (!newText.isBlank()) {
            adapter.clearItems()
            searchResultList.clear()
            searchResultCounts = 0
            viewModel.lastQuery = newText
            var searchResults = listOf<SearchResult>()
            launch(Main){
                App.db.bookChapterDao().getChapterList(viewModel.bookUrl).map{ chapter ->
                    val job = async(IO){
                        if (isLocalBook || adapter.cacheFileNames.contains(BookHelp.formatChapterName(chapter))) {
                            searchResults = searchChapter(newText, chapter)
                        }
                    }
                    job.await()
                    if (searchResults.isNotEmpty()){
                        searchResultList.addAll(searchResults)
                        tv_current_search_info.text = "搜索结果：$searchResultCounts"
                        adapter.addItems(searchResults)
                        searchResults = listOf<SearchResult>()
                    }
                }
            }
        }
    }

    private suspend fun searchChapter(query: String, chapter: BookChapter?): List<SearchResult> {
        val searchResults: MutableList<SearchResult> = mutableListOf()
        var positions : List<Int> = listOf()
        var replaceContents: List<String>? = null
        var totalContents = ""
        if (chapter != null){
            viewModel.book?.let { bookSource ->
                val bookContent = BookHelp.getContent(bookSource, chapter)
                if (bookContent != null){
                    //搜索替换后的正文
                    val job = async(IO) {
                        chapter.title = when (AppConfig.chineseConverterType) {
                            1 -> HanLP.convertToSimplifiedChinese(chapter.title)
                            2 -> HanLP.convertToTraditionalChinese(chapter.title)
                            else -> chapter.title
                        }
                        replaceContents = BookHelp.disposeContent(
                            chapter.title,
                            bookSource.name,
                            bookSource.bookUrl,
                            bookContent,
                            bookSource.useReplaceRule
                        )
                    }
                    job.await()
                    while (replaceContents == null){
                        delay(100L)
                    }
                    totalContents = replaceContents!!.joinToString("")
                    positions = searchPosition(totalContents, query)
                    var count = 1
                    positions.map{
                        val construct = constructText(totalContents, it, query)
                        val result = SearchResult(index = searchResultCounts,
                            indexWithinChapter = count,
                            text = construct[1] as String,
                            chapterTitle = chapter.title,
                            query = query,
                            chapterIndex = chapter.index,
                            newPosition = construct[0] as Int,
                            contentPosition = it
                        )
                        count += 1
                        searchResultCounts += 1
                        searchResults.add(result)
                    }
                }
            }
        }
        return searchResults
    }

    private fun searchPosition(content: String, pattern: String): List<Int> {
        val position : MutableList<Int> = mutableListOf()
        var index = content.indexOf(pattern)
        while(index >= 0){
            position.add(index)
            index = content.indexOf(pattern, index + 1);
        }
        return position
    }

    private fun constructText(content: String, position: Int, query: String): Array<Any>{
        // 构建关键词周边文字，在搜索结果里显示
        // todo: 判断段落，只在关键词所在段落内分割
        // todo: 利用标点符号分割完整的句
        // todo: length和设置结合，自由调整周边文字长度
        val length = 20
        var po1 = position - length
        var po2 = position + query.length + length
        if (po1 <0) {
            po1 = 0
        }
        if (po2 > content.length){
            po2 = content.length
        }
        val newPosition = position - po1
        val newText = content.substring(po1, po2)
        return arrayOf(newPosition, newText)
    }

    val isLocalBook: Boolean
        get() = viewModel.book?.isLocalBook() == true

    override fun openSearchResult(searchResult: SearchResult) {

        val searchData = Intent()
        searchData.putExtra("index", searchResult.chapterIndex)
        searchData.putExtra("contentPosition", searchResult.contentPosition)
        searchData.putExtra("query", searchResult.query)
        searchData.putExtra("indexWithinChapter", searchResult.indexWithinChapter)
        activity?.setResult(RESULT_OK, searchData)
        activity?.finish()


    }

    override fun durChapterIndex(): Int {
        return durChapterIndex
    }

}