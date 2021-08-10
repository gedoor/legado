package io.legado.app.ui.dict

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.help.http.get
import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.toastOnUi
import org.jsoup.Jsoup
import java.util.regex.Pattern

class DictViewModel(application: Application) : BaseViewModel(application) {

    var dictHtmlData: MutableLiveData<String> = MutableLiveData()

    fun dict(word: String) {
        if(isChinese(word)){
            baiduDict(word)
        }else{
            haiciDict(word)
        }

    }

    /**
     * 海词英文词典
     *
     * @param word
     */
    private fun haiciDict(word: String) {
        execute {
            val body = okHttpClient.newCallStrResponse {
                get("https://apii.dict.cn/mini.php", mapOf(Pair("q", word)))
            }.body
            val jsoup = Jsoup.parse(body!!)
            jsoup.body()
        }.onSuccess {
            dictHtmlData.postValue(it.html())
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }

    /**
     * 百度汉语词典
     *
     * @param word
     */
    private fun baiduDict(word: String) {
        execute {
            val body = okHttpClient.newCallStrResponse {
                get("https://dict.baidu.com/s", mapOf(Pair("wd", word)))
            }.body
            val jsoup = Jsoup.parse(body!!)
            jsoup.select("script").remove()//移除script
            jsoup.select("#word-header").remove()//移除单字的header
            jsoup.select("#term-header").remove()//移除词语的header
            jsoup.select(".more-button").remove()//移除展示更多
            jsoup.select(".disactive").remove()
            jsoup.select("#download-wrapper").remove()//移除下载广告
            jsoup.select("#right-panel").remove()//移除右侧广告
            jsoup.select("#content-panel")
        }.onSuccess {
            dictHtmlData.postValue(it.html())
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }

    /**
     * 判断是否包含汉字
     * @param str
     * @return
     */

    fun isChinese(str: String): Boolean {
        val p = Pattern.compile("[\u4e00-\u9fa5]")
        val m = p.matcher(str)
        return m.find()
    }

}