package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.Cookie

@Dao
interface CookieDao {

    @Query("SELECT * FROM cookies Where url = :url")
    fun get(url: String): Cookie?

    @Query("select * from cookies where url like '%|%'")
    fun getOkHttpCookies(): List<Cookie>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg cookie: Cookie)

    @Update
    fun update(vararg cookie: Cookie)

    @Query("delete from cookies where url = :url")
    fun delete(url: String)

    @Query("delete from cookies where url like '%|%'")
    fun deleteOkHttp()
}