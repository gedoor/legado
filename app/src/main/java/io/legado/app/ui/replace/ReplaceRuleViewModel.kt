package io.legado.app.ui.replace

import android.app.Application
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.*
import java.io.File

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

    fun toBottom(rule: ReplaceRule) {
        execute {
            rule.order = appDb.replaceRuleDao.maxOrder + 1
            appDb.replaceRuleDao.update(rule)
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

    fun enableSelection(rules: LinkedHashSet<ReplaceRule>) {
        execute {
            val list = arrayListOf<ReplaceRule>()
            rules.forEach {
                list.add(it.copy(isEnabled = true))
            }
            appDb.replaceRuleDao.update(*list.toTypedArray())
        }
    }

    fun disableSelection(rules: LinkedHashSet<ReplaceRule>) {
        execute {
            val list = arrayListOf<ReplaceRule>()
            rules.forEach {
                list.add(it.copy(isEnabled = false))
            }
            appDb.replaceRuleDao.update(*list.toTypedArray())
        }
    }

    fun delSelection(rules: LinkedHashSet<ReplaceRule>) {
        execute {
            appDb.replaceRuleDao.delete(*rules.toTypedArray())
        }
    }

    fun exportSelection(sources: LinkedHashSet<ReplaceRule>, file: File) {
        execute {
            val json = GSON.toJson(sources)
            FileUtils.createFileIfNotExist(file, "exportReplaceRule.json")
                .writeText(json)
        }.onSuccess {
            context.toastOnUi("成功导出至\n${file.absolutePath}")
        }.onError {
            context.toastOnUi("导出失败\n${it.localizedMessage}")
        }
    }

    fun exportSelection(sources: LinkedHashSet<ReplaceRule>, doc: DocumentFile) {
        execute {
            val json = GSON.toJson(sources)
            doc.findFile("exportReplaceRule.json")?.delete()
            doc.createFile("", "exportReplaceRule.json")
                ?.writeText(context, json)
        }.onSuccess {
            context.toastOnUi("成功导出至\n${doc.uri.path}")
        }.onError {
            context.toastOnUi("导出失败\n${it.localizedMessage}")
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
