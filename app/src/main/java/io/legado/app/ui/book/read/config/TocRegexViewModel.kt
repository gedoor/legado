package io.legado.app.ui.book.read.config

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.help.DefaultData
import io.legado.app.help.http.HttpHelper
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray

class TocRegexViewModel(application: Application) : BaseViewModel(application) {

    fun saveRule(rule: TxtTocRule) {
        execute {
            if (rule.serialNumber < 0) {
                rule.serialNumber = App.db.txtTocRule().lastOrderNum + 1
            }
            App.db.txtTocRule().insert(rule)
        }
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultTocRules()
        }
    }

    fun importOnLine(url: String, finally: (msg: String) -> Unit) {
        execute {
            HttpHelper.simpleGetAsync(url)?.let { json ->
                GSON.fromJsonArray<TxtTocRule>(json)?.let {
                    App.db.txtTocRule().insert(*it.toTypedArray())
                }
            }
        }.onSuccess {
            finally("导入成功")
        }.onError {
            finally("导入失败")
        }
    }

}