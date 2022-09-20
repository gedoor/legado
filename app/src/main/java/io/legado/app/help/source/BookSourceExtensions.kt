package io.legado.app.help.source

import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.ExploreKind
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 采用md5作为key可以在分类修改后自动重新计算,不需要手动刷新
 */

private val mutexMap by lazy { hashMapOf<String, Mutex>() }
private val exploreKindsMap by lazy { ConcurrentHashMap<String, List<ExploreKind>>() }
private val aCache by lazy { ACache.get("explore") }

private fun BookSource.exploreKindsKey(): String {
    return MD5Utils.md5Encode(bookSourceUrl + exploreUrl)
}

suspend fun BookSource.exploreKinds(): List<ExploreKind> {
    val exploreKindsKey = exploreKindsKey()
    exploreKindsMap[exploreKindsKey]?.let { return it }
    val exploreUrl = exploreUrl
    if (exploreUrl.isNullOrBlank()) {
        return emptyList()
    }
    val mutex = mutexMap[bookSourceUrl] ?: Mutex().apply { mutexMap[bookSourceUrl] = this }
    mutex.withLock {
        exploreKindsMap[exploreKindsKey()]?.let { return it }
        val kinds = arrayListOf<ExploreKind>()
        var ruleStr: String = exploreUrl
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                if (exploreUrl.startsWith("<js>", false)
                    || exploreUrl.startsWith("@js:", false)
                ) {
                    ruleStr = aCache.getAsString(bookSourceUrl) ?: ""
                    if (ruleStr.isBlank()) {
                        val jsStr = if (exploreUrl.startsWith("@")) {
                            exploreUrl.substring(4)
                        } else {
                            exploreUrl.substring(4, exploreUrl.lastIndexOf("<"))
                        }
                        ruleStr = evalJS(jsStr).toString().trim()
                        aCache.put(exploreKindsKey, ruleStr)
                    }
                }
                if (ruleStr.isJsonArray()) {
                    GSON.fromJsonArray<ExploreKind>(ruleStr).getOrThrow()?.let {
                        kinds.addAll(it)
                    }
                } else {
                    ruleStr.split("(&&|\n)+".toRegex()).forEach { kindStr ->
                        val kindCfg = kindStr.split("::")
                        kinds.add(ExploreKind(kindCfg.first(), kindCfg.getOrNull(1)))
                    }
                }
            }.onFailure {
                kinds.add(ExploreKind("ERROR:${it.localizedMessage}", it.stackTraceToString()))
                it.printOnDebug()
            }
        }
        exploreKindsMap[exploreKindsKey] = kinds
        return kinds
    }
}

suspend fun BookSource.clearExploreKindsCache() {
    withContext(Dispatchers.IO) {
        aCache.remove(bookSourceUrl)
        exploreKindsMap.remove(exploreKindsKey())
    }
}