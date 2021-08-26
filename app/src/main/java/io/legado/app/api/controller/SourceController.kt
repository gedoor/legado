package io.legado.app.api.controller


import android.text.TextUtils
import io.legado.app.api.ReturnData
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.help.BookSourceAnalyzer
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.msg

object SourceController {

    val sources: ReturnData
        get() {
            val bookSources = appDb.bookSourceDao.all
            val returnData = ReturnData()
            return if (bookSources.isEmpty()) {
                returnData.setErrorMsg("设备书源列表为空")
            } else returnData.setData(bookSources)
        }

    fun saveSource(postData: String?): ReturnData {
        val returnData = ReturnData()
        postData ?: return returnData.setErrorMsg("数据不能为空")
        kotlin.runCatching {
            val bookSource = BookSourceAnalyzer.jsonToBookSource(postData)
            if (bookSource != null) {
                if (TextUtils.isEmpty(bookSource.bookSourceName) || TextUtils.isEmpty(bookSource.bookSourceUrl)) {
                    returnData.setErrorMsg("书源名称和URL不能为空")
                } else {
                    appDb.bookSourceDao.insert(bookSource)
                    returnData.setData("")
                }
            } else {
                returnData.setErrorMsg("转换书源失败")
            }
        }.onFailure {
            returnData.setErrorMsg(it.msg)
        }
        return returnData
    }

    fun saveSources(postData: String?): ReturnData {
        val okSources = arrayListOf<BookSource>()
        kotlin.runCatching {
            val bookSources = GSON.fromJsonArray<BookSource>(postData)
            if (bookSources != null) {
                for (bookSource in bookSources) {
                    if (bookSource.bookSourceName.isBlank() || bookSource.bookSourceUrl.isBlank()) {
                        continue
                    }
                    appDb.bookSourceDao.insert(bookSource)
                    okSources.add(bookSource)
                }
            }
        }
        return ReturnData().setData(okSources)
    }

    fun getSource(parameters: Map<String, List<String>>): ReturnData {
        val url = parameters["url"]?.firstOrNull()
        val returnData = ReturnData()
        if (url.isNullOrEmpty()) {
            return returnData.setErrorMsg("参数url不能为空，请指定书源地址")
        }
        val bookSource = appDb.bookSourceDao.getBookSource(url)
            ?: return returnData.setErrorMsg("未找到书源，请检查书源地址")
        return returnData.setData(bookSource)
    }

    fun deleteSources(postData: String?): ReturnData {
        kotlin.runCatching {
            GSON.fromJsonArray<BookSource>(postData)?.let {
                it.forEach { source ->
                    appDb.bookSourceDao.delete(source)
                }
            }
        }
        return ReturnData().setData("已执行"/*okSources*/)
    }
}
