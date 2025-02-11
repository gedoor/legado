package io.legado.app.ui.javascript

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.ui.widget.text.AccentTextView
import io.legado.app.utils.toastOnUi


class ConfirmationDialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 必须先设置布局再获取控件
        setContentView(R.layout.confirmation_dialog)

        // 正确顺序：先设置布局再获取视图
        val messageView = findViewById<TextView>(R.id.message)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val btnNegative = findViewById<AccentTextView>(R.id.btn_negative)
        val btnPositive = findViewById<AccentTextView>(R.id.btn_positive)

        val url = intent?.dataString
        if (url.isNullOrBlank()) {
            finish()
            return
        }

        // 处理来源标签显示
        val sourceTag = intent.getStringExtra("sourceTag").takeIf { !it.isNullOrBlank() } ?: "当前来源"
        messageView.text = "$sourceTag 正在请求跳转外部链接/应用，是否跳转？"

        // 设置其他组件
        toolbar.setNavigationOnClickListener { finish() }
        btnNegative.setOnClickListener { finish() }
        btnPositive.setOnClickListener {
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
