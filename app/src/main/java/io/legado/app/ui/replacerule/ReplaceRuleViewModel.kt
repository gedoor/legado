package io.legado.app.ui.replacerule

import android.app.Application
import android.net.Uri
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.http.HttpHelper
import io.legado.app.help.storage.ImportOldData
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import java.io.File

class ReplaceRuleViewModel(application: Application) : BaseViewModel(application) {
    fun importSourceFromFilePath(path: String, finally: (msg: String) -> Unit) {
        execute {
            val content = if (path.isContentPath()) {
                //在前面被解码了，如果不进行编码，中文会无法识别
                val newPath = Uri.encode(path, ":/.")
                DocumentFile.fromSingleUri(context, Uri.parse(newPath))?.readText(context)
            } else {
                val file = File(path)
                if (file.exists()) {
                    file.readText()
                } else {
                    null
                }
            }
            if (content != null) {
                importSource(content, finally)
            } else {
                withContext(Dispatchers.Main) {
                    finally("打开文件出错")
                }
            }
        }.onError {
            finally(it.localizedMessage ?: "打开文件出错")
        }
    }
    fun importSource(text: String, showMsg: (msg: String) -> Unit) {
        execute {
            if (text.isAbsUrl()) {
                HttpHelper.simpleGet(text)?.let {
                    ImportOldData.importOldReplaceRule(it)
                }
            } else {
                ImportOldData.importOldReplaceRule(text)
            }
        }.onError {
            showMsg(it.localizedMessage ?: "ERROR")
        }.onSuccess {
            showMsg(context.getString(R.string.success))
        }
    }

    fun update(vararg rule: ReplaceRule) {
        execute {
            App.db.replaceRuleDao().update(*rule)
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

    fun upOrder() {
        execute {
            val rules = App.db.replaceRuleDao().all
            for ((index, rule) in rules.withIndex()) {
                rule.order = index + 1
            }
            App.db.replaceRuleDao().update(*rules.toTypedArray())
        }
    }

    fun enableSelection(rules: LinkedHashSet<ReplaceRule>) {
        execute {
            val list = arrayListOf<ReplaceRule>()
            rules.forEach {
                list.add(it.copy(isEnabled = true))
            }
            App.db.replaceRuleDao().update(*list.toTypedArray())
        }
    }

    fun disableSelection(rules: LinkedHashSet<ReplaceRule>) {
        execute {
            val list = arrayListOf<ReplaceRule>()
            rules.forEach {
                list.add(it.copy(isEnabled = false))
            }
            App.db.replaceRuleDao().update(*list.toTypedArray())
        }
    }

    fun delSelection(rules: LinkedHashSet<ReplaceRule>) {
        execute {
            App.db.replaceRuleDao().delete(*rules.toTypedArray())
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
