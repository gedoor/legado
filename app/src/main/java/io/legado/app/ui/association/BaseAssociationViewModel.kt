package io.legado.app.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.config.ThemeConfig
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.isJsonArray

abstract class BaseAssociationViewModel(application: Application) : BaseViewModel(application) {

    val successLive = MutableLiveData<Pair<String, String>>()
    val errorLive = MutableLiveData<String>()

    fun importJson(json: String) {
        //暂时根据文件内容判断属于什么
        when {
            json.contains("bookSourceUrl") ->
                successLive.postValue(Pair("bookSource", json))
            json.contains("sourceUrl") ->
                successLive.postValue(Pair("rssSource", json))
            json.contains("pattern") ->
                successLive.postValue(Pair("replaceRule", json))
            json.contains("themeName") ->
                successLive.postValue(Pair("theme", json))
            json.contains("name") && json.contains("rule") ->
                successLive.postValue(Pair("txtRule", json))
            json.contains("name") && json.contains("url") ->
                successLive.postValue(Pair("httpTts", json))
            else -> errorLive.postValue("格式不对")
        }
    }

    fun importTextTocRule(json: String, finally: (title: String, msg: String) -> Unit) {
        execute {
            if (json.isJsonArray()) {
                GSON.fromJsonArray<TxtTocRule>(json).getOrThrow()?.let {
                    appDb.txtTocRuleDao.insert(*it.toTypedArray())
                } ?: throw NoStackTraceException("格式不对")
            } else {
                GSON.fromJsonObject<TxtTocRule>(json).getOrThrow()?.let {
                    appDb.txtTocRuleDao.insert(it)
                } ?: throw NoStackTraceException("格式不对")
            }
        }.onSuccess {
            finally.invoke(context.getString(R.string.success), "导入Txt规则成功")
        }.onError {
            finally.invoke(
                context.getString(R.string.error),
                it.localizedMessage ?: context.getString(R.string.unknown_error)
            )
        }
    }

    fun importTheme(json: String, finally: (title: String, msg: String) -> Unit) {
        execute {
            if (json.isJsonArray()) {
                GSON.fromJsonArray<ThemeConfig.Config>(json).getOrThrow()?.forEach {
                    ThemeConfig.addConfig(it)
                }
            } else {
                GSON.fromJsonObject<ThemeConfig.Config>(json).getOrThrow()?.let {
                    ThemeConfig.addConfig(it)
                }
            }
        }.onSuccess {
            finally.invoke(context.getString(R.string.success), "导入主题成功")
        }.onError {
            finally.invoke(
                context.getString(R.string.error),
                it.localizedMessage ?: context.getString(R.string.unknown_error)
            )
        }
    }


}