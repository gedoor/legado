package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.EpubChapter

@Dao
interface EpubChapterDao {
    @get:Query("select * from epubChapters")
    val all: List<EpubChapter>

    @Query("select count(*) from epubChapters Where bookUrl = :bookUrl")
    fun getCnt(bookUrl: String): Int

    @Query("select * from epubChapters Where bookUrl = :bookUrl and parentHref = :parentHref ")
    fun get(bookUrl: String, parentHref: String): List<EpubChapter>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg chapter: EpubChapter)

    @Query("delete from epubChapters")
    fun clear()

    @Query("delete from epubChapters Where bookUrl = :bookUrl")
    fun deleteByName(bookUrl: String)

    @Delete
    fun delete(vararg chapter: EpubChapter)

    @Update
    fun update(vararg chapter: EpubChapter)

}