package io.legado.app.utils

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import io.legado.app.R
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import java.io.File

fun Uri.isContentScheme() = this.scheme == "content"

/**
 * 读取URI
 */
fun Uri.read(activity: AppCompatActivity, success: (name: String, bytes: ByteArray) -> Unit) {
    try {
        if (isContentScheme()) {
            val doc = DocumentFile.fromSingleUri(activity, this)
            doc ?: error("未获取到文件")
            val name = doc.name ?: error("未获取到文件名")
            val fileBytes = DocumentUtils.readBytes(activity, doc.uri)
            fileBytes ?: error("读取文件出错")
            success.invoke(name, fileBytes)
        } else {
            PermissionsCompat.Builder(activity)
                .addPermissions(
                    Permissions.READ_EXTERNAL_STORAGE,
                    Permissions.WRITE_EXTERNAL_STORAGE
                )
                .rationale(R.string.bg_image_per)
                .onGranted {
                    RealPathUtil.getPath(activity, this)?.let { path ->
                        val imgFile = File(path)
                        success.invoke(imgFile.name, imgFile.readBytes())
                    }
                }
                .request()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        activity.toastOnUi(e.localizedMessage ?: "read uri error")
    }
}

/**
 * 读取URI
 */
fun Uri.read(fragment: Fragment, success: (name: String, bytes: ByteArray) -> Unit) {
    try {
        if (isContentScheme()) {
            val doc = DocumentFile.fromSingleUri(fragment.requireContext(), this)
            doc ?: error("未获取到文件")
            val name = doc.name ?: error("未获取到文件名")
            val fileBytes = DocumentUtils.readBytes(fragment.requireContext(), doc.uri)
            fileBytes ?: error("读取文件出错")
            success.invoke(name, fileBytes)
        } else {
            PermissionsCompat.Builder(fragment)
                .addPermissions(
                    Permissions.READ_EXTERNAL_STORAGE,
                    Permissions.WRITE_EXTERNAL_STORAGE
                )
                .rationale(R.string.bg_image_per)
                .onGranted {
                    RealPathUtil.getPath(fragment.requireContext(), this)?.let { path ->
                        val imgFile = File(path)
                        success.invoke(imgFile.name, imgFile.readBytes())
                    }
                }
                .request()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        fragment.toastOnUi(e.localizedMessage ?: "read uri error")
    }
}

@Throws(Exception::class)
fun Uri.readBytes(context: Context): ByteArray? {
    if (this.isContentScheme()) {
        return DocumentUtils.readBytes(context, this)
    } else {
        val path = RealPathUtil.getPath(context, this)
        if (path?.isNotEmpty() == true) {
            return File(path).readBytes()
        }
    }
    return null
}

@Throws(Exception::class)
fun Uri.readText(context: Context): String? {
    readBytes(context)?.let {
        return String(it)
    }
    return null
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