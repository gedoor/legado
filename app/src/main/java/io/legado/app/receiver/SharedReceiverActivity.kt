package io.legado.app.receiver

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.main.MainActivity
import io.legado.app.utils.startActivity
import splitties.init.appCtx

class SharedReceiverActivity : AppCompatActivity() {

    private val receivingType = "text/plain"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
        finish()
    }

    private fun initIntent() {
        when {
            Intent.ACTION_SEND == intent.action && intent.type == receivingType -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                    if (openUrl(it)) {
                        startActivity<SearchActivity> {
                            putExtra("key", it)
                        }
                    }
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && Intent.ACTION_PROCESS_TEXT == intent.action
                    && intent.type == receivingType -> {
                intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)?.let {
                    if (openUrl(it)) {
                        startActivity<SearchActivity> {
                            putExtra("key", it)
                        }
                    }
                }
            }
            intent.getStringExtra("action") == "readAloud" -> {
                MediaButtonReceiver.readAloud(appCtx, false)
            }
        }
    }

    private fun openUrl(text: String): Boolean {
        if (text.isBlank()) {
            return false
        }
        val urls = text.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val result = StringBuilder()
        for (url in urls) {
            if (url.matches("http.+".toRegex()))
                result.append("\n").append(url.trim { it <= ' ' })
        }
        return if (result.length > 1) {
            startActivity<MainActivity>()
            false
        } else {
            true
        }
    }
}