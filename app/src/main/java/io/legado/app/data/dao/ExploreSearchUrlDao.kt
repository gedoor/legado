package io.legado.app.data.dao

import androidx.paging.DataSource
import androidx.room.*
import io.legado.app.data.entities.ExploreSearchUrl


@Dao
interface ExploreSearchUrlDao {

    companion object {
        private const val ORDER_DEFAULT = "ORDER BY sourceId ASC, defOrder ASC"
        private const val ORDER_USAGE = "ORDER BY usage DESC, lastUseTime DESC"
        private const val ORDER_TIME = "ORDER BY lastUseTime DESC"
        private const val QUERY_NAME = "name LIKE '%' || :name || '%'"
        private const val QUERY_ENABLED_EXPLORE = "WHERE type = 0 AND isEnabled = 1"
    }

    // 用于发现列表，默认排序
    @Query("SELECT * FROM explore_search_urls $QUERY_ENABLED_EXPLORE $ORDER_DEFAULT")
    fun observeExploreUrls(): DataSource.Factory<Int, ExploreSearchUrl>

    // 用于发现列表，按使用次数排序
    @Query("SELECT * FROM explore_search_urls $QUERY_ENABLED_EXPLORE $ORDER_USAGE")
    fun observeExploreUrlsByUsage(): DataSource.Factory<Int, ExploreSearchUrl>

    // 用于发现列表，按使用时间排序
    @Query("SELECT * FROM explore_search_urls $QUERY_ENABLED_EXPLORE $ORDER_TIME")
    fun observeExploreUrlsByTime(): DataSource.Factory<Int, ExploreSearchUrl>

    // 用于搜索时的发现列表，默认排序
    @Query("SELECT * FROM explore_search_urls $QUERY_ENABLED_EXPLORE AND $QUERY_NAME $ORDER_DEFAULT")
    fun observeFilteredExploreUrls(name: String): DataSource.Factory<Int, ExploreSearchUrl>

    // 用于搜索时的发现列表，按使用次数排序
    @Query("SELECT * FROM explore_search_urls $QUERY_ENABLED_EXPLORE AND $QUERY_NAME $ORDER_USAGE")
    fun observeFilteredExploreUrlsByUsage(): DataSource.Factory<Int, ExploreSearchUrl>

    // 用于搜索时的发现列表，按使用时间排序
    @Query("SELECT * FROM explore_search_urls $QUERY_ENABLED_EXPLORE AND $QUERY_NAME $ORDER_TIME")
    fun observeFilteredExploreUrlsByTime(): DataSource.Factory<Int, ExploreSearchUrl>

    // 获取特定书源的发现
    @Query("SELECT * FROM explore_search_urls $QUERY_ENABLED_EXPLORE AND sourceId = :sourceId")
    fun findExploreUrlsBySourceId(sourceId: Int): List<ExploreSearchUrl>

    // 获取特定书源的搜索链接
    @Query("SELECT * FROM explore_search_urls WHERE type = 1 AND sourceId = :sourceId")
    fun findSearchUrlsBySourceId(sourceId: Int): List<ExploreSearchUrl>

    // 所有的搜索链接
    @get:Query("SELECT * FROM explore_search_urls WHERE type = 1")
    val allSearchUrls: List<ExploreSearchUrl>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg keywords: ExploreSearchUrl)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(keyword: ExploreSearchUrl): Long

    @Update
    fun update(vararg keywords: ExploreSearchUrl)

    @Delete
    fun delete(vararg keywords: ExploreSearchUrl)

    // 批量删除特定书源的发现和搜索链接，一般用于更新书源时
    @Query("DELETE FROM explore_search_urls WHERE sourceId = :sourceId")
    fun deleteBySourceId(sourceId: Int)

}