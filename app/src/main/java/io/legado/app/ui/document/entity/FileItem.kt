package io.legado.app.ui.document.entity

import android.graphics.drawable.Drawable

/**
 * 文件项信息
 *
 * @author 李玉江[QQ:1032694760]
 * @since 2014-05-23 18:02
 */
class FileItem : JavaBean() {
    var icon: Drawable? = null
    var name: String? = null
    var path = "/"
    var size: Long = 0
    var isDirectory = false
}
