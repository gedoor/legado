package io.legado.app.ui.rss.read

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.script.rhino.runScriptWithContext
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppConst.imagePathKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssReadRecord
import io.legado.app.data.entities.RssSource
import io.legado.app.data.entities.RssStar
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.TTS
import io.legado.app.help.http.newCallResponseBody
import io.legado.app.help.http.okHttpClient
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.rss.Rss
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.ACache
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.writeBytes
import kotlinx.coroutines.Dispatchers.IO
import splitties.init.appCtx
import java.util.Date
import kotlin.coroutines.coroutineContext


class ReadRssViewModel(application: Application) : BaseViewModel(application) {
    var rssSource: RssSource? = null
    var rssArticle: RssArticle? = null
    var tts: TTS? = null
    val contentLiveData = MutableLiveData<String>()
    val urlLiveData = MutableLiveData<AnalyzeUrl>()
    val htmlLiveData = MutableLiveData<String>()
    var rssStar: RssStar? = null
    val upTtsMenuData = MutableLiveData<Boolean>()
    val upStarMenuData = MutableLiveData<Boolean>()
    var headerMap: Map<String, String> = emptyMap()
    var origin: String? = null

    fun initData(intent: Intent) {
        execute {
            origin = intent.getStringExtra("origin") ?: return@execute
            val link = intent.getStringExtra("link")
            rssSource = appDb.rssSourceDao.getByKey(origin!!)
            headerMap = runScriptWithContext {
                rssSource?.getHeaderMap() ?: emptyMap()
            }
            if (link != null) {
                rssStar = appDb.rssStarDao.get(origin!!, link)
                val sort = intent.getStringExtra("sort")
                rssArticle = rssStar?.toRssArticle()
                    ?: if (sort == null) {
                        appDb.rssArticleDao.getByLink(origin!!, link)
                    } else {
                        appDb.rssArticleDao.get(origin!!, link, sort)
                    }

                rssArticle?.let { article ->
                    if (!article.description.isNullOrBlank()) {
                        contentLiveData.postValue(article.description!!)
                    } else {
                        rssSource?.let {
                            val ruleContent = it.ruleContent
                            if (!ruleContent.isNullOrBlank()) {
                                loadContent(article, ruleContent)
                            } else {
                                loadUrl(article.link, article.origin)
                            }
                        } ?: loadUrl(article.link, article.origin)
                    }
                } ?: return@execute
            } else {
                val ruleContent = rssSource?.ruleContent
                val startHtml = intent.getBooleanExtra("startHtml", false)
                val openUrl = intent.getStringExtra("openUrl")
                if (startHtml) {
                    htmlLiveData.postValue(hbHtml())
                }
                else if (ruleContent.isNullOrBlank()) {
                    loadUrl(openUrl ?: origin!!, origin!!)
                }
                else if (rssSource!!.singleUrl) {
                    htmlLiveData.postValue(ruleContent)
                }
                else if (openUrl != null) {
                    val title = intent.getStringExtra("title") ?: rssSource!!.sourceName
                    val rssArticle = appDb.rssArticleDao.getByLink(origin!!, openUrl) ?: RssArticle(
                        origin!!, title, title, link = openUrl)
                    loadContent(rssArticle, ruleContent)
                }
            }
        }.onFinally {
            upStarMenuData.postValue(true)
        }
    }

    private suspend fun loadUrl(url: String, baseUrl: String) {
        val analyzeUrl = AnalyzeUrl(
            mUrl = url,
            baseUrl = baseUrl,
            source = rssSource,
            coroutineContext = coroutineContext,
            hasLoginHeader = false
        )
        urlLiveData.postValue(analyzeUrl)
    }

    private fun loadContent(rssArticle: RssArticle, ruleContent: String) {
        val source = rssSource ?: return
        Rss.getContent(viewModelScope, rssArticle, ruleContent, source)
            .onSuccess(IO) { body ->
                rssArticle.description = body
                appDb.rssArticleDao.insert(rssArticle)
                rssStar?.let {
                    it.description = body
                    appDb.rssStarDao.insert(it)
                }
                this@ReadRssViewModel.rssArticle = rssArticle
                contentLiveData.postValue(body)
            }.onError {
                contentLiveData.postValue("加载正文失败\n${it.stackTraceToString()}")
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

    fun addFavorite() {
        execute {
            rssStar ?: rssArticle?.toStar()?.let {
                appDb.rssStarDao.insert(it)
                rssStar = it
            }
        }.onSuccess {
            upStarMenuData.postValue(true)
        }
    }

    fun updateFavorite() {
        execute {
            rssArticle?.toStar()?.let {
                appDb.rssStarDao.update(it)
                rssStar = it
            }
        }.onSuccess {
            upStarMenuData.postValue(true)
        }
    }

    fun delFavorite() {
        execute {
            rssStar?.let {
                appDb.rssStarDao.delete(it.origin, it.link)
                rssStar = null
            }
        }.onSuccess {
            upStarMenuData.postValue(true)
        }
    }

    fun saveImage(webPic: String?, uri: Uri) {
        webPic ?: return
        execute {
            val fileName = "${AppConst.fileNameFormat.format(Date(System.currentTimeMillis()))}.jpg"
            val byteArray = webData2bitmap(webPic) ?: throw NoStackTraceException("NULL")
            uri.writeBytes(context, fileName, byteArray)
        }.onError {
            ACache.get().remove(imagePathKey)
            context.toastOnUi("保存图片失败:${it.localizedMessage}")
        }.onSuccess {
            context.toastOnUi("保存成功")
        }
    }

    private suspend fun webData2bitmap(data: String): ByteArray? {
        return if (URLUtil.isValidUrl(data)) {
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

    fun hbHtml(): String {
        rssSource?.let{
            var processedHtml = it.startHtml!!.let{ html  ->
                try {
                    when {
                        html.startsWith("@js:") -> it.evalJS( html.substring(4)).toString()
                        html.startsWith("<js>") -> it.evalJS(html.substring(4, html.lastIndexOf("<"))).toString()
                        else -> html
                    }
                } catch (e: Throwable) {
                    e.printOnDebug()
                    html
                }
            }
            val javascript = rssSource?.startJs
            if (!javascript.isNullOrBlank()) {
                processedHtml = if (processedHtml.contains("</body>")) {
                    processedHtml.replaceFirst("</body>", "<script>$javascript</script></body>")
                } else {
                    "<body>$processedHtml<script>$javascript</script></body>"
                }
            }
            val style = rssSource?.run { startStyle ?: style } ?: """"img{max-width:100% !important; width:auto; height:auto;}video{object-fit:fill; max-width:100% !important; width:auto; height:auto;}body{word-wrap:break-word; height:auto;max-width: 100%; width:auto;}"""
            processedHtml = if (processedHtml.contains("<head>")) {
                processedHtml.replaceFirst("<head>", "<head><style>$style</style>")
            } else {
                "<head><style>$style</style></head>$processedHtml"
            }
            return processedHtml
        }
        return "<body>rssSource is null</body>"
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

    fun readRss(title: String, link: String, origin: String?) {
        execute {
            val rssReadRecord = RssReadRecord(
                record = link,
                title = title,
                origin = origin ?: "",
                readTime = System.currentTimeMillis()
            )
            appDb.rssReadRecordDao.insertRecord(rssReadRecord)
        }
    }

}