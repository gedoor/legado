package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.parcelize.Parcelize

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
): Parcelable {

    enum class TYPE {
        WEBDAV, ALIYUN, GOOGLEYUN
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

    fun getWebDavConfig(): WebDavConfig? {
        return GSON.fromJsonObject<WebDavConfig>(config).getOrNull()
    }

    @Parcelize
    data class WebDavConfig(
        var url: String? = null,
        var username: String? = null,
        var password: String? = null,
    ) : Parcelable

}