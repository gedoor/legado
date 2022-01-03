package io.legado.app.utils

import java.io.File

fun File.getFile(vararg subDirFiles: String): File {
    val path = FileUtils.getPath(this, *subDirFiles)
    return File(path)
}

fun File.exists(vararg subDirFiles: String): Boolean {
    return getFile(*subDirFiles).exists()
}

