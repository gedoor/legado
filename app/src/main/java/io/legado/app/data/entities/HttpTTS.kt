package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.jayway.jsonpath.DocumentContext
import io.legado.app.utils.GSON
import io.legado.app.utils.jsonPath
import io.legado.app.utils.readLong
import io.legado.app.utils.readString

/**
 * 在线朗读引擎
 */
@Entity(tableName = "httpTTS")
data class HttpTTS(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    var name: String = "",
    var url: String = "",
    var contentType: String? = null,
    override var concurrentRate: String? = "0",
    override var loginUrl: String? = null,
    override var loginUi: String? = null,
    override var header: String? = null,
    var loginCheckJs: String? = null,
) : BaseSource {

    @Ignore
    constructor() : this(id = System.currentTimeMillis())

    override fun getTag(): String {
        return name
    }

    override fun getKey(): String {
        return "httpTts:$id"
    }

    override fun getSource(): BaseSource {
        return this
    }

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {

        fun fromJsonDoc(doc: DocumentContext): HttpTTS? {
            return kotlin.runCatching {
                val loginUi = doc.read<Any>("$.loginUi")
                HttpTTS(
                    id = doc.readLong("$.id") ?: System.currentTimeMillis(),
                    name = doc.readString("$.name")!!,
                    url = doc.readString("$.url")!!,
                    contentType = doc.readString("$.contentType"),
                    concurrentRate = doc.readString("$.concurrentRate"),
                    loginUrl = doc.readString("$.loginUrl"),
                    loginUi = if (loginUi is List<*>) GSON.toJson(loginUi) else loginUi?.toString(),
                    header = doc.readString("$.header"),
                    loginCheckJs = doc.readString("$.loginCheckJs")
                )
            }.getOrNull()
        }

        fun fromJson(json: String): HttpTTS? {
            return fromJsonDoc(jsonPath.parse(json))
        }

        fun fromJsonArray(jsonArray: String): ArrayList<HttpTTS> {
            val sources = arrayListOf<HttpTTS>()
            val doc = jsonPath.parse(jsonArray).read<List<*>>("$")
            doc.forEach {
                val jsonItem = jsonPath.parse(it)
                fromJsonDoc(jsonItem)?.let { source ->
                    sources.add(source)
                }
            }
            return sources
        }

    }

}