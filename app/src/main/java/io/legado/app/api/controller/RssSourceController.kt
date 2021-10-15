package io.legado.app.api.controller


import android.text.TextUtils
import io.legado.app.api.ReturnData
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssSource
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.msg

object RssSourceController {

    val sources: ReturnData
        get() {
            val source = appDb.rssSourceDao.all
            val returnData = ReturnData()
            return if (source.isEmpty()) {
                returnData.setErrorMsg("源列表为空")
            } else returnData.setData(source)
        }

    fun saveSource(postData: String?): ReturnData {
        val returnData = ReturnData()
        postData ?: return returnData.setErrorMsg("数据不能为空")
        kotlin.runCatching {
            val source = GSON.fromJsonObject<RssSource>(postData)
            if (source != null) {
                if (TextUtils.isEmpty(source.sourceName) || TextUtils.isEmpty(source.sourceUrl)) {
                    returnData.setErrorMsg("源名称和URL不能为空")
                } else {
                    appDb.rssSourceDao.insert(source)
                    returnData.setData("")
                }
            } else {
                returnData.setErrorMsg("转换源失败")
            }
        }.onFailure {
            returnData.setErrorMsg(it.msg)
        }
        return returnData
    }

    fun saveSources(postData: String?): ReturnData {
        val okSources = arrayListOf<RssSource>()
        val source = GSON.fromJsonArray<RssSource>(postData)
        if (source != null) {
            for (rssSource in source) {
                if (rssSource.sourceName.isBlank() || rssSource.sourceUrl.isBlank()) {
                    continue
                }
                appDb.rssSourceDao.insert(rssSource)
                okSources.add(rssSource)
            }
        } else {
            return ReturnData().setErrorMsg("转换源失败")
        }
        return ReturnData().setData(okSources)
    }

    fun getSource(parameters: Map<String, List<String>>): ReturnData {
        val url = parameters["url"]?.firstOrNull()
        val returnData = ReturnData()
        if (url.isNullOrEmpty()) {
            return returnData.setErrorMsg("参数url不能为空，请指定书源地址")
        }
        val source = appDb.rssSourceDao.getByKey(url)
            ?: return returnData.setErrorMsg("未找到源，请检查源地址")
        return returnData.setData(source)
    }

    fun deleteSources(postData: String?): ReturnData {
        kotlin.runCatching {
            GSON.fromJsonArray<RssSource>(postData)?.let {
                it.forEach { source ->
                    appDb.rssSourceDao.delete(source)
                }
            }
        }
        return ReturnData().setData("已执行"/*okSources*/)
    }
}
