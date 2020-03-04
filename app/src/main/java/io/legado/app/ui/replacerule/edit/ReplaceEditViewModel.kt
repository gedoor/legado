package io.legado.app.ui.replacerule.edit

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.ReplaceRule

class ReplaceEditViewModel(application: Application) : BaseViewModel(application) {

    val replaceRuleData = MutableLiveData<ReplaceRule>()

    fun initData(bundle: Bundle) {
        execute {
            replaceRuleData.value ?: let {
                val id = bundle.getLong("id")
                if (id > 0) {
                    App.db.replaceRuleDao().findById(id)?.let {
                        replaceRuleData.postValue(it)
                    }
                } else {
                    bundle.getString("pattern")?.let { pattern ->
                        val isRegex = bundle.getBoolean("isRegex")
                        replaceRuleData.postValue(
                            ReplaceRule(name = pattern, pattern = pattern, isRegex = isRegex)
                        )
                    }
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
