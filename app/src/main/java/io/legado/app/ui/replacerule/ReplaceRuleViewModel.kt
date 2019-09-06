package io.legado.app.ui.replacerule

import android.app.Application
import android.text.TextUtils
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.splitNotBlank

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

    fun toTop(rule: ReplaceRule) {
        execute {
            rule.order = App.db.replaceRuleDao().minOrder - 1
            App.db.replaceRuleDao().update(rule)
        }
    }

    fun addGroup(group: String) {
        execute {
            val sources = App.db.replaceRuleDao().noGroup
            sources.map { source ->
                source.group = group
            }
            App.db.replaceRuleDao().update(*sources.toTypedArray())
        }
    }

    fun upGroup(oldGroup: String, newGroup: String?) {
        execute {
            val sources = App.db.replaceRuleDao().getByGroup(oldGroup)
            sources.map { source ->
                source.group?.splitNotBlank(",")?.toHashSet()?.let {
                    it.remove(oldGroup)
                    if (!newGroup.isNullOrEmpty())
                        it.add(newGroup)
                    source.group = TextUtils.join(",", it)
                }
            }
            App.db.replaceRuleDao().update(*sources.toTypedArray())
        }
    }

    fun delGroup(group: String) {
        execute {
            execute {
                val sources = App.db.replaceRuleDao().getByGroup(group)
                sources.map { source ->
                    source.group?.splitNotBlank(",")?.toHashSet()?.let {
                        it.remove(group)
                        source.group = TextUtils.join(",", it)
                    }
                }
                App.db.replaceRuleDao().update(*sources.toTypedArray())
            }
        }
    }
}
