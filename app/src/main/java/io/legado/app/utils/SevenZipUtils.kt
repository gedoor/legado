package io.legado.app.utils

import android.annotation.SuppressLint
import android.os.ParcelFileDescriptor
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.channels.FileChannel
@Suppress("unused")
object SevenZipUtils {



    fun un7zToPath(inputStream: InputStream, path: String){
        un7zToPath(SevenZFile(SeekableInMemoryByteChannel(inputStream.readBytes())),path)
    }
    fun un7zToPath(pfd: ParcelFileDescriptor, path: String){
        un7zToPath(SevenZFile(ParcelFileDescriptorChannel(pfd)),path)
    }
    @SuppressLint("NewApi")
    fun un7zToPath(fileChannel: FileChannel, path: String){
        un7zToPath(SevenZFile(fileChannel),path)
    }
    fun un7zToPath(file: File, path: String){
        un7zToPath(SevenZFile(file),path)
    }


    fun un7zToPath(sevenZFile:SevenZFile, path: String){
        var entry: SevenZArchiveEntry
        while (sevenZFile.nextEntry.also { entry=it }!=null){
            val entryFile = File(path, entry.name)
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
               sevenZFile.getInputStream(entry).copyTo(it)
            }
        }
    }
}