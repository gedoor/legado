package io.legado.app.ui.book.manga

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.ActivityMangeBinding
import io.legado.app.databinding.ViewLoadMoreBinding
import io.legado.app.help.book.isImage
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.model.ReadMange
import io.legado.app.model.ReadMange.mFirstLoading
import io.legado.app.model.recyclerView.MangeContent
import io.legado.app.model.recyclerView.ReaderLoading
import io.legado.app.ui.book.changesource.ChangeBookSourceDialog
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.manga.rv.MangaAdapter
import io.legado.app.ui.book.manga.rv.PreloadScrollListener
import io.legado.app.ui.book.read.MangaMenu
import io.legado.app.ui.book.read.ReadBookActivity.Companion.RESULT_DELETED
import io.legado.app.ui.book.toc.TocActivityResult
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.DebugLog
import io.legado.app.utils.StartActivityContract
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.gone
import io.legado.app.utils.immersionFullScreen
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.setLightStatusBar
import io.legado.app.utils.setNavigationBarColorAuto
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible

class ReadMangaActivity : VMBaseActivity<ActivityMangeBinding, MangaViewModel>(),
    ReadMange.Callback, ChangeBookSourceDialog.CallBack, MangaMenu.CallBack {

    private val menuLayoutIsVisible get() = binding.mangaMenu.isVisible
    private val mAdapter: MangaAdapter by lazy {
        MangaAdapter()
    }
    private val loadMoreView by lazy {
        LoadMoreView(this).apply {
            setBackgroundColor(getCompatColor(R.color.book_ant_10))
            getLoading().loadingColor = getCompatColor(R.color.white)
            getLoadingText().setTextColor(getCompatColor(R.color.white))
        }
    }

    //打开目录返回选择章节返回结果
    private val tocActivity =
        registerForActivityResult(TocActivityResult()) {
            it?.let {
                binding.flLoading.isVisible = true
                viewModel.openChapter(it.first, it.second)
            }
        }
    private val bookInfoActivity =
        registerForActivityResult(StartActivityContract(BookInfoActivity::class.java)) {
            if (it.resultCode == RESULT_OK) {
                setResult(RESULT_DELETED)
                super.finish()
            }
        }
    override val binding by viewBinding(ActivityMangeBinding::inflate)
    override val viewModel by viewModels<MangaViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        immersionFullScreen(WindowInsetsControllerCompat(window, binding.root))
        ReadMange.register(this)
        binding.mRecyclerMange.run {
            adapter = mAdapter
            itemAnimator = null
            setItemViewCacheSize(20)
            setPreScrollListener { _, _, dy, position ->
                if (dy > 0 && position + 2 > mAdapter.getCurrentList().size - 3) {
                    if (mAdapter.getCurrentList().last() is ReaderLoading) {
                        val nextIndex =
                            (mAdapter.getCurrentList().last() as ReaderLoading).mNextChapterIndex
                        if (nextIndex != -1) {
                            scrollToBottom(false, nextIndex)
                        }
                    }
                }
            }
            setNestedPreScrollListener { _, _, _, position ->
                if (mAdapter.getCurrentList()
                        .isNotEmpty() && position <= mAdapter.getCurrentList().lastIndex
                ) {
                    try {
                        val content = mAdapter.getCurrentList()[position]
                        if (content is MangeContent) {
                            ReadMange.durChapterPos = content.mDurChapterPos.minus(1)
                            upText(
                                content.mChapterPagePos,
                                content.mChapterPageCount,
                                content.mDurChapterPos,
                                content.mDurChapterCount
                            )
                        }
                    } catch (e: Exception) {
                        e.printOnDebug()
                    }

                }
            }
            addOnScrollListener(
                PreloadScrollListener(
                    binding.mRecyclerMange.layoutManager as LinearLayoutManager,
                    10
                )
            )
            onToucheMiddle {
                if (!binding.mangaMenu.isVisible) {
                    binding.mangaMenu.runMenuIn()
                }
            }
        }
        binding.retry.setOnClickListener {
            binding.llLoading.isVisible = true
            binding.retry.isGone = true
            mFirstLoading = false
            ReadMange.loadContent()
        }

        mAdapter.addFooterView {
            ViewLoadMoreBinding.bind(loadMoreView)
        }
        loadMoreView.setOnClickListener {
            if (!loadMoreView.isLoading && !ReadMange.gameOver) {
                scrollToBottom(true, ReadMange.durChapterPagePos)
            }
        }
        loadMoreView.gone()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.initData(intent)
    }

    private fun scrollToBottom(forceLoad: Boolean = false, index: Int) {
        if ((loadMoreView.hasMore && !loadMoreView.isLoading) && !ReadMange.gameOver || forceLoad) {
            loadMoreView.hasMore()
            ReadMange.moveToNextChapter(index)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        Looper.myQueue().addIdleHandler {
            viewModel.initData(intent)
            false
        }
    }

    override fun loadContentFinish(list: MutableList<Any>) {
        if (!this.isDestroyed) {
            mAdapter.submitList(list) {
                if (!mFirstLoading) {
                    if (list.size > 1) {
                        binding.infobar.isVisible = true
                        upText(
                            ReadMange.durChapterPagePos,
                            ReadMange.durChapterPageCount,
                            ReadMange.durChapterPos,
                            ReadMange.durChapterCount
                        )
                    }

                    if (ReadMange.durChapterPos + 2 > mAdapter.getCurrentList().size - 3) {
                        val nextIndex =
                            (mAdapter.getCurrentList().last() as ReaderLoading).mNextChapterIndex
                        scrollToBottom(index = nextIndex)
                    } else {
                        DebugLog.d("tag","当前位置---${ReadMange.durChapterPos}")
                        if (ReadMange.durChapterPos != 0) {
                            binding.mRecyclerMange.scrollToPosition(ReadMange.durChapterPos)
                        }
                    }
                }
                ReadMange.chapterChanged = false
                loadMoreView.visible()
                mFirstLoading = true
                loadMoreView.stopLoad()
            }
        }
    }

    private fun upText(
        chapterPagePos: Int, chapterPageCount: Int, chapterPos: Int, chapterCount: Int,
    ) {
        binding.infobar.update(
            chapterPagePos,
            chapterPageCount,
            chapterPagePos.minus(1f).div(chapterPageCount.minus(1f)),
            chapterPos,
            chapterCount
        )
    }

    override fun onPause() {
        super.onPause()
        if (ReadMange.inBookshelf) {
            ReadMange.saveRead()
        }
    }

    override fun loadComplete() {
        binding.flLoading.isGone = true
    }

    override fun loadFail() {
        if (!mFirstLoading) {
            binding.llLoading.isGone = true
            binding.retry.isVisible = true
        } else {
            loadMoreView.error(null, "加载失败，点击重试")
        }
    }

    override fun noData() {
        loadMoreView.noMore("暂无章节了！")
    }

    override val chapterList: MutableList<Any>
        get() = mAdapter.getCurrentList()

    override fun onDestroy() {
        ReadMange.unregister()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Glide.get(this).clearMemory()
    }

    override val oldBook: Book?
        get() = ReadMange.book

    override fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        if (book.isImage) {
            binding.flLoading.isVisible = true
            ReadMange.chapterChanged = true
            viewModel.changeTo(book, toc)
        } else {
            toastOnUi("所选择的源不是漫画源")
        }
    }


    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_manga, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    /**
     * 菜单
     */
    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_change_source -> {
                binding.mangaMenu.runMenuOut()
                ReadMange.book?.let {
                    showDialogFragment(ChangeBookSourceDialog(it.name, it.author))
                }
            }

            R.id.menu_catalog -> {
                ReadMange.book?.let {
                    tocActivity.launch(it.bookUrl)
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun upNavigationBarColor() {
        when {
            binding.mangaMenu.isVisible -> super.upNavigationBarColor()
            !AppConfig.immNavigationBar -> super.upNavigationBarColor()
            else -> setNavigationBarColorAuto(ReadBookConfig.bgMeanColor)
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun upNavigationBar(value: Boolean) {
        binding.mangaMenu.isVisible = value
    }

    override fun openBookInfoActivity() {
        ReadMange.book?.let {
            bookInfoActivity.launch {
                putExtra("name", it.name)
                putExtra("author", it.author)
            }
        }
    }

    override fun upSystemUiVisibility(value: Boolean) {
        upSystemUiVisibility(isInMultiWindow, !menuLayoutIsVisible, false)
        upNavigationBarColor()
        upNavigationBar(value)
        if (!value) {
            immersionFullScreen(WindowInsetsControllerCompat(window, binding.root))
        }
    }

    /**
     * 更新状态栏,导航栏
     */
    fun upSystemUiVisibility(
        isInMultiWindow: Boolean,
        toolBarHide: Boolean = true,
        useBgMeanColor: Boolean = false,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.run {
                if (toolBarHide && ReadBookConfig.hideNavigationBar) {
                    hide(WindowInsets.Type.navigationBars())
                } else {
                    show(WindowInsets.Type.navigationBars())
                }
                if (toolBarHide && ReadBookConfig.hideStatusBar) {
                    hide(WindowInsets.Type.statusBars())
                } else {
                    show(WindowInsets.Type.statusBars())
                }
            }
        }
        upSystemUiVisibilityO(isInMultiWindow, toolBarHide)
        if (toolBarHide) {
            setLightStatusBar(ReadBookConfig.durConfig.curStatusIconDark())
        } else {
            val statusBarColor =
                if (AppConfig.readBarStyleFollowPage
                    && ReadBookConfig.durConfig.curBgType() == 0
                    || useBgMeanColor
                ) {
                    ReadBookConfig.bgMeanColor
                } else {
                    ThemeStore.statusBarColor(this, AppConfig.isTransparentStatusBar)
                }
            setLightStatusBar(ColorUtils.isColorLight(statusBarColor))
        }
    }

    @Suppress("DEPRECATION")
    private fun upSystemUiVisibilityO(
        isInMultiWindow: Boolean,
        toolBarHide: Boolean = true,
    ) {
        var flag = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        if (!isInMultiWindow) {
            flag = flag or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        if (ReadBookConfig.hideNavigationBar) {
            flag = flag or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            if (toolBarHide) {
                flag = flag or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }
        }
        if (ReadBookConfig.hideStatusBar && toolBarHide) {
            flag = flag or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        window.decorView.systemUiVisibility = flag
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action
        val isDown = action == 0

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (isDown && !binding.mangaMenu.canShowMenu) {
                binding.mangaMenu.runMenuIn()
                return true
            }
            if (!isDown && !binding.mangaMenu.canShowMenu) {
                binding.mangaMenu.canShowMenu = true
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

}