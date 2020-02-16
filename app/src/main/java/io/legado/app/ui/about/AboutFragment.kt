package io.legado.app.ui.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.toast

class AboutFragment : PreferenceFragmentCompat() {

    private val licenseUrl = "https://github.com/gedoor/legado/blob/master/LICENSE"
    private val disclaimerUrl = "https://gedoor.github.io/MyBookshelf/disclaimer.html"
    private val qqGroups = linkedMapOf(
        Pair("(QQ群VIP1)701903217", "-iolizL4cbJSutKRpeImHlXlpLDZnzeF"),
        Pair("(QQ群VIP2)263949160", "xwfh7_csb2Gf3Aw2qexEcEtviLfLfd4L"),
        Pair("(QQ群1)805192012", "6GlFKjLeIk5RhQnR3PNVDaKB6j10royo"),
        Pair("(QQ群2)773736122", "5Bm5w6OgLupXnICbYvbgzpPUgf0UlsJF"),
        Pair("(QQ群3)981838750", "g_Sgmp2nQPKqcZQ5qPcKLHziwX_mpps9"),
        Pair("(QQ群4)256929088", "czEJPLDnT4Pd9SKQ6RoRVzKhDxLchZrO"),
        Pair("(QQ群5)811843556", "zKZ2UYGZ7o5CzcA6ylxzlqi21si_iqaX")
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)
        findPreference<Preference>("check_update")?.summary =
            "${getString(R.string.version)} ${App.INSTANCE.versionName}"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "contributors" -> openIntent(Intent.ACTION_VIEW, R.string.contributors_url)
            "update_log" -> showUpdateLog()
            "check_update" -> openIntent(Intent.ACTION_VIEW, R.string.latest_release_url)
            "mail" -> openIntent(Intent.ACTION_SENDTO, "mailto:kunfei.ge@gmail.com")
            "git" -> openIntent(Intent.ACTION_VIEW, R.string.this_github_url)
            "home_page" -> openIntent(Intent.ACTION_VIEW, R.string.home_page_url)
            "license" -> openIntent(Intent.ACTION_VIEW, licenseUrl)
            "disclaimer" -> openIntent(Intent.ACTION_VIEW, disclaimerUrl)
            "qq" -> showQqGroups()
            "gzGzh" -> sendToClip("开源阅读软件")
        }
        return super.onPreferenceTreeClick(preference)
    }

    @Suppress("SameParameterValue")
    private fun openIntent(intentName: String, @StringRes addressID: Int) {
        openIntent(intentName, getString(addressID))
    }

    private fun openIntent(intentName: String, address: String) {
        try {
            val intent = Intent(intentName)
            intent.data = Uri.parse(address)
            startActivity(intent)
        } catch (e: Exception) {
            toast(R.string.can_not_open)
        }
    }

    private fun showUpdateLog() {
        val log = String(requireContext().assets.open("updateLog.md").readBytes())
        TextDialog.show(childFragmentManager, log, TextDialog.MD)
    }

    private fun showQqGroups() {
        alert(title = R.string.join_qq_group) {
            val names = arrayListOf<String>()
            qqGroups.forEach {
                names.add(it.key)
            }
            items(names) { _, index ->
                qqGroups[names[index]]?.let {
                    if (!joinQQGroup(it)) {
                        sendToClip(it)
                    }
                }
            }
        }.show()
    }

    private fun joinQQGroup(key: String): Boolean {
        val intent = Intent()
        intent.data =
            Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D$key")
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            startActivity(intent)
            false
        } catch (e: java.lang.Exception) {
            true
        }
    }

    private fun sendToClip(text: String) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = ClipData.newPlainText(null, text)
        clipboard?.let {
            clipboard.setPrimaryClip(clipData)
            toast(R.string.copy_complete)
        }
    }
}