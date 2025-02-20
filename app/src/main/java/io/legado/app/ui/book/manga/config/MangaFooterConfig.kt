package io.legado.app.ui.book.manga.config

import androidx.annotation.Keep

@Keep
data class MangaFooterConfig(
    var chapterLabelDisable: Boolean = false,
    var chapterDisable: Boolean = false,
    var pageNumberLabelDisable: Boolean = false,
    var pageNumberDisable: Boolean = false,
    var progressRatioLabelDisable: Boolean = false,
    var progressRatioDisable: Boolean = false,
    var footerOrientation: Boolean = false,//默认靠左
    var footerDisable: Boolean = false
)