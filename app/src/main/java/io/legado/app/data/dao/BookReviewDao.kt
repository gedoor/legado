package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.legado.app.data.entities.BookReview

@Dao
interface BookReviewDao {

    @Query("select * from reviews")
    fun getReviewCountAllList(): List<BookReview>

    @Query("select * from reviews where chapterUrl = :chapterUrl")
    fun getReviewCountList(chapterUrl: String): List<BookReview>

    @Query("select * from reviews where reviewCountUrl = :reviewCountUrl and `reviewSegmentId` = :reviewSegmentId")
    fun getReview(reviewCountUrl: String, reviewSegmentId: String): BookReview?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bookReview: BookReview)

    @Update
    fun update(vararg bookReview: BookReview)

    @Query("delete from reviews where reviewCountUrl = :reviewCountUrl")
    fun delByUrl(reviewCountUrl: String)
}