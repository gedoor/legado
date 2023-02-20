package io.legado.app.ui.dict

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.DictRule
import io.legado.app.help.DefaultData
import io.legado.app.utils.toastOnUi

class DictViewModel(application: Application) : BaseViewModel(application) {
    var dictRules: List<DictRule>? = null
    var dictHtmlData: MutableLiveData<String> = MutableLiveData()

    fun initData(onSuccess: () -> Unit) {
        execute {
            DefaultData.dictRules
        }.onSuccess {
            dictRules = it
            onSuccess.invoke()
        }
    }

    fun dict(dictRule: DictRule, word: String) {
        execute {
            dictRule.search(word)
        }.onSuccess {
            dictHtmlData.postValue(it)
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }


}