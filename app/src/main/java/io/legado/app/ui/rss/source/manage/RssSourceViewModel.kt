package io.legado.app.ui.rss.source.manage

import android.app.Application
import android.content.Intent
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppConst
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssSource
import io.legado.app.help.DefaultData
import io.legado.app.utils.*
import java.io.File

class RssSourceViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(vararg sources: RssSource) {
        execute {
            val minOrder = appDb.rssSourceDao.minOrder - 1
            sources.forEachIndexed { index, rssSource ->
                rssSource.customOrder = minOrder - index
            }
            appDb.rssSourceDao.update(*sources)
        }
    }

    fun bottomSource(vararg sources: RssSource) {
        execute {
            val maxOrder = appDb.rssSourceDao.maxOrder + 1
            sources.forEachIndexed { index, rssSource ->
                rssSource.customOrder = maxOrder + index
            }
            appDb.rssSourceDao.update(*sources)
        }
    }

    fun del(rssSource: RssSource) {
        execute { appDb.rssSourceDao.delete(rssSource) }
    }

    fun update(vararg rssSource: RssSource) {
        execute { appDb.rssSourceDao.update(*rssSource) }
    }

    fun upOrder() {
        execute {
            val sources = appDb.rssSourceDao.all
            for ((index: Int, source: RssSource) in sources.withIndex()) {
                source.customOrder = index + 1
            }
            appDb.rssSourceDao.update(*sources.toTypedArray())
        }
    }

    fun enableSelection(sources: List<RssSource>) {
        execute {
            val list = arrayListOf<RssSource>()
            sources.forEach {
                list.add(it.copy(enabled = true))
            }
            appDb.rssSourceDao.update(*list.toTypedArray())
        }
    }

    fun disableSelection(sources: List<RssSource>) {
        execute {
            val list = arrayListOf<RssSource>()
            sources.forEach {
                list.add(it.copy(enabled = false))
            }
            appDb.rssSourceDao.update(*list.toTypedArray())
        }
    }

    fun delSelection(sources: List<RssSource>) {
        execute {
            appDb.rssSourceDao.delete(*sources.toTypedArray())
        }
    }

    fun exportSelection(sources: List<RssSource>, file: File) {
        execute {
            val json = GSON.toJson(sources)
            FileUtils.createFileIfNotExist(file, "exportRssSource.json")
                .writeText(json)
        }.onSuccess {
            context.toastOnUi("成功导出至\n${file.absolutePath}")
        }.onError {
            context.toastOnUi("导出失败\n${it.localizedMessage}")
        }
    }

    fun exportSelection(sources: List<RssSource>, doc: DocumentFile) {
        execute {
            val json = GSON.toJson(sources)
            doc.findFile("exportRssSource.json")?.delete()
            doc.createFile("", "exportRssSource.json")
                ?.writeText(context, json)
        }.onSuccess {
            context.toastOnUi("成功导出至\n${doc.uri.path}")
        }.onError {
            context.toastOnUi("导出失败\n${it.localizedMessage}")
        }
    }

    fun shareSelection(sources: List<RssSource>, success: ((intent: Intent) -> Unit)) {
        execute {
            val tmpSharePath = "${context.filesDir}/shareRssSource.json"
            FileUtils.delete(tmpSharePath)
            val intent = Intent(Intent.ACTION_SEND)
            val file = FileUtils.createFileWithReplace(tmpSharePath)
            file.writeText(GSON.toJson(sources))
            val fileUri = FileProvider.getUriForFile(context, AppConst.authority, file)
            intent.type = "text/*"
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent
        }.onSuccess {
            success.invoke(it)
        }.onError {
            toastOnUi(it.msg)
        }
    }

    fun addGroup(group: String) {
        execute {
            val sources = appDb.rssSourceDao.noGroup
            sources.map { source ->
                source.sourceGroup = group
            }
            appDb.rssSourceDao.update(*sources.toTypedArray())
        }
    }

    fun upGroup(oldGroup: String, newGroup: String?) {
        execute {
            val sources = appDb.rssSourceDao.getByGroup(oldGroup)
            sources.map { source ->
                source.sourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                    it.remove(oldGroup)
                    if (!newGroup.isNullOrEmpty())
                        it.add(newGroup)
                    source.sourceGroup = TextUtils.join(",", it)
                }
            }
            appDb.rssSourceDao.update(*sources.toTypedArray())
        }
    }

    fun delGroup(group: String) {
        execute {
            execute {
                val sources = appDb.rssSourceDao.getByGroup(group)
                sources.map { source ->
                    source.sourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                        it.remove(group)
                        source.sourceGroup = TextUtils.join(",", it)
                    }
                }
                appDb.rssSourceDao.update(*sources.toTypedArray())
            }
        }
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultRssSources()
        }
    }

}