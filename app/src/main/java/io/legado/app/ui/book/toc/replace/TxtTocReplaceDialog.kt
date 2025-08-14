package io.legado.app.ui.book.toc.replace

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.DialogTocReplaceBinding
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.theme.primaryColor
import io.legado.app.model.ReadBook
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.book.toc.TocViewModel
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.applyTint
import io.legado.app.utils.longToast
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.readText
import io.legado.app.utils.setLayout
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.showHelp
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * txt目录替换
 */
class TxtTocReplaceDialog() : BaseDialogFragment(R.layout.dialog_toc_replace),
    Toolbar.OnMenuItemClickListener {

    interface CallBack {
        fun onTocReplaced()
    }

    constructor(bookUrl: String?) : this() {
        arguments = Bundle().apply {
            putString("bookUrl", bookUrl)
        }
    }

    private var callBack: CallBack? = null
    private val binding by viewBinding(DialogTocReplaceBinding::bind)
    val viewModel by viewModels<TocViewModel>()
    private val adapter by lazy { TxtTocReplaceAdapter(requireContext()) }
    private var bookUrl: String? = null
    private var importedChapters: List<BookChapter> = emptyList()
    private val importDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            importToc(uri)
        }
    }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.8f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        bookUrl = arguments?.getString("bookUrl")
        binding.toolBar.setTitle(R.string.txt_toc_replace)
        binding.toolBar.inflateMenu(R.menu.txt_toc_replace)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
        initView()
        initData()

        (activity as? CallBack)?.let {
            callBack = it
        }
    }

    private fun initView() = binding.run {
        recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        recyclerView.adapter = adapter
        tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        tvOk.setOnClickListener {
            replaceToc()
        }
    }

    private fun initData() {
        bookUrl?.let { url ->
            execute {
                val originalChapters = appDb.bookChapterDao.getChapterList(url)
                    .filter { it.index != 0 }

                withContext(Dispatchers.Main) {
                    if (importedChapters.isEmpty()) {
                        val emptyList = originalChapters.map { it to BookChapter(title = "无数据") }
                        adapter.setItems(emptyList, adapter.diffItemCallBack)
                    }
                }
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_import_local -> importDoc.launch {
                mode = HandleFileContract.FILE
                allowExtensions = arrayOf("txt", "json")
            }
            //R.id.menu_import_onLine -> showImportDialog()
            //R.id.menu_import_qr -> qrCodeResult.launch()
            R.id.menu_help -> showHelp("txtTocReplaceHelp")
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
        return false
    }

    private fun importToc(uri: Uri) {
        execute {
            val originalChapters = appDb.bookChapterDao.getChapterList(bookUrl?:"")
                .filter { it.index != 0 }

            val text = uri.readText(requireContext())
            importedChapters = Gson().fromJson(text, Array<BookChapter>::class.java).toList()

            val pairedList = mutableListOf<Pair<BookChapter, BookChapter>>()
            val maxSize = maxOf(originalChapters.size, importedChapters.size)

            for (i in 0 until maxSize) {
                val original = originalChapters.getOrNull(i) ?: BookChapter(title = "无数据")
                val imported = importedChapters.getOrNull(i) ?: BookChapter(title = "无数据")
                pairedList.add(original to imported)
            }

            withContext(Dispatchers.Main) {
                adapter.setItems(pairedList, adapter.diffItemCallBack)
            }
        }.onError {
            it.printOnDebug()
            AppLog.put("导入失败\n${it.localizedMessage}", it, true)
        }
    }

    private fun replaceToc() {
        execute {
            val bookUrl = bookUrl ?: throw Exception("bookUrl为空")
            val book = ReadBook.book ?: throw Exception("未找到书籍")
            val chapters = appDb.bookChapterDao.getChapterList(bookUrl)
                .sortedBy { it.index }
             var volumeCount = 0
            
            chapters.forEachIndexed { i, chapter ->
                if (chapter.index == 0) return@forEachIndexed

                val importedIndex = i - 1
                if (importedIndex in importedChapters.indices) {
                    val contentProcessor = ContentProcessor.get(book.name, book.origin)
                    var content = BookHelp.getContent(book, chapter) ?: return@forEachIndexed
                    content = contentProcessor.getContent(book, chapter, content, includeTitle = false)
                        .toString()
                    val importedChapter = importedChapters[importedIndex]

                    Coroutine.async {
                        chapter.title = importedChapter.title
                        appDb.bookChapterDao.update(chapter)
                        BookHelp.saveText(book, chapter, content)
                    }
                }
            }
        }.onStart {
            binding.rlLoading.visible()
        }.onSuccess {
            callBack?.onTocReplaced()
            dismissAllowingStateLoss()
            longToast("目录替换成功")
        }.onError {
            it.printOnDebug()
            AppLog.put("目录替换失败\n${it.localizedMessage}", it, true)
        }.onFinally {
            binding.rlLoading.gone()
        }
    }
}