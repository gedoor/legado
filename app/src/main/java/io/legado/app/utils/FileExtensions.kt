package io.legado.app.utils

import android.net.Uri
import java.io.File

fun File.getFile(vararg subDirFiles: String): File {
    val path = FileUtils.getPath(this, *subDirFiles)
    return File(path)
}

fun File.exists(vararg subDirFiles: String): Boolean {
    return getFile(*subDirFiles).exists()
}

@Throws(Exception::class)
fun File.listFileDocs(filter: FileDocFilter? = null): ArrayList<FileDoc> {
    val docList = arrayListOf<FileDoc>()
    listFiles()
    listFiles()?.forEach {
        val item = FileDoc(
            it.name,
            it.isDirectory,
            it.length(),
            it.lastModified(),
            Uri.fromFile(it)
        )
        if (filter == null || filter.invoke(item)) {
            docList.add(item)
        }
    }
    return docList
}


