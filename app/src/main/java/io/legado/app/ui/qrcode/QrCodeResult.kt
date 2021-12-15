package io.legado.app.ui.qrcode

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class QrCodeResult : ActivityResultContract<Unit?, String?>() {

    override fun createIntent(context: Context, input: Unit?): Intent {
        return Intent(context, QrCodeActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode == RESULT_OK) {
            intent?.getStringExtra("result")?.let {
                return it
            }
        }
        return null
    }

}