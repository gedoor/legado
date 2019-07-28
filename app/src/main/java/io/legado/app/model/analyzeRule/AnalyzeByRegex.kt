package io.legado.app.model.analyzeRule

import android.text.TextUtils
import java.util.*
import java.util.regex.Pattern

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
            for (groupIndex in 0 until resM.groupCount()) {
                info.add(resM.group(groupIndex))
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
                for (groupIndex in 0 until resM.groupCount()) {
                    info.add(resM.group(groupIndex))
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

    // 拆分正则表达式替换规则(如:$\d{1,2}或${name}) /*注意:千万别用正则表达式拆分字符串,效率太低了!*/
    fun splitRegexRule(str: String, ruleParam: MutableList<String>, ruleType: MutableList<Int>) {
        if (TextUtils.isEmpty(str)) {
            ruleParam.add("")
            ruleType.add(0)
            return
        }
        var index = 0
        var start = 0
        val len = str.length
        while (index < len) {
            if (str[index] == '$') {
                if (str[index + 1] == '{') {
                    if (index > start) {
                        ruleParam.add(str.substring(start, index))
                        ruleType.add(0)
                        start = index
                    }
                    index += 2
                    while (index < len) {
                        if (str[index] == '}') {
                            ruleParam.add(str.substring(start + 2, index))
                            ruleType.add(-1)
                            start = ++index
                            break
                        } else if (str[index] == '$' || str[index] == '@') {
                            break
                        }
                        index++
                    }
                } else if (str[index + 1] in '0'..'9') {
                    if (index > start) {
                        ruleParam.add(str.substring(start, index))
                        ruleType.add(0)
                        start = index
                    }
                    if (index + 2 < len && str[index + 2] >= '0' && str[index + 2] <= '9') {
                        ruleParam.add(str.substring(start, index + 3))
                        ruleType.add(string2Int(ruleParam[ruleParam.size - 1]))
                        index += 3
                        start = index
                    } else {
                        ruleParam.add(str.substring(start, index + 2))
                        ruleType.add(string2Int(ruleParam[ruleParam.size - 1]))
                        index += 2
                        start = index
                    }
                } else {
                    ++index
                }
            } else {
                ++index
            }
        }
        if (index > start) {
            ruleParam.add(str.substring(start, index))
            ruleType.add(0)
        }
    }

    // 存取字符串中的put&get参数
    fun checkKeys(ruleStr: String, analyzer: AnalyzeRule): String {
        var str = ruleStr
        if (str.contains("@put:{")) {
            val putMatcher = Pattern.compile("@put:\\{([^,]*):([^\\}]*)\\}").matcher(str)
            while (putMatcher.find()) {
                str = str.replace(putMatcher.group(0), "")
                analyzer.put(putMatcher.group(1), putMatcher.group(2))
            }
        }
        if (str.contains("@get:{")) {
            val getMatcher = Pattern.compile("@get:\\{([^\\}]*)\\}").matcher(str)
            while (getMatcher.find()) {
                str = str.replace(getMatcher.group(), analyzer[getMatcher.group(1)] ?: "")
            }
        }
        return str
    }

    // String数字转int数字的高效方法(利用ASCII值判断)
    private fun string2Int(s: String): Int {
        var r = 0
        var n: Char
        var i = 0
        val l = s.length
        while (i < l) {
            n = s[i]
            if (n in '0'..'9') {
                r = r * 10 + (n.toInt() - 0x30) //'0-9'的ASCII值为0x30-0x39
            }
            i++
        }
        return r
    }
}