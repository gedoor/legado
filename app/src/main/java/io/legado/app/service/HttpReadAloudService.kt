package io.legado.app.service

import android.app.PendingIntent
import android.net.Uri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern
import io.legado.app.constant.EventBus
import io.legado.app.exception.ConcurrentException
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.ReadAloud
import io.legado.app.model.ReadBook
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.*
import kotlinx.coroutines.*
import okhttp3.Response
import org.mozilla.javascript.WrappedException
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.*
import javax.script.ScriptException

/**
 * 在线朗读
 */
class HttpReadAloudService : BaseReadAloudService(),
    Player.Listener {

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build()
    }
    private val ttsFolderPath: String by lazy {
        cacheDir.absolutePath + File.separator + "httpTTS" + File.separator
    }
    private var speechRate: Int = AppConfig.speechRatePlay
    private val cacheFiles = hashSetOf<String>()
    private var downloadTask: Coroutine<*>? = null
    private var playIndexJob: Job? = null
    private var downloadErrorNo: Int = 0
    private var playErrorNo = 0

    override fun onCreate() {
        super.onCreate()
        exoPlayer.addListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadTask?.cancel()
        exoPlayer.release()
    }

    override fun play() {
        exoPlayer.stop()
        if (contentList.isEmpty()) {
            AppLog.putDebug("朗读列表为空")
            ReadBook.readAloud()
        } else {
            super.play()
            kotlin.runCatching {
                val tts = ReadAloud.httpTTS ?: throw NoStackTraceException("httpTts is null")
                val fileName =
                    md5SpeakFileName(
                        tts.url,
                        AppConfig.ttsSpeechRate.toString(),
                        contentList[nowSpeak]
                    )
                if (nowSpeak == 0 && downloadTask?.isActive != true) {
                    downloadAudio()
                } else {
                    val file = getSpeakFileAsMd5(fileName)
                    if (file.exists()) {
                        playAudio(file)
                    } else {
                        downloadAudio()
                    }
                }
            }.onFailure {
                toastOnUi("朗读出错:${it.localizedMessage}")
            }
        }
    }

    override fun playStop() {
        exoPlayer.stop()
    }

    private fun playNext() {
        readAloudNumber += contentList[nowSpeak].length + 1
        if (nowSpeak < contentList.lastIndex) {
            nowSpeak++
            play()
        } else {
            nextChapter()
        }
    }

    private fun downloadAudio() {
        downloadTask?.cancel()
        downloadTask = execute {
            clearSpeakCache()
            removeCacheFile()
            val httpTts = ReadAloud.httpTTS ?: return@execute
            contentList.forEachIndexed { index, content ->
                ensureActive()
                val fileName =
                    md5SpeakFileName(httpTts.url, speechRate.toString(), content)
                val speakText = content.replace(AppPattern.notReadAloudRegex, "")
                if (hasSpeakFile(fileName)) { //已经下载好的语音缓存
                    if (index == nowSpeak) {
                        val file = getSpeakFileAsMd5(fileName)
                        playAudio(file)
                    }
                } else if (hasSpeakCache(fileName)) { //缓存文件还在，可能还没下载完
                    return@forEachIndexed
                } else if (speakText.isEmpty()) {
                    createSilentSound(fileName)
                    return@forEachIndexed
                } else {
                    runCatching {
                        createSpeakCache(fileName)
                        val analyzeUrl = AnalyzeUrl(
                            httpTts.url,
                            speakText = speakText,
                            speakSpeed = speechRate,
                            source = httpTts,
                            headerMapF = httpTts.getHeaderMap(true)
                        )
                        var response = analyzeUrl.getResponseAwait()
                        ensureActive()
                        httpTts.loginCheckJs?.takeIf { checkJs ->
                            checkJs.isNotBlank()
                        }?.let { checkJs ->
                            response = analyzeUrl.evalJS(checkJs, response) as Response
                        }
                        httpTts.contentType?.takeIf { ct ->
                            ct.isNotBlank()
                        }?.let { ct ->
                            response.headers["Content-Type"]?.let { contentType ->
                                if (!contentType.matches(ct.toRegex())) {
                                    throw NoStackTraceException(response.body!!.string())
                                }
                            }
                        }
                        ensureActive()
                        response.body!!.bytes().let { bytes ->
                            val file = createSpeakFileAsMd5IfNotExist(fileName)
                            file.writeBytes(bytes)
                            removeSpeakCache(fileName)
                            if (index == nowSpeak) {
                                playAudio(file)
                            }
                        }
                        downloadErrorNo = 0
                    }.onFailure {
                        when (it) {
                            is CancellationException -> removeSpeakCache(fileName)
                            is ConcurrentException -> {
                                removeSpeakCache(fileName)
                                delay(it.waitTime.toLong())
                                downloadAudio()
                            }
                            is ScriptException, is WrappedException -> {
                                AppLog.put("js错误\n${it.localizedMessage}", it)
                                toastOnUi("js错误\n${it.localizedMessage}")
                                it.printOnDebug()
                                cancel()
                                pauseReadAloud(true)
                            }
                            is SocketTimeoutException, is ConnectException -> {
                                removeSpeakCache(fileName)
                                downloadErrorNo++
                                if (downloadErrorNo > 5) {
                                    val msg = "tts超时或连接错误超过5次\n${it.localizedMessage}"
                                    AppLog.put(msg, it)
                                    toastOnUi(msg)
                                    pauseReadAloud(true)
                                } else {
                                    downloadAudio()
                                }
                            }
                            else -> {
                                removeSpeakCache(fileName)
                                downloadErrorNo++
                                val msg = "tts下载错误\n${it.localizedMessage}"
                                AppLog.put(msg, it)
                                it.printOnDebug()
                                if (downloadErrorNo > 5) {
                                    pauseReadAloud(true)
                                } else {
                                    createSilentSound(fileName)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    private fun playAudio(file: File) {
        if (requestFocus()) {
            launch {
                kotlin.runCatching {
                    val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.playWhenReady = true
                    exoPlayer.prepare()
                }.onFailure {
                    it.printOnDebug()
                }
            }
        }
    }

    private fun md5SpeakFileName(url: String, ttsConfig: String, content: String): String {
        return MD5Utils.md5Encode16(textChapter?.title ?: "") + "_" +
                MD5Utils.md5Encode16("$url-|-$ttsConfig-|-$content")
    }

    private fun createSilentSound(fileName: String) {
        val file = createSpeakFileAsMd5IfNotExist(fileName)
        file.writeBytes(resources.openRawResource(R.raw.silent_sound).readBytes())
    }

    @Synchronized
    private fun clearSpeakCache() = cacheFiles.clear()

    @Synchronized
    private fun hasSpeakCache(name: String) = cacheFiles.contains(name)

    @Synchronized
    private fun createSpeakCache(name: String) = cacheFiles.add(name)

    @Synchronized
    private fun removeSpeakCache(name: String) = cacheFiles.remove(name)

    private fun hasSpeakFile(name: String) =
        FileUtils.exist("${ttsFolderPath}$name.mp3")

    private fun getSpeakFileAsMd5(name: String): File =
        File("${ttsFolderPath}$name.mp3")

    private fun createSpeakFileAsMd5IfNotExist(name: String): File =
        FileUtils.createFileIfNotExist("${ttsFolderPath}$name.mp3")

    /**
     * 移除缓存文件
     */
    private fun removeCacheFile() {
        val titleMd5 = MD5Utils.md5Encode16(textChapter?.title ?: "")
        FileUtils.listDirsAndFiles(ttsFolderPath)?.forEach {
            if (!it.name.startsWith(titleMd5) && Date().time - it.lastModified() > 600000) {
                FileUtils.delete(it.absolutePath)
            }
        }
    }


    override fun pauseReadAloud(pause: Boolean) {
        super.pauseReadAloud(pause)
        kotlin.runCatching {
            playIndexJob?.cancel()
            exoPlayer.pause()
        }
    }

    override fun resumeReadAloud() {
        super.resumeReadAloud()
        kotlin.runCatching {
            exoPlayer.play()
            upPlayPos()
        }
    }

    private fun upPlayPos() {
        playIndexJob?.cancel()
        val textChapter = textChapter ?: return
        playIndexJob = launch {
            postEvent(EventBus.TTS_PROGRESS, readAloudNumber + 1)
            if (exoPlayer.duration <= 0) {
                return@launch
            }
            val speakTextLength = contentList[nowSpeak].length
            if (speakTextLength <= 0) {
                return@launch
            }
            val sleep = exoPlayer.duration / speakTextLength
            val start = speakTextLength * exoPlayer.currentPosition / exoPlayer.duration
            for (i in start..contentList[nowSpeak].length) {
                if (readAloudNumber + i > textChapter.getReadLength(pageIndex + 1)) {
                    pageIndex++
                    ReadBook.moveToNextPage()
                    postEvent(EventBus.TTS_PROGRESS, readAloudNumber + i)
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
        speechRate = AppConfig.speechRatePlay
        downloadAudio()
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
                playNext()
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        AppLog.put("朗读错误\n${contentList[nowSpeak]}", error)
        playErrorNo++
        if (playErrorNo >= 5) {
            toastOnUi("朗读连续5次错误, 最后一次错误代码(${error.localizedMessage})")
            ReadAloud.pause(this)
        } else {
            playNext()
        }
    }

    override fun aloudServicePendingIntent(actionStr: String): PendingIntent? {
        return servicePendingIntent<HttpReadAloudService>(actionStr)
    }
}