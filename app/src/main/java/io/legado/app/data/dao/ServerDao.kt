package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.Server
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {

    @Query("select * from servers order by sortNumber")
    fun observeAll(): Flow<List<Server>>

    @get:Query("select * from servers order by sortNumber")
    val all: List<Server>

    @Query("select * from servers where id = :id")
    fun get(id: Long): Server?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg server: Server)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg server: Server)

    @Delete
    fun delete(vararg server: Server)

    @Query("delete from servers where id = :id")
    fun delete(id: Long)

    @Query("delete from servers where id < 0")
    fun deleteDefault()
}