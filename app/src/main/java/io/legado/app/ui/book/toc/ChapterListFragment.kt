package io.legado.app.ui.book.toc

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.EventBus
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.FragmentChapterListBinding
import io.legado.app.help.BookHelp
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.ui.widget.recycler.UpLinearLayoutManager
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.getViewModelOfActivity
import io.legado.app.utils.observeEvent
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.sdk27.listeners.onClick
import kotlin.math.min

class ChapterListFragment : VMBaseFragment<ChapterListViewModel>(R.layout.fragment_chapter_list),
    ChapterListAdapter.Callback,
    ChapterListViewModel.ChapterListCallBack {
    override val viewModel: ChapterListViewModel
        get() = getViewModelOfActivity(ChapterListViewModel::class.java)
    private val binding by viewBinding(FragmentChapterListBinding::bind)
    lateinit var adapter: ChapterListAdapter
    private var durChapterIndex = 0
    private lateinit var mLayoutManager: UpLinearLayoutManager
    private var tocLiveData: LiveData<List<BookChapter>>? = null
    private var scrollToDurChapter = false

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) = with(binding) {
        viewModel.chapterCallBack = this@ChapterListFragment
        val bbg = bottomBackground
        val btc = requireContext().getPrimaryTextColor(ColorUtils.isColorLight(bbg))
        llChapterBaseInfo.setBackgroundColor(bbg)
        tvCurrentChapterInfo.setTextColor(btc)
        ivChapterTop.setColorFilter(btc)
        ivChapterBottom.setColorFilter(btc)
        initRecyclerView()
        initView()
        initBook()
    }

    private fun initRecyclerView() {
        adapter = ChapterListAdapter(requireContext(), this)
        mLayoutManager = UpLinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = mLayoutManager
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
    }

    private fun initView() = with(binding) {
        ivChapterTop.onClick { mLayoutManager.scrollToPositionWithOffset(0, 0) }
        ivChapterBottom.onClick {
            if (adapter.itemCount > 0) {
                mLayoutManager.scrollToPositionWithOffset(adapter.itemCount - 1, 0)
            }
        }
        tvCurrentChapterInfo.onClick {
            mLayoutManager.scrollToPositionWithOffset(durChapterIndex, 0)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initBook() {
        launch {
            initDoc()
            viewModel.book?.let {
                durChapterIndex = it.durChapterIndex
                binding.tvCurrentChapterInfo.text =
                    "${it.durChapterTitle}(${it.durChapterIndex + 1}/${it.totalChapterNum})"
                initCacheFileNames(it)
            }
        }
    }

    private fun initDoc() {
        tocLiveData?.removeObservers(this@ChapterListFragment)
        tocLiveData = App.db.bookChapterDao.observeByBook(viewModel.bookUrl)
        tocLiveData?.observe(viewLifecycleOwner, {
            adapter.setItems(it)
            if (!scrollToDurChapter) {
                mLayoutManager.scrollToPositionWithOffset(durChapterIndex, 0)
                scrollToDurChapter = true
            }
        })
    }

    private fun initCacheFileNames(book: Book) {
        launch(IO) {
            adapter.cacheFileNames.addAll(BookHelp.getChapterFiles(book))
            withContext(Main) {
                adapter.notifyItemRangeChanged(0, adapter.itemCount, true)
            }
        }
    }

    override fun observeLiveBus() {
        observeEvent<BookChapter>(EventBus.SAVE_CONTENT) { chapter ->
            viewModel.book?.bookUrl?.let { bookUrl ->
                if (chapter.bookUrl == bookUrl) {
                    adapter.cacheFileNames.add(chapter.getFileName())
                    adapter.notifyItemChanged(chapter.index, true)
                }
            }
        }
    }

    override fun startChapterListSearch(newText: String?) {
        if (newText.isNullOrBlank()) {
            initDoc()
        } else {
            tocLiveData?.removeObservers(this)
            tocLiveData = App.db.bookChapterDao.liveDataSearch(viewModel.bookUrl, newText)
            tocLiveData?.observe(viewLifecycleOwner, {
                adapter.setItems(it)
            })
        }
    }

    override val isLocalBook: Boolean
        get() = viewModel.book?.isLocalBook() == true

    override fun durChapterIndex(): Int {
        return min(durChapterIndex, adapter.itemCount)
    }

    override fun openChapter(bookChapter: BookChapter) {
        activity?.setResult(RESULT_OK, Intent().putExtra("index", bookChapter.index))
        activity?.finish()
    }

}