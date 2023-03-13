package io.legado.app.utils

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.utils.compress.RarUtils
import io.legado.app.utils.compress.SevenZipUtils
import io.legado.app.utils.compress.ZipUtils
import splitties.init.appCtx
import java.io.File

/* 自动判断压缩文件后缀 然后再调用具体的实现 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
object ArchiveUtils {

    // 临时目录 下次启动自动删除
    val TEMP_PATH: String by lazy {
        appCtx.externalCache.getFile("ArchiveTemp").createFolderReplace().absolutePath
    }

    fun deCompress(
        archiveUri: Uri,
        path: String = TEMP_PATH
    ): FileDoc {
        return deCompress(FileDoc.fromUri(archiveUri, false), path)
    }

    fun deCompress(
        archivePath: String,
        path: String = TEMP_PATH
    ): FileDoc {
        return deCompress(Uri.parse(archivePath), path)
    }

    fun deCompress(
        archiveFile: File,
        path: String = TEMP_PATH
    ): FileDoc {
        return deCompress(FileDoc.fromFile(archiveFile), path)
    }

    fun deCompress(
        archiveDoc: DocumentFile,
        path: String = TEMP_PATH
    ): FileDoc {
        return deCompress(FileDoc.fromDocumentFile(archiveDoc), path)
    }

    fun deCompress(
        archiveFileDoc: FileDoc,
        path: String = TEMP_PATH
    ): FileDoc {
        if (archiveFileDoc.isDir) throw IllegalArgumentException("Unexpected Folder input")
        val name = archiveFileDoc.name
        val workPathFileDoc = getCacheFolderFileDoc(name, path)
        val workPath = workPathFileDoc.toString()
        archiveFileDoc.uri.inputStream(appCtx).getOrThrow().use {
            when {
                name.endsWith(".zip", ignoreCase = true) -> ZipUtils.unZipToPath(it, workPath)
                name.endsWith(".rar", ignoreCase = true) -> RarUtils.unRarToPath(it, workPath)
                name.endsWith(".7z", ignoreCase = true) -> SevenZipUtils.un7zToPath(it, workPath)
                else -> throw IllegalArgumentException("Unexpected archive format")
            }
        }
        return workPathFileDoc
    }

    private fun getCacheFolderFileDoc(
        archiveName: String,
        workPath: String
    ): FileDoc {
        return FileDoc.fromUri(Uri.parse(workPath), true)
            .createFolderIfNotExist(MD5Utils.md5Encode16(archiveName))
    }
}