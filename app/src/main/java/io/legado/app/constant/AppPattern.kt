package io.legado.app.constant

import java.util.regex.Pattern

@Suppress("RegExpRedundantEscape")
object AppPattern {
    val JS_PATTERN: Pattern =
        Pattern.compile("<js>([\\w\\W]+?)</js>|@js:([\\w\\W]*)", Pattern.CASE_INSENSITIVE)
    val EXP_PATTERN: Pattern = Pattern.compile("\\{\\{([\\w\\W]*?)\\}\\}")

    //匹配格式化后的图片格式
    val imgPattern: Pattern = Pattern.compile("<img src=\"([^>]+)\">")

    val nameRegex = Regex("\\s+作\\s*者.*|\\s+\\S+\\s+著")
    val authorRegex = Regex("^.*?作\\s*者[:：\\s]*|\\s+著")
    val fileNameRegex = Regex("[\\\\/:*?\"<>|.]")
    val splitGroupRegex = Regex("[,;，；]")
}
