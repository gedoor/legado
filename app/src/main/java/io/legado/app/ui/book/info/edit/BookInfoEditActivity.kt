package io.legado.app.ui.book.info.edit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.documentfile.provider.DocumentFile
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.ui.book.changecover.ChangeCoverDialog
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_book_info_edit.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.toast
import java.io.File

class BookInfoEditActivity :
    VMBaseActivity<BookInfoEditViewModel>(R.layout.activity_book_info_edit),
    ChangeCoverDialog.CallBack {

    private val resultSelectCover = 132

    override val viewModel: BookInfoEditViewModel
        get() = getViewModel(BookInfoEditViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.bookData.observe(this, { upView(it) })
        if (viewModel.bookData.value == null) {
            intent.getStringExtra("bookUrl")?.let {
                viewModel.loadBook(it)
            }
        }
        initEvent()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_info_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> saveData()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initEvent() {
        tv_change_cover.onClick {
            viewModel.bookData.value?.let {
                ChangeCoverDialog.show(supportFragmentManager, it.name, it.author)
            }
        }
        tv_select_cover.onClick {
            selectImage()
        }
        tv_refresh_cover.onClick {
            viewModel.book?.customCoverUrl = tie_cover_url.text?.toString()
            upCover()
        }
    }

    private fun upView(book: Book) {
        tie_book_name.setText(book.name)
        tie_book_author.setText(book.author)
        tie_cover_url.setText(book.getDisplayCover())
        tie_book_intro.setText(book.getDisplayIntro())
        upCover()
    }

    private fun upCover() {
        viewModel.book.let {
            iv_cover.load(it?.getDisplayCover(), it?.name, it?.author)
        }
    }

    private fun saveData() {
        viewModel.book?.let { book ->
            book.name = tie_book_name.text?.toString() ?: ""
            book.author = tie_book_author.text?.toString() ?: ""
            val customCoverUrl = tie_cover_url.text?.toString()
            book.customCoverUrl = if (customCoverUrl == book.coverUrl) null else customCoverUrl
            book.customIntro = tie_book_intro.text?.toString()
            viewModel.saveBook(book) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, resultSelectCover)
    }

    override fun coverChangeTo(coverUrl: String) {
        viewModel.book?.customCoverUrl = coverUrl
        tie_cover_url.setText(coverUrl)
        upCover()
    }

    private fun coverChangeTo(uri: Uri) {
        if (uri.isContentScheme()) {
            val doc = DocumentFile.fromSingleUri(this, uri)
            doc?.name?.let {
                var file = this.externalFilesDir
                file = FileUtils.createFileIfNotExist(file, "covers", it)
                kotlin.runCatching {
                    DocumentUtils.readBytes(this, doc.uri)
                }.getOrNull()?.let { byteArray ->
                    file.writeBytes(byteArray)
                    coverChangeTo(file.absolutePath)
                } ?: toast("获取文件出错")
            }
        } else {
            PermissionsCompat.Builder(this)
                .addPermissions(
                    Permissions.READ_EXTERNAL_STORAGE,
                    Permissions.WRITE_EXTERNAL_STORAGE
                )
                .rationale(R.string.bg_image_per)
                .onGranted {
                    RealPathUtil.getPath(this, uri)?.let { path ->
                        val imgFile = File(path)
                        if (imgFile.exists()) {
                            var file = this.externalFilesDir
                            file = FileUtils.createFileIfNotExist(file, "covers", imgFile.name)
                            file.writeBytes(imgFile.readBytes())
                            coverChangeTo(file.absolutePath)
                        }
                    }
                }
                .request()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            resultSelectCover -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri ->
                        coverChangeTo(uri)
                    }
                }
            }
        }
    }
}