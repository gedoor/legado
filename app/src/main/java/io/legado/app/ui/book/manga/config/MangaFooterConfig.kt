package io.legado.app.ui.book.manga.config

import androidx.annotation.Keep
import io.legado.app.ui.widget.ReaderInfoBarView

@Keep
data class MangaFooterConfig(
    var disableChapterLabel: Boolean = false,
    var disableChapter: Boolean = false,
    var disablePageNumberLabel: Boolean = false,
    var disablePageNumber: Boolean = false,
    var disableProgressRatioLabel: Boolean = false,
    var disableProgressRatio: Boolean = false,
    var footerOrientation: Int = ReaderInfoBarView.ALIGN_LEFT,//默认靠左
    var disableFooter: Boolean = false
)