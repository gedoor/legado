package io.legado.app.ui.rss.read

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.URLUtil
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppConst
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.data.entities.RssStar
import io.legado.app.help.TTS
import io.legado.app.help.http.newCallResponseBody
import io.legado.app.help.http.okHttpClient
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.rss.Rss
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import splitties.init.appCtx
import java.io.File
import java.util.*


class ReadRssViewModel(application: Application) : BaseViewModel(application) {
    var rssSource: RssSource? = null
    var rssArticle: RssArticle? = null
    var tts: TTS? = null
    val contentLiveData = MutableLiveData<String>()
    val urlLiveData = MutableLiveData<AnalyzeUrl>()
    var rssStar: RssStar? = null
    val upTtsMenuData = MutableLiveData<Boolean>()
    val upStarMenuData = MutableLiveData<Boolean>()

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
            upStarMenuData.postValue(true)
        }
    }

    private fun loadUrl(url: String, baseUrl: String) {
        val analyzeUrl = AnalyzeUrl(
            mUrl = url,
            baseUrl = baseUrl,
            headerMapF = rssSource?.getHeaderMap()
        )
        urlLiveData.postValue(analyzeUrl)
    }

    private fun loadContent(rssArticle: RssArticle, ruleContent: String) {
        rssSource?.let { source ->
            Rss.getContent(viewModelScope, rssArticle, ruleContent, source)
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

    fun refresh(finish: () -> Unit) {
        rssArticle?.let { rssArticle ->
            rssSource?.let {
                val ruleContent = it.ruleContent
                if (!ruleContent.isNullOrBlank()) {
                    loadContent(rssArticle, ruleContent)
                } else {
                    finish.invoke()
                }
            } ?: let {
                appCtx.toastOnUi("订阅源不存在")
                finish.invoke()
            }
        } ?: finish.invoke()
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
            upStarMenuData.postValue(true)
        }
    }

    fun saveImage(webPic: String?, uri: Uri) {
        webPic ?: return
        execute {
            val fileName = "${AppConst.fileNameFormat.format(Date(System.currentTimeMillis()))}.jpg"
            webData2bitmap(webPic)?.let { biteArray ->
                if (uri.isContentScheme()) {
                    DocumentFile.fromTreeUri(context, uri)?.let { doc ->
                        DocumentUtils.createFileIfNotExist(doc, fileName)
                            ?.writeBytes(context, biteArray)
                    }
                } else {
                    val dir = File(uri.path ?: uri.toString())
                    val file = FileUtils.createFileIfNotExist(dir, fileName)
                    file.writeBytes(biteArray)
                }
            } ?: throw Throwable("NULL")
        }.onError {
            context.toastOnUi("保存图片失败:${it.localizedMessage}")
        }.onSuccess {
            context.toastOnUi("保存成功")
        }
    }

    private suspend fun webData2bitmap(data: String): ByteArray? {
        return if (URLUtil.isValidUrl(data)) {
            @Suppress("BlockingMethodInNonBlockingContext")
            okHttpClient.newCallResponseBody {
                url(data)
            }.bytes()
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
    fun readAloud(text: String) {
        if (tts == null) {
            tts = TTS().apply {
                setSpeakStateListener(object : TTS.SpeakStateListener {
                    override fun onStart() {
                        upTtsMenuData.postValue(true)
                    }

                    override fun onDone() {
                        upTtsMenuData.postValue(false)
                    }
                })
            }
        }
        tts?.speak(text)
    }

    override fun onCleared() {
        super.onCleared()
        tts?.clearTts()
    }

}