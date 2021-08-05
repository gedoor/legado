package io.legado.app.utils

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

object ActivityResultContractUtils {

    class SelectImage : ActivityResultContract<Int, Pair<Int?, Uri?>?>() {

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


}