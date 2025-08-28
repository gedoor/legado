package io.legado.app.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.ReadBook
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.servicePendingIntent
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Timer
import kotlin.collections.set
import kotlin.concurrent.schedule
import kotlin.coroutines.coroutineContext

/**
 * Edge大声朗读
 */
@SuppressLint("UnsafeOptInUsageError")
class TTSEdgeAloudService : BaseReadAloudService(), Player.Listener {

    private val exoPlayer: ExoPlayer by lazy {
        val dataSourceFactory = ByteArrayDataSourceFactory(audioCache)
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
        ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }
    private val tag = "TTSEdgeAloudService"
    private var speechRate: Int = AppConfig.speechRatePlay + 5
    private var downloadTask: Coroutine<*>? = null
    private var playIndexJob: Job? = null
    private var playErrorNo = 0
    private var isReloadAudio = 0
    private val downloadTaskActiveLock = Mutex()
    private val edgeSpeakFetch = EdgeSpeakFetch()
    private val audioCache = HashMap<String, ByteArray>()
    private val audioCacheList = arrayListOf<String>()
    private var previousMediaId = ""
    private val silentBytes: ByteArray by lazy {
        resources.openRawResource(R.raw.silent_sound).readBytes()
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer.addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadTask?.cancel()
        exoPlayer.release()
        edgeSpeakFetch.release()
        removeAllCache()
    }

    override fun play() {
        pageChanged = false
        exoPlayer.stop()
        if (!requestFocus()) return
        if (contentList.isEmpty()) {
            AppLog.putDebug("朗读列表为空")
            ReadBook.readAloud()
        } else {
            super.play()
            downloadAndPlayAudios()
        }
    }

    override fun playStop() {
        exoPlayer.stop()
        playIndexJob?.cancel()
    }

    private fun updateNextPos() {
        readAloudNumber += contentList[nowSpeak].length + 1 - paragraphStartPos
        paragraphStartPos = 0
        if (nowSpeak < contentList.lastIndex) {
            nowSpeak++
        } else {
            nextChapter()
        }
    }

    private fun downloadAndPlayAudios() {
        removeUnUseCache()
        exoPlayer.clearMediaItems()
        Log.d(tag, "clearMediaItems audioCache Size= ${audioCache.size}")
        downloadTask?.cancel()
        downloadTask = execute {
            downloadTaskActiveLock.withLock {
                ensureActive()
                contentList.forEachIndexed { index, content ->
                    ensureActive()
                    if (index < nowSpeak) return@forEachIndexed
                    var text = content
                    if (paragraphStartPos > 0 && index == nowSpeak) {
                        text = text.substring(paragraphStartPos)
                    }
                    val fileName = md5SpeakFileName(text)
                    val speakText = text.replace(AppPattern.notReadAloudRegex, "")
                    if (!isCached(fileName)) {
                        Log.i(tag, "无缓存down===> $speakText  MD5:$fileName")
                        runCatching {
                            getSpeakStream(speakText, fileName)
                        }.onFailure {
                            Log.e(tag, "downloadAndPlayAudios runCatch onFailure")
                            when (it) {
                                is CancellationException -> Unit
                                else -> pauseReadAloud()
                            }
                            return@execute
                        }
                    }else{
                        Log.i(tag, "有缓存跳过===> $speakText  MD5:$fileName")
                    }
                    val mediaItem = MediaItem.Builder()
                        .setUri("memory://media/$fileName".toUri())
                        .setMediaId(fileName)
                        .build()
                    launch(Main) {
                        exoPlayer.addMediaItem(mediaItem)
                    }
                }
                preDownloadAudios()
            }
        }.onError {
            AppLog.put("朗读下载出错\n${it.localizedMessage}", it, true)
        }
    }

    private suspend fun preDownloadAudios() {
        Log.i(tag, "准备预下载音频===> ${ReadBook.nextTextChapter}")
        val textChapter = ReadBook.nextTextChapter ?: return
        val contentList =
            textChapter.getNeedReadAloud(0, readAloudByPage, 0, 1).splitToSequence("\n")
                .filter { it.isNotEmpty() }.take(10).toList()
        contentList.forEach { content ->
            coroutineContext.ensureActive()
            val fileName = md5SpeakFileName(content)
            val speakText = content.replace(AppPattern.notReadAloudRegex, "")
            if (!isCached(fileName)) {
                runCatching {
                    getSpeakStream(speakText, fileName)
                    Log.i(tag, "pre预下载音频===> $speakText MD5:$fileName")
                }.onFailure {
                    Log.e(tag, "preDownloadAudios runCatch onFailure")
                }
            }
        }
    }

