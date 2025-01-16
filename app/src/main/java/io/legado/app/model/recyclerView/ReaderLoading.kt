package io.legado.app.model.recyclerView

data class ReaderLoading(
    val mChapterPagePos: Int = 0,
    val mMessage: String?=null,
    val mNextIndex: Int=0,
    val mCurrentIndex: Int=0,
    val mLoadNext: Boolean? = null,
    val mStateComplete: Boolean = false
)
