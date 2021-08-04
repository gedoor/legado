package io.legado.app.ui.document

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

@Suppress("unused")
class FilePicker : ActivityResultContract<FilePickerParam, Uri?>() {

    companion object {
        const val DIRECTORY = 0
        const val FILE = 1
    }

    override fun createIntent(context: Context, input: FilePickerParam?): Intent {
        val intent = Intent(context, FilePickerActivity::class.java)
        input?.let {
            intent.putExtra("mode", it.mode)
            intent.putExtra("title", it.title)
            intent.putExtra("allowExtensions", it.allowExtensions)
            intent.putExtra("otherActions", it.otherActions)
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode == RESULT_OK) {
            return intent?.data
        }
        return null
    }

}

@Suppress("ArrayInDataClass")
data class FilePickerParam(
    var mode: Int = 0,
    var title: String? = null,
    var allowExtensions: Array<String> = arrayOf(),
    var otherActions: Array<String>? = null,
)
