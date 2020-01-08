package io.legado.app.ui.rss.source.manage

import android.app.Application
import android.text.TextUtils
import com.jayway.jsonpath.JsonPath
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.api.IHttpGetApi
import io.legado.app.data.entities.RssSource
import io.legado.app.help.FileHelp
import io.legado.app.help.http.HttpHelper
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.Restore.jsonPath
import io.legado.app.utils.*
import org.jetbrains.anko.toast
import java.io.File

class RssSourceViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(rssSource: RssSource) {
        execute {
            rssSource.customOrder = App.db.rssSourceDao().minOrder - 1
            App.db.rssSourceDao().insert(rssSource)
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

    fun enableSelection(ids: LinkedHashSet<String>) {
        execute {
            App.db.rssSourceDao().enableSection(*ids.toTypedArray())
        }
    }

    fun disableSelection(ids: LinkedHashSet<String>) {
        execute {
            App.db.rssSourceDao().disableSection(*ids.toTypedArray())
        }
    }

    fun delSelection(ids: LinkedHashSet<String>) {
        execute {
            App.db.rssSourceDao().delSection(*ids.toTypedArray())
        }
    }

    fun exportSelection(ids: LinkedHashSet<String>) {
        execute {
            App.db.rssSourceDao().getRssSources(*ids.toTypedArray()).let {
                val json = GSON.toJson(it)
                val file =
                    FileHelp.getFile(Backup.exportPath + File.separator + "exportRssSource.json")
                file.writeText(json)
            }
        }.onSuccess {
            context.toast("成功导出至\n${Backup.exportPath}")
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
            val file = File(path)
            if (file.exists()) {
                GSON.fromJsonArray<RssSource>(file.readText())?.let {
                    App.db.rssSourceDao().insert(*it.toTypedArray())
                }
            }
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
                            App.db.rssSourceDao().insert(*it.toTypedArray())
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
                    App.db.rssSourceDao().insert(*rssSources.toTypedArray())
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
            finally(it ?: "导入完成")
        }
    }

    private fun importSourceUrl(url: String): Int {
        NetworkUtils.getBaseUrl(url)?.let {
            val response = HttpHelper.getApiService<IHttpGetApi>(it).get(url, mapOf()).execute()
            response.body()?.let { body ->
                val sources = mutableListOf<RssSource>()
                val items: List<Map<String, Any>> = jsonPath.parse(body).read("$")
                for (item in items) {
                    val jsonItem = jsonPath.parse(item)
                    GSON.fromJsonObject<RssSource>(jsonItem.jsonString())?.let { source ->
                        sources.add(source)
                    }
                }
                App.db.rssSourceDao().insert(*sources.toTypedArray())
                return sources.size
            }
        }
        return 0
    }
}