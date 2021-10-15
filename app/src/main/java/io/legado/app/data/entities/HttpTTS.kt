package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 在线朗读引擎
 */
@Entity(tableName = "httpTTS")
data class HttpTTS(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    var name: String = "",
    var url: String = "",
    override var concurrentRate: String? = null,
    override var loginUrl: String? = null,
    override var loginUi: String? = null,
    override var header: String? = null,
    var loginCheckJs: String? = null,
) : BaseSource {

    override fun getTag(): String {
        return name
    }

    override fun getKey(): String {
        return "httpTts:$id"
    }

    override fun getSource(): BaseSource {
        return this
    }

}