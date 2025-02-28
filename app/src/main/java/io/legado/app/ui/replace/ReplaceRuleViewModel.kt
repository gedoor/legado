package io.legado.app.ui.replace

import android.app.Application
import android.text.TextUtils
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.splitNotBlank

/**
 * 替换规则数据修改
 * 修改数据要copy,直接修改会导致界面不刷新
 */
class ReplaceRuleViewModel(application: Application) : BaseViewModel(application) {

    fun update(vararg rule: ReplaceRule) {
        execute {
            appDb.replaceRuleDao.update(*rule)
        }
    }

    fun delete(rule: ReplaceRule) {
        execute {
            appDb.replaceRuleDao.delete(rule)
        }
    }

    fun toTop(rule: ReplaceRule) {
        execute {
            rule.order = appDb.replaceRuleDao.minOrder - 1
            appDb.replaceRuleDao.update(rule)
        }
    }

    fun topSelect(rules: List<ReplaceRule>) {
        execute {
            var minOrder = appDb.replaceRuleDao.minOrder - rules.size
            rules.forEach {
                it.order = ++minOrder
            }
            appDb.replaceRuleDao.update(*rules.toTypedArray())
        }
    }

    fun toBottom(rule: ReplaceRule) {
        execute {
            rule.order = appDb.replaceRuleDao.maxOrder + 1
            appDb.replaceRuleDao.update(rule)
        }
    }

    fun bottomSelect(rules: List<ReplaceRule>) {
        execute {
            var maxOrder = appDb.replaceRuleDao.maxOrder
            rules.forEach {
                it.order = maxOrder++
            }
            appDb.replaceRuleDao.update(*rules.toTypedArray())
        }
    }

    fun upOrder() {
        execute {
            val rules = appDb.replaceRuleDao.all
            for ((index, rule) in rules.withIndex()) {
                rule.order = index + 1
            }
            appDb.replaceRuleDao.update(*rules.toTypedArray())
        }
    }

    fun enableSelection(rules: List<ReplaceRule>) {
        execute {
            val array = Array(rules.size) {
                rules[it].copy(isEnabled = true)
            }
            appDb.replaceRuleDao.update(*array)
        }
    }

    fun disableSelection(rules: List<ReplaceRule>) {
        execute {
            val array = Array(rules.size) {
                rules[it].copy(isEnabled = false)
            }
            appDb.replaceRuleDao.update(*array)
        }
    }

    fun delSelection(rules: List<ReplaceRule>) {
        execute {
            appDb.replaceRuleDao.delete(*rules.toTypedArray())
        }
    }

    fun addGroup(group: String) {
        execute {
            val sources = appDb.replaceRuleDao.noGroup
            sources.map { source ->
                source.group = group
            }
            appDb.replaceRuleDao.update(*sources.toTypedArray())
        }
    }

    fun upGroup(oldGroup: String, newGroup: String?) {
        execute {
            val sources = appDb.replaceRuleDao.getByGroup(oldGroup)
            sources.map { source ->
                source.group?.splitNotBlank(",")?.toHashSet()?.let {
                    it.remove(oldGroup)
                    if (!newGroup.isNullOrEmpty())
                        it.add(newGroup)
                    source.group = TextUtils.join(",", it)
                }
            }
            appDb.replaceRuleDao.update(*sources.toTypedArray())
        }
    }

    fun delGroup(group: String) {
        execute {
            execute {
                val sources = appDb.replaceRuleDao.getByGroup(group)
                sources.map { source ->
                    source.group?.splitNotBlank(",")?.toHashSet()?.let {
                        it.remove(group)
                        source.group = TextUtils.join(",", it)
                    }
                }
                appDb.replaceRuleDao.update(*sources.toTypedArray())
            }
        }
    }
}
