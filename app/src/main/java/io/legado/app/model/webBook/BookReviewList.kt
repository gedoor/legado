package io.legado.app.model.webBook

import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookReview
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.ReviewRule
import io.legado.app.exception.NoStackTraceException
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import kotlinx.coroutines.ensureActive
import splitties.init.appCtx
import kotlin.coroutines.coroutineContext

object BookReviewList {
    suspend fun analyzeReviewCountList(
        bookSource: BookSource,
        book: Book,
        bookChapter: BookChapter,
        baseUrl: String,
        redirectUrl: String,
        body: String?
    ): List<BookReview> {
        body ?: throw NoStackTraceException(
            appCtx.getString(R.string.error_get_web_content, baseUrl)
        )
        val reviewCountList = ArrayList<BookReview>()
        Debug.log(bookSource.bookSourceUrl, "≡获取成功:${baseUrl}")
        Debug.log(bookSource.bookSourceUrl, body, state = 50)
        val reviewRule = bookSource.getReviewRule()
        val analyzeRule = AnalyzeRule(book, bookSource)
        analyzeRule.setContent(body, baseUrl)
        analyzeRule.setRedirectUrl(redirectUrl)
        analyzeRule.chapter = bookChapter
        coroutineContext.ensureActive()
        val listRule = reviewRule.reviewCountList ?: ""
        val reviewCountData =
            analyzeReviewCountList(
                bookSource, book, bookChapter, redirectUrl, baseUrl, body,
                reviewRule, listRule, log = true
            )
        reviewCountList.addAll(reviewCountData)
        coroutineContext.ensureActive()
        return reviewCountList
    }

    suspend fun analyzeReviewList(
        bookSource: BookSource,
        book: Book,
        bookReview: BookReview,
        segmentIndex: Int,
        isReviewChild: Boolean,
        analyzeUrl: AnalyzeUrl,
        baseUrl: String,
        redirectUrl: String,
        body: String?
    ): List<BookReview> {
        body ?: throw NoStackTraceException(
            appCtx.getString(R.string.error_get_web_content, baseUrl)
        )
        val reviewList = ArrayList<BookReview>()
        Debug.log(bookSource.bookSourceUrl, "≡获取成功:${analyzeUrl.ruleUrl}")
        Debug.log(bookSource.bookSourceUrl, body, state = 60)
        val reviewRule = bookSource.getReviewRule()
        val analyzeRule = AnalyzeRule(book, bookSource)
        analyzeRule.setContent(body, baseUrl)
        analyzeRule.setRedirectUrl(redirectUrl)
        analyzeRule.review = bookReview
        analyzeRule.setContent(body, baseUrl)
        analyzeRule.setRedirectUrl(redirectUrl)
        coroutineContext.ensureActive()
        var reverse = false
        var listRule = reviewRule.reviewList ?: ""
        if (listRule.startsWith("-")) {
            reverse = true
            listRule = listRule.substring(1)
        }
        if (listRule.startsWith("+")) {
            listRule = listRule.substring(1)
        }
        val reviewListData =
            analyzeReviewList(
                bookSource, book, bookReview, segmentIndex, isReviewChild,
                redirectUrl, baseUrl, body, reviewRule, listRule, log = true
            )
        reviewList.addAll(reviewListData)
        //反转
        if (reverse) {
            reviewList.reverse()
        }
        coroutineContext.ensureActive()
        return reviewList
    }

    private suspend fun analyzeReviewCountList(
        bookSource: BookSource,
        book: Book,
        bookChapter: BookChapter,
        redirectUrl: String,
        baseUrl: String,
        body: String,
        reviewRule: ReviewRule,
        listRule: String,
        log: Boolean = false
    ): List<BookReview> {
        val analyzeRule = AnalyzeRule(book, bookSource)
        analyzeRule.setContent(body, baseUrl)
        analyzeRule.setRedirectUrl(redirectUrl)
        analyzeRule.chapter = bookChapter
        // 获取段评数量列表
        val reviewCountList = arrayListOf<BookReview>()
        Debug.log(bookSource.bookSourceUrl, "┌获取段评数量列表", log)
        val elements = analyzeRule.getElements(listRule)
        Debug.log(bookSource.bookSourceUrl, "└列表大小:${elements.size}", log)
        coroutineContext.ensureActive()
        if (elements.isNotEmpty()) {
            Debug.log(bookSource.bookSourceUrl, "┌解析段评数量列表", log)
            val reviewSegmentIdRule = analyzeRule.splitSourceRule(reviewRule.reviewSegmentId)
            val reviewCountRule = analyzeRule.splitSourceRule(reviewRule.reviewCount)
            elements.forEachIndexed { index, item ->
                coroutineContext.ensureActive()
                analyzeRule.setContent(item)
                val bookReview = BookReview(
                    reviewCountUrl = baseUrl,
                    bookUrl = book.bookUrl,
                    baseUrl = baseUrl
                )
                analyzeRule.review = bookReview
                bookReview.reviewSegmentId = analyzeRule.getString(reviewSegmentIdRule)
                bookReview.reviewCount = analyzeRule.getString(reviewCountRule)
                bookReview.chapterUrl = bookChapter.getAbsoluteURL()
                if (bookReview.reviewSegmentId.isNotBlank()) {
                    reviewCountList.add(bookReview)
                }
            }
            if (reviewCountList.isEmpty()) {
                Debug.log(bookSource.bookSourceUrl, "◇段评数量列表为空", log)
            }  else {
                Debug.log(bookSource.bookSourceUrl, "≡段评信息", log)
                Debug.log(bookSource.bookSourceUrl, "◇段评索引:${reviewCountList[0].reviewSegmentId}", log)
                Debug.log(bookSource.bookSourceUrl, "◇段评数量:${reviewCountList[0].reviewCount}", log)
            }
            Debug.log(bookSource.bookSourceUrl, "└段评数量列表解析完成", log)
        }
        return reviewCountList
    }

    private suspend fun analyzeReviewList(
        bookSource: BookSource,
        book: Book,
        bookReview: BookReview,
        segmentIndex: Int,
        isReviewChild: Boolean,
        redirectUrl: String,
        baseUrl: String,
        body: String,
        reviewRule: ReviewRule,
        listRule: String,
        log: Boolean = false
    ): List<BookReview> {
        val analyzeRule = AnalyzeRule(book, bookSource)
        analyzeRule.setContent(body, baseUrl)
        val rUrl = analyzeRule.setRedirectUrl(redirectUrl)
        // 获取段评列表
        val reviewList = arrayListOf<BookReview>()
        Debug.log(bookSource.bookSourceUrl, "┌获取段评列表", log)
        val elements = analyzeRule.getElements(listRule)
        Debug.log(bookSource.bookSourceUrl, "└列表大小:${elements.size}", log)
        coroutineContext.ensureActive()
        if (elements.isNotEmpty()) {
            Debug.log(bookSource.bookSourceUrl, "┌解析段评列表", log)
            val reviewContentRule = analyzeRule.splitSourceRule(reviewRule.reviewContent)
            val reviewImgUrlRule = analyzeRule.splitSourceRule(reviewRule.reviewImgUrl)
            val reviewPostAvatarRule = analyzeRule.splitSourceRule(reviewRule.reviewPostAvatar)
            val reviewPostNameRule = analyzeRule.splitSourceRule(reviewRule.reviewPostName)
            val quoteReviewDefaultRule = analyzeRule.splitSourceRule(reviewRule.quoteReviewDefault)
            val reviewPostTimeRule = analyzeRule.splitSourceRule(reviewRule.reviewPostTime)
            val reviewLikeCountRule = analyzeRule.splitSourceRule(reviewRule.reviewLikeCount)
            val quoteReviewUrlRule = analyzeRule.splitSourceRule(reviewRule.quoteReviewUrl)
            val quoteReviewCountRule = analyzeRule.splitSourceRule(reviewRule.quoteReviewCount)
            elements.forEach { item ->
                coroutineContext.ensureActive()
                analyzeRule.setContent(item)
                val review = BookReview()
                analyzeRule.review = review
                review.baseUrl = baseUrl
                review.bookUrl = bookReview.bookUrl
                review.reviewCountUrl = bookReview.reviewCountUrl
                review.reviewSegmentId = segmentIndex.toString()
                //获取正文
                review.reviewContent = analyzeRule.getString(reviewContentRule)
                review.reviewImgUrl = analyzeRule.getString(reviewImgUrlRule)
                review.reviewPostAvatar = analyzeRule.getString(reviewPostAvatarRule)
                review.reviewPostName = analyzeRule.getString(reviewPostNameRule)
                review.reviewPostTime = analyzeRule.getString(reviewPostTimeRule)
                review.reviewLikeCount = analyzeRule.getString(reviewLikeCountRule)
                review.quoteReviewDefault = analyzeRule.getString(quoteReviewDefaultRule)
                review.quoteReviewUrl = analyzeRule.getString(quoteReviewUrlRule)
                review.quoteReviewCount = analyzeRule.getString(quoteReviewCountRule)
                review.isReviewChild = isReviewChild
                //处理默认展开段评
                if (!review.quoteReviewDefault.isNullOrBlank()) {
                    review.isReviewChild = true
                    review.quoteReviewCount = null
                }
                if (bookReview.reviewSegmentId.isNotBlank()) {
                    reviewList.add(review)
                }
            }
            coroutineContext.ensureActive()
            if (reviewList.isEmpty()) {
                Debug.log(bookSource.bookSourceUrl, "◇段评列表为空", log)
            }  else {
                Debug.log(bookSource.bookSourceUrl, "≡段评列表信息", log)
                Debug.log(bookSource.bookSourceUrl, "◇段评内容:${reviewList[0].reviewContent}", log)
                Debug.log(bookSource.bookSourceUrl, "◇段评发布者名称:${reviewList[0].reviewPostName}", log)
                Debug.log(bookSource.bookSourceUrl, "◇段评发布者头像:${reviewList[0].reviewPostAvatar}", log)
                Debug.log(bookSource.bookSourceUrl, "◇段评发布时间:${reviewList[0].reviewPostTime}", log)
                Debug.log(bookSource.bookSourceUrl, "◇段评内容:${reviewList[0].reviewContent}", log)
                Debug.log(bookSource.bookSourceUrl, "◇段评配图:${reviewList[0].reviewImgUrl}", log)
                Debug.log(bookSource.bookSourceUrl, "◇段评点赞数量:${reviewList[0].reviewLikeCount}", log)
                Debug.log(bookSource.bookSourceUrl, "◇段评默认展开:${reviewList[0].quoteReviewDefault}", log)
                Debug.log(bookSource.bookSourceUrl, "◇段评展开数量:${reviewList[0].quoteReviewCount}", log)
                Debug.log(bookSource.bookSourceUrl, "◇段评展开URL:${reviewList[0].quoteReviewUrl}", log)
            }
            Debug.log(bookSource.bookSourceUrl, "└段评列表解析完成", log)
        }
        return reviewList
    }
}
