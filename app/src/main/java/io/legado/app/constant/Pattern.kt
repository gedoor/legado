package io.legado.app.constant

import java.util.regex.Pattern

object Pattern {
    val JS_PATTERN = Pattern.compile("(<js>[\\w\\W]*?</js>|@js:[\\w\\W]*$)", Pattern.CASE_INSENSITIVE)
    val EXP_PATTERN = Pattern.compile("\\{\\{([\\w\\W]*?)\\}\\}")
}