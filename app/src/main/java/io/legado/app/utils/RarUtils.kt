package io.legado.app.utils

import androidx.annotation.Keep
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Keep
@Suppress("unused","MemberVisibilityCanBePrivate")
object RarUtils {
    fun unRarToPath(inputStream: InputStream, destDir: File?) {
        Archive(inputStream).use {
            unRarToPath(it, destDir)
        }
        inputStream.close()
    }
    fun unRarToPath(byteArray: ByteArray, destDir: File?) {
        Archive(ByteArrayInputStream(byteArray)).use {
            unRarToPath(it, destDir)
        }
    }

    fun unRarToPath(filePath:String, destDir: File?) {
        Archive(File(filePath)).use {
            unRarToPath(it, destDir)
        }
    }

    fun unRarToPath(file: File, destDir: File?) {
        Archive(file).use {
            unRarToPath(it, destDir)
        }
    }

    fun unRarToPath(archive: Archive, destDir: File?) {
        var entry: FileHeader
        while (archive.nextFileHeader().also { entry = it } != null) {
            val entryFile = File(destDir, entry.fileName)
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
    }

}