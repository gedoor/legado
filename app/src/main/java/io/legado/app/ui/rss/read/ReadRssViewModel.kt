package io.legado.app.ui.rss.read

import android.app.Application
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.model.Rss
import io.legado.app.model.analyzeRule.AnalyzeUrl
import kotlinx.coroutines.launch
import java.util.*

class ReadRssViewModel(application: Application) : BaseViewModel(application),
    TextToSpeech.OnInitListener {
    var callBack: CallBack? = null
    var rssSource: RssSource? = null
    var rssArticle: RssArticle? = null
    val contentLiveData = MutableLiveData<String>()
    val urlLiveData = MutableLiveData<AnalyzeUrl>()
    var star = false
    var textToSpeech: TextToSpeech = TextToSpeech(context, this)

    fun initData(intent: Intent) {
        execute {
            val origin = intent.getStringExtra("origin")
            val link = intent.getStringExtra("link")
            if (origin != null && link != null) {
                rssSource = App.db.rssSourceDao().getByKey(origin)
                star = App.db.rssStarDao().get(origin, link) != null
                rssArticle = App.db.rssArticleDao().get(origin, link)
                rssArticle?.let { rssArticle ->
                    if (!rssArticle.description.isNullOrBlank()) {
                        contentLiveData.postValue(rssArticle.description)
                    } else {
                        rssSource?.let {
                            val ruleContent = it.ruleContent
                            if (!ruleContent.isNullOrBlank()) {
                                loadContent(rssArticle, ruleContent)
                            } else {
                                loadUrl(rssArticle)
                            }
                        } ?: loadUrl(rssArticle)
                    }
                }
            }
        }.onFinally {
            callBack?.upStarMenu()
        }
    }

    private fun loadUrl(rssArticle: RssArticle) {
        val analyzeUrl = AnalyzeUrl(
            rssArticle.link,
            baseUrl = rssArticle.origin,
            useWebView = true,
            headerMapF = rssSource?.getHeaderMap()
        )
        urlLiveData.postValue(analyzeUrl)
    }

    private fun loadContent(rssArticle: RssArticle, ruleContent: String) {
        Rss.getContent(rssArticle, ruleContent, this)
            .onSuccess {
                contentLiveData.postValue(it)
            }
    }

    fun star() {
        execute {
            rssArticle?.let {
                if (star) {
                    App.db.rssStarDao().delete(it.origin, it.link)
                } else {
                    App.db.rssStarDao().insert(it.toStar())
                }
            }
        }.onSuccess {
            callBack?.upStarMenu()
        }
    }

    fun clHtml(content: String): String {
        return if (content.contains("<style>|style=".toRegex())) {
            content
        } else {
            """
                <style>
                    img{max-width:100% !important; width:auto; height:auto;}
                    video{object-fit:fill; max-width:100% !important; width:auto; height:auto;}
                    body{word-wrap:break-word; height:auto;max-width: 100%; width:auto;}
                </style>$content
             """
        }
    }

    override fun onInit(status: Int) {
        launch {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.CHINA
                textToSpeech.setOnUtteranceProgressListener(TTSUtteranceListener())
            } else {
                toast(R.string.tts_init_failed)
            }
        }
    }

    fun readAloud(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rss")
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    /**
     * 朗读监听
     */
    private inner class TTSUtteranceListener : UtteranceProgressListener() {

        override fun onStart(s: String) {

        }

        override fun onDone(s: String) {

        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            super.onRangeStart(utteranceId, start, end, frame)

        }

        override fun onError(s: String) {

        }

    }
    interface CallBack {
        fun upStarMenu()
    }
}