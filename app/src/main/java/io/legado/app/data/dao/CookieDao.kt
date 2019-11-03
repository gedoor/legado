package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface CookieDao {

    @Query("select * from cookies where url = :url")
    fun get(url: String)


}