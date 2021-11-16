package io.legado.app.ui.book.local.rule

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.help.DefaultData
import io.legado.app.help.http.newCallResponseBody
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.text
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray

class TxtTocRuleViewModel(app: Application) : BaseViewModel(app) {

    fun save(txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.insert(txtTocRule)
        }
    }

    fun del(txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.delete(txtTocRule)
        }
    }

    fun update(vararg txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.update(*txtTocRule)
        }
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultTocRules()
        }
    }

    fun importOnLine(url: String, finally: (msg: String) -> Unit) {
        execute {
            okHttpClient.newCallResponseBody {
                url(url)
            }.text("utf-8").let { json ->
                GSON.fromJsonArray<TxtTocRule>(json)?.let {
                    appDb.txtTocRuleDao.insert(*it.toTypedArray())
                }
            }
        }.onSuccess {
            finally("导入成功")
        }.onError {
            finally("导入失败")
        }
    }

}