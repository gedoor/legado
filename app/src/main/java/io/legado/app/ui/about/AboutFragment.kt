package io.legado.app.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.R
import io.legado.app.constant.AppConst.appInfo
import io.legado.app.help.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.*

class AboutFragment : PreferenceFragmentCompat() {

    private val licenseUrl = "https://github.com/gedoor/legado/blob/master/LICENSE"
    private val disclaimerUrl = "https://gedoor.github.io/MyBookshelf/disclaimer.html"
    private val qqGroups = linkedMapOf(
        Pair("(QQ群VIP1)701903217", "-iolizL4cbJSutKRpeImHlXlpLDZnzeF"),
        Pair("(QQ群VIP2)263949160", "xwfh7_csb2Gf3Aw2qexEcEtviLfLfd4L"),
        Pair("(QQ群VIP3)680280282", "_N0i7yZObjKSeZQvzoe2ej7j02kLnOOK"),
        Pair("(QQ群VIP4)682555679", "VF2UwvUCuaqlo6pddWTe_kw__a1_Fr8O"),
        Pair("(QQ群1)805192012", "6GlFKjLeIk5RhQnR3PNVDaKB6j10royo"),
        Pair("(QQ群2)773736122", "5Bm5w6OgLupXnICbYvbgzpPUgf0UlsJF"),
        Pair("(QQ群3)981838750", "g_Sgmp2nQPKqcZQ5qPcKLHziwX_mpps9"),
        Pair("(QQ群4)256929088", "czEJPLDnT4Pd9SKQ6RoRVzKhDxLchZrO"),
        Pair("(QQ群5)811843556", "zKZ2UYGZ7o5CzcA6ylxzlqi21si_iqaX"),
        Pair("(QQ群6)870270970", "FeCF8iSxfQbe90HPvGsvcqs5P5oSeY5n"),
        Pair("(QQ群7)15987187", "S2g2TMD0LGd3sefUADd1AbyPEW2o2XfC"),
        Pair("(QQ群8)1079926194", "gg2qFH8q9IPFaCHV3H7CqCN-YljvazE1"),
        Pair("(QQ群9)892108780", "Ci_O3aysKjEBfplOWeCud-rxl71TjU2Q")
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)
        findPreference<Preference>("update_log")?.summary =
            "${getString(R.string.version)} ${appInfo.versionName}"
        if (AppConfig.isGooglePlay) {
            preferenceScreen.removePreferenceRecursively("check_update")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "contributors" -> openUrl(R.string.contributors_url)
            "update_log" -> showUpdateLog()
            "check_update" -> openUrl(R.string.latest_release_url)
            "mail" -> requireContext().sendMail("kunfei.ge@gmail.com")
            "sourceRuleSummary" -> openUrl(R.string.source_rule_url)
            "git" -> openUrl(R.string.this_github_url)
            "home_page" -> openUrl(R.string.home_page_url)
            "license" -> requireContext().openUrl(licenseUrl)
            "disclaimer" -> requireContext().openUrl(disclaimerUrl)
            "qq" -> showQqGroups()
            "gzGzh" -> requireContext().sendToClip(getString(R.string.legado_gzh))
            "crashLog" -> showCrashLogs()
            "tg" -> openUrl(R.string.tg_url)
            "discord" -> openUrl(R.string.discord_url)
        }
        return super.onPreferenceTreeClick(preference)
    }

    @Suppress("SameParameterValue")
    private fun openUrl(@StringRes addressID: Int) {
        requireContext().openUrl(getString(addressID))
    }

    private fun showUpdateLog() {
        val log = String(requireContext().assets.open("updateLog.md").readBytes())
        TextDialog.show(childFragmentManager, log, TextDialog.MD)
    }

    private fun showQqGroups() {
        alert(titleResource = R.string.join_qq_group) {
            val names = arrayListOf<String>()
            qqGroups.forEach {
                names.add(it.key)
            }
            items(names) { _, index ->
                qqGroups[names[index]]?.let {
                    if (!joinQQGroup(it)) {
                        requireContext().sendToClip(it)
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
        kotlin.runCatching {
            startActivity(intent)
            return true
        }.onFailure {
            toastOnUi("添加失败,请手动添加")
        }
        return false
    }

    private fun showCrashLogs() {
        context?.externalCacheDir?.let { exCacheDir ->
            val crashDir = FileUtils.getFile(exCacheDir, "crash")
            val crashLogs = crashDir.listFiles()
            val crashLogNames = arrayListOf<String>()
            crashLogs?.forEach {
                crashLogNames.add(it.name)
            }
            context?.selector(R.string.crash_log, crashLogNames) { _, select ->
                crashLogs?.getOrNull(select)?.let { logFile ->
                    TextDialog.show(childFragmentManager, logFile.readText())
                }
            }
        }
    }

}