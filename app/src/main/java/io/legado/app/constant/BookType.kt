package io.legado.app.constant

import androidx.annotation.IntDef

object BookType {
    const val default = 0           // 0 文本
    const val audio = 1             // 1 音频
    const val image = 2             //图片
    const val local = "loc_book"

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(default, audio, image)
    annotation class Type
}