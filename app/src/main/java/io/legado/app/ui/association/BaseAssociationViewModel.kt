package io.legado.app.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel

abstract class BaseAssociationViewModel(application: Application) : BaseViewModel(application) {

    val successLive = MutableLiveData<Pair<String, String>>()
    val errorLive = MutableLiveData<String>()

    fun importJson(json: String) {
        //暂时根据文件内容判断属于什么
        when {
            json.contains("bookSourceUrl") ->
                successLive.postValue(Pair("bookSource", json))
            json.contains("sourceUrl") ->
                successLive.postValue(Pair("rssSource", json))
            json.contains("pattern") ->
                successLive.postValue(Pair("replaceRule", json))
            json.contains("themeName") ->
                successLive.postValue(Pair("theme", json))
            json.contains("name") && json.contains("rule") ->
                successLive.postValue(Pair("txtRule", json))
            json.contains("name") && json.contains("url") ->
                successLive.postValue(Pair("httpTts", json))
            else -> errorLive.postValue("格式不对")
        }
    }

}