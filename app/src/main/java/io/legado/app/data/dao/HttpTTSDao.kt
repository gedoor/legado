package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.HttpTTS
import kotlinx.coroutines.flow.Flow

@Dao
interface HttpTTSDao {

    @get:Query("select * from httpTTS order by name")
    val all: List<HttpTTS>

    @Query("select * from httpTTS order by name")
    fun flowAll(): Flow<List<HttpTTS>>

    @get:Query("select count(*) from httpTTS")
    val count: Int

    @Query("select * from httpTTS where id = :id")
    fun get(id: Long): HttpTTS?

    @Query("select name from httpTTS where id = :id")
    fun getName(id: Long): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg httpTTS: HttpTTS)

    @Delete
    fun delete(vararg httpTTS: HttpTTS)

    @Update
    fun update(vararg httpTTS: HttpTTS)

    @Query("delete from httpTTS where id < 0")
    fun deleteDefault()
}