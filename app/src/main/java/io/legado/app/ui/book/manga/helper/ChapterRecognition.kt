package io.legado.app.ui.book.manga.helper


object ChapterRecognition {

    private const val NUMBER_PATTERN = """([0-9]+)(\.[0-9]+)?(\.?[a-z]+)?"""

    private val basic = Regex("""(?<=ch\.) *$NUMBER_PATTERN""")
    private val number = Regex(NUMBER_PATTERN)

    private val unwanted = Regex("""\b(?:v|ver|vol|version|volume|season|s)[^a-z]?[0-9]+""")

    private val unwantedWhiteSpace = Regex("""\s(?=extra|special|omake)""")

    fun parseChapterNumber(
        chapterName: String,
        chapterNumber: Double? = null,
    ): Double {
        if (chapterNumber != null && (chapterNumber == -2.0 || chapterNumber > -1.0)) {
            return chapterNumber
        }

        val cleanChapterName = chapterName.lowercase()
            .replace(',', '.')
            .replace('-', '.')
            .replace(unwantedWhiteSpace, "")

        val numberMatch = number.findAll(cleanChapterName)

        when {
            numberMatch.none() -> {
                return chapterNumber ?: -1.0
            }

            numberMatch.count() > 1 -> {
                unwanted.replace(cleanChapterName, "").let { name ->
                    basic.find(name)?.let { return getChapterNumberFromMatch(it) }
                    number.find(name)?.let { return getChapterNumberFromMatch(it) }
                }
            }
        }

        return getChapterNumberFromMatch(numberMatch.first())
    }

    private fun getChapterNumberFromMatch(match: MatchResult): Double {
        return match.let {
            val initial = it.groups[1]?.value?.toDouble()!!
            val subChapterDecimal = it.groups[2]?.value
            val subChapterAlpha = it.groups[3]?.value
            val addition = checkForDecimal(subChapterDecimal, subChapterAlpha)
            initial.plus(addition)
        }
    }

    private fun checkForDecimal(decimal: String?, alpha: String?): Double {
        if (!decimal.isNullOrEmpty()) {
            return decimal.toDouble()
        }

        if (!alpha.isNullOrEmpty()) {
            if (alpha.contains("extra")) {
                return 0.99
            }

            if (alpha.contains("omake")) {
                return 0.98
            }

            if (alpha.contains("special")) {
                return 0.97
            }

            val trimmedAlpha = alpha.trimStart('.')
            if (trimmedAlpha.length == 1) {
                return parseAlphaPostFix(trimmedAlpha[0])
            }
        }

        return 0.0
    }

    private fun parseAlphaPostFix(alpha: Char): Double {
        val number = alpha.code - ('a'.code - 1)
        if (number >= 10) return 0.0
        return number / 10.0
    }
}
