package io.legado.app.constant

import androidx.annotation.IntDef

@Suppress("ConstPropertyName")
object SourceType {

    const val book = 0
    const val rss = 1

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(book, rss)
    annotation class Type

}
