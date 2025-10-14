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
import io.legado.app.ui.book.read.content.AiSummaryProvider
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
        Log.d("AiSummary", "[GEMINI] onAiCoarseClick 已调用")
        binding.readMenu.runMenuOut()
        AppConfig.aiSummaryModeEnabled = !AppConfig.aiSummaryModeEnabled
        binding.readMenu.setAiCoarseState(AppConfig.aiSummaryModeEnabled)

        if (AppConfig.aiSummaryModeEnabled) {
            val chapterIndex = ReadBook.curTextChapter!!.chapter.index
            val chapter = ReadBook.curTextChapter!!.chapter
            Log.d("AiSummary", "尝试为章节 $chapterIndex ('${chapter.title}') 手动生成摘要。")
            if (!AiSummaryState.inProgress.add(chapterIndex)) {
                Log.d("AiSummary", "章节 $chapterIndex ('${chapter.title}') 的手动生成任务已在进行中，已跳过。")
                activity.toastOnUi("本章AI摘要正在生成中，请稍候")
                AppConfig.aiSummaryModeEnabled = false
                binding.readMenu.setAiCoarseState(false)
                return
            }
            Log.d("AiSummary", "章节 $chapterIndex ('${chapter.title}') 的新手动生成任务已开始。")

            val book = ReadBook.book!!
            lastPreCacheChapterIndex = chapterIndex

            val cachedSummary = AiSummaryProvider.getAiSummaryFromCache(book, chapter)
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
                AiSummaryProvider.getAiSummary(
                    content = originalContent,
                    onResponse = { summaryBuilder.append(it) },
                    onFinish = {
                        inProgressSnackbar?.dismiss()
                        AiSummaryState.inProgress.remove(chapterIndex)
                        val finalSummary = summaryBuilder.toString()
                        if (finalSummary.isNotEmpty()) {
                            activity.toastOnUi("生成成功")
                            AiSummaryProvider.saveAiSummaryToCache(book, chapter, finalSummary)
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

        internal fun upAiWordCount() {
        lifecycleScope.launch(Dispatchers.Main) {
            val indicator = binding.tvNextCachedIndicator
            indicator.clearAnimation()

            if (activity.isAiSummaryReplaceMode) {
                // Force show icon for dialog-replace mode
                binding.aiIcon.visible(true)
                binding.tvAiWordCount.visible(false)
                indicator.visible(false)
                return@launch
            }

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
                        AiSummaryProvider.getAiSummaryFromCache(book, chapter)
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
                            val isCached = AiSummaryProvider.getAiSummaryFromCache(book, nextChapter) != null
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
        // 延迟执行，避免同时触发多个任务
        delay(delay)

        Log.d("AiSummary", "任务开始: 章节 ${chapter.index} ('${chapter.title}'). 准备检查缓存.")
        // 首先，在没有锁定的情况下检查是否已缓存，以快速跳过
        if (AiSummaryProvider.getAiSummaryFromCache(book, chapter) != null) {
            Log.d("AiSummary", "任务退出: 章节 ${chapter.index} ('${chapter.title}') 已有缓存.")
            return // 如果已缓存，则直接返回
        }

        Log.d("AiSummary", "准备锁定: 章节 ${chapter.index} ('${chapter.title}').")
        // 原子性地检查并添加章节索引到进行中的集合。
        if (!AiSummaryState.inProgress.add(chapter.index)) {
            Log.d("AiSummary", "任务退出: 章节 ${chapter.index} ('${chapter.title}') 已在处理中，跳过.")
            return // 如果已在处理中，则跳过
        }
        
        Log.d("AiSummary", "锁定成功: 章节 ${chapter.index} ('${chapter.title}') 开始处理.")

        try {
            // 获取信号量许可，控制并发任务数量
            preCacheSemaphore?.acquire()
            Log.d("AiSummary", "获取信号量: 章节 ${chapter.index} ('${chapter.title}').")
            try {
                // 获取信号量后再次检查缓存，因为其他任务可能已经完成
                if (AiSummaryProvider.getAiSummaryFromCache(book, chapter) != null) {
                    Log.d("AiSummary", "任务退出: 章节 ${chapter.index} ('${chapter.title}') 在获取信号量后发现已有缓存.")
                    return // 如果已缓存，则直接返回
                }

                // 获取章节的原始内容
                val chapterContent = BookHelp.getOriginalContent(book, chapter)
                // 如果内容为空或null，则无法生成摘要
                if (chapterContent.isNullOrEmpty()) {
                    Log.d("AiSummary", "任务退出: 章节 ${chapter.index} ('${chapter.title}') 内容为空.")
                    return // 返回
                }

                // 如果章节内容太短，则跳过摘要生成
                if (chapterContent.length <= 1000) {
                    Log.d("AiSummary", "任务退出: 章节 ${chapter.index} ('${chapter.title}') 内容太短.")
                    return // 返回
                }

                // 切换到主线程以显示UI提示
                withContext(Dispatchers.Main) {
                    activity.toastOnUi("开始缓存: ${chapter.title}")
                }
                // 记录任务开始时间
                val startTime = System.currentTimeMillis()
                // 用于构建摘要的 StringBuilder
                val summaryBuilder = StringBuilder()
                
                Log.d("AiSummary", "调用getAiSummary: 章节 ${chapter.index} ('${chapter.title}').")
                // 调用AI摘要生成函数
                AiSummaryProvider.getAiSummary(
                    content = chapterContent, // 传入章节内容
                    onResponse = { summaryBuilder.append(it) }, // 实时追加收到的摘要片段
                    onFinish = { // 完成时的回调
                        Log.d("AiSummary", "onFinish回调: 章节 ${chapter.index} ('${chapter.title}') 摘要生成成功.")
                        val finalSummary = summaryBuilder.toString() // 获取完整的摘要
                        if (finalSummary.isNotEmpty()) { // 如果摘要不为空
                            // 将摘要保存到缓存
                            AiSummaryProvider.saveAiSummaryToCache(book, chapter, finalSummary)
                            // 计算耗时
                            val duration = (System.currentTimeMillis() - startTime) / 1000
                            // 创建提示消息
                            val message = "(${chapter.title}) 已缓存, 耗时 ${duration} 秒"
                            // 在UI上显示消息
                            activity.toastOnUi(message)
                        } else {
                            Log.d("AiSummary", "onFinish回调: 章节 ${chapter.index} ('${chapter.title}') 生成的摘要为空.")
                        }
                    },
                    onError = { e -> // 发生错误时的回调
                        Log.e("AiSummary", "onError回调: 章节 ${chapter.index} ('${chapter.title}') 发生错误: $e")
                    }
                )
                Log.d("AiSummary", "getAiSummary调用结束: 章节 ${chapter.index} ('${chapter.title}').")

            } finally {
                // 释放信号量许可
                Log.d("AiSummary", "释放信号量: 章节 ${chapter.index} ('${chapter.title}').")
                preCacheSemaphore?.release()
            }
        } finally {
            // 从进行中集合中移除该章节索引，表示处理结束
            Log.d("AiSummary", "任务解锁: 章节 ${chapter.index} ('${chapter.title}') 从inProgress中移除.")
            AiSummaryState.inProgress.remove(chapter.index)
        }
    }
}