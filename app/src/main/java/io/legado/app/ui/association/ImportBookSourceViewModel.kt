package io.legado.app.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.jayway.jsonpath.JsonPath
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.help.AppConfig
import io.legado.app.help.SourceHelp
import io.legado.app.help.storage.OldRule
import io.legado.app.help.storage.Restore
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.isJsonArray
import io.legado.app.utils.isJsonObject
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toText

class ImportBookSourceViewModel(app: Application) : BaseViewModel(app) {
    var groupName: String? = null
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<BookSource>()
    val checkSources = arrayListOf<BookSource?>()
    val selectStatus = arrayListOf<Boolean>()

    fun isSelectAll(): Boolean {
        selectStatus.forEach {
            if (!it) {
                return false
            }
        }
        return true
    }

    fun selectCount(): Int {
        var count = 0
        selectStatus.forEach {
            if (it) {
                count++
            }
        }
        return count
    }

    fun importSelect(finally: () -> Unit) {
        execute {
            val keepName = AppConfig.importKeepName
            val selectSource = arrayListOf<BookSource>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    val source = allSources[index]
                    if (groupName != null) {
                        source.bookSourceGroup = groupName
                    }
                    if (keepName) {
                        checkSources[index]?.let {
                            source.bookSourceName = it.bookSourceName
                            source.bookSourceGroup = it.bookSourceGroup
                            source.customOrder = it.customOrder
                        }
                    }
                    selectSource.add(source)
                }
            }
            SourceHelp.insertBookSource(*selectSource.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            val mText = text.trim()
            when {
                mText.isJsonObject() -> {
                    val json = JsonPath.parse(mText)
                    val urls = json.read<List<String>>("$.sourceUrls")
                    if (!urls.isNullOrEmpty()) {
                        urls.forEach {
                            importSourceUrl(it)
                        }
                    } else {
                        OldRule.jsonToBookSource(mText)?.let {
                            allSources.add(it)
                        }
                    }
                }
                mText.isJsonArray() -> {
                    val items: List<Map<String, Any>> = Restore.jsonPath.parse(mText).read("$")
                    for (item in items) {
                        val jsonItem = Restore.jsonPath.parse(item)
                        OldRule.jsonToBookSource(jsonItem.jsonString())?.let {
                            allSources.add(it)
                        }
                    }
                }
                mText.isAbsUrl() -> {
                    importSourceUrl(mText)
                }
                else -> throw Exception(context.getString(R.string.wrong_format))
            }
        }.onError {
            it.printStackTrace()
            errorLiveData.postValue(it.localizedMessage ?: "")
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceUrl(url: String) {
        RxHttp.get(url).toText("utf-8").await().let { body ->
            val items: List<Map<String, Any>> = Restore.jsonPath.parse(body).read("$")
            for (item in items) {
                val jsonItem = Restore.jsonPath.parse(item)
                OldRule.jsonToBookSource(jsonItem.jsonString())?.let { source ->
                    allSources.add(source)
                }
            }
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val source = appDb.bookSourceDao.getBookSource(it.bookSourceUrl)
                checkSources.add(source)
                selectStatus.add(source == null || source.lastUpdateTime < it.lastUpdateTime)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}