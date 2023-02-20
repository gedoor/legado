package io.legado.app.ui.dict.rule

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.DictRule
import io.legado.app.help.DefaultData

class DictRuleViewModel(application: Application) : BaseViewModel(application) {


    fun upsert(vararg dictRule: DictRule) {
        execute {
            appDb.dictRuleDao.upsert(*dictRule)
        }
    }

    fun delete(vararg dictRule: DictRule) {
        execute {
            appDb.dictRuleDao.delete(*dictRule)
        }
    }

    fun upSortNumber() {
        execute {
            val rules = appDb.dictRuleDao.all
            for ((index, rule) in rules.withIndex()) {
                rule.sortNumber = index + 1
            }
            appDb.dictRuleDao.upsert(*rules.toTypedArray())
        }
    }

    fun enableSelection(vararg dictRule: DictRule) {
        execute {
            dictRule.forEach {
                it.enabled = true
            }
            appDb.dictRuleDao.upsert(*dictRule)
        }
    }

    fun disableSelection(vararg dictRule: DictRule) {
        execute {
            dictRule.forEach {
                it.enabled = false
            }
            appDb.dictRuleDao.upsert(*dictRule)
        }
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultDictRules()
        }
    }

}