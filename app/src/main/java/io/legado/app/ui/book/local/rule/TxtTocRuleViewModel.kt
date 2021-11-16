package io.legado.app.ui.book.local.rule

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.TxtTocRule

class TxtTocRuleViewModel(app: Application) : BaseViewModel(app) {

    fun del(txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.delete(txtTocRule)
        }
    }

    fun update(txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.update(txtTocRule)
        }
    }

}