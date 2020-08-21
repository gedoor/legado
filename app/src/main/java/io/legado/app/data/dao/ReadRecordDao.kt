package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.ReadRecord

@Dao
interface ReadRecordDao {

    @get:Query("select * from readRecord")
    val all: List<ReadRecord>

    @Query("select readTime from readRecord where bookName = :bookName")
    fun getReadTime(bookName: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg readRecord: ReadRecord)

    @Update
    fun update(vararg record: ReadRecord)

}