package io.legado.app.help

import io.legado.app.App
import io.legado.app.data.entities.Cache
import io.legado.app.utils.ACache

@Suppress("unused")
object CacheManager {

    /**
     * saveTime 单位为秒
     */
    @JvmOverloads
    fun put(key: String, value: Any, saveTime: Int = 0) {
        if (value is ByteArray) {
            ACache.get(App.INSTANCE).put(key, value, saveTime)
        } else {
            val deadline = if (saveTime == 0) 0 else System.currentTimeMillis() + saveTime * 1000
            val cache = Cache(key, value.toString(), deadline)
            App.db.cacheDao().insert(cache)
        }
    }

    fun get(key: String): String? {
        return App.db.cacheDao().get(key, System.currentTimeMillis())
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

    fun getByteArray(key: String) {
        ACache.get(App.INSTANCE).getAsBinary(key)
    }
}