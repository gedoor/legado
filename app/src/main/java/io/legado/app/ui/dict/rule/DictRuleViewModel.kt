package io.legado.app.ui.dict.rule

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.DictRule

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

}