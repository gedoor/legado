package io.legado.app.ui.file.entity

import android.graphics.drawable.Drawable

/**
 * 文件项信息
 */
data class FileItem(
    var icon: Drawable,
    var name: String,
    var path: String = "/",
    var size: Long = 0,
    var isDirectory: Boolean = false,
)
