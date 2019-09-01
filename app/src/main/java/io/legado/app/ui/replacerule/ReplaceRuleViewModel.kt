package io.legado.app.ui.replacerule

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.ReplaceRule

class ReplaceRuleViewModel(application: Application) : BaseViewModel(application) {


    fun update(rule: ReplaceRule) {
        execute {
            App.db.replaceRuleDao().update(rule)
        }
    }

    fun delete(rule: ReplaceRule) {
        execute {
            App.db.replaceRuleDao().delete(rule)
        }
    }

}
