package io.legado.app.model.recyclerView

data class ReaderLoading(
    val mChapterIndex: Int = 0,
    val mMessage: String? = null,
    val mNextChapterIndex: Int = 0,
    var mLoading: Boolean = false,
)
