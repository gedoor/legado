package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.legado.app.data.entities.Cache

@Dao
interface CacheDao {

    @Query("select * from caches where `key` = :key")
    fun get(key: String): Cache?

    @Query("select value from caches where `key` = :key and (deadline = 0 or deadline > :now)")
    fun get(key: String, now: Long): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg cache: Cache)

    @Query("delete from caches where `key` = :key")
    fun delete(key: String)

    @Query("delete from caches where deadline > 0 and deadline < :now")
    fun clearDeadline(now: Long)

}