package io.legado.app.ui.about


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.ACache
import kotlinx.android.synthetic.main.activity_donate.*
import org.jetbrains.anko.toast
import java.net.URLEncoder

/**
 * Created by GKF on 2018/1/13.
 * 捐赠页面
 */

class DonateActivity : BaseActivity(R.layout.activity_donate) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        ATH.applyEdgeEffectColor(scroll_view)
        vw_zfb_tz.setOnClickListener { aliDonate(this) }
        cv_wx_gzh.setOnClickListener {
            val clipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipData = ClipData.newPlainText(null, "开源阅读软件")
            clipboard?.let {
                clipboard.setPrimaryClip(clipData)
                toast(R.string.copy_complete)
            }
        }
        vw_zfb_hb.setOnClickListener { openActionViewIntent("https://gedoor.github.io/MyBookshelf/zfbhbrwm.png") }
        vw_zfb_rwm.setOnClickListener { openActionViewIntent("https://gedoor.github.io/MyBookshelf/zfbskrwm.jpg") }
        vw_wx_rwm.setOnClickListener { openActionViewIntent("https://gedoor.github.io/MyBookshelf/wxskrwm.jpg") }
        vw_qq_rwm.setOnClickListener { openActionViewIntent("https://gedoor.github.io/MyBookshelf/qqskrwm.jpg") }
        vw_zfb_hb_ssm.setOnClickListener { getZfbHb(this) }
    }

    private fun getZfbHb(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = ClipData.newPlainText(null, "537954522")
        clipboard?.let {
            clipboard.setPrimaryClip(clipData)
            Toast.makeText(context, "高级功能已开启\n红包码已复制\n支付宝首页搜索“537954522” 立即领红包", Toast.LENGTH_LONG)
                .show()
        }
        try {
            val packageManager = context.applicationContext.packageManager
            val intent = packageManager.getLaunchIntentForPackage("com.eg.android.AlipayGphone")!!
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            ACache.get(this, cacheDir = false).put("proTime", System.currentTimeMillis())
        }
    }

    private fun openActionViewIntent(address: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(address)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, R.string.can_not_open, Toast.LENGTH_SHORT).show()
        }

    }

    private fun aliDonate(context: Context) {
        try {
            val qrCode = URLEncoder.encode("tsx06677nwdk3javroq4ef0", "utf-8")
            val aliPayQr =
                "alipayqr://platformapi/startapp?saId=10000007&qrcode=https://qr.alipay.com/$qrCode"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(aliPayQr))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}
