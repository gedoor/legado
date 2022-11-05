package io.legado.app.ui.association

import android.app.Application
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.databinding.DialogAddToBookshelfBinding
import io.legado.app.exception.NoStackTraceException
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.setLayout
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding

class AddToBookshelfDialog() : BaseDialogFragment(R.layout.dialog_add_to_bookshelf) {

    constructor(bookUrl: String, finishOnDismiss: Boolean = false) : this() {
        arguments = Bundle().apply {
            putString("bookUrl", bookUrl)
            putBoolean("finishOnDismiss", finishOnDismiss)
        }
    }

    val binding by viewBinding(DialogAddToBookshelfBinding::bind)
    val viewModel by viewModels<ViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (arguments?.getBoolean("finishOnDismiss") == true) {
            activity?.finish()
        }
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val bookUrl = arguments?.getString("bookUrl")
        if (bookUrl.isNullOrBlank()) {
            toastOnUi("url不能为空")
            dismiss()
            return
        }
        viewModel.loadStateLiveData.observe(this) {
            //todo
        }
        viewModel.load(bookUrl) {
            //todo
        }
    }

    class ViewModel(application: Application) : BaseViewModel(application) {

        val loadStateLiveData = MutableLiveData<Boolean>()

        fun load(bookUrl: String, success: (book: Book) -> Unit) {
            execute {
                val sources = appDb.bookSourceDao.hasBookUrlPattern
                appDb.bookDao.getBook(bookUrl)?.let {
                    throw NoStackTraceException("${it.name} 已在书架")
                }
                val baseUrl = NetworkUtils.getBaseUrl(bookUrl)
                    ?: throw NoStackTraceException("书籍地址格式不对")
                var source = appDb.bookSourceDao.getBookSource(baseUrl)
                if (source == null) {
                    sources.forEach { bookSource ->
                        if (bookUrl.matches(bookSource.bookUrlPattern!!.toRegex())) {
                            source = bookSource
                            return@forEach
                        }
                    }
                }
                source?.let { bookSource ->
                    val book = Book(
                        bookUrl = bookUrl,
                        origin = bookSource.bookSourceUrl,
                        originName = bookSource.bookSourceName
                    )
                    WebBook.getBookInfoAwait(bookSource, book)
                    book.order = appDb.bookDao.minOrder - 1
                    return@execute book
                } ?: throw NoStackTraceException("未找到匹配书源")
            }.onSuccess {
                success.invoke(it)
            }.onStart {
                loadStateLiveData.postValue(true)
            }.onFinally {
                loadStateLiveData.postValue(false)
            }
        }

    }

}