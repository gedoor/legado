package io.legado.app.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.http.newCall
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.text
import io.legado.app.help.storage.OldReplace
import io.legado.app.utils.isAbsUrl

class ImportReplaceRuleViewModel(app: Application) : BaseViewModel(app) {
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<ArrayList<ReplaceRule>>()

    private val allRules = arrayListOf<ReplaceRule>()

    fun import(text: String) {
        execute {
            if (text.isAbsUrl()) {
                okHttpClient.newCall {
                    url(text)
                }.text("utf-8").let {
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