package io.legado.app.ui.dict

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.help.http.get
import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.toastOnUi
import org.jsoup.Jsoup

class DictViewModel(application: Application) : BaseViewModel(application) {

    var dictHtmlData: MutableLiveData<String> = MutableLiveData()

    fun dict(word: String) {
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

}