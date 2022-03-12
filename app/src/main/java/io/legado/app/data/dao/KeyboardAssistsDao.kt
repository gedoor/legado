package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.KeyboardAssist
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyboardAssistsDao {

    @get:Query("select * from keyboardAssists order by serialNo")
    val all: List<KeyboardAssist>

    @Query("select * from keyboardAssists where type = :type order by serialNo")
    fun getByType(type: Int): List<KeyboardAssist>

    @get:Query("select * from keyboardAssists order by serialNo")
    val flowAll: Flow<List<KeyboardAssist>>

    @Query("select * from keyboardAssists where type = :type order by serialNo")
    fun flowByType(type: Int): Flow<List<KeyboardAssist>>

    @get:Query("select max(serialNo) from keyboardAssists order by serialNo")
    val maxSerialNo: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg keyboardAssist: KeyboardAssist)

    @Update
    fun update(vararg keyboardAssist: KeyboardAssist)

    @Delete
    fun delete(vararg keyboardAssist: KeyboardAssist)

}