package io.legado.app.ui.widget.dynamiclayout

import androidx.annotation.IntDef
import androidx.annotation.StringRes

interface ViewSwitcher {

    companion object {
        const val SHOW_CONTENT_VIEW = 0
        const val SHOW_ERROR_VIEW = 1
        const val SHOW_EMPTY_VIEW = 2
        const val SHOW_PROGRESS_VIEW = 3
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(SHOW_CONTENT_VIEW, SHOW_ERROR_VIEW, SHOW_EMPTY_VIEW, SHOW_PROGRESS_VIEW)
    annotation class Visibility

    fun showErrorView(message: CharSequence)

    fun showErrorView(@StringRes messageId: Int)

    fun showEmptyView()

    fun showProgressView()

    fun showContentView()

}