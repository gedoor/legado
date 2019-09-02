package io.legado.app.ui.replacerule

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
                val id = intent.getLongExtra("data", -1)
                if (id > 0) {
                    App.db.replaceRuleDao().findById(id)?.let {
                        replaceRuleData.postValue(it)
                    }
                }
            }
        }
    }

    fun save(replaceRule: ReplaceRule, success: () -> Unit) {
        execute {
            App.db.replaceRuleDao().insert(replaceRule)
        }.onSuccess {
            success()
        }
    }

}
