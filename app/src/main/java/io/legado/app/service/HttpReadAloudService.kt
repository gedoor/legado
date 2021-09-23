package io.legado.app.service

import android.app.PendingIntent
import android.media.MediaPlayer
import io.legado.app.constant.AppLog
import io.legado.app.constant.EventBus
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.ReadAloud
import io.legado.app.model.ReadBook
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.*

class HttpReadAloudService : BaseReadAloudService(),
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {

    private val mediaPlayer = MediaPlayer()
    private val ttsFolder: String by lazy {
        externalCacheDir!!.absolutePath + File.separator + "httpTTS"
    }
    private var task: Coroutine<*>? = null
    private var playingIndex = -1
    private var playIndexJob: Job? = null

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

    private fun downloadAudio() {
        task?.cancel()
        task = execute {
            removeCacheFile()
            ReadAloud.httpTTS?.let { httpTts ->
                contentList.forEachIndexed { index, item ->
                    if (isActive) {
                        val fileName =
                            md5SpeakFileName(httpTts.url, AppConfig.ttsSpeechRate.toString(), item)
                        if (hasSpeakFile(fileName)) { //已经下载好的语音缓存
                            if (index == nowSpeak) {
                                val file = getSpeakFileAsMd5(fileName)
                                val fis = FileInputStream(file)
                                playAudio(fis.fd)
                            }
                        } else if (hasSpeakCacheFile(fileName)) { //缓存文件还在，可能还没下载完
                            return@let
                        } else { //没有下载并且没有缓存文件
                            try {
                                createSpeakCacheFile(fileName)
                                AnalyzeUrl(
                                    httpTts.url,
                                    speakText = item,
                                    speakSpeed = AppConfig.ttsSpeechRate,
                                    source = httpTts,
                                    headerMapF = httpTts.getHeaderMap(true)
                                ).getByteArray().let { bytes ->
                                    ensureActive()
                                    val file = getSpeakFileAsMd5IfNotExist(fileName)
                                    file.writeBytes(bytes)
                                    removeSpeakCacheFile(fileName)
                                    val fis = FileInputStream(file)
                                    if (index == nowSpeak) {
                                        playAudio(fis.fd)
                                    }
                                }
                            } catch (e: SocketTimeoutException) {
                                removeSpeakCacheFile(fileName)
                                toastOnUi("tts接口超时，尝试重新获取")
                                downloadAudio()
                            } catch (e: ConnectException) {
                                removeSpeakCacheFile(fileName)
                                toastOnUi("网络错误")
                            } catch (e: IOException) {
                                val file = getSpeakFileAsMd5(fileName)
                                if (file.exists()) {
                                    FileUtils.deleteFile(file.absolutePath)
                                }
                                toastOnUi("tts文件解析错误")
                            } catch (e: Exception) {
                                removeSpeakCacheFile(fileName)
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
                e.printOnDebug()
            }
        }
    }

    private fun speakFilePath() = ttsFolder + File.separator
    private fun md5SpeakFileName(url: String, ttsConfig: String, content: String): String {
        return MD5Utils.md5Encode16(textChapter!!.title) + "_" + MD5Utils.md5Encode16("$url-|-$ttsConfig-|-$content")
    }

    private fun hasSpeakFile(name: String) =
        FileUtils.exist("${speakFilePath()}$name.mp3")

    private fun hasSpeakCacheFile(name: String) =
        FileUtils.exist("${speakFilePath()}$name.mp3.cache")

    private fun createSpeakCacheFile(name: String): File =
        FileUtils.createFileWithReplace("${speakFilePath()}$name.mp3.cache")

    private fun removeSpeakCacheFile(name: String) {
        FileUtils.delete("${speakFilePath()}$name.mp3.cache")
    }

    private fun getSpeakFileAsMd5(name: String): File =
        FileUtils.getFile(File(speakFilePath()), "$name.mp3")

    private fun getSpeakFileAsMd5IfNotExist(name: String): File =
        FileUtils.createFileIfNotExist("${speakFilePath()}$name.mp3")

    private fun removeCacheFile() {
        FileUtils.listDirsAndFiles(speakFilePath())?.forEach {
            if (it == null) {
                return@forEach
            }
            if (Regex(""".+\.mp3$""").matches(it.name)) { //mp3缓存文件
                val reg =
                    """^${MD5Utils.md5Encode16(textChapter!!.title)}_[a-z0-9]{16}\.mp3$""".toRegex()
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

    private var errorNo = 0

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        if (what == -38 && extra == 0) {
            play()
            return true
        }
        AppLog.addLog("朗读错误,($what, $extra)")
        errorNo++
        if (errorNo >= 3) {
            toastOnUi("朗读连续3次错误, 最后一次错误代码($what, $extra)")
            ReadAloud.pause(this)
        } else {
            playNext()
        }
        return true
    }

    override fun onCompletion(mp: MediaPlayer?) {
        errorNo = 0
        playNext()
    }

    override fun aloudServicePendingIntent(actionStr: String): PendingIntent? {
        return servicePendingIntent<HttpReadAloudService>(actionStr)
    }
}