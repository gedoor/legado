package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.*
import io.legado.app.utils.GSON
import kotlinx.parcelize.Parcelize

/**
 * 服务器
 */
@Parcelize
@TypeConverters(Server.Converters::class)
@Entity(tableName = "servers")
data class Server(
    @PrimaryKey
    var id: Long = System.currentTimeMillis(),
    var name: String = "",
    var type: TYPE = TYPE.WEBDAV,
    var config: Config? = null,
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

    @Parcelize
    data class Config(
        /* webdav */
        var username: String? = null,
        var password: String? = null,
    
    ): Parcelable
    
    class Converters {
        @TypeConverter
        fun configToString(config: Server.Config?): String = GSON.toJson(config)

        @TypeConverter
        fun stringToConfig(json: String?) = GSON.fromJsonObject<Server.Config>(json).getOrNull()
    }

}