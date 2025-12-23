package io.legado.app.ui.file

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import io.legado.app.help.IntentData
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.utils.RealPathUtil
import io.legado.app.utils.externalFiles
import io.legado.app.utils.putJson
import splitties.init.appCtx

@Suppress("unused")
class HandleFileContract :
    ActivityResultContract<(HandleFileContract.HandleFileParam.() -> Unit)?, HandleFileContract.Result>() {

    private var requestCode: Int = 0

    override fun createIntent(context: Context, input: (HandleFileParam.() -> Unit)?): Intent {
        val intent = Intent(context, HandleFileActivity::class.java)
        val handleFileParam = HandleFileParam()
        input?.let {
            handleFileParam.apply(input)
        }
        if (handleFileParam.mode == IMAGE) {
            handleFileParam.allowExtensions = arrayOf("jpg", "png", "bmp", "webp")
        }
        handleFileParam.let {
            requestCode = it.requestCode
            intent.putExtra("mode", it.mode)
            intent.putExtra("title", it.title)
            intent.putExtra("allowExtensions", it.allowExtensions)
            intent.putJson("otherActions", it.otherActions)
            it.fileData?.let { fileData ->
                intent.putExtra("fileName", fileData.name)
                intent.putExtra("fileKey", IntentData.put(fileData.data))
                intent.putExtra("contentType", fileData.type)
            }
            intent.putExtra("value", it.value)
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Result {
        val uri = if (resultCode != RESULT_OK || intent?.data == null ||
            RealPathUtil.getTreePath(intent.data!!)
                ?.startsWith(appCtx.externalFiles.parent!!) == true
        ) {
            null
        } else {
            intent.data
        }
        return Result(uri, requestCode, intent?.getStringExtra("value"))
    }

    companion object {
        const val DIR = 0
        const val FILE = 1
        const val DIR_SYS = 2
        const val EXPORT = 3
        const val IMAGE = 4
    }

    @Suppress("ArrayInDataClass")
    data class HandleFileParam(
        var mode: Int = DIR,
        var title: String? = null,
        var allowExtensions: Array<String> = arrayOf(),
        var otherActions: ArrayList<SelectItem<Int>>? = null,
        var fileData: FileData? = null,
        var requestCode: Int = 0,
        var value: String? = null
    )

    data class Result(
        val uri: Uri?,
        val requestCode: Int,
        val value: String?
    )

    data class FileData(
        val name: String,
        val data: Any,
        val type: String
    )

}