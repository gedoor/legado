package io.legado.app.ui.book.toc.rule

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

    fun del(vararg txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.delete(*txtTocRule)
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
                GSON.fromJsonArray<TxtTocRule>(json).getOrThrow().let {
                    appDb.txtTocRuleDao.insert(*it.toTypedArray())
                }
            }
        }.onSuccess {
            finally("导入成功")
        }.onError {
            finally("导入失败\n${it.localizedMessage}")
        }
    }

    fun toTop(vararg rules: TxtTocRule) {
        execute {
            val minOrder = appDb.txtTocRuleDao.minOrder - 1
            rules.forEachIndexed { index, source ->
                source.serialNumber = minOrder - index
            }
            appDb.txtTocRuleDao.update(*rules)
        }
    }

    fun toBottom(vararg sources: TxtTocRule) {
        execute {
            val maxOrder = appDb.txtTocRuleDao.maxOrder + 1
            sources.forEachIndexed { index, source ->
                source.serialNumber = maxOrder + index
            }
            appDb.txtTocRuleDao.update(*sources)
        }
    }

    fun upOrder() {
        execute {
            val sources = appDb.txtTocRuleDao.all
            for ((index: Int, source: TxtTocRule) in sources.withIndex()) {
                source.serialNumber = index + 1
            }
            appDb.txtTocRuleDao.update(*sources.toTypedArray())
        }
    }

    fun enableSelection(vararg txtTocRule: TxtTocRule) {
        execute {
            val array = txtTocRule.map { it.copy(enable = true) }.toTypedArray()
            appDb.txtTocRuleDao.insert(*array)
        }
    }

    fun disableSelection(vararg txtTocRule: TxtTocRule) {
        execute {
            val array = txtTocRule.map { it.copy(enable = false) }.toTypedArray()
            appDb.txtTocRuleDao.insert(*array)
        }
    }

}
