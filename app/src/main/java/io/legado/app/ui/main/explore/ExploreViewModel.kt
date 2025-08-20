package io.legado.app.ui.main.explore

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.BookSourceType
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.config.SourceConfig
import io.legado.app.help.source.SourceHelp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefString

class ExploreViewModel(application: Application) : BaseViewModel(application) {

    // 新增：当前选中的书源类型
    val selectedSourceType = MutableLiveData<Int>(BookSourceType.default)
    
    // 新增：当前选中的书源
    val currentBookSource = MutableLiveData<BookSource?>(null)
    
    // 新增：当前选中的分组
    val selectedGroup = MutableLiveData<String?>(null)
    
    // 新增：当前选中的分类
    val selectedCategory = MutableLiveData<String?>(null)
    // 新增：是否已自动选择过默认书源
    private var hasAutoSelectedDefault = false

    
    // 新增：书源类型列表 - 在Fragment中动态获取字符串资源
    val sourceTypes = listOf(
        BookSourceType.default to "小说",
        BookSourceType.audio to "音频", 
        BookSourceType.image to "漫画",
        BookSourceType.file to "文件"
    )

    fun topSource(bookSource: BookSourcePart) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            bookSource.customOrder = minXh - 1
            appDb.bookSourceDao.upOrder(bookSource)
        }
    }

    fun deleteSource(source: BookSourcePart) {
        execute {
            SourceHelp.deleteBookSource(source.bookSourceUrl)
        }
    }
    
    // 新增：设置书源类型
    fun setSourceType(type: Int) {
        selectedSourceType.value = type
        // 重置其他筛选条件
        currentBookSource.value = null
        selectedGroup.value = null
        selectedCategory.value = null
        hasAutoSelectedDefault = false
    }
    
    // 新增：设置当前书源
    fun setCurrentBookSource(bookSource: BookSource?) {
        currentBookSource.value = bookSource
        // 重置分类筛选
        selectedCategory.value = null
        // 记住最近一次选择（按类型存储）
        if (bookSource != null) {
            val type = selectedSourceType.value ?: BookSourceType.default
            val key = "explore_last_source_$type"
            appCtx.putPrefString(key, bookSource.bookSourceUrl)
        }
    }
    
    // 新增：设置分组
    fun setGroup(group: String?) {
        selectedGroup.value = group
        // 重置分类筛选
        selectedCategory.value = null
    }
    
    // 新增：设置分类
    fun setCategory(category: String?) {
        selectedCategory.value = category
    }
    
    // 新增：获取筛选后的书源列表
    suspend fun getFilteredBookSources(): List<BookSourcePart> = withContext(Dispatchers.IO) {
        val type = selectedSourceType.value ?: BookSourceType.default
        val group = selectedGroup.value
        val source = currentBookSource.value

        return@withContext when {
            source != null -> listOf(source.toBookSourcePart())
            group != null -> appDb.bookSourceDao.getEnabledPartByGroup(group)
                .filter { it.hasExploreUrl && it.enabledExplore }
            else -> appDb.bookSourceDao.getEnabledByType(type)
                .map { it.toBookSourcePart() }
                .filter { it.hasExploreUrl && it.enabledExplore }
        }
    }

    // 新增：确保默认选中第一个可用书源
    suspend fun ensureDefaultCurrentSource() = withContext(Dispatchers.IO) {
        if (hasAutoSelectedDefault || currentBookSource.value != null) return@withContext
        val type = selectedSourceType.value ?: BookSourceType.default
        val enabledList = appDb.bookSourceDao.getEnabledByType(type)
            .filter { it.enabledExplore && !it.exploreUrl.isNullOrBlank() }
        // 优先使用上次选择
        val savedKey = "explore_last_source_$type"
        val savedUrl = appCtx.getPrefString(savedKey)
        val saved = enabledList.firstOrNull { it.bookSourceUrl == savedUrl }
        val first = saved ?: (enabledList.minByOrNull { it.customOrder } ?: return@withContext)
        hasAutoSelectedDefault = true
        // 切回主线程更新
        withContext(Dispatchers.Main) {
            setCurrentBookSource(first)
        }
    }
    
    // 新增：获取可用的分组列表
    fun getAvailableGroups(): List<String> {
        val type = selectedSourceType.value ?: BookSourceType.default
        return appDb.bookSourceDao.getEnabledByType(type)
            .mapNotNull { it.bookSourceGroup }
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    
    // 新增：获取当前书源的分组信息
    fun getCurrentSourceGroup(): String? {
        return currentBookSource.value?.bookSourceGroup
    }
    
    // 新增：获取当前书源的名称
    fun getCurrentSourceName(): String? {
        return currentBookSource.value?.bookSourceName
    }
    
    // 新增：获取当前书源的完整信息
    fun getCurrentSourceDisplayInfo(): String? {
        val source = currentBookSource.value ?: return null
        return if (source.bookSourceGroup.isNullOrBlank()) {
            source.bookSourceName
        } else {
            "${source.bookSourceName} (${source.bookSourceGroup})"
        }
    }
    
    // 新增：添加书籍到书架
    fun addToBookshelf(book: SearchBook) {
        execute {
            try {
                // 转换为 Book 实体
                val bookEntity = book.toBook()
                // 添加到数据库
                appDb.bookDao.insert(bookEntity)
                // 记录操作
                AppLog.put("书籍已添加到书架: ${book.name}")
            } catch (e: Exception) {
                AppLog.put("添加书籍到书架失败: ${book.name}", e)
            }
        }
    }
}