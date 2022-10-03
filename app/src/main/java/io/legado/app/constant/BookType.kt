package io.legado.app.constant

import androidx.annotation.IntDef

/**
 * 以二进制位来区分,可能一本书籍包含多个类型,每一位代表一个类型,数值为2的n次方
 * 以二进制位来区分,数据库查询更高效, 数值>=8和老版本类型区分开
 */
object BookType {
    /**
     * 8 文本
     */
    const val text = 0b1000

    /**
     * 32 音频
     */
    const val audio = 0b100000

    /**
     * 64 图片
     */
    const val image = 0b1000000

    /**
     * 128 只提供下载服务的网站
     */
    const val webFile = 0b10000000

    /**
     * 256 本地
     */
    const val local = 0b100000000


    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(text, audio, image, webFile)
    annotation class Type

    /**
     * 本地书籍书源标志
     */
    const val localTag = "loc_book"

    /**
     * 书源已webDav::开头的书籍,可以从webDav更新或重新下载
     */
    const val webDavTag = "webDav::"

}