package io.legado.app.utils

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract

class SelectImageContract : ActivityResultContract<Int, Pair<Int?, Uri?>?>() {

    var requestCode: Int? = null

    override fun createIntent(context: Context, input: Int?): Intent {
        requestCode = input
        return Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("image/*")
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Pair<Int?, Uri?>? {
        if (resultCode == RESULT_OK) {
            return Pair(requestCode, intent?.data)
        }
        return null
    }

}

class StartActivityForResult(private val cls: Class<*>) :
    ActivityResultContract<Intent.() -> Unit, ActivityResult>() {

    override fun createIntent(context: Context, input: Intent.() -> Unit): Intent {
        val intent = Intent(context, cls)
        intent.apply(input)
        return intent
    }

    override fun parseResult(
        resultCode: Int, intent: Intent?
    ): ActivityResult {
        return ActivityResult(resultCode, intent)
    }

}