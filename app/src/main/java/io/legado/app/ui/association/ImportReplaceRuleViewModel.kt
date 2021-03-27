package io.legado.app.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.storage.OldReplace
import io.legado.app.utils.isAbsUrl
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toText

class ImportReplaceRuleViewModel(app: Application) : BaseViewModel(app) {
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<ArrayList<ReplaceRule>>()

    private val allRules = arrayListOf<ReplaceRule>()

    fun import(text: String) {
        execute {
            if (text.isAbsUrl()) {
                RxHttp.get(text).toText("utf-8").await().let {
                    val rules = OldReplace.jsonToReplaceRules(it)
                    allRules.addAll(rules)
                }
            } else {
                val rules = OldReplace.jsonToReplaceRules(text)
                allRules.addAll(rules)
            }
        }.onError {
            errorLiveData.postValue(it.localizedMessage ?: "ERROR")
        }.onSuccess {
            successLiveData.postValue(allRules)
        }
    }
}