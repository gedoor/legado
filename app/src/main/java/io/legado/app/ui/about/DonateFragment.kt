package io.legado.app.ui.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import io.legado.app.R
import io.legado.app.utils.ACache
import io.legado.app.utils.PreferenceFragmentSupport
import io.legado.app.utils.openUrl
import io.legado.app.utils.sendToClip
import org.jetbrains.anko.longToast

class DonateFragment : PreferenceFragmentSupport() {

    private val zfbHbRwmUrl = "https://gitee.com/gekunfei/Donate/raw/master/zfbhbrwm.png"
    private val zfbSkRwmUrl = "https://gitee.com/gekunfei/Donate/raw/master/zfbskrwm.jpg"
    private val wxZsRwmUrl = "https://gitee.com/gekunfei/Donate/raw/master/wxskrwm.jpg"
    private val qqSkRwmUrl = "https://gitee.com/gekunfei/Donate/raw/master/qqskrwm.jpg"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.donate)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
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
        context.longToast("高级功能已开启\n红包码已复制\n支付宝首页搜索“537954522” 立即领红包")
        try {
            val packageManager = context.applicationContext.packageManager
            val intent = packageManager.getLaunchIntentForPackage("com.eg.android.AlipayGphone")!!
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            ACache.get(requireContext(), cacheDir = false)
                .put("proTime", System.currentTimeMillis())
        }
    }

}