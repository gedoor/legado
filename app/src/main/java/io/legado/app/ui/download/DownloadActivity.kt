package io.legado.app.ui.download

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.EventBus
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.service.help.Download
import io.legado.app.ui.filechooser.FileChooserDialog
import io.legado.app.utils.ACache
import io.legado.app.utils.applyTint
import io.legado.app.utils.getViewModel
import io.legado.app.utils.observeEvent
import kotlinx.android.synthetic.main.activity_download.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast


class DownloadActivity : VMBaseActivity<DownloadViewModel>(R.layout.activity_download),
    FileChooserDialog.CallBack,
    DownloadAdapter.CallBack {
    private val exportRequestCode = 32
    lateinit var adapter: DownloadAdapter
    private var bookshelfLiveData: LiveData<List<Book>>? = null
    private var menu: Menu? = null
    private var exportPosition = -1

    override val viewModel: DownloadViewModel
        get() = getViewModel(DownloadViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initLiveData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.download, menu)
        this.menu = menu
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_download -> launch(IO) {
                App.db.bookDao().webBooks.forEach { book ->
                    Download.start(
                        this@DownloadActivity,
                        book.bookUrl,
                        book.durChapterIndex,
                        book.totalChapterNum
                    )
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(this)
        adapter = DownloadAdapter(this, this)
        recycler_view.adapter = adapter
    }

    private fun initLiveData() {
        bookshelfLiveData?.removeObservers(this)
        bookshelfLiveData = App.db.bookDao().observeDownload()
        bookshelfLiveData?.observe(this, Observer {
            adapter.setItems(it)
            initCacheSize(it)
        })
    }

    private fun initCacheSize(books: List<Book>) {
        launch(IO) {
            books.forEach { book ->
                val chapterCaches = hashSetOf<String>()
                val cacheNames = BookHelp.getChapterFiles(book)
                App.db.bookChapterDao().getChapterList(book.bookUrl).forEach { chapter ->
                    if (cacheNames.contains(BookHelp.formatChapterName(chapter))) {
                        chapterCaches.add(chapter.url)
                    }
                }
                adapter.cacheChapters[book.bookUrl] = chapterCaches
                withContext(Dispatchers.Main) {
                    adapter.notifyItemRangeChanged(0, adapter.getActualItemCount(), true)
                }
            }
        }
    }

    override fun observeLiveBus() {
        observeEvent<HashMap<String, LinkedHashSet<BookChapter>>>(EventBus.UP_DOWNLOAD) {
            if (it.isEmpty()) {
                menu?.findItem(R.id.menu_download)?.setIcon(R.drawable.ic_play_24dp)
                menu?.applyTint(this)
            } else {
                menu?.findItem(R.id.menu_download)?.setIcon(R.drawable.ic_stop_black_24dp)
                menu?.applyTint(this)
            }
            adapter.downloadMap = it
            adapter.notifyItemRangeChanged(0, adapter.getActualItemCount(), true)
        }
        observeEvent<BookChapter>(EventBus.SAVE_CONTENT) {
            adapter.cacheChapters[it.bookUrl]?.add(it.url)
        }
    }

    override fun export(position: Int) {
        exportPosition = position
        alert {
            titleResource = R.string.select_folder
            items(resources.getStringArray(R.array.select_folder).toList()) { _, index ->
                when (index) {
                    0 -> {
                        val path = ACache.get(this@DownloadActivity).getAsString("exportBookPath")
                        if (path.isNullOrEmpty()) {
                            toast("没有默认路径")
                        } else {
                            adapter.getItem(exportPosition)?.let {
                                viewModel.export(path, it)
                            }
                        }
                    }
                    1 -> {
                        try {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            startActivityForResult(intent, exportRequestCode)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                            toast(e.localizedMessage ?: "ERROR")
                        }
                    }
                    2 -> {
                        PermissionsCompat.Builder(this@DownloadActivity)
                            .addPermissions(*Permissions.Group.STORAGE)
                            .rationale(R.string.tip_perm_request_storage)
                            .onGranted {
                                FileChooserDialog.show(
                                    supportFragmentManager,
                                    exportRequestCode,
                                    mode = FileChooserDialog.DIRECTORY
                                )
                            }
                            .request()
                    }
                }
            }
        }.show()
    }

    override fun onFilePicked(requestCode: Int, currentPath: String) {
        when (requestCode) {
            exportRequestCode -> {
                adapter.getItem(exportPosition)?.let {
                    viewModel.export(currentPath, it)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            exportRequestCode -> if (resultCode == Activity.RESULT_OK) {
                data?.data?.let { uri ->
                    adapter.getItem(exportPosition)?.let {
                        viewModel.export(uri.toString(), it)
                    }
                }
            }

        }
    }
}