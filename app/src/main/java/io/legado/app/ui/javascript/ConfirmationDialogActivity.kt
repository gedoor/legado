package io.legado.app.ui.javascript

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.ui.widget.text.AccentTextView
import io.legado.app.utils.toastOnUi


class ConfirmationDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.confirmation_dialog)

        val url = intent?.dataString
        if (url.isNullOrBlank()) {
            finish()
            return
        }

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        findViewById<AccentTextView>(R.id.btn_negative).setOnClickListener { finish() }

        findViewById<AccentTextView>(R.id.btn_positive).setOnClickListener {
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (e: ActivityNotFoundException) {
                toastOnUi(R.string.can_not_open)
            } catch (e: Exception) {
                AppLog.put("打开链接失败", e)
                toastOnUi(R.string.error_occurred)
            }
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