    private suspend fun getSpeakStream(speakText: String, fileName: String): String {
        if (speakText.isEmpty()) {
            cacheAudio(fileName, silentBytes)
            return "fail"
        }
        try {
            return withContext(Dispatchers.IO) {
                val inputStream = edgeSpeakFetch.synthesizeText(speakText, speechRate)
                cacheAudio(fileName, inputStream)
                "success"
            }
        } catch (e: Exception) {
            Log.i(tag, "edgeSpeakFetch失败: $e")
            cacheAudio(fileName, silentBytes)
        }
        return "fail"
    }

    private fun md5SpeakFileName(content: String): String {
        return MD5Utils.md5Encode16(MD5Utils.md5Encode16("$speechRate|$content"))
    }

    override fun pauseReadAloud(abandonFocus: Boolean) {
        super.pauseReadAloud(abandonFocus)
        Log.i(tag, "pauseReadAloud")
        kotlin.runCatching {
            playIndexJob?.cancel()
            exoPlayer.pause()
        }

    }

    override fun resumeReadAloud() {
        super.resumeReadAloud()
        kotlin.runCatching {
            if (pageChanged) {
                play()
            } else {
                exoPlayer.play()
                upPlayPos()
            }
        }
    }

    private fun upPlayPos() {
        playIndexJob?.cancel()
        val textChapter = textChapter ?: return
        playIndexJob = lifecycleScope.launch {
            upTtsProgress(readAloudNumber + 1)
            if (exoPlayer.duration <= 0) {
                return@launch
            }
            val speakTextLength = if (nowSpeak in contentList.indices) {
                contentList[nowSpeak].length
            } else {
                Log.e(tag, "nowSpeak 越界: nowSpeak=$nowSpeak, contentList.size=${contentList.size}")
                contentList.size-1
            }
            if (speakTextLength <= 0) {
                return@launch
            }
            val sleep = exoPlayer.duration / speakTextLength
            val start = speakTextLength * exoPlayer.currentPosition / exoPlayer.duration
            for (i in start..contentList[nowSpeak].length) {
                if (pageIndex + 1 < textChapter.pageSize && readAloudNumber + i > textChapter.getReadLength(
                        pageIndex + 1
                    )
                ) {
                    pageIndex++
                    ReadBook.moveToNextPage()
                    upTtsProgress(readAloudNumber + i.toInt())
                }
                delay(sleep)
            }
        }
    }

    /**
     * 更新朗读速度
     */
    override fun upSpeechRate(reset: Boolean) {
        downloadTask?.cancel()
        exoPlayer.stop()
        speechRate = AppConfig.speechRatePlay + 5
        downloadAndPlayAudios()

    }

    /**
     * 重新下载本章节
     */
    private fun reloadAudio() {
        removeAllCache()
        previousMediaId = ""
        downloadTask?.cancel()
        exoPlayer.stop()
        downloadAndPlayAudios()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when (playbackState) {
            Player.STATE_IDLE -> {
                // 空闲
            }

            Player.STATE_BUFFERING -> {
                // 缓冲中
            }

            Player.STATE_READY -> {
                // 准备好
                if (pause) return
                exoPlayer.play()
                upPlayPos()
            }

            Player.STATE_ENDED -> {
                // 结束
                playErrorNo = 0
                isReloadAudio = 0
                updateNextPos()
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
            }
        }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        when (reason) {
            Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED -> {
                if (!timeline.isEmpty && exoPlayer.playbackState == Player.STATE_IDLE) {
                    exoPlayer.prepare()
                }
            }

            else -> {}
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        Log.d(tag, "onMediaItemTransition")
        if (mediaItem?.mediaId.toString().isNotEmpty()) {
            previousMediaId = mediaItem?.mediaId.toString()
        }
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) return
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            playErrorNo = 0
            isReloadAudio = 0
        }
        updateNextPos()
        upPlayPos()
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        // 打印详细错误信息（日志中搜索 "Source error detail"）
        Log.e(tag, "Source error detail: ${error.cause?.message}", error)
        // 错误类型判断（如格式不支持、IO 错误等）
        Log.e(tag, "playErrorNo errorCode ${error.errorCode}")
        Log.e(tag, "playErrorNo: $playErrorNo")
        AppLog.put("朗读错误\n${contentList[nowSpeak]}", error)
        playErrorNo++
        if (playErrorNo >= 5) {
            toastOnUi("朗读连续5次错误, 最后一次错误代码(${error.localizedMessage})")
            AppLog.put("朗读连续5次错误, 最后一次错误代码(${error.localizedMessage})", error)
            // 把这一章节的音频重新加载
            if (isReloadAudio == 0) {
                playErrorNo = 0
                isReloadAudio++
                Timer().schedule(2000) {
                    reloadAudio()
                    Log.e(tag, "重试本章节")
                }
            } else {
                pauseReadAloud()
            }
        } else {
            if (exoPlayer.hasNextMediaItem()) {
                exoPlayer.seekToNextMediaItem()
                exoPlayer.prepare()
            } else {
                exoPlayer.clearMediaItems()
                updateNextPos()
            }
        }
    }

    override fun aloudServicePendingIntent(actionStr: String): PendingIntent? {
        return servicePendingIntent<TTSEdgeAloudService>(actionStr)
    }

    private fun cacheAudio(key: String, inputStream: InputStream): Boolean {
        return try {
            audioCache[key] = inputStream.toByteArray()
            audioCacheList.add(key)
            Log.d(tag, "成功缓存 cacheAudio: $key")
            true
        } catch (e: Exception) {
            Log.d(tag, "缓存失败 cacheAudio: $key")
            e.printStackTrace()
            false
        }
    }

    private fun cacheAudio(key: String, byteArray: ByteArray): Boolean {
        audioCache[key] = byteArray
        audioCacheList.add(key)
        return true
    }

    private fun removeCache(key: String) {
        audioCache.remove(key)
    }

    // 移除不会再使用的缓存, 0 ~ 上次朗读的文件下标
    private fun removeUnUseCache() {
        Log.d(tag, "removeUnUseCache previousMediaId: $previousMediaId")
        if (previousMediaId.isEmpty()) return
        val targetIndex = audioCacheList.indexOf(previousMediaId)
        if (targetIndex <= 0) return // 索引为0或-1时无需处理（-1：未找到；0：无前置元素）

        Log.d(tag, "removeUnUseCache targetIndex: $targetIndex")

        val itemsToRemove = audioCacheList.subList(0, targetIndex)
        itemsToRemove.forEach {
            Log.d(tag, "批量移除: $it")
            removeCache(it)
        }
        itemsToRemove.clear()
        Log.d(tag, "removeUnUseCache: ${audioCacheList.size}")

    }

    private fun removeAllCache() {
        audioCache.clear()
        audioCacheList.clear()
    }

    private fun isCached(key: String) = audioCache.containsKey(key)

    private fun InputStream.toByteArray(): ByteArray {
        val output = ByteArrayOutputStream()
        // 使用use自动关闭流
        this.use { input ->
            output.use { out ->
                input.copyTo(out) // 复制数据到输出流
            }
        }
        return output.toByteArray()
    }

}


@UnstableApi
class ByteArrayDataSource(private val mediaMap: HashMap<String, ByteArray>) : DataSource {

    private var readPosition = 0
    private var opened = false
    private var currByteArray = ByteArray(0)


    override fun addTransferListener(transferListener: TransferListener) {
        // 不需要实现
    }

    override fun open(dataSpec: DataSpec): Long {
        val mediaId = getMediaIdFromUri(dataSpec.uri)
        if (!mediaMap.containsKey(mediaId)) return 0


        currByteArray = mediaMap[mediaId] as ByteArray
        readPosition = dataSpec.position.toInt()
        val length =
            if (dataSpec.length == C.LENGTH_UNSET.toLong()) currByteArray.size - readPosition else dataSpec.length.toInt()
        if (readPosition + length > currByteArray.size) {
            throw IOException("Unsatisfiable range: $readPosition + $length > ${currByteArray.size}")
        }
        opened = true
        return length.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (!opened) {
            throw IOException("DataSource not opened")
        }
        val remaining = currByteArray.size - readPosition
        if (remaining == 0) {
            return C.RESULT_END_OF_INPUT
        }
        val bytesToRead = minOf(readLength, remaining)
        System.arraycopy(currByteArray, readPosition, buffer, offset, bytesToRead)
        readPosition += bytesToRead
        return bytesToRead
    }

    override fun getUri(): Uri? = null

    override fun close() {
        opened = false
    }

    /**
     * 从自定义URI中提取媒体ID
     * URI格式应为: memory://media/$mediaId
     */
    private fun getMediaIdFromUri(uri: Uri?): String {
        Assertions.checkNotNull(uri)
        return uri!!.lastPathSegment ?: throw IllegalArgumentException("Invalid media URI: $uri")
    }
}


@UnstableApi
class ByteArrayDataSourceFactory(private val mediaMap: HashMap<String, ByteArray>) :
    DataSource.Factory {
    override fun createDataSource(): DataSource {
        return ByteArrayDataSource(mediaMap)
    }
}