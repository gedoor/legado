package io.legado.app.ui.book.read.review

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.legado.app.BuildConfig
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookReview
import io.legado.app.data.entities.BookSource
import io.legado.app.model.ReadBook
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.stackTraceStr
import kotlinx.coroutines.Dispatchers

class ReadReviewViewModel(application: Application) : BaseViewModel(application) {
    val reviewsData = MutableLiveData<List<BookReview>>()
    val errorLiveData = MutableLiveData<String>()
    private var bookSource: BookSource? = null
    private var bookReview: BookReview? = null
    private var book: Book? = null
    private var segmentIndex: String? = null
    private var reviewCount: String? = null
    private var page = 1

    fun initData(arguments: Bundle?) {
        execute {
            book = ReadBook.book
            bookSource = ReadBook.bookSource
            reviewCount = arguments?.getString("reviewCount")
            segmentIndex = arguments?.getString("segmentIndex")
            if (bookReview == null && segmentIndex != null) {
                bookReview = appDb.bookReviewDao.getReview(book!!.reviewCountUrl, segmentIndex!!)
            }
            loadReviews(false)
        }
    }

    fun loadReviews(isReviewChild: Boolean) {
        val source = bookSource
        val book = book
        val review = bookReview
        if (source == null || book == null || review == null || segmentIndex == null) {
            errorLiveData.postValue("未知错误，获取段评失败")
            return
        }
        WebBook.getReviewList(
            viewModelScope,
            book, source, review,
            page, segmentIndex!!.toInt(),
            isReviewChild
        ).timeout(if (BuildConfig.DEBUG) 0L else 30000L)
            .onSuccess(Dispatchers.IO) { reviews ->
                reviewsData.postValue(reviews)
                page++
            }.onError {
                it.printOnDebug()
                errorLiveData.postValue(it.stackTraceStr)
            }
    }
}