package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.RssReadRecord

@Dao
interface RssReadRecordDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertRecord(vararg rssReadRecord: RssReadRecord)

    @Query("select * from rssReadRecords order by readTime desc")
    fun getRecords(): List<RssReadRecord>

    @get:Query("select count(1) from rssReadRecords")
    val countRecords: Int

    @Query("delete from rssReadRecords")
    fun deleteAllRecord()

}