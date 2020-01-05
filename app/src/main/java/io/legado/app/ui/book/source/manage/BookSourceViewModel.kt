package io.legado.app.ui.book.source.manage

import android.app.Application
import android.text.TextUtils
import com.jayway.jsonpath.JsonPath
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import io.legado.app.data.api.IHttpGetApi
import io.legado.app.data.entities.BookSource
import io.legado.app.help.FileHelp
import io.legado.app.help.http.HttpHelper
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.OldRule
import io.legado.app.help.storage.Restore.jsonPath
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.toast
import java.io.File

class BookSourceViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(bookSource: BookSource) {
        execute {
            bookSource.customOrder = App.db.bookSourceDao().minOrder - 1
            App.db.bookSourceDao().insert(bookSource)
        }
    }

    fun del(bookSource: BookSource) {
        execute { App.db.bookSourceDao().delete(bookSource) }
    }

    fun update(vararg bookSource: BookSource) {
        execute { App.db.bookSourceDao().update(*bookSource) }
    }

    fun upOrder() {
        execute {
            val sources = App.db.bookSourceDao().all
            for ((index: Int, source: BookSource) in sources.withIndex()) {
                source.customOrder = index + 1
            }
            App.db.bookSourceDao().update(*sources.toTypedArray())
        }
    }

    fun enableSelection(ids: LinkedHashSet<String>) {
        execute {
            ids.forEach {
                App.db.bookSourceDao().enableSection(it)
            }
        }
    }

    fun disableSelection(ids: LinkedHashSet<String>) {
        execute {
            ids.forEach {
                App.db.bookSourceDao().disableSection(it)
            }
        }
    }

    fun enableSelectExplore(ids: LinkedHashSet<String>) {
        execute {
            ids.forEach {
                App.db.bookSourceDao().enableSectionExplore(it)
            }
        }
    }

    fun disableSelectExplore(ids: LinkedHashSet<String>) {
        execute {
            ids.forEach {
                App.db.bookSourceDao().disableSectionExplore(it)
            }
        }
    }

    fun delSelection(ids: LinkedHashSet<String>) {
        execute {
            ids.forEach {
                App.db.bookSourceDao().delSection(it)
            }
        }
    }

    fun exportSelection(ids: LinkedHashSet<String>) {
        execute {
            ids.map {
                App.db.bookSourceDao().getBookSource(it)
            }.let {
                val json = GSON.toJson(it)
                val file =
                    FileHelp.getFile(Backup.exportPath + File.separator + "exportBookSource.json")
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
            val sources = App.db.bookSourceDao().noGroup
            sources.map { source ->
                source.bookSourceGroup = group
            }
            App.db.bookSourceDao().update(*sources.toTypedArray())
        }
    }

    fun upGroup(oldGroup: String, newGroup: String?) {
        execute {
            val sources = App.db.bookSourceDao().getByGroup(oldGroup)
            sources.map { source ->
                source.bookSourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                    it.remove(oldGroup)
                    if (!newGroup.isNullOrEmpty())
                        it.add(newGroup)
                    source.bookSourceGroup = TextUtils.join(",", it)
                }
            }
            App.db.bookSourceDao().update(*sources.toTypedArray())
        }
    }

    fun delGroup(group: String) {
        execute {
            execute {
                val sources = App.db.bookSourceDao().getByGroup(group)
                sources.map { source ->
                    source.bookSourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                        it.remove(group)
                        source.bookSourceGroup = TextUtils.join(",", it)
                    }
                }
                App.db.bookSourceDao().update(*sources.toTypedArray())
            }
        }
    }

    fun importSourceFromFilePath(path: String, finally: (msg: String) -> Unit) {
        execute {
            val file = File(path)
            if (file.exists()) {
                importSource(file.readText(), finally)
            } else {
                withContext(Dispatchers.Main) {
                    finally("打开文件出错")
                }
            }
        }.onError {
            finally(it.localizedMessage ?: "打开文件出错")
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
                        OldRule.jsonToBookSource(text1)?.let {
                            App.db.bookSourceDao().insert(it)
                            count = 1
                        }
                    }
                    "导入${count}条"
                }
                text1.isJsonArray() -> {
                    val bookSources = mutableListOf<BookSource>()
                    val items: List<Map<String, Any>> = jsonPath.parse(text1).read("$")
                    for (item in items) {
                        val jsonItem = jsonPath.parse(item)
                        OldRule.jsonToBookSource(jsonItem.jsonString())?.let {
                            bookSources.add(it)
                        }
                    }
                    App.db.bookSourceDao().insert(*bookSources.toTypedArray())
                    "导入${bookSources.size}条"
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
                val bookSources = mutableListOf<BookSource>()
                val items: List<Map<String, Any>> = jsonPath.parse(body).read("$")
                for (item in items) {
                    val jsonItem = jsonPath.parse(item)
                    OldRule.jsonToBookSource(jsonItem.jsonString())?.let { source ->
                        bookSources.add(source)
                    }
                }
                App.db.bookSourceDao().insert(*bookSources.toTypedArray())
                return bookSources.size
            }
        }
        return 0
    }
}