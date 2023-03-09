package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * 服务器
 */
@Parcelize
@Entity(tableName = "servers")
data class Server(
    @PrimaryKey
    var id: Long = System.currentTimeMillis(),
    var name: String = "",
    var type: TYPE = TYPE.WEBDAV,
    var config: String? = null,
    var sortNumber: Int = 0
) : Parcelable {

    enum class TYPE {
        WEBDAV
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is Server) {
            return id == other.id
        }
        return false
    }

    fun getConfigJsonObject(): JSONObject? {
        val json = config
        json ?: return null
        return JSONObject(json)
    }

    fun getWebDavConfig(): WebDavConfig? {
        return if (type == TYPE.WEBDAV) GSON.fromJsonObject<WebDavConfig>(config).getOrNull() else null
    }

    @Parcelize
    data class WebDavConfig(
        var url: String,
        var username: String,
        var password: String
    ) : Parcelable

}