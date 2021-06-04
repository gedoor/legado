package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.legado.app.data.entities.IdealDetailEntity

@Dao
interface BookIdealDetailDao {


    /**
     * 根据想法id查询想法内容列表
     */
    @Query("SELECT * FROM book_ideal_list WHERE `idealId` = idealId")
    fun getIdealListById(idealId: Long): LiveData<List<IdealDetailEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertIDeal(vararg idealDetail: IdealDetailEntity)

}