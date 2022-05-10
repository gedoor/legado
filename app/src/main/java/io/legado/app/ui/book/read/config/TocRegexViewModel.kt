package io.legado.app.ui.book.read.config

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.help.DefaultData

class TocRegexViewModel(application: Application) : BaseViewModel(application) {

    fun saveRule(rule: TxtTocRule) {
        execute {
            if (rule.serialNumber < 0) {
                rule.serialNumber = appDb.txtTocRuleDao.maxOrder + 1
            }
            appDb.txtTocRuleDao.insert(rule)
        }
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultTocRules()
        }
    }

}