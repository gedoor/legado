package com.script.rhino

import androidx.collection.LruCache
import kotlin.math.min

class ClassNameMatcher(classNames: List<String>) {

    private val sortedClassNames = classNames.sorted()
    private val matchCache = LruCache<String, Boolean>(64)

    fun match(className: String): Boolean {
        matchCache[className]?.let {
            return it
        }
        val match = matchInternal(className)
        matchCache.put(className, match)
        return match
    }

    private fun matchInternal(className: String): Boolean {
        val index = sortedClassNames.fastBinarySearch { prefix ->
            comparePrefix(className, prefix)
        }
        if (index >= 0) {
            return true
        }
        val prefix = sortedClassNames.getOrNull(-index - 2) ?: return false
        return className.getOrNull(prefix.length) == '.' && className.startsWith(prefix)
    }

    private fun comparePrefix(className: String, prefix: String): Int {
        val len = min(className.length, prefix.length)
        for (i in 0 ..< len) {
            val c1 = className[i]
            val c2 = prefix[i]
            if (c1 != c2) return c2 - c1
        }
        return prefix.length - className.length
    }

}
