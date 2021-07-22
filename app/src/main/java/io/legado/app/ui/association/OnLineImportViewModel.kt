package io.legado.app.ui.association

import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.HttpTTS
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.ThemeConfig
import io.legado.app.help.http.newCall
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.text
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.isJsonArray

class OnLineImportViewModel(app: App) : BaseViewModel(app) {

    fun importTextTocRule(url: String, finally: (title: String, msg: String) -> Unit) {
        execute {
            okHttpClient.newCall {
                url(url)
            }.text("utf-8").let { json ->
                if (json.isJsonArray()) {
                    GSON.fromJsonArray<TxtTocRule>(json)?.let {
                        appDb.txtTocRuleDao.insert(*it.toTypedArray())
                    } ?: throw Exception("格式不对")
                } else {
                    GSON.fromJsonObject<TxtTocRule>(json)?.let {
                        appDb.txtTocRuleDao.insert(it)
                    } ?: throw Exception("格式不对")
                }
            }
        }.onSuccess {
            finally.invoke(context.getString(R.string.success), "导入Txt规则成功")
        }.onError {
            finally.invoke(context.getString(R.string.error), it.localizedMessage ?: "未知错误")
        }
    }

    fun importHttpTTS(url: String, finally: (title: String, msg: String) -> Unit) {
        execute {
            okHttpClient.newCall {
                url(url)
            }.text("utf-8").let { json ->
                if (json.isJsonArray()) {
                    GSON.fromJsonArray<HttpTTS>(json)?.let {
                        appDb.httpTTSDao.insert(*it.toTypedArray())
                        return@execute it.size
                    } ?: throw Exception("格式不对")
                } else {
                    GSON.fromJsonObject<HttpTTS>(json)?.let {
                        appDb.httpTTSDao.insert(it)
                        return@execute 1
                    } ?: throw Exception("格式不对")
                }
            }
        }.onSuccess {
            finally.invoke(context.getString(R.string.success), "导入${it}朗读引擎")
        }.onError {
            finally.invoke(context.getString(R.string.error), it.localizedMessage ?: "未知错误")
        }
    }

    fun importTheme(url: String, finally: (title: String, msg: String) -> Unit) {
        execute {
            okHttpClient.newCall {
                url(url)
            }.text("utf-8").let { json ->
                if (json.isJsonArray()) {
                    GSON.fromJsonArray<ThemeConfig.Config>(json)?.forEach {
                        ThemeConfig.addConfig(it)
                    } ?: throw Exception("格式不对")
                } else {
                    GSON.fromJsonObject<ThemeConfig.Config>(json)?.let {
                        ThemeConfig.addConfig(it)
                    } ?: throw Exception("格式不对")
                }
            }
        }.onSuccess {
            finally.invoke(context.getString(R.string.success), "导入主题成功")
        }.onError {
            finally.invoke(context.getString(R.string.error), it.localizedMessage ?: "未知错误")
        }
    }

    fun importReadConfig(url: String, finally: (title: String, msg: String) -> Unit) {
        execute {
            @Suppress("BlockingMethodInNonBlockingContext")
            val bytes = okHttpClient.newCall {
                url(url)
            }.bytes()
            val config = ReadBookConfig.import(bytes)
            ReadBookConfig.configList.forEachIndexed { index, c ->
                if (c.name == config.name) {
                    ReadBookConfig.configList[index] = config
                    return@execute config.name
                }
                ReadBookConfig.configList.add(config)
                return@execute config.name
            }
        }.onSuccess {
            finally.invoke(context.getString(R.string.success), "导入排版成功")
        }.onError {
            finally.invoke(context.getString(R.string.error), it.localizedMessage ?: "未知错误")
        }
    }

}