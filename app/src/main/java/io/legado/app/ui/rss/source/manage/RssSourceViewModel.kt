package io.legado.app.ui.rss.source.manage

import android.app.Application
import android.net.Uri
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import com.jayway.jsonpath.JsonPath
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.RssSource
import io.legado.app.help.SourceHelp
import io.legado.app.help.http.HttpHelper
import io.legado.app.help.storage.Restore.jsonPath
import io.legado.app.utils.*
import org.jetbrains.anko.toast
import java.io.File

class RssSourceViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(vararg sources: RssSource) {
        execute {
            val minOrder = App.db.rssSourceDao().minOrder - 1
            sources.forEachIndexed { index, rssSource ->
                rssSource.customOrder = minOrder - index
            }
            App.db.rssSourceDao().update(*sources)
        }
    }

    fun bottomSource(vararg sources: RssSource) {
        execute {
            val maxOrder = App.db.rssSourceDao().maxOrder + 1
            sources.forEachIndexed { index, rssSource ->
                rssSource.customOrder = maxOrder + index
            }
            App.db.rssSourceDao().update(*sources)
        }
    }

    fun del(rssSource: RssSource) {
        execute { App.db.rssSourceDao().delete(rssSource) }
    }

    fun update(vararg rssSource: RssSource) {
        execute { App.db.rssSourceDao().update(*rssSource) }
    }

    fun upOrder() {
        execute {
            val sources = App.db.rssSourceDao().all
            for ((index: Int, source: RssSource) in sources.withIndex()) {
                source.customOrder = index + 1
            }
            App.db.rssSourceDao().update(*sources.toTypedArray())
        }
    }

    fun enableSelection(sources: List<RssSource>) {
        execute {
            val list = arrayListOf<RssSource>()
            sources.forEach {
                list.add(it.copy(enabled = true))
            }
            App.db.rssSourceDao().update(*list.toTypedArray())
        }
    }

    fun disableSelection(sources: List<RssSource>) {
        execute {
            val list = arrayListOf<RssSource>()
            sources.forEach {
                list.add(it.copy(enabled = false))
            }
            App.db.rssSourceDao().update(*list.toTypedArray())
        }
    }

    fun delSelection(sources: List<RssSource>) {
        execute {
            App.db.rssSourceDao().delete(*sources.toTypedArray())
        }
    }

    fun exportSelection(sources: List<RssSource>, file: File) {
        execute {
            val json = GSON.toJson(sources)
            FileUtils.createFileIfNotExist(file, "exportRssSource.json")
                .writeText(json)
        }.onSuccess {
            context.toast("成功导出至\n${file.absolutePath}")
        }.onError {
            context.toast("导出失败\n${it.localizedMessage}")
        }
    }

    fun exportSelection(sources: List<RssSource>, doc: DocumentFile) {
        execute {
            val json = GSON.toJson(sources)
            doc.findFile("exportRssSource.json")?.delete()
            doc.createFile("", "exportRssSource.json")
                ?.writeText(context, json)
        }.onSuccess {
            context.toast("成功导出至\n${doc.uri.path}")
        }.onError {
            context.toast("导出失败\n${it.localizedMessage}")
        }
    }

    fun addGroup(group: String) {
        execute {
            val sources = App.db.rssSourceDao().noGroup
            sources.map { source ->
                source.sourceGroup = group
            }
            App.db.rssSourceDao().update(*sources.toTypedArray())
        }
    }

    fun upGroup(oldGroup: String, newGroup: String?) {
        execute {
            val sources = App.db.rssSourceDao().getByGroup(oldGroup)
            sources.map { source ->
                source.sourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                    it.remove(oldGroup)
                    if (!newGroup.isNullOrEmpty())
                        it.add(newGroup)
                    source.sourceGroup = TextUtils.join(",", it)
                }
            }
            App.db.rssSourceDao().update(*sources.toTypedArray())
        }
    }

    fun delGroup(group: String) {
        execute {
            execute {
                val sources = App.db.rssSourceDao().getByGroup(group)
                sources.map { source ->
                    source.sourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                        it.remove(group)
                        source.sourceGroup = TextUtils.join(",", it)
                    }
                }
                App.db.rssSourceDao().update(*sources.toTypedArray())
            }
        }
    }

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
            if (null != content) {
                GSON.fromJsonArray<RssSource>(content)?.let {
                    SourceHelp.insertRssSource(*it.toTypedArray())
                }
            }
        }.onSuccess {
            finally.invoke(context.getString(R.string.success))
        }
    }

    fun importSource(text: String, finally: (msg: String) -> Unit) {
        execute {
            val text1 = text.trim()
            when {
                text1.isJsonObject() -> {
                    val json = JsonPath.parse(text1)
                    val urls = json.read<List<String>>("$.sourceUrls")
                    var count = 0
                    if (!urls.isNullOrEmpty()) {
                        urls.forEach {
                            count += importSourceUrl(it)
                        }
                    } else {
                        GSON.fromJsonArray<RssSource>(text1)?.let {
                            SourceHelp.insertRssSource(*it.toTypedArray())
                            count = 1
                        }
                    }
                    "导入${count}条"
                }
                text1.isJsonArray() -> {
                    val rssSources = mutableListOf<RssSource>()
                    val items: List<Map<String, Any>> = jsonPath.parse(text1).read("$")
                    for (item in items) {
                        val jsonItem = jsonPath.parse(item)
                        GSON.fromJsonObject<RssSource>(jsonItem.jsonString())?.let {
                            rssSources.add(it)
                        }
                    }
                    SourceHelp.insertRssSource(*rssSources.toTypedArray())
                    "导入${rssSources.size}条"
                }
                text1.isAbsUrl() -> {
                    val count = importSourceUrl(text1)
                    "导入${count}条"
                }
                else -> "格式不对"
            }
        }.onError {
            finally(it.localizedMessage ?: "")
        }.onSuccess {
            finally(it)
        }
    }

    private fun importSourceUrl(url: String): Int {
        HttpHelper.simpleGet(url, "UTF-8")?.let { body ->
            val sources = mutableListOf<RssSource>()
            val items: List<Map<String, Any>> = jsonPath.parse(body).read("$")
            for (item in items) {
                val jsonItem = jsonPath.parse(item)
                GSON.fromJsonObject<RssSource>(jsonItem.jsonString())?.let { source ->
                    sources.add(source)
                }
            }
            SourceHelp.insertRssSource(*sources.toTypedArray())
            return sources.size
        }
        return 0
    }

}