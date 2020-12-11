package io.legado.app.ui.book.read.config

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.help.DefaultData
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toText

class TocRegexViewModel(application: Application) : BaseViewModel(application) {

    fun saveRule(rule: TxtTocRule) {
        execute {
            if (rule.serialNumber < 0) {
                rule.serialNumber = App.db.txtTocRule.lastOrderNum + 1
            }
            App.db.txtTocRule.insert(rule)
        }
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultTocRules()
        }
    }

    fun importOnLine(url: String, finally: (msg: String) -> Unit) {
        execute {
            RxHttp.get(url).toText("utf-8").await().let { json ->
                GSON.fromJsonArray<TxtTocRule>(json)?.let {
                    App.db.txtTocRule.insert(*it.toTypedArray())
                }
            }
        }.onSuccess {
            finally("导入成功")
        }.onError {
            finally("导入失败")
        }
    }

}