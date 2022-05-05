package io.legado.app.help

import androidx.collection.LruCache
import io.legado.app.data.appDb
import io.legado.app.data.entities.Cache
import io.legado.app.model.analyzeRule.QueryTTF
import io.legado.app.utils.ACache
import splitties.init.appCtx

@Suppress("unused")
object CacheManager {

    private val queryTTFMap = hashMapOf<String, Pair<Long, QueryTTF>>()
    private val memoryLruCache = object : LruCache<String, Cache>(100) {
        override fun sizeOf(key: String, value: Cache): Int {
            return 1
        }
    }

    /**
     * saveTime 单位为秒
     */
    @JvmOverloads
    fun put(key: String, value: Any, saveTime: Int = 0) {
        val deadline =
            if (saveTime == 0) 0 else System.currentTimeMillis() + saveTime * 1000
        when (value) {
            is QueryTTF -> queryTTFMap[key] = Pair(deadline, value)
            is ByteArray -> ACache.get(appCtx).put(key, value, saveTime)
            else -> {
                val cache = Cache(key, value.toString(), deadline)
                memoryLruCache.put(key, cache)
                appDb.cacheDao.insert(cache)
            }
        }
    }

    fun get(key: String): String? {
        getFromMemory(key)?.let {
            return it
        }
        val cache = appDb.cacheDao.get(key)
        if (cache != null && (cache.deadline == 0L || cache.deadline > System.currentTimeMillis())) {
            memoryLruCache.put(key, cache)
            return cache.value
        }
        return null
    }

    //从内存中获取数据 使用lruCache 支持过期功能
    private fun getFromMemory(key: String): String? {
        val cache = memoryLruCache.get(key) ?: return null
        val deadline = cache.deadline
        return if (deadline == 0L || deadline > System.currentTimeMillis()) {
            cache.value
        } else {
            memoryLruCache.remove(key)
            null
        }
    }

    fun getInt(key: String): Int? {
        return get(key)?.toIntOrNull()
    }

    fun getLong(key: String): Long? {
        return get(key)?.toLongOrNull()
    }

    fun getDouble(key: String): Double? {
        return get(key)?.toDoubleOrNull()
    }

    fun getFloat(key: String): Float? {
        return get(key)?.toFloatOrNull()
    }

    fun getByteArray(key: String): ByteArray? {
        return ACache.get(appCtx).getAsBinary(key)
    }

    fun getQueryTTF(key: String): QueryTTF? {
        val cache = queryTTFMap[key] ?: return null
        if (cache.first == 0L || cache.first > System.currentTimeMillis()) {
            return cache.second
        }
        return null
    }

    fun putFile(key: String, value: String, saveTime: Int = 0) {
        ACache.get(appCtx).put(key, value, saveTime)
    }

    fun getFile(key: String): String? {
        return ACache.get(appCtx).getAsString(key)
    }

    fun delete(key: String) {
        appDb.cacheDao.delete(key)
        memoryLruCache.remove(key)
        ACache.get(appCtx).remove(key)
    }
}