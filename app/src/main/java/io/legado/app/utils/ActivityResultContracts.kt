package io.legado.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

fun <T> ActivityResultLauncher<T?>.launch() {
    launch(null)
}

class SelectImageContract : ActivityResultContract<Int?, SelectImageContract.Result>() {

    private val delegate = ActivityResultContracts.PickVisualMedia()
    var requestCode: Int? = null

    override fun createIntent(context: Context, input: Int?): Intent {
        requestCode = input
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        return delegate.createIntent(context, request)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Result {
        return Result(requestCode, delegate.parseResult(resultCode, intent))
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
