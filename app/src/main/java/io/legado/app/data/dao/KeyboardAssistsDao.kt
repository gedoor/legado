package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.KeyboardAssist

@Dao
interface KeyboardAssistsDao {

    @get:Query("select * from keyboardAssists")
    val all: List<KeyboardAssist>

    @Query("select * from keyboardAssists where type = :type")
    fun getByType(type: Int): List<KeyboardAssist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg keyboardAssist: KeyboardAssist)

    @Update
    fun update(vararg keyboardAssist: KeyboardAssist)

    @Delete
    fun delete(vararg keyboardAssist: KeyboardAssist)
}