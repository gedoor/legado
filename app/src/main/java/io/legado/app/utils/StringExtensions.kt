package io.legado.app.utils

fun String?.safeTrim() = if (this.isNullOrBlank()) null else this.trim()

fun String?.isContentPath(): Boolean = this?.startsWith("content://") == true

fun String?.isAbsUrl() =
    this?.let {
        it.startsWith("http://", true)
                || it.startsWith("https://", true)
    } ?: false

fun String?.isJson(): Boolean =
    this?.run {
        val str = this.trim()
        when {
            str.startsWith("{") && str.endsWith("}") -> true
            str.startsWith("[") && str.endsWith("]") -> true
            else -> false
        }
    } ?: false

fun String?.isJsonObject(): Boolean =
    this?.run {
        val str = this.trim()
        str.startsWith("{") && str.endsWith("}")
    } ?: false

fun String?.isJsonArray(): Boolean =
    this?.run {
        val str = this.trim()
        str.startsWith("[") && str.endsWith("]")
    } ?: false

fun String?.htmlFormat(): String =
    this?.replace("</?(?:div|p|b|br|hr|h\\d|article|dd|dl|span|link|title)[^>]*>".toRegex(), "\n")
        ?.replace("<[script>]*.*?>|&nbsp;".toRegex(), "")
        ?.replace("\\s*\\n+\\s*".toRegex(), "\n　　")
        ?.replace("^[\\n\\s]+".toRegex(), "　　")
        ?.replace("[\\n\\s]+$".toRegex(), "")
        ?: ""

fun String.splitNotBlank(vararg delimiter: String): Array<String> = run {
    this.split(*delimiter).map { it.trim() }.filterNot { it.isBlank() }.toTypedArray()
}

fun String.splitNotBlank(regex: Regex, limit: Int = 0): Array<String> = run {
    this.split(regex, limit).map { it.trim() }.filterNot { it.isBlank() }.toTypedArray()
}

fun String.toStringArray(): Array<String> {
    var codePointIndex = 0
    return Array(codePointCount(0, length)) {
        substring(
            codePointIndex,
            offsetByCodePoints(codePointIndex, 1)
                .apply { codePointIndex = this }
        )
    }
}

fun Char?.isHAN(): Boolean {
    this ?: return false
    val ub: Character.UnicodeBlock = Character.UnicodeBlock.of(this) ?: return false
    return ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C
            || ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D
            || ub === Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || ub === Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT
}
