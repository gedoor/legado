package io.legado.app.model.analyzeRule

import androidx.annotation.Keep
import java.util.*
import java.util.regex.Pattern

@Keep
object AnalyzeByRegex {

    fun getElement(res: String, regs: Array<String>, index: Int = 0): List<String>? {
        var vIndex = index
        val resM = Pattern.compile(regs[vIndex]).matcher(res)
        if (!resM.find()) {
            return null
        }
        // 判断索引的规则是最后一个规则
        return if (vIndex + 1 == regs.size) {
            // 新建容器
            val info = arrayListOf<String>()
            for (groupIndex in 0..resM.groupCount()) {
                info.add(resM.group(groupIndex)!!)
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

    fun getElements(res: String, regs: Array<String>, index: Int = 0): List<List<String>> {
        var vIndex = index
        val resM = Pattern.compile(regs[vIndex]).matcher(res)
        if (!resM.find()) {
            return arrayListOf()
        }
        // 判断索引的规则是最后一个规则
        if (vIndex + 1 == regs.size) {
            // 创建书息缓存数组
            val books = ArrayList<List<String>>()
            // 提取列表
            do {
                // 新建容器
                val info = arrayListOf<String>()
                for (groupIndex in 0..resM.groupCount()) {
                    info.add(resM.group(groupIndex)!!)
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