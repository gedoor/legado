package io.legado.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import io.legado.app.data.entities.rule.RowUi
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray

@TypeConverters(HttpTTS.Converters::class)
@Entity(tableName = "httpTTS")
data class HttpTTS(
    @PrimaryKey
    val id: Long = System.currentTimeMillis(),
    var name: String = "",
    var url: String = "",
    override var concurrentRate: String? = null,
    override var loginUrl: String? = null,
    override var loginUi: List<RowUi>? = null,
    override var header: String? = null,
    var loginCheckJs: String? = null,
) : BaseSource {

    override fun getTag(): String {
        return name
    }

    override fun getKey(): String {
        return md5Encode(url)
    }

    override fun getSource(): BaseSource {
        return this
    }

    class Converters {

        @TypeConverter
        fun loginUiRuleToString(loginUi: List<RowUi>?): String = GSON.toJson(loginUi)

        @TypeConverter
        fun stringToLoginRule(json: String?): List<RowUi>? = GSON.fromJsonArray(json)

    }
}