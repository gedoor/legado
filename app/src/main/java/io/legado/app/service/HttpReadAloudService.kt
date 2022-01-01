package io.legado.app.service

import android.app.PendingIntent
import android.media.MediaPlayer
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern
import io.legado.app.constant.EventBus
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.ConcurrentException
import io.legado.app.model.NoStackTraceException
import io.legado.app.model.ReadAloud
import io.legado.app.model.ReadBook
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Response
import org.mozilla.javascript.WrappedException
import timber.log.Timber
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.*
import javax.script.ScriptException

class HttpReadAloudService : BaseReadAloudService(),
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {

    private val mediaPlayer = MediaPlayer()
    private val ttsFolderPath: String by lazy {
        externalCacheDir!!.absolutePath + File.separator + "httpTTS" + File.separator
    }
    private val cacheFiles = hashSetOf<String>()
    private var task: Coroutine<*>? = null
    private var playingIndex = -1
    private var playIndexJob: Job? = null
    private val mutex = Mutex()

    override fun onCreate() {
        super.onCreate()
        mediaPlayer.setOnErrorListener(this)
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnCompletionListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        task?.cancel()
        mediaPlayer.release()
    }

    override fun newReadAloud(play: Boolean) {
        mediaPlayer.reset()
        playingIndex = -1
        super.newReadAloud(play)
    }

    override fun play() {
        if (contentList.isEmpty()) return
        ReadAloud.httpTTS?.let {
            val fileName =
                md5SpeakFileName(it.url, AppConfig.ttsSpeechRate.toString(), contentList[nowSpeak])
            if (nowSpeak == 0) {
                downloadAudio()
            } else {
                val file = getSpeakFileAsMd5(fileName)
                if (file.exists()) {
                    playAudio(FileInputStream(file).fd)
                } else {
                    downloadAudio()
                }
            }
        }
    }

    override fun playStop() {
        mediaPlayer.stop()
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

    private var downloadErrorNo: Int = 0

    private fun downloadAudio() {
        task?.cancel()
        task = execute {
            clearSpeakCache()
            removeCacheFile()
            val httpTts = ReadAloud.httpTTS ?: return@execute
            contentList.forEachIndexed { index, item ->
                ensureActive()
                val speakText = item.replace(AppPattern.notReadAloudRegex, "")
                val fileName =
                    md5SpeakFileName(
                        httpTts.url,
                        AppConfig.ttsSpeechRate.toString(),
                        speakText
                    )
                if (hasSpeakFile(fileName)) { //已经下载好的语音缓存
                    if (index == nowSpeak) {
                        val file = getSpeakFileAsMd5(fileName)
                        val fis = FileInputStream(file)
                        playAudio(fis.fd)
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
                            speakSpeed = AppConfig.ttsSpeechRate,
                            source = httpTts,
                            headerMapF = httpTts.getHeaderMap(true)
                        )
                        var response = mutex.withLock {
                            analyzeUrl.getResponseAwait()
                        }
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
                            ensureActive()
                            val file = createSpeakFileAsMd5IfNotExist(fileName)
                            file.writeBytes(bytes)
                            removeSpeakCache(fileName)
                            val fis = FileInputStream(file)
                            if (index == nowSpeak) {
                                playAudio(fis.fd)
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
                                Timber.e(it)
                                cancel()
                                pauseReadAloud(true)
                            }
                            is SocketTimeoutException, is ConnectException -> {
                                removeSpeakCache(fileName)
                                downloadErrorNo++
                                if (playErrorNo > 5) {
                                    downloadErrorNo = 0
                                    createSilentSound(fileName)
                                    val msg = "tts超时或连接错误超过5次\n${it.localizedMessage}"
                                    AppLog.put(msg, it)
                                    toastOnUi(msg)
                                } else {
                                    downloadAudio()
                                }
                            }
                            else -> {
                                removeSpeakCache(fileName)
                                createSilentSound(fileName)
                                val msg = "tts下载错误\n${it.localizedMessage}"
                                AppLog.put(msg, it)
                                Timber.e(it)
                            }
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    private fun playAudio(fd: FileDescriptor) {
        if (playingIndex != nowSpeak && requestFocus()) {
            try {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(fd)
                mediaPlayer.prepareAsync()
                playingIndex = nowSpeak
                postEvent(EventBus.TTS_PROGRESS, readAloudNumber + 1)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun md5SpeakFileName(url: String, ttsConfig: String, content: String): String {
        return MD5Utils.md5Encode16(textChapter!!.title) + "_" + MD5Utils.md5Encode16("$url-|-$ttsConfig-|-$content")
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

    private fun removeCacheFile() {
        val cacheRegex = Regex(""".+\.mp3$""")
        val reg = """^${MD5Utils.md5Encode16(textChapter!!.title)}_[a-z0-9]{16}\.mp3$""".toRegex()
        FileUtils.listDirsAndFiles(ttsFolderPath)?.forEach {
            if (cacheRegex.matches(it.name)) { //mp3缓存文件
                if (!reg.matches(it.name)) {
                    FileUtils.deleteFile(it.absolutePath)
                }
            } else {
                if (Date().time - it.lastModified() > 30000) {
                    FileUtils.deleteFile(it.absolutePath)
                }
            }
        }
    }


    override fun pauseReadAloud(pause: Boolean) {
        super.pauseReadAloud(pause)
        kotlin.runCatching {
            playIndexJob?.cancel()
            mediaPlayer.pause()
        }
    }

    override fun resumeReadAloud() {
        super.resumeReadAloud()
        kotlin.runCatching {
            if (playingIndex == -1) {
                play()
            } else {
                mediaPlayer.start()
                upPlayPos()
            }
        }
    }

    private fun upPlayPos() {
        playIndexJob?.cancel()
        val textChapter = textChapter ?: return
        playIndexJob = launch {
            postEvent(EventBus.TTS_PROGRESS, readAloudNumber + 1)
            if (mediaPlayer.duration <= 0) {
                return@launch
            }
            val speakTextLength = contentList[nowSpeak].length
            if (speakTextLength <= 0) {
                return@launch
            }
            val sleep = mediaPlayer.duration / speakTextLength
            val start = speakTextLength * mediaPlayer.currentPosition / mediaPlayer.duration
            for (i in start..contentList[nowSpeak].length) {
                if (readAloudNumber + i > textChapter.getReadLength(pageIndex + 1)) {
                    pageIndex++
                    ReadBook.moveToNextPage()
                    postEvent(EventBus.TTS_PROGRESS, readAloudNumber + i)
                }
                delay(sleep.toLong())
            }
        }
    }

    /**
     * 更新朗读速度
     */
    override fun upSpeechRate(reset: Boolean) {
        task?.cancel()
        mediaPlayer.stop()
        playingIndex = -1
        downloadAudio()
    }

    override fun onPrepared(mp: MediaPlayer?) {
        super.play()
        if (pause) return
        mediaPlayer.start()
        upPlayPos()
    }

    private var playErrorNo = 0

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        if (what == -38 && extra == 0) {
            play()
            return true
        }
        AppLog.put("朗读错误($what, $extra)\n${contentList[nowSpeak]}")
        playErrorNo++
        if (playErrorNo >= 5) {
            toastOnUi("朗读连续5次错误, 最后一次错误代码($what, $extra)")
            ReadAloud.pause(this)
        } else {
            playNext()
        }
        return true
    }

    override fun onCompletion(mp: MediaPlayer?) {
        playErrorNo = 0
        playNext()
    }

    override fun aloudServicePendingIntent(actionStr: String): PendingIntent? {
        return servicePendingIntent<HttpReadAloudService>(actionStr)
    }
}