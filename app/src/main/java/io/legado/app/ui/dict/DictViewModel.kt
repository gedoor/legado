package io.legado.app.ui.dict

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.help.http.get
import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import org.jsoup.Jsoup

class DictViewModel(application: Application) : BaseViewModel(application) {

    var dictHtmlData: MutableLiveData<String> = MutableLiveData()

    fun dict(word: String) {
        execute {
            val body = okHttpClient.newCallStrResponse {
                get("http://apii.dict.cn/mini.php", mapOf(Pair("q", word)))
            }.body
            val jsoup = Jsoup.parse(body)
            jsoup.body()
        }.onSuccess {
            dictHtmlData.postValue(it.html())
        }.onError {
            toastOnUi(it.localizedMessage)
        }

    }

}