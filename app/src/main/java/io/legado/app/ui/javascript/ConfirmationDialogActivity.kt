package io.legado.app.ui.javascript

import android.content.ActivityNotFoundException
import android.content.Intent
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
        setContentView(R.layout.confirmation_dialog)

        val messageView = findViewById<TextView>(R.id.message)
        val toolbar = findViewById<Toolbar>(R.id.tool_bar)
        val btnNegative = findViewById<AccentTextView>(R.id.btn_negative)
        val btnPositive = findViewById<AccentTextView>(R.id.btn_positive)

        // 获取原始 Intent 的数据和 MIME 类型
        val uri = intent?.data
        val mimeType = intent?.getStringExtra("mimeType")
        if (uri == null) {
            finish()
            return
        }

        // 处理来源标签显示
        val sourceTag = intent.getStringExtra("sourceTag").takeIf { !it.isNullOrBlank() } ?: "当前来源"
        messageView.text = "$sourceTag 正在请求跳转外部链接/应用，是否跳转？"

        toolbar.setNavigationOnClickListener { finish() }
        btnNegative.setOnClickListener { finish() }
        btnPositive.setOnClickListener {
            try {
                // 创建目标 Intent 并设置类型
                val targetIntent = Intent(Intent.ACTION_VIEW).apply {
                    // 同时设置 Data 和 Type
                    if (!mimeType.isNullOrBlank()) {
                        setDataAndType(uri, mimeType)
                    } else {
                        data = uri
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                // 验证是否有应用可以处理
                if (targetIntent.resolveActivity(packageManager) != null) {
                    startActivity(targetIntent)
                } else {
                    toastOnUi(R.string.can_not_open)
                }
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
