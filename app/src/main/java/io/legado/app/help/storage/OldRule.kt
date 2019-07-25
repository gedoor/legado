package io.legado.app.help.storage

import io.legado.app.utils.GSON
import java.util.regex.Pattern

object OldRule {
    private val headerPattern = Pattern.compile("@Header:\\{.+?\\}", Pattern.CASE_INSENSITIVE)
    private val jsPattern = Pattern.compile("\\{\\{.+?\\}\\}", Pattern.CASE_INSENSITIVE)

    fun toNewUrl(oldUrl: String?): String? {
        if (oldUrl == null) return null
        val map = HashMap<String, String>()
        var url: String = oldUrl
        var mather = headerPattern.matcher(url)
        if (mather.find()) {
            val header = mather.group()
            url = url.replace(header, "")
            map["headers"] = header.substring(8)
        }
        var urlList = url.split("|")
        url = urlList[0]
        if (urlList.size > 1) {
            map["charset"] = urlList[1].split("=")[1]
        }
        mather = jsPattern.matcher(url)
        val jsList = arrayListOf<String>()
        while (mather.find()) {
            jsList.add(mather.group())
            url = url.replace(jsList.last(), "$${jsList.size - 1}")
        }
        url = url.replace("{", "<").replace("}", ">")
        url = url.replace("searchKey", "{{key}}")
        url = url.replace("searchPage", "{{page}}")
        for ((index, item) in jsList.withIndex()) {
            url = url.replace("$$index", item.replace("searchKey", "key").replace("searchPage", "page"))
        }
        urlList = url.split("@")
        url = urlList[0]
        if (urlList.size > 1) {
            map["method"] = "POST"
            map["body"] = urlList[1]
        }
        if (map.size > 0) {
            url += "," + GSON.toJson(map)
        }
        return url
    }
}