package io.legado.app.constant

import java.util.regex.Pattern

@Suppress("RegExpRedundantEscape")
object AppPattern {
    val JS_PATTERN: Pattern =
        Pattern.compile("(<js>[\\w\\W]*?</js>|@js:[\\w\\W]*$)", Pattern.CASE_INSENSITIVE)
    val EXP_PATTERN: Pattern = Pattern.compile("\\{\\{([\\w\\W]*?)\\}\\}")

    //图片有data-开头的数据属性时优先用数据属性作为src，没有数据属性时才匹配src
    val imgPattern: Pattern =
        Pattern.compile(
            "<img(?:(?![^>]*data-)[^>]*src|[^>]*data-)[^=]*= *\"([^\"{]+(?:\\{(?:[^{}]|\\{[^{}]*\\})*\\})?)\"[^>]*>",
            Pattern.CASE_INSENSITIVE
        )

    val nameRegex = Regex("\\s+作\\s*者.*|\\s+\\S+\\s+著")
    val authorRegex = Regex("^.*?作\\s*者[:：\\s]*|\\s+著")
    val fileNameRegex = Regex("[\\\\/:*?\"<>|.]")
    val splitGroupRegex = Regex("[,;，；]")
}
