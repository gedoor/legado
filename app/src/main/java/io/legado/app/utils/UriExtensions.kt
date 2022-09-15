package io.legado.app.utils

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.exception.NoStackTraceException
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

fun Uri.isContentScheme() = this.scheme == "content"

/**
 * 读取URI
 */
fun AppCompatActivity.readUri(
    uri: Uri?,
    success: (fileDoc: FileDoc, inputStream: InputStream) -> Unit
) {
    uri ?: return
    try {
        if (uri.isContentScheme()) {
            val doc = DocumentFile.fromSingleUri(this, uri)
            doc ?: throw NoStackTraceException("未获取到文件")
            val fileDoc = FileDoc.fromDocumentFile(doc)
            contentResolver.openInputStream(uri)!!.use { inputStream ->
                success.invoke(fileDoc, inputStream)
            }
        } else {
            PermissionsCompat.Builder(this)
                .addPermissions(
                    Permissions.READ_EXTERNAL_STORAGE,
                    Permissions.WRITE_EXTERNAL_STORAGE
                )
                .rationale(R.string.get_storage_per)
                .onGranted {
                    RealPathUtil.getPath(this, uri)?.let { path ->
                        val file = File(path)
                        val fileDoc = FileDoc.fromFile(file)
                        FileInputStream(file).use { inputStream ->
                            success.invoke(fileDoc, inputStream)
                        }
                    }
                }
                .request()
        }
    } catch (e: Exception) {
        e.printOnDebug()
        toastOnUi(e.localizedMessage ?: "read uri error")
    }
}

/**
 * 读取URI
 */
fun Fragment.readUri(uri: Uri?, success: (fileDoc: FileDoc, inputStream: InputStream) -> Unit) {
    uri ?: return
    try {
        if (uri.isContentScheme()) {
            val doc = DocumentFile.fromSingleUri(requireContext(), uri)
            doc ?: throw NoStackTraceException("未获取到文件")
            val fileDoc = FileDoc.fromDocumentFile(doc)
            requireContext().contentResolver.openInputStream(uri)!!.use { inputStream ->
                success.invoke(fileDoc, inputStream)
            }
        } else {
            PermissionsCompat.Builder(this)
                .addPermissions(
                    Permissions.READ_EXTERNAL_STORAGE,
                    Permissions.WRITE_EXTERNAL_STORAGE
                )
                .rationale(R.string.get_storage_per)
                .onGranted {
                    RealPathUtil.getPath(requireContext(), uri)?.let { path ->
                        val file = File(path)
                        val fileDoc = FileDoc.fromFile(file)
                        FileInputStream(file).use { inputStream ->
                            success.invoke(fileDoc, inputStream)
                        }

                    }
                }
                .request()
        }
    } catch (e: Exception) {
        e.printOnDebug()
        toastOnUi(e.localizedMessage ?: "read uri error")
    }
}

@Throws(Exception::class)
fun Uri.readBytes(context: Context): ByteArray {
    return if (this.isContentScheme()) {
        DocumentUtils.readBytes(context, this)
    } else {
        val path = RealPathUtil.getPath(context, this)
        if (path?.isNotEmpty() == true) {
            File(path).readBytes()
        } else {
            throw NoStackTraceException("获取文件真实地址失败\n${this.path}")
        }
    }
}

@Throws(Exception::class)
fun Uri.readText(context: Context): String {
    readBytes(context).let {
        return String(it)
    }
}

@Throws(Exception::class)
fun Uri.writeBytes(
    context: Context,
    byteArray: ByteArray
): Boolean {
    if (this.isContentScheme()) {
        return DocumentUtils.writeBytes(context, byteArray, this)
    } else {
        val path = RealPathUtil.getPath(context, this)
        if (path?.isNotEmpty() == true) {
            File(path).writeBytes(byteArray)
            return true
        }
    }
    return false
}

@Throws(Exception::class)
fun Uri.writeText(context: Context, text: String): Boolean {
    return writeBytes(context, text.toByteArray())
}

fun Uri.writeBytes(
    context: Context,
    fileName: String,
    byteArray: ByteArray
): Boolean {
    if (this.isContentScheme()) {
        DocumentFile.fromTreeUri(context, this)?.let { pDoc ->
            DocumentUtils.createFileIfNotExist(pDoc, fileName)?.let {
                return it.uri.writeBytes(context, byteArray)
            }
        }
    } else {
        FileUtils.createFileWithReplace(path + File.separatorChar + fileName)
            .writeBytes(byteArray)
        return true
    }
    return false
}

fun Uri.inputStream(context: Context): InputStream? {
    val uri = this
    try {
        if (isContentScheme()) {
            val doc = DocumentFile.fromSingleUri(context, uri)
            doc ?: throw NoStackTraceException("未获取到文件")
            return context.contentResolver.openInputStream(uri)!!
        } else {
            RealPathUtil.getPath(context, uri)?.let { path ->
                val file = File(path)
                return FileInputStream(file)
            }
        }
    } catch (e: Exception) {
        e.printOnDebug()
        context.toastOnUi("读取inputStream失败：${e.localizedMessage}")
        AppLog.put("读取inputStream失败：${e.localizedMessage}", e)
    }
    return null
}