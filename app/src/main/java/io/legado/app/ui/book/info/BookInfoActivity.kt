package io.legado.app.ui.book.info

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.LinearLayout
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.BookType
import io.legado.app.constant.Theme
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ActivityBookInfoBinding
import io.legado.app.help.BlurTransformation
import io.legado.app.help.ImageLoader
import io.legado.app.help.IntentDataHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.ui.audio.AudioPlayActivity
import io.legado.app.ui.book.changecover.ChangeCoverDialog
import io.legado.app.ui.book.changesource.ChangeSourceDialog
import io.legado.app.ui.book.group.GroupSelectDialog
import io.legado.app.ui.book.info.edit.BookInfoEditActivity
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.book.toc.ChapterListActivity
import io.legado.app.ui.widget.image.CoverImageView
import io.legado.app.utils.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast


class BookInfoActivity :
    VMBaseActivity<ActivityBookInfoBinding, BookInfoViewModel>(toolBarTheme = Theme.Dark),
    GroupSelectDialog.CallBack,
    ChapterListAdapter.CallBack,
    ChangeSourceDialog.CallBack,
    ChangeCoverDialog.CallBack {

    private val requestCodeChapterList = 568
    private val requestCodeInfoEdit = 562
    private val requestCodeRead = 432

    override val viewModel: BookInfoViewModel
        get() = getViewModel(BookInfoViewModel::class.java)

    override fun getViewBinding(): ActivityBookInfoBinding {
        return ActivityBookInfoBinding.inflate(layoutInflater)
    }

    @SuppressLint("PrivateResource")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.transparent()
        binding.arcView.setBgColor(backgroundColor)
        binding.llInfo.setBackgroundColor(backgroundColor)
        binding.scrollView.setBackgroundColor(backgroundColor)
        binding.flAction.setBackgroundColor(bottomBackground)
        binding.tvShelf.setTextColor(getPrimaryTextColor(ColorUtils.isColorLight(bottomBackground)))
        binding.tvToc.text = getString(R.string.toc_s, getString(R.string.loading))
        viewModel.bookData.observe(this, { showBook(it) })
        viewModel.chapterListData.observe(this, { upLoading(false, it) })
        viewModel.initData(intent)
        initOnClick()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_info, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_edit -> {
                if (viewModel.inBookshelf) {
                    viewModel.bookData.value?.let {
                        startActivityForResult<BookInfoEditActivity>(
                            requestCodeInfoEdit,
                            Pair("bookUrl", it.bookUrl)
                        )
                    }
                } else {
                    toast(R.string.after_add_bookshelf)
                }
            }
            R.id.menu_share_it -> {
                viewModel.bookData.value?.let {
                    val bookJson = GSON.toJson(it)
                    val shareStr = "${it.bookUrl}#$bookJson"
                    shareWithQr(it.name, shareStr)
                }
            }
            R.id.menu_refresh -> {
                upLoading(true)
                viewModel.bookData.value?.let {
                    if (it.isLocalBook()) {
                        it.tocUrl = ""
                    }
                    viewModel.loadBookInfo(it, false)
                }
            }
            R.id.menu_copy_url -> viewModel.bookData.value?.bookUrl?.let {
                sendToClip(it)
            } ?: toast(R.string.no_book)
            R.id.menu_can_update -> {
                if (viewModel.inBookshelf) {
                    viewModel.bookData.value?.let {
                        it.canUpdate = !it.canUpdate
                        viewModel.saveBook()
                    }
                } else {
                    toast(R.string.after_add_bookshelf)
                }
            }
            R.id.menu_clear_cache -> viewModel.clearCache()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_can_update)?.isChecked =
            viewModel.bookData.value?.canUpdate ?: true
        return super.onMenuOpened(featureId, menu)
    }

    private fun showBook(book: Book) = with(binding) {
        showCover(book)
        tvName.text = book.name
        tvAuthor.text = getString(R.string.author_show, book.getRealAuthor())
        tvOrigin.text = getString(R.string.origin_show, book.originName)
        tvLasted.text = getString(R.string.lasted_show, book.latestChapterTitle)
        tvIntro.text = book.getDisplayIntro()
        upTvBookshelf()
        val kinds = book.getKindList()
        if (kinds.isEmpty()) {
            lbKind.gone()
        } else {
            lbKind.visible()
            lbKind.setLabels(kinds)
        }
        upGroup(book.group)
    }

    private fun showCover(book: Book) {
        binding.ivCover.load(book.getDisplayCover(), book.name, book.author)
        ImageLoader.load(this, book.getDisplayCover())
            .transition(DrawableTransitionOptions.withCrossFade(1500))
            .thumbnail(defaultCover())
            .apply(bitmapTransform(BlurTransformation(this, 25)))
            .into(binding.bgBook)  //模糊、渐变、缩小效果
    }

    private fun defaultCover(): RequestBuilder<Drawable> {
        return ImageLoader.load(this, CoverImageView.defaultDrawable)
            .apply(bitmapTransform(BlurTransformation(this, 25)))
    }

    private fun upLoading(isLoading: Boolean, chapterList: List<BookChapter>? = null) {
        when {
            isLoading -> {
                binding.tvToc.text = getString(R.string.toc_s, getString(R.string.loading))
            }
            chapterList.isNullOrEmpty() -> {
                binding.tvToc.text = getString(R.string.toc_s, getString(R.string.error_load_toc))
            }
            else -> {
                viewModel.bookData.value?.let {
                    if (it.durChapterIndex < chapterList.size) {
                        binding.tvToc.text =
                            getString(R.string.toc_s, chapterList[it.durChapterIndex].title)
                    } else {
                        binding.tvToc.text = getString(R.string.toc_s, chapterList.last().title)
                    }
                }
            }
        }
    }

    private fun upTvBookshelf() {
        if (viewModel.inBookshelf) {
            binding.tvShelf.text = getString(R.string.remove_from_bookshelf)
        } else {
            binding.tvShelf.text = getString(R.string.add_to_shelf)
        }
    }

    private fun upGroup(groupId: Long) {
        viewModel.loadGroup(groupId) {
            if (it.isNullOrEmpty()) {
                binding.tvGroup.text = getString(R.string.group_s, getString(R.string.no_group))
            } else {
                binding.tvGroup.text = getString(R.string.group_s, it)
            }
        }
    }

    private fun initOnClick() = with(binding) {
        ivCover.onClick {
            viewModel.bookData.value?.let {
                ChangeCoverDialog.show(supportFragmentManager, it.name, it.author)
            }
        }
        tvRead.onClick {
            viewModel.bookData.value?.let {
                readBook(it)
            }
        }
        tvShelf.onClick {
            if (viewModel.inBookshelf) {
                deleteBook()
            } else {
                viewModel.addToBookshelf {
                    upTvBookshelf()
                }
            }
        }
        tvOrigin.onClick {
            viewModel.bookData.value?.let {
                startActivity<BookSourceEditActivity>(Pair("data", it.origin))
            }
        }
        tvChangeSource.onClick {
            viewModel.bookData.value?.let {
                ChangeSourceDialog.show(supportFragmentManager, it.name, it.author)
            }
        }
        tvTocView.onClick {
            if (!viewModel.inBookshelf) {
                viewModel.saveBook {
                    viewModel.saveChapterList {
                        openChapterList()
                    }
                }
            } else {
                openChapterList()
            }
        }
        tvChangeGroup.onClick {
            viewModel.bookData.value?.let {
                GroupSelectDialog.show(supportFragmentManager, it.group)
            }
        }
        tvAuthor.onClick {
            startActivity<SearchActivity>(Pair("key", viewModel.bookData.value?.author))
        }
        tvName.onClick {
            startActivity<SearchActivity>(Pair("key", viewModel.bookData.value?.name))
        }
    }

    @SuppressLint("InflateParams")
    private fun deleteBook() {
        viewModel.bookData.value?.let {
            if (it.isLocalBook()) {
                alert(
                    titleResource = R.string.sure,
                    messageResource = R.string.sure_del
                ) {
                    val checkBox = CheckBox(this@BookInfoActivity).apply {
                        setText(R.string.delete_book_file)
                    }
                    val view = LinearLayout(this@BookInfoActivity).apply {
                        setPadding(16.dp, 0, 16.dp, 0)
                        addView(checkBox)
                    }
                    customView = view
                    positiveButton(R.string.yes) {
                        viewModel.delBook(checkBox.isChecked) {
                            finish()
                        }
                    }
                    negativeButton(R.string.no)
                }.show()
            } else {
                viewModel.delBook {
                    upTvBookshelf()
                }
            }
        }
    }

    private fun openChapterList() {
        if (viewModel.chapterListData.value.isNullOrEmpty()) {
            toast(R.string.chapter_list_empty)
            return
        }
        viewModel.bookData.value?.let {
            startActivityForResult<ChapterListActivity>(
                requestCodeChapterList,
                Pair("bookUrl", it.bookUrl)
            )
        }
    }

    private fun readBook(book: Book) {
        if (!viewModel.inBookshelf) {
            viewModel.saveBook {
                viewModel.saveChapterList {
                    startReadActivity(book)
                }
            }
        } else {
            viewModel.saveBook {
                startReadActivity(book)
            }
        }
    }

    private fun startReadActivity(book: Book) {
        when (book.type) {
            BookType.audio -> startActivityForResult<AudioPlayActivity>(
                requestCodeRead,
                Pair("bookUrl", book.bookUrl),
                Pair("inBookshelf", viewModel.inBookshelf)
            )
            else -> startActivityForResult<ReadBookActivity>(
                requestCodeRead,
                Pair("bookUrl", book.bookUrl),
                Pair("inBookshelf", viewModel.inBookshelf),
                Pair("key", IntentDataHelp.putData(book))
            )
        }
    }

    override val oldBook: Book?
        get() = viewModel.bookData.value

    override fun changeTo(book: Book) {
        upLoading(true)
        viewModel.changeTo(book)
    }

    override fun coverChangeTo(coverUrl: String) {
        viewModel.bookData.value?.let {
            it.coverUrl = coverUrl
            viewModel.saveBook()
            showCover(it)
        }
    }

    override fun openChapter(chapter: BookChapter) {
        if (chapter.index != viewModel.durChapterIndex) {
            viewModel.bookData.value?.let {
                it.durChapterIndex = chapter.index
                it.durChapterPos = 0
                readBook(it)
            }
        }
    }

    override fun durChapterIndex(): Int {
        return viewModel.durChapterIndex
    }

    override fun upGroup(requestCode: Int, groupId: Long) {
        upGroup(groupId)
        viewModel.bookData.value?.group = groupId
        if (viewModel.inBookshelf) {
            viewModel.saveBook()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            requestCodeInfoEdit ->
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.upEditBook()
                }
            requestCodeChapterList ->
                if (resultCode == Activity.RESULT_OK) {
                    viewModel.bookData.value?.let {
                        data?.getIntExtra("index", it.durChapterIndex)?.let { index ->
                            if (it.durChapterIndex != index) {
                                it.durChapterIndex = index
                                it.durChapterPos = 0
                            }
                            startReadActivity(it)
                        }
                    }
                } else {
                    if (!viewModel.inBookshelf) {
                        viewModel.delBook()
                    }
                }
            requestCodeRead -> if (resultCode == Activity.RESULT_OK) {
                viewModel.inBookshelf = true
                upTvBookshelf()
            }
        }
    }
}