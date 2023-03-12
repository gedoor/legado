package io.legado.app.utils

import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
@Suppress("unused")
object RarUtils {
    fun unRarToPath(inputStream: InputStream, path: String){
        val archive= Archive(inputStream)
        var entry: FileHeader
        while (archive.nextFileHeader().also { entry=it }!=null){
            val entryFile = File(path, entry.fileName)
            if (entry.isDirectory) {
                if (!entryFile.exists()) {
                    entryFile.mkdirs()
                }
                continue
            }
            if (entryFile.parentFile?.exists() != true) {
                entryFile.parentFile?.mkdirs()
            }
            if (!entryFile.exists()) {
                entryFile.createNewFile()
                entryFile.setReadable(true)
                entryFile.setExecutable(true)
            }
            FileOutputStream(entryFile).use {
                archive.getInputStream(entry).copyTo(it)
            }

        }
        inputStream.close()
    }

}