package io.legado.app.ui.replace.edit

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.ReplaceRule

class ReplaceEditViewModel(application: Application) : BaseViewModel(application) {

    val replaceRuleData = MutableLiveData<ReplaceRule>()

    fun initData(intent: Intent) {
        execute {
            replaceRuleData.value ?: let {
                val id = intent.getLongExtra("id", -1)
                if (id > 0) {
                    App.db.replaceRuleDao().findById(id)?.let {
                        replaceRuleData.postValue(it)
                    }
                } else {
                    val pattern = intent.getStringExtra("pattern") ?: ""
                    val isRegex = intent.getBooleanExtra("isRegex", false)
                    val scope = intent.getStringExtra("scope")
                    val rule = ReplaceRule(
                        name = pattern,
                        pattern = pattern,
                        isRegex = isRegex,
                        scope = scope
                    )
                    replaceRuleData.postValue(rule)
                }
            }
        }
    }

    fun save(replaceRule: ReplaceRule, success: () -> Unit) {
        execute {
            if (replaceRule.order == 0) {
                replaceRule.order = App.db.replaceRuleDao().maxOrder + 1
            }
            App.db.replaceRuleDao().insert(replaceRule)
        }.onSuccess {
            success()
        }
    }

}
