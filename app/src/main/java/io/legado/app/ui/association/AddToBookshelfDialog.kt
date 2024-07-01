package io.legado.app.ui.association

import android.annotation.SuppressLint
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
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.DialogAddToBookshelfBinding
import io.legado.app.exception.NoStackTraceException
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.webBook.WebBook
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 添加书籍链接到书架，需要对应网站书源
 * ${origin}/${path}, {origin: bookSourceUrl}
 * 按以下顺序尝试匹配书源并添加网址
 * - UrlOption中的指定的书源网址bookSourceUrl
 * - 在所有启用的书源中匹配orgin
 * - 在所有启用的书源中使用详情页正则匹配${origin}/${path}, {origin: bookSourceUrl}
 */
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

    @SuppressLint("SetTextI18n")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val bookUrl = arguments?.getString("bookUrl")
        if (bookUrl.isNullOrBlank()) {
            toastOnUi("url不能为空")
            dismiss()
            return
        }
        viewModel.loadStateLiveData.observe(this) {
            if (it) {
                binding.rotateLoading.visible()
                binding.bookInfo.invisible()
            } else {
                binding.rotateLoading.gone()
            }
        }
        viewModel.loadErrorLiveData.observe(this) {
            toastOnUi(it)
            dismiss()
        }
        viewModel.load(bookUrl) {
            binding.bookInfo.visible()
            binding.tvName.text = it.name
            binding.tvAuthor.text = it.author
            binding.tvOrigin.text = it.originName
        }
        binding.tvCancel.setOnClickListener {
            dismiss()
        }
        binding.tvOk.setOnClickListener {
            viewModel.saveBook {
                it?.let {
                    dismiss()
                } ?: toastOnUi(R.string.no_book)
            }
        }
        binding.tvRead.setOnClickListener {
            viewModel.saveBook {
                it?.let {
                    startActivity<ReadBookActivity> {
                        putExtra("bookUrl", it.bookUrl)
                        putExtra("inBookshelf", false)
                    }
                    dismiss()
                } ?: toastOnUi(R.string.no_book)
            }
        }
    }

    class ViewModel(application: Application) : BaseViewModel(application) {

        val loadStateLiveData = MutableLiveData<Boolean>()
        val loadErrorLiveData = MutableLiveData<String>()
        var book: Book? = null

        fun load(bookUrl: String, success: (book: Book) -> Unit) {
            execute {
                appDb.bookDao.getBook(bookUrl)?.let {
                    throw NoStackTraceException("${it.name} 已在书架")
                }
                val baseUrl = NetworkUtils.getBaseUrl(bookUrl)
                    ?: throw NoStackTraceException("书籍地址格式不对")
                val urlMatcher = AnalyzeUrl.paramPattern.matcher(bookUrl)
                if (urlMatcher.find()) {
                    val origin = GSON.fromJsonObject<AnalyzeUrl.UrlOption>(
                        bookUrl.substring(urlMatcher.end())
                    ).getOrNull()?.getOrigin()
                    origin?.let {
                        val source = appDb.bookSourceDao.getBookSource(it)
                        source?.let {
                            getBookInfo(bookUrl, source)?.let { book ->
                                return@execute book
                            }
                        }
                    }
                }
                appDb.bookSourceDao.getBookSourceAddBook(baseUrl)?.let { source ->
                    getBookInfo(bookUrl, source)?.let { book ->
                        return@execute book
                    }
                }
                appDb.bookSourceDao.hasBookUrlPattern.forEach { source ->
                    if (bookUrl.matches(source.bookUrlPattern!!.toRegex())) {
                        getBookInfo(bookUrl, source)?.let { book ->
                            return@execute book
                        }
                    }
                }
                throw NoStackTraceException("未找到匹配书源")
            }.onError {
                AppLog.put("添加书籍 $bookUrl 出错", it)
                loadErrorLiveData.postValue(it.localizedMessage)
            }.onSuccess {
                book = it
                success.invoke(it)
            }.onStart {
                loadStateLiveData.postValue(true)
            }.onFinally {
                loadStateLiveData.postValue(false)
            }
        }

        private suspend fun getBookInfo(bookUrl: String, source: BookSource): Book? {
            return kotlin.runCatching {
                val book = Book(
                    bookUrl = bookUrl,
                    origin = source.bookSourceUrl,
                    originName = source.bookSourceName
                )
                WebBook.getBookInfoAwait(source, book)
            }.getOrNull()
        }

        fun saveBook(success: (book: Book?) -> Unit) {
            execute {
                book?.save()
                book
            }.onSuccess {
                success.invoke(it)
            }
        }

    }

}