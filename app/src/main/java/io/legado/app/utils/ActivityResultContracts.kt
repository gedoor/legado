package io.legado.app.utils

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract

fun ActivityResultLauncher<*>.launch() {
    launch(null)
}

class SelectImageContract : ActivityResultContract<Int?, SelectImageContract.Result>() {

    var requestCode: Int? = null

    override fun createIntent(context: Context, input: Int?): Intent {
        requestCode = input
        return Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("image/*")
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Result {
        if (resultCode == RESULT_OK) {
            return Result(requestCode, intent?.data)
        }
        return Result(requestCode, null)
    }

    data class Result(
        val requestCode: Int?,
        val uri: Uri? = null
    )

}

class StartActivityContract(private val cls: Class<*>) :
    ActivityResultContract<(Intent.() -> Unit)?, ActivityResult>() {

    override fun createIntent(context: Context, input: (Intent.() -> Unit)?): Intent {
        val intent = Intent(context, cls)
        input?.let {
            intent.apply(input)
        }
        return intent
    }

    override fun parseResult(
        resultCode: Int, intent: Intent?
    ): ActivityResult {
        return ActivityResult(resultCode, intent)
    }

}
