package io.legado.app.service

import android.app.PendingIntent
import android.net.Uri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.script.ScriptException
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
    private var speechRate: Int = AppConfig.speechRatePlay + 5
    private var downloadTask: Coroutine<*>? = null
    private var playIndexJob: Job? = null
    private var downloadTaskIsActive = false
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
                if (nowSpeak == 0) {
                    downloadAudio()
                } else {
                    val fileName = md5SpeakFileName(contentList[nowSpeak])
                    val file = getSpeakFileAsMd5(fileName)
                    if (file.exists()) {
                        playAudio(file)
                    } else if (!downloadTaskIsActive) {
                        downloadAudio()
                    }
                }
            }.onFailure {
                toastOnUi("朗读出错:${it.localizedMessage}")
                AppLog.put("朗读出错:${it.localizedMessage}", it)
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
        launch {
            downloadTask?.cancel()
            while (downloadTaskIsActive) {
                //在线tts大部分只能单线程,等待上次访问结束
                delay(100)
            }
            downloadTask = execute {
                removeCacheFile()
                val httpTts = ReadAloud.httpTTS ?: throw NoStackTraceException("tts is null")
                contentList.forEachIndexed { index, content ->
                    ensureActive()
                    val fileName = md5SpeakFileName(content)
                    val speakText = content.replace(AppPattern.notReadAloudRegex, "")
                    if (hasSpeakFile(fileName)) { //已经下载好的语音缓存
                        if (index == nowSpeak) {
                            val file = getSpeakFileAsMd5(fileName)
                            playAudio(file)
                        }
                    } else if (speakText.isEmpty()) {
                        AppLog.put(
                            "阅读段落内容为空，使用无声音频代替。\n朗读文本：$content"
                        )
                        createSilentSound(fileName)
                        if (index == nowSpeak) {
                            val file = getSpeakFileAsMd5(fileName)
                            playAudio(file)
                        }
                        return@forEachIndexed
                    } else {
                        runCatching {
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
                                        throw NoStackTraceException("TTS服务器返回错误：" + response.body!!.string())
                                    }
                                }
                            }
                            ensureActive()
                            response.body!!.bytes().let { bytes ->
                                val file = createSpeakFileAsMd5IfNotExist(fileName)
                                file.writeBytes(bytes)
                                if (index == nowSpeak) {
                                    playAudio(file)
                                }
                            }
                            downloadErrorNo = 0
                        }.onFailure {
                            when (it) {
                                is CancellationException -> Unit
                                is ConcurrentException -> {
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
                                    downloadErrorNo++
                                    val msg = "tts下载错误\n${it.localizedMessage}"
                                    AppLog.put(msg, it)
                                    it.printOnDebug()
                                    if (downloadErrorNo > 5) {
                                        AppLog.put("TTS服务器连续5次错误，已暂停阅读。")
                                        toastOnUi("TTS服务器连续5次错误，已暂停阅读。")
                                        pauseReadAloud(true)
                                    } else {
                                        AppLog.put("TTS下载音频出错，使用无声音频代替。\n朗读文本：$content")
                                        createSilentSound(fileName)
                                        if (index == nowSpeak) {
                                            val file = getSpeakFileAsMd5(fileName)
                                            playAudio(file)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }.onStart {
                downloadTaskIsActive = true
            }.onError {
                AppLog.put("朗读下载出错\n${it.localizedMessage}", it)
            }.onFinally {
                downloadTaskIsActive = false
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

    private fun md5SpeakFileName(content: String): String {
        return MD5Utils.md5Encode16(textChapter?.title ?: "") + "_" +
                MD5Utils.md5Encode16("${ReadAloud.httpTTS?.url}-|-$speechRate-|-$content")
    }

    private fun createSilentSound(fileName: String) {
        val file = createSpeakFileAsMd5IfNotExist(fileName)
        file.writeBytes(resources.openRawResource(R.raw.silent_sound).readBytes())
    }

    private fun hasSpeakFile(name: String): Boolean {
        return FileUtils.exist("${ttsFolderPath}$name.mp3")
    }

    private fun getSpeakFileAsMd5(name: String): File {
        return File("${ttsFolderPath}$name.mp3")
    }

    private fun createSpeakFileAsMd5IfNotExist(name: String): File {
        return FileUtils.createFileIfNotExist("${ttsFolderPath}$name.mp3")
    }

    /**
     * 移除缓存文件
     */
    private fun removeCacheFile() {
        val titleMd5 = MD5Utils.md5Encode16(textChapter?.title ?: "")
        FileUtils.listDirsAndFiles(ttsFolderPath)?.forEach {
            val isSilentSound = it.length() == 2160L
            if ((!it.name.startsWith(titleMd5)
                        && System.currentTimeMillis() - it.lastModified() > 600000)
                || isSilentSound
            ) {
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
        speechRate = AppConfig.speechRatePlay + 5
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
            AppLog.put("朗读连续5次错误, 最后一次错误代码(${error.localizedMessage})", error)
            ReadAloud.pause(this)
        } else {
            playNext()
        }
    }

    override fun aloudServicePendingIntent(actionStr: String): PendingIntent? {
        return servicePendingIntent<HttpReadAloudService>(actionStr)
    }
}