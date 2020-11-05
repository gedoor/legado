package io.legado.app.ui.association

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import com.jayway.jsonpath.JsonPath
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.BookSource
import io.legado.app.help.http.HttpHelper
import io.legado.app.help.storage.OldRule
import io.legado.app.help.storage.Restore
import io.legado.app.utils.*
import java.io.File

class ImportBookSourceViewModel(app: Application) : BaseViewModel(app) {

    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<BookSource>()
    val sourceCheckState = arrayListOf<Boolean>()
    val selectStatus = arrayListOf<Boolean>()

    fun importSourceFromFilePath(path: String) {
        execute {
            val content = if (path.isContentScheme()) {
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
                importSource(content)
            } else {
                errorLiveData.postValue(context.getString(R.string.error_read_file))
            }
        }.onError {
            it.printStackTrace()
            errorLiveData.postValue(context.getString(R.string.error_read_file))
        }
    }

    fun importSource(text: String) {
        execute {
            val text1 = text.trim()
            when {
                text1.isJsonObject() -> {
                    val json = JsonPath.parse(text1)
                    val urls = json.read<List<String>>("$.sourceUrls")
                    if (!urls.isNullOrEmpty()) {
                        urls.forEach {
                            importSourceUrl(it)
                        }
                    } else {
                        OldRule.jsonToBookSource(text1)?.let {
                            allSources.add(it)
                        }
                    }
                }
                text1.isJsonArray() -> {
                    val items: List<Map<String, Any>> = Restore.jsonPath.parse(text1).read("$")
                    for (item in items) {
                        val jsonItem = Restore.jsonPath.parse(item)
                        OldRule.jsonToBookSource(jsonItem.jsonString())?.let {
                            allSources.add(it)
                        }
                    }
                }
                text1.isAbsUrl() -> {
                    importSourceUrl(text1)
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

    private fun importSourceUrl(url: String) {
        HttpHelper.simpleGet(url, "UTF-8").let { body ->
            if (body == null) {
                throw Exception(context.getString(R.string.error_get_data))
            }
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
                val has = App.db.bookSourceDao().getBookSource(it.bookSourceUrl) != null
                sourceCheckState.add(has)
                selectStatus.add(!has)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}