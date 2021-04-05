package io.legado.app.ui.rss.read

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.webkit.URLUtil
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppConst
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.data.entities.RssStar
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.rss.Rss
import io.legado.app.utils.DocumentUtils
import io.legado.app.utils.FileUtils
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.writeBytes
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toByteArray
import java.io.File
import java.util.*


class ReadRssViewModel(application: Application) : BaseViewModel(application),
    TextToSpeech.OnInitListener {
    var callBack: CallBack? = null
    var rssSource: RssSource? = null
    var rssArticle: RssArticle? = null
    val contentLiveData = MutableLiveData<String>()
    val urlLiveData = MutableLiveData<AnalyzeUrl>()
    var rssStar: RssStar? = null
    var textToSpeech: TextToSpeech? = null
    private var ttsInitFinish = false
    private var ttsTextList = arrayListOf<String>()

    fun initData(intent: Intent) {
        execute {
            val origin = intent.getStringExtra("origin")
            val link = intent.getStringExtra("link")
            origin?.let {
                rssSource = appDb.rssSourceDao.getByKey(origin)
                if (link != null) {
                    rssStar = appDb.rssStarDao.get(origin, link)
                    rssArticle = rssStar?.toRssArticle() ?: appDb.rssArticleDao.get(origin, link)
                    rssArticle?.let { rssArticle ->
                        if (!rssArticle.description.isNullOrBlank()) {
                            contentLiveData.postValue(rssArticle.description!!)
                        } else {
                            rssSource?.let {
                                val ruleContent = it.ruleContent
                                if (!ruleContent.isNullOrBlank()) {
                                    loadContent(rssArticle, ruleContent)
                                } else {
                                    loadUrl(rssArticle.link, rssArticle.origin)
                                }
                            } ?: loadUrl(rssArticle.link, rssArticle.origin)
                        }
                    }
                } else {
                    val ruleContent = rssSource?.ruleContent
                    if (ruleContent.isNullOrBlank()) {
                        loadUrl(origin, origin)
                    } else {
                        val rssArticle = RssArticle()
                        rssArticle.origin = origin
                        rssArticle.link = origin
                        rssArticle.title = rssSource!!.sourceName
                        loadContent(rssArticle, ruleContent)
                    }
                }
            }
        }.onFinally {
            callBack?.upStarMenu()
        }
    }

    private fun loadUrl(url: String, baseUrl: String) {
        val analyzeUrl = AnalyzeUrl(
            ruleUrl = url,
            baseUrl = baseUrl,
            useWebView = true,
            headerMapF = rssSource?.getHeaderMap()
        )
        urlLiveData.postValue(analyzeUrl)
    }

    private fun loadContent(rssArticle: RssArticle, ruleContent: String) {
        rssSource?.let { source ->
            Rss.getContent(this, rssArticle, ruleContent, source)
                .onSuccess(IO) { body ->
                    rssArticle.description = body
                    appDb.rssArticleDao.insert(rssArticle)
                    rssStar?.let {
                        it.description = body
                        appDb.rssStarDao.insert(it)
                    }
                    contentLiveData.postValue(body)
                }
        }
    }

    fun favorite() {
        execute {
            rssStar?.let {
                appDb.rssStarDao.delete(it.origin, it.link)
                rssStar = null
            } ?: rssArticle?.toStar()?.let {
                appDb.rssStarDao.insert(it)
                rssStar = it
            }
        }.onSuccess {
            callBack?.upStarMenu()
        }
    }

    fun saveImage(webPic: String?, path: String) {
        webPic ?: return
        execute {
            val fileName = "${AppConst.fileNameFormat.format(Date(System.currentTimeMillis()))}.jpg"
            webData2bitmap(webPic)?.let { biteArray ->
                if (path.isContentScheme()) {
                    val uri = Uri.parse(path)
                    DocumentFile.fromTreeUri(context, uri)?.let { doc ->
                        DocumentUtils.createFileIfNotExist(doc, fileName)
                            ?.writeBytes(context, biteArray)
                    }
                } else {
                    val file = FileUtils.createFileIfNotExist(File(path), fileName)
                    file.writeBytes(biteArray)
                }
            } ?: throw Throwable("NULL")
        }.onError {
            toastOnUi("保存图片失败:${it.localizedMessage}")
        }.onSuccess {
            toastOnUi("保存成功")
        }
    }

    private suspend fun webData2bitmap(data: String): ByteArray? {
        return if (URLUtil.isValidUrl(data)) {
            RxHttp.get(data).toByteArray().await()
        } else {
            Base64.decode(data.split(",").toTypedArray()[1], Base64.DEFAULT)
        }
    }

    fun clHtml(content: String): String {
        return when {
            !rssSource?.style.isNullOrEmpty() -> {
                """
                    <style>
                        ${rssSource?.style}
                    </style>
                    $content
                """.trimIndent()
            }
            content.contains("<style>".toRegex()) -> {
                content
            }
            else -> {
                """
                    <style>
                        img{max-width:100% !important; width:auto; height:auto;}
                        video{object-fit:fill; max-width:100% !important; width:auto; height:auto;}
                        body{word-wrap:break-word; height:auto;max-width: 100%; width:auto;}
                    </style>
                    $content
                """.trimIndent()
            }
        }
    }

    @Synchronized
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.CHINA
            textToSpeech?.setOnUtteranceProgressListener(TTSUtteranceListener())
            ttsInitFinish = true
            play()
        } else {
            launch {
                toastOnUi(R.string.tts_init_failed)
            }
        }
    }

    @Synchronized
    private fun play() {
        if (!ttsInitFinish) return
        textToSpeech?.stop()
        ttsTextList.forEach {
            textToSpeech?.speak(it, TextToSpeech.QUEUE_ADD, null, "rss")
        }
    }

    fun readAloud(textArray: Array<String>) {
        ttsTextList.clear()
        ttsTextList.addAll(textArray)
        textToSpeech?.let {
            play()
        } ?: let {
            textToSpeech = TextToSpeech(context, this)
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    /**
     * 朗读监听
     */
    private inner class TTSUtteranceListener : UtteranceProgressListener() {

        override fun onStart(s: String) {
            callBack?.upTtsMenu(true)
        }

        override fun onDone(s: String) {
            callBack?.upTtsMenu(false)
        }

        override fun onError(s: String) {

        }

    }

    interface CallBack {
        fun upStarMenu()
        fun upTtsMenu(isPlaying: Boolean)
    }
}