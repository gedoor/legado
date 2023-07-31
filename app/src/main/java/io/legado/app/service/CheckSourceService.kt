package io.legado.app.service

import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.script.ScriptException
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.AppConst
import io.legado.app.constant.BookSourceType
import io.legado.app.constant.EventBus
import io.legado.app.constant.IntentAction
import io.legado.app.constant.NotificationId
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.exception.ContentEmptyException
import io.legado.app.exception.NoStackTraceException
import io.legado.app.exception.TocEmptyException
import io.legado.app.help.IntentData
import io.legado.app.help.config.AppConfig
import io.legado.app.help.source.exploreKinds
import io.legado.app.model.CheckSource
import io.legado.app.model.Debug
import io.legado.app.model.webBook.WebBook
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.utils.activityPendingIntent
import io.legado.app.utils.postEvent
import io.legado.app.utils.servicePendingIntent
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.mozilla.javascript.WrappedException
import splitties.init.appCtx
import java.util.concurrent.Executors
import kotlin.math.min

/**
 * 校验书源
 */
class CheckSourceService : BaseService() {
    private var threadCount = AppConfig.threadCount
    private var searchCoroutine =
        Executors.newFixedThreadPool(min(threadCount, AppConst.MAX_THREAD)).asCoroutineDispatcher()
    private val allIds = ArrayList<String>()
    private val checkedIds = ArrayList<String>()
    private var processIndex = 0
    private var notificationMsg = appCtx.getString(R.string.service_starting)

    private val notificationBuilder by lazy {
        NotificationCompat.Builder(this, AppConst.channelIdReadAloud)
            .setSmallIcon(R.drawable.ic_network_check)
            .setOngoing(true)
            .setContentTitle(getString(R.string.check_book_source))
            .setContentIntent(
                activityPendingIntent<BookSourceActivity>("activity")
            )
            .addAction(
                R.drawable.ic_stop_black_24dp,
                getString(R.string.cancel),
                servicePendingIntent<CheckSourceService>(IntentAction.stop)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            IntentAction.start -> IntentData.get<List<String>>("checkSourceSelectedIds")?.let {
                check(it)
            }

            IntentAction.resume -> upNotification()
            IntentAction.stop -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Debug.finishChecking()
        searchCoroutine.close()
        postEvent(EventBus.CHECK_SOURCE_DONE, 0)
    }

    private fun check(ids: List<String>) {
        if (allIds.isNotEmpty()) {
            toastOnUi("已有书源在校验,等完成后再试")
            return
        }
        allIds.clear()
        checkedIds.clear()
        allIds.addAll(ids)
        processIndex = 0
        threadCount = min(allIds.size, threadCount)
        notificationMsg = getString(R.string.progress_show, "", 0, allIds.size)
        upNotification()
        for (i in 0 until threadCount) {
            check()
        }
    }

    /**
     * 检测
     */
    private fun check() {
        val index = processIndex
        synchronized(this) {
            processIndex++
        }
        lifecycleScope.launch(IO) {
            if (index < allIds.size) {
                val sourceUrl = allIds[index]
                appDb.bookSourceDao.getBookSource(sourceUrl)?.let { source ->
                    check(source)
                } ?: onNext(sourceUrl, "")
            }
        }
    }

    /**
     *校验书源
     */
    private fun check(source: BookSource) {
        execute(
            context = searchCoroutine,
            start = CoroutineStart.LAZY,
            executeContext = IO
        ) {
            Debug.startChecking(source)
            var searchWord = CheckSource.keyword
            source.ruleSearch?.checkKeyWord?.let {
                if (it.isNotBlank()) {
                    searchWord = it
                }
            }
            source.removeInvalidGroups()
            source.bookSourceComment = source.bookSourceComment
                ?.split("\n\n")
                ?.filterNot {
                    it.startsWith("// Error: ")
                }?.joinToString("\n")
            //校验搜索书籍
            if (CheckSource.checkSearch) {
                if (!source.searchUrl.isNullOrBlank()) {
                    source.removeGroup("搜索链接规则为空")
                    val searchBooks = WebBook.searchBookAwait(source, searchWord)
                    if (searchBooks.isEmpty()) {
                        source.addGroup("搜索失效")
                    } else {
                        source.removeGroup("搜索失效")
                        checkBook(searchBooks.first().toBook(), source)
                    }
                } else {
                    source.addGroup("搜索链接规则为空")
                }
            }
            //校验发现书籍
            if (CheckSource.checkDiscovery && !source.exploreUrl.isNullOrBlank()) {
                val exs = source.exploreKinds()
                var url: String? = null
                for (ex in exs) {
                    url = ex.url
                    if (!url.isNullOrBlank()) {
                        break
                    }
                }
                if (url.isNullOrBlank()) {
                    source.addGroup("发现规则为空")
                } else {
                    source.removeGroup("发现规则为空")
                    val exploreBooks = WebBook.exploreBookAwait(source, url)
                    if (exploreBooks.isEmpty()) {
                        source.addGroup("发现失效")
                    } else {
                        source.removeGroup("发现失效")
                        checkBook(exploreBooks.first().toBook(), source, false)
                    }
                }
            }
            val finalCheckMessage = source.getInvalidGroupNames()
            if (finalCheckMessage.isNotBlank()) throw NoStackTraceException(finalCheckMessage)
        }.timeout(CheckSource.timeout)
            .onError(searchCoroutine) {
                when (it) {
                    is TimeoutCancellationException -> source.addGroup("校验超时")
                    is ScriptException, is WrappedException -> source.addGroup("js失效")
                    !is NoStackTraceException -> source.addGroup("网站失效")
                }
                source.bookSourceComment =
                    "// Error: ${it.localizedMessage}" + if (source.bookSourceComment.isNullOrBlank())
                        "" else "\n\n${source.bookSourceComment}"
                Debug.updateFinalMessage(source.bookSourceUrl, "校验失败:${it.localizedMessage}")
            }.onSuccess(searchCoroutine) {
                Debug.updateFinalMessage(source.bookSourceUrl, "校验成功")
            }.onFinally(IO) {
                source.respondTime = Debug.getRespondTime(source.bookSourceUrl)
                appDb.bookSourceDao.update(source)
                onNext(source.bookSourceUrl, source.bookSourceName)
            }.start()
    }

    /**
     *校验书源的详情目录正文
     */
    private suspend fun checkBook(book: Book, source: BookSource, isSearchBook: Boolean = true) {
        kotlin.runCatching {
            var mBook = book
            //校验详情
            if (CheckSource.checkInfo) {
                if (mBook.tocUrl.isBlank()) {
                    mBook = WebBook.getBookInfoAwait(source, mBook)
                }
                //校验目录
                if (CheckSource.checkCategory &&
                    source.bookSourceType != BookSourceType.file
                ) {
                    val toc = WebBook.getChapterListAwait(source, mBook).getOrThrow()
                        .filter { !(it.isVolume && it.url.startsWith(it.title)) }
                    val nextChapterUrl = toc.getOrNull(1)?.url ?: toc.first().url
                    //校验正文
                    if (CheckSource.checkContent) {
                        WebBook.getContentAwait(
                            bookSource = source,
                            book = mBook,
                            bookChapter = toc.first(),
                            nextChapterUrl = nextChapterUrl,
                            needSave = false
                        )
                    }
                }
            }
        }.onFailure {
            val bookType = if (isSearchBook) "搜索" else "发现"
            when (it) {
                is ContentEmptyException -> source.addGroup("${bookType}正文失效")
                is TocEmptyException -> source.addGroup("${bookType}目录失效")
                else -> throw it
            }
        }.onSuccess {
            val bookType = if (isSearchBook) "搜索" else "发现"
            source.removeGroup("${bookType}目录失效")
            source.removeGroup("${bookType}正文失效")
        }
    }

    private fun onNext(sourceUrl: String, sourceName: String) {
        synchronized(this) {
            check()
            checkedIds.add(sourceUrl)
            notificationMsg =
                getString(R.string.progress_show, sourceName, checkedIds.size, allIds.size)
            upNotification()
            if (processIndex > allIds.size + threadCount - 1) {
                stopSelf()
            }
        }
    }

    /**
     * 更新通知
     */
    override fun upNotification() {
        notificationBuilder.setContentText(notificationMsg)
        notificationBuilder.setProgress(allIds.size, checkedIds.size, false)
        postEvent(EventBus.CHECK_SOURCE, notificationMsg)
        startForeground(NotificationId.CheckSourceService, notificationBuilder.build())
    }

}