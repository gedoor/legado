package io.legado.app.ui.dict

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.help.DefaultData
import io.legado.app.utils.toastOnUi
import java.util.regex.Pattern

class DictViewModel(application: Application) : BaseViewModel(application) {

    var dictHtmlData: MutableLiveData<String> = MutableLiveData()

    fun dict(word: String) {
        if (isChinese(word)) {
            baiduDict(word)
        } else {
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
            DefaultData.dictRules[1].search(word)
        }.onSuccess {
            dictHtmlData.postValue(it)
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
            DefaultData.dictRules[0].search(word)
        }.onSuccess {
            dictHtmlData.postValue(it)
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }

    /**
     * 判断是否包含汉字
     * @param str
     * @return
     */

    private fun isChinese(str: String): Boolean {
        val p = Pattern.compile("[\u4e00-\u9fa5]")
        val m = p.matcher(str)
        return m.find()
    }

}