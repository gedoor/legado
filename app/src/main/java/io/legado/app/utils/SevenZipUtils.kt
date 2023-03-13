package io.legado.app.utils

import android.annotation.SuppressLint
import android.os.ParcelFileDescriptor
import androidx.annotation.Keep
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.channels.FileChannel

@Keep
@Suppress("unused","MemberVisibilityCanBePrivate")
object SevenZipUtils {

    fun un7zToPath(inputStream: InputStream, path:String){
        un7zToPath(inputStream,File(path))
    }
    fun un7zToPath(byteArray: ByteArray, path:String){
        un7zToPath(byteArray,File(path))
    }
    fun un7zToPath(pfd: ParcelFileDescriptor, path:String){
        un7zToPath(pfd,File(path))
    }
    fun un7zToPath(fileChannel: FileChannel, path:String){
        un7zToPath(fileChannel,File(path))
    }

    fun un7zToPath(inputStream: InputStream, destDir: File?){
        un7zToPath(SevenZFile(SeekableInMemoryByteChannel(inputStream.readBytes())),destDir)
    }
    fun un7zToPath(byteArray: ByteArray, destDir: File?){
        un7zToPath(SevenZFile(SeekableInMemoryByteChannel(byteArray)),destDir)
    }
    fun un7zToPath(pfd: ParcelFileDescriptor, destDir: File?){
        un7zToPath(SevenZFile(ParcelFileDescriptorChannel(pfd)),destDir)
    }
    @SuppressLint("NewApi")
    fun un7zToPath(fileChannel: FileChannel, destDir: File?){
        un7zToPath(SevenZFile(fileChannel),destDir)
    }
    fun un7zToPath(file: File, destDir: File?){
        un7zToPath(SevenZFile(file),destDir)
    }
    fun un7zToPath(filePath: String, destDir: File?){
        un7zToPath(SevenZFile(File(filePath)),destDir)
    }


    fun un7zToPath(sevenZFile:SevenZFile, destDir: File?){
        var entry: SevenZArchiveEntry?
        while (sevenZFile.nextEntry.also { entry=it }!=null) {
            val entryFile = File(destDir, entry!!.name)
            if (entry!!.isDirectory) {
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