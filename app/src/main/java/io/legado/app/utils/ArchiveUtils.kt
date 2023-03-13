package io.legado.app.utils

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.util.regex.Pattern

import splitties.init.appCtx

/* 自动判断压缩文件后缀 然后再调用具体的实现 */
object ArchiveUtils {

    // 临时目录 下次启动自动删除
    val TEMP_PATH: String by lazy {
        appCtx.externalCache.getFile("ArchiveTemp").createFolderReplace().absolutePath
    }

    val ZIP_REGEX = Regex(".*\\.zip", RegexOption.IGNORE_CASE)
    val RAR_REGEX = Regex(".*\\.rar", RegexOption.IGNORE_CASE)
    val SERVEZ_REGEX = Regex(".*\\.7z", RegexOption.IGNORE_CASE)

    fun deCompress(
        archiveUri: Uri,
        path: String? = TEMP_PATH
    ): FileDoc {
        return deCompress(FileDoc.fromUri(archiveUri, false), path)
    }

    fun deCompress(
        archivePath: String,
        path: String? = TEMP_PATH
    ): FileDoc {
        return deCompress(Uri.parse(archivePath)), false), path)
    }

    fun deCompress(
        archiveFile: File,
        path: String? = TEMP_PATH
    ): FileDoc {
        return deCompress(FileDoc.fromFile(archiveFile), path)
    }

    fun deCompress(
        archiveDoc: DocumentFile,
        path: String? = TEMP_PATH
    ): FileDoc {
        return deCompress(FileDoc.fromDocumentFile(archiveDoc), path)
    }

    fun deCompress(
        archiveFileDoc: FileDoc,
        path: String? = TEMP_PATH
    ): FileDoc {
        if (archiveFileDoc.isDir) throw IllegalArgumentException("Unexpected Folder input")
        val name = archiveFileDoc.name
        archiveFileDoc.uri.inputStream(appCtx).getOrThrow().use {
            when {
                ZIP_REGEX.matches(name) -> ZipUtils.unZipToPath(it, path) 
                RAR_REGEX.matches(name) -> RarUtils.unRarToPath(it, path)
                SERVEZ_REGEX.matches(name) -> SevenZipUtils.un7zToPath(it, path)
                else -> throw IllegalArgumentException("Unexpected archive format")
            }
        }
        return getCacheFolderFileDoc(name, path)
    }

    private fun getCacheFolderFileDoc(
        archiveName: String,
        workPath: String
    ): FileDoc {
        return FileDoc.fromUri(Uri.parse(workPath), true)
            .createFolderIfNotExist(MD5Utils.md5Encode16(archiveName))
    }
}