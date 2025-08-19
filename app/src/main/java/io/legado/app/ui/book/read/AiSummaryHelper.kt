package io.legado.app.ui.book.read

import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.snackbar.Snackbar
import io.legado.app.R
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ActivityBookReadBinding
import io.legado.app.help.book.BookHelp
import io.legado.app.help.config.AppConfig
import io.legado.app.model.AiSummaryState
import io.legado.app.model.ReadBook
import android.util.Log
import io.legado.app.ui.book.read.content.ZhanweifuBookHelp
import io.legado.app.utils.LogUtils
import io.legado.app.utils.getPrefString
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import io.legado.app.constant.PreferKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.Semaphore

class AiSummaryHelper(
    private val activity: ReadBookActivity,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val binding: ActivityBookReadBinding
) {

    private var preCacheSemaphore: Semaphore? = null
    private val preCacheMutex = Mutex()
    private var inProgressSnackbar: Snackbar? = null
    private var lastPreCacheChapterIndex: Int = -1

    private val blinkAnim by lazy {
        AnimationUtils.loadAnimation(activity, R.anim.blink)
    }

    init {
        val concurrencyLimit = activity.getPrefString(PreferKey.aiSummaryChapterCount, "3")?.toIntOrNull() ?: 3
        if (concurrencyLimit > 0) {
            preCacheSemaphore = Semaphore(concurrencyLimit)
        }
    }

    fun onActivityCreated() {
        binding.readMenu.setAiCoarseState(AppConfig.aiSummaryModeEnabled)
    }

    fun onPageChanged() {
        upAiWordCount()
        inProgressSnackbar?.dismiss()
        if (AppConfig.aiSummaryModeEnabled) {
            if (AiSummaryState.inProgress.contains(ReadBook.durChapterIndex)) {
                inProgressSnackbar = Snackbar.make(binding.root, "AI摘要正在生成中...", Snackbar.LENGTH_INDEFINITE)
                inProgressSnackbar?.show()
            }
            if (ReadBook.durChapterIndex != lastPreCacheChapterIndex) {
                preCacheNextChapterSummary()
                lastPreCacheChapterIndex = ReadBook.durChapterIndex
            }
        }
    }

    fun contentLoadFinish() {
        upAiWordCount()
    }

    fun onAiCoarseClick() {
        Log.d("AiSummary", "[GEMINI] onAiCoarseClick called")
        binding.readMenu.runMenuOut()
        AppConfig.aiSummaryModeEnabled = !AppConfig.aiSummaryModeEnabled
        binding.readMenu.setAiCoarseState(AppConfig.aiSummaryModeEnabled)

        if (AppConfig.aiSummaryModeEnabled) {
            val chapterIndex = ReadBook.curTextChapter!!.chapter.index
            val chapter = ReadBook.curTextChapter!!.chapter
            LogUtils.d("AiSummary", "Attempting to manually generate summary for chapter $chapterIndex ('${chapter.title}').")
            if (!AiSummaryState.inProgress.add(chapterIndex)) {
                LogUtils.d("AiSummary", "Manual generation for chapter $chapterIndex ('${chapter.title}') skipped, task already in progress.")
                activity.toastOnUi("本章AI摘要正在生成中，请稍候")
                AppConfig.aiSummaryModeEnabled = false
                binding.readMenu.setAiCoarseState(false)
                return
            }
            LogUtils.d("AiSummary", "New manual generation task for chapter $chapterIndex ('${chapter.title}') started.")

            val book = ReadBook.book!!
            lastPreCacheChapterIndex = chapterIndex

            val cachedSummary = ZhanweifuBookHelp.getAiSummaryFromCache(book, chapter)
            if (cachedSummary != null) {
                ReadBook.loadContent(false)
                preCacheNextChapterSummary()
                AiSummaryState.inProgress.remove(chapterIndex) // Release lock
                return
            }

            val originalContent = ReadBook.curTextChapter?.getContent()
            if (originalContent.isNullOrEmpty()) {
                activity.toastOnUi("本章无内容，无法生成摘要")
                AppConfig.aiSummaryModeEnabled = false
                binding.readMenu.setAiCoarseState(false)
                AiSummaryState.inProgress.remove(chapterIndex) // Release lock
                return
            }

            lifecycleScope.launch {
                inProgressSnackbar = Snackbar.make(
                    binding.root,
                    "正在为本章 (${chapter.title}) 生成摘要...",
                    Snackbar.LENGTH_INDEFINITE
                )
                inProgressSnackbar?.show()
                val summaryBuilder = StringBuilder()
                ZhanweifuBookHelp.getAiSummary(
                    content = originalContent,
                    onResponse = { summaryBuilder.append(it) },
                    onFinish = {
                        inProgressSnackbar?.dismiss()
                        AiSummaryState.inProgress.remove(chapterIndex)
                        val finalSummary = summaryBuilder.toString()
                        if (finalSummary.isNotEmpty()) {
                            activity.toastOnUi("生成成功")
                            ZhanweifuBookHelp.saveAiSummaryToCache(book, chapter, finalSummary)
                            ReadBook.loadContent(false)
                            preCacheNextChapterSummary()
                        }
                    },
                    onError = {
                        inProgressSnackbar?.dismiss()
                        AiSummaryState.inProgress.remove(chapterIndex)
                        activity.toastOnUi(it)
                        AppConfig.aiSummaryModeEnabled = false
                        binding.readMenu.setAiCoarseState(false)
                    }
                )
            }
        } else {
            ReadBook.loadContent(false)
        }
    }

    fun dismissInProgressSnackbar() {
        inProgressSnackbar?.dismiss()
    }

    private fun upAiWordCount() {
        lifecycleScope.launch(Dispatchers.Main) {
            val indicator = binding.tvNextCachedIndicator
            indicator.clearAnimation()

            if (!AppConfig.aiSummaryModeEnabled) {
                binding.tvAiWordCount.visible(false)
                binding.aiIcon.visible(false)
                indicator.visible(false)
                return@launch
            }

            ReadBook.book?.let { book ->
                // Update current chapter's summary UI
                ReadBook.curTextChapter?.chapter?.let { chapter ->
                    val summary = withContext(Dispatchers.IO) {
                        ZhanweifuBookHelp.getAiSummaryFromCache(book, chapter)
                    }
                    val summaryLength = summary?.length ?: 0
                    val originalLength = withContext(Dispatchers.IO) {
                        BookHelp.getOriginalContent(book, chapter)?.length ?: 0
                    }
                    if (summaryLength > 0) {
                        binding.tvAiWordCount.text = "(${originalLength}->${summaryLength})"
                        binding.tvAiWordCount.visible(true)
                        binding.aiIcon.visible(true)
                    } else {
                        binding.tvAiWordCount.visible(false)
                        binding.aiIcon.visible(false)
                    }
                } ?: run {
                    binding.tvAiWordCount.visible(false)
                    binding.aiIcon.visible(false)
                }

                // Update next chapter cached indicator
                val nextChapterIndex = ReadBook.durChapterIndex + 1
                if (nextChapterIndex < ReadBook.chapterSize) {
                    val isNextInProgress = AiSummaryState.inProgress.contains(nextChapterIndex)
                    val (isNextCached, nextChapterWordCount) = withContext(Dispatchers.IO) {
                        val nextChapter = appDb.bookChapterDao.getChapter(book.bookUrl, nextChapterIndex)
                        if (nextChapter != null) {
                            val isCached = ZhanweifuBookHelp.getAiSummaryFromCache(book, nextChapter) != null
                            val wordCount = if (!isCached) {
                                BookHelp.getOriginalContent(book, nextChapter)?.length ?: 0
                            } else {
                                0
                            }
                            Pair(isCached, wordCount)
                        } else {
                            Pair(false, 0)
                        }
                    }
                    val isAnyOtherInProgress = AiSummaryState.inProgress.any { it != nextChapterIndex }

                    indicator.clearAnimation()
                    indicator.background = null
                    indicator.text = "●"

                    when {
                        isNextInProgress -> {
                            indicator.setTextColor(ContextCompat.getColor(activity, R.color.indicator_blue))
                            indicator.startAnimation(blinkAnim)
                            indicator.visible(true)
                        }
                        !isNextCached && nextChapterWordCount in 1..1000 -> {
                            indicator.text = ""
                            indicator.setBackgroundResource(R.drawable.indicator_half_red_green)
                            indicator.visible(true)
                        }
                        !isNextCached -> {
                            indicator.setTextColor(ContextCompat.getColor(activity, R.color.indicator_red))
                            indicator.visible(true)
                        }
                        isNextCached -> {
                            indicator.setTextColor(ContextCompat.getColor(activity, R.color.indicator_green))
                            if (isAnyOtherInProgress) {
                                indicator.startAnimation(blinkAnim)
                            }
                            indicator.visible(true)
                        }
                    }
                } else {
                    indicator.visible(false)
                }

            } ?: run {
                binding.tvAiWordCount.visible(false)
                binding.aiIcon.visible(false)
                indicator.visible(false)
            }
        }
    }

    private fun preCacheNextChapterSummary() {
        val lookaheadCount = activity.getPrefString(PreferKey.aiSummaryChapterCount, "3")?.toIntOrNull() ?: 3
        if (lookaheadCount == 0) return

        val book = ReadBook.book ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            for (i in 1..lookaheadCount) {
                val targetIndex = ReadBook.durChapterIndex + i
                if (targetIndex >= ReadBook.chapterSize) break

                val chapter = appDb.bookChapterDao.getChapter(book.bookUrl, targetIndex) ?: continue
                
                launch {
                    runPreCacheTask(book, chapter, i * 10000L)
                }
            }
        }
    }

    private suspend fun runPreCacheTask(book: Book, chapter: BookChapter, delay: Long) {
        delay(delay)

        // First, check if cached without locking
        if (ZhanweifuBookHelp.getAiSummaryFromCache(book, chapter) != null) {
            return
        }

        LogUtils.d("AiSummary", "Attempting to pre-cache summary for chapter ${chapter.index} ('${chapter.title}').")
        // Atomically check and add the chapter index to the in-progress set.
        // If add() returns false, it means the chapter is already being processed.
        if (!AiSummaryState.inProgress.add(chapter.index)) {
            LogUtils.d("AiSummary", "Pre-caching for chapter ${chapter.index} ('${chapter.title}') already in progress. Skipping.")
            return
        }
        LogUtils.d("AiSummary", "New pre-cache task for chapter ${chapter.index} ('${chapter.title}') started.")

        try {
            preCacheSemaphore?.acquire()
            try {
                Log.d("AiSummary", "runPreCacheTask: Starting generation for '${chapter.title}'.")

                // Double check cache after acquiring semaphore, another task might have finished.
                if (ZhanweifuBookHelp.getAiSummaryFromCache(book, chapter) != null) {
                    return
                }

                val chapterContent = BookHelp.getOriginalContent(book, chapter)
                if (chapterContent.isNullOrEmpty()) {
                    Log.d("AiSummary", "runPreCacheTask: Original content for '${chapter.title}' is null or empty. Cannot generate summary.")
                    return
                }

                if (chapterContent.length <= 1000) {
                    Log.d("AiSummary", "runPreCacheTask: Chapter '${chapter.title}' is too short (${chapterContent.length} words). Skipping summary generation.")
                    return
                }

                withContext(Dispatchers.Main) {
                    activity.toastOnUi("开始缓存: ${chapter.title}")
                }
                val startTime = System.currentTimeMillis()
                val summaryBuilder = StringBuilder()
                ZhanweifuBookHelp.getAiSummary(
                    content = chapterContent,
                    onResponse = { summaryBuilder.append(it) },
                    onFinish = {
                        val finalSummary = summaryBuilder.toString()
                        if (finalSummary.isNotEmpty()) {
                            Log.d("AiSummary", "Successfully generated summary for '${chapter.title}'")
                            ZhanweifuBookHelp.saveAiSummaryToCache(book, chapter, finalSummary)
                            val duration = (System.currentTimeMillis() - startTime) / 1000
                            val message = "(${chapter.title}) 已缓存, 耗时 ${duration} 秒"
                            activity.toastOnUi(message)
                        } else {
                            Log.d("AiSummary", "Finished generating summary for '${chapter.title}', but summary was empty.")
                        }
                    },
                    onError = {
                        Log.e("runPreCacheTask for ${chapter.title}", it)
                    }
                )
            } finally {
                preCacheSemaphore?.release()
            }
        } finally {
            AiSummaryState.inProgress.remove(chapter.index)
        }
    }
}