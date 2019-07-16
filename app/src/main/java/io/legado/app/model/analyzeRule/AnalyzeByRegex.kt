package io.legado.app.model.analyzeRule

import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.arrayListOf
import kotlin.collections.set

object AnalyzeByRegex {

    fun getElement(res: String, regs: Array<String>, index: Int = 0): Map<String, String>? {
        var vIndex = index
        val resM = Pattern.compile(regs[vIndex]).matcher(res)
        if (!resM.find()) {
            return null
        }
        // 判断索引的规则是最后一个规则
        return if (vIndex + 1 == regs.size) {
            // 新建容器
            val info = HashMap<String, String>()
            for (groupIndex in 1 until resM.groupCount()) {
                info["$$groupIndex"] = resM.group(groupIndex)
            }
            info
        } else {
            val result = StringBuilder()
            do {
                result.append(resM.group())
            } while (resM.find())
            getElement(result.toString(), regs, ++vIndex)
        }
    }

    fun getElements(res: String, regs: Array<String>, index: Int = 0): List<Map<String, String>> {
        var vIndex = index
        val resM = Pattern.compile(regs[vIndex]).matcher(res)
        if (!resM.find()) {
            return arrayListOf()
        }
        // 判断索引的规则是最后一个规则
        if (vIndex + 1 == regs.size) {
            // 创建书息缓存数组
            val books = ArrayList<Map<String, String>>()
            // 提取书籍列表
            do {
                // 新建容器
                val info = HashMap<String, String>()
                for (groupIndex in 1 until resM.groupCount()) {
                    info["$$groupIndex"] = resM.group(groupIndex)
                }
                books.add(info)
            } while (resM.find())
            return books
        } else {
            val result = StringBuilder()
            do {
                result.append(resM.group())
            } while (resM.find())
            return getElements(result.toString(), regs, ++vIndex)
        }
    }
}