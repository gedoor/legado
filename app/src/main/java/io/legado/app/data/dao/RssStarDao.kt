package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.RssStar
import kotlinx.coroutines.flow.Flow

@Dao
interface RssStarDao {

    @get:Query("select * from rssStars order by starTime desc")
    val all: List<RssStar>

    @Query("select `group` from rssStars group by `group`")
    fun groupList(): List<String>

    @Query("select * from rssStars where `group` = :group order by starTime desc")
    fun getByGroup(group: String): Flow<List<RssStar>>

    @Query("update rssStars set `group` = '默认分组' where `group` is null or `group` = ''")
    fun updateGroup()

    @Query("select * from rssStars where origin = :origin and link = :link")
    fun get(origin: String, link: String): RssStar?

    @Query("select * from rssStars order by starTime desc")
    fun liveAll(): Flow<List<RssStar>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rssStar: RssStar)

    @Update
    fun update(vararg rssStar: RssStar)

    @Query("delete from rssStars where origin = :origin")
    fun delete(origin: String)

    @Query("delete from rssStars where origin = :origin and link = :link")
    fun delete(origin: String, link: String)
}