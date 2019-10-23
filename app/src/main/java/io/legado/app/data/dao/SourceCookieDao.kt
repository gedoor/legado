package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface SourceCookieDao {

    @Query("SELECT cookie FROM cookies Where url = :url")
    fun getCookieByUrl(url: String): String?

}