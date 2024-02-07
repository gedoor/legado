package io.legado.app.constant

import androidx.annotation.IntDef

/**
 * 以二进制位来区分,可能一本书籍包含多个类型,每一位代表一个类型,数值为2的n次方
 * 以二进制位来区分,数据库查询更高效, 数值>=8和老版本类型区分开
 */
@Suppress("ConstPropertyName")
object BookType {
    /**
     * 8 文本
     */
    const val text = 0b1000

    /**
     * 16 更新失败
     */
    const val updateError = 0b10000

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

    /**
     * 512 压缩包 表明书籍文件是从压缩包内解压来的
     */
    const val archive = 0b1000000000

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(text, updateError, audio, image, webFile, local, archive)
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