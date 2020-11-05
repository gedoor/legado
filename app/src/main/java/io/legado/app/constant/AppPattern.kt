package io.legado.app.constant

import java.util.regex.Pattern

@Suppress("RegExpRedundantEscape")
object AppPattern {
    val JS_PATTERN: Pattern =
        Pattern.compile("(<js>[\\w\\W]*?</js>|@js:[\\w\\W]*$)", Pattern.CASE_INSENSITIVE)
    val EXP_PATTERN: Pattern = Pattern.compile("\\{\\{([\\w\\W]*?)\\}\\}")
    val imgPattern: Pattern =
        Pattern.compile("<img .*?src.*?=.*?\"(.*?(?:,\\{.*\\})?)\".*?>", Pattern.CASE_INSENSITIVE)

    val nameRegex = Regex("\\s+作\\s*者.*")
    val authorRegex = Regex(".*?作\\s*?者[:：]")
    val fileNameRegex = Regex("[\\\\/:*?\"<>|.]")
    val splitGroupRegex = Regex("[,;，；]")
}