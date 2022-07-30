package io.legado.app.ui.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.R
import io.legado.app.utils.*


class DonateFragment : PreferenceFragmentCompat() {

    private val zfbHbRwmUrl =
        "https://www.legado.top/assets/images/zfbhbrwm-6dfbcd1d680cfd831b93490a91052656.png"
    private val zfbSkRwmUrl =
        "https://www.legado.top/assets/images/zfbskrwm-66379bdee8214093872696e413f6dda9.jpg"
    private val wxZsRwmUrl =
        "https://www.legado.top/assets/images/wxskrwm-d8e6963d6ae122a3c2e818f3c4bc09cf.jpg"
    private val qqSkRwmUrl =
        "https://www.legado.top/assets/images/qqskrwm-2c10b25f67f4354eec5ab5bd6080285f.jpg"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.donate)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "wxZsm" -> requireContext().openUrl(wxZsRwmUrl)
            "zfbHbRwm" -> requireContext().openUrl(zfbHbRwmUrl)
            "zfbSkRwm" -> requireContext().openUrl(zfbSkRwmUrl)
            "qqSkRwm" -> requireContext().openUrl(qqSkRwmUrl)
            "zfbHbSsm" -> getZfbHb(requireContext())
            "gzGzh" -> requireContext().sendToClip("开源阅读")
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun getZfbHb(context: Context) {
        requireContext().sendToClip("537954522")
        context.longToastOnUi("高级功能已开启\n红包码已复制\n支付宝首页搜索“537954522” 立即领红包")
        try {
            val packageManager = context.applicationContext.packageManager
            val intent = packageManager.getLaunchIntentForPackage("com.eg.android.AlipayGphone")!!
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printOnDebug()
        } finally {
            ACache.get(cacheDir = false)
                .put("proTime", System.currentTimeMillis())
        }
    }

}