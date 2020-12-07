package io.legado.app.ui.replace

import android.app.Application
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.writeText
import org.jetbrains.anko.toast
import java.io.File

class ReplaceRuleViewModel(application: Application) : BaseViewModel(application) {

    fun update(vararg rule: ReplaceRule) {
        execute {
            App.db.replaceRuleDao.update(*rule)
        }
    }

    fun delete(rule: ReplaceRule) {
        execute {
            App.db.replaceRuleDao.delete(rule)
        }
    }

    fun toTop(rule: ReplaceRule) {
        execute {
            rule.order = App.db.replaceRuleDao.minOrder - 1
            App.db.replaceRuleDao.update(rule)
        }
    }

    fun toBottom(rule: ReplaceRule) {
        execute {
            rule.order = App.db.replaceRuleDao.maxOrder + 1
            App.db.replaceRuleDao.update(rule)
        }
    }

    fun upOrder() {
        execute {
            val rules = App.db.replaceRuleDao.all
            for ((index, rule) in rules.withIndex()) {
                rule.order = index + 1
            }
            App.db.replaceRuleDao.update(*rules.toTypedArray())
        }
    }

    fun enableSelection(rules: LinkedHashSet<ReplaceRule>) {
        execute {
            val list = arrayListOf<ReplaceRule>()
            rules.forEach {
                list.add(it.copy(isEnabled = true))
            }
            App.db.replaceRuleDao.update(*list.toTypedArray())
        }
    }

    fun disableSelection(rules: LinkedHashSet<ReplaceRule>) {
        execute {
            val list = arrayListOf<ReplaceRule>()
            rules.forEach {
                list.add(it.copy(isEnabled = false))
            }
            App.db.replaceRuleDao.update(*list.toTypedArray())
        }
    }

    fun delSelection(rules: LinkedHashSet<ReplaceRule>) {
        execute {
            App.db.replaceRuleDao.delete(*rules.toTypedArray())
        }
    }

    fun exportSelection(sources: LinkedHashSet<ReplaceRule>, file: File) {
        execute {
            val json = GSON.toJson(sources)
            FileUtils.createFileIfNotExist(file, "exportReplaceRule.json")
                .writeText(json)
        }.onSuccess {
            context.toast("成功导出至\n${file.absolutePath}")
        }.onError {
            context.toast("导出失败\n${it.localizedMessage}")
        }
    }

    fun exportSelection(sources: LinkedHashSet<ReplaceRule>, doc: DocumentFile) {
        execute {
            val json = GSON.toJson(sources)
            doc.findFile("exportReplaceRule.json")?.delete()
            doc.createFile("", "exportReplaceRule.json")
                ?.writeText(context, json)
        }.onSuccess {
            context.toast("成功导出至\n${doc.uri.path}")
        }.onError {
            context.toast("导出失败\n${it.localizedMessage}")
        }
    }

    fun addGroup(group: String) {
        execute {
            val sources = App.db.replaceRuleDao.noGroup
            sources.map { source ->
                source.group = group
            }
            App.db.replaceRuleDao.update(*sources.toTypedArray())
        }
    }

    fun upGroup(oldGroup: String, newGroup: String?) {
        execute {
            val sources = App.db.replaceRuleDao.getByGroup(oldGroup)
            sources.map { source ->
                source.group?.splitNotBlank(",")?.toHashSet()?.let {
                    it.remove(oldGroup)
                    if (!newGroup.isNullOrEmpty())
                        it.add(newGroup)
                    source.group = TextUtils.join(",", it)
                }
            }
            App.db.replaceRuleDao.update(*sources.toTypedArray())
        }
    }

    fun delGroup(group: String) {
        execute {
            execute {
                val sources = App.db.replaceRuleDao.getByGroup(group)
                sources.map { source ->
                    source.group?.splitNotBlank(",")?.toHashSet()?.let {
                        it.remove(group)
                        source.group = TextUtils.join(",", it)
                    }
                }
                App.db.replaceRuleDao.update(*sources.toTypedArray())
            }
        }
    }
}
