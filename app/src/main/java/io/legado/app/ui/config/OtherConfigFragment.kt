package io.legado.app.ui.config

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.preference.ListPreference
import androidx.preference.Preference
import io.legado.app.R
import io.legado.app.base.BasePreferenceFragment
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import io.legado.app.lib.theme.ATH
import io.legado.app.receiver.SharedReceiverActivity
import io.legado.app.service.WebService
import io.legado.app.ui.main.MainActivity
import io.legado.app.ui.widget.image.CoverImageView
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.utils.*
import splitties.init.appCtx
import java.io.File


class OtherConfigFragment : BasePreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val packageManager = appCtx.packageManager
    private val componentName = ComponentName(
        appCtx,
        SharedReceiverActivity::class.java.name
    )
    private val webPort get() = getPrefInt(PreferKey.webPort, 1122)
    private val selectCoverImage = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it ?: return@registerForActivityResult
        setCoverFromUri(it)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        putPrefBoolean(PreferKey.processText, isProcessTextEnabled())
        addPreferencesFromResource(R.xml.pref_config_other)
        upPreferenceSummary(PreferKey.userAgent, AppConfig.userAgent)
        upPreferenceSummary(PreferKey.preDownloadNum, AppConfig.preDownloadNum.toString())
        upPreferenceSummary(PreferKey.threadCount, AppConfig.threadCount.toString())
        upPreferenceSummary(PreferKey.webPort, webPort.toString())
        upPreferenceSummary(PreferKey.defaultCover, getPrefString(PreferKey.defaultCover))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        ATH.applyEdgeEffectColor(listView)
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            PreferKey.userAgent -> showUserAgentDialog()
            PreferKey.preDownloadNum -> NumberPickerDialog(requireContext())
                .setTitle(getString(R.string.pre_download))
                .setMaxValue(9999)
                .setMinValue(1)
                .setValue(AppConfig.preDownloadNum)
                .show {
                    AppConfig.preDownloadNum = it
                }
            PreferKey.threadCount -> NumberPickerDialog(requireContext())
                .setTitle(getString(R.string.threads_num_title))
                .setMaxValue(999)
                .setMinValue(1)
                .setValue(AppConfig.threadCount)
                .show {
                    AppConfig.threadCount = it
                }
            PreferKey.webPort -> NumberPickerDialog(requireContext())
                .setTitle(getString(R.string.web_port_title))
                .setMaxValue(60000)
                .setMinValue(1024)
                .setValue(webPort)
                .show {
                    putPrefInt(PreferKey.webPort, it)
                }
            PreferKey.cleanCache -> clearCache()
            PreferKey.defaultCover -> if (getPrefString(PreferKey.defaultCover).isNullOrEmpty()) {
                selectCoverImage.launch("image/*")
            } else {
                selector(items = arrayListOf("删除图片", "选择图片")) { _, i ->
                    if (i == 0) {
                        removePref(PreferKey.defaultCover)
                    } else {
                        selectCoverImage.launch("image/*")
                    }
                }
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferKey.preDownloadNum -> {
                upPreferenceSummary(key, AppConfig.preDownloadNum.toString())
            }
            PreferKey.threadCount -> {
                upPreferenceSummary(key, AppConfig.threadCount.toString())
                postEvent(PreferKey.threadCount, "")
            }
            PreferKey.webPort -> {
                upPreferenceSummary(key, webPort.toString())
                if (WebService.isRun) {
                    WebService.stop(requireContext())
                    WebService.start(requireContext())
                }
            }
            PreferKey.recordLog -> LogUtils.upLevel()
            PreferKey.processText -> sharedPreferences?.let {
                setProcessTextEnable(it.getBoolean(key, true))
            }
            PreferKey.showRss -> postEvent(EventBus.SHOW_RSS, "")
            PreferKey.defaultCover -> upPreferenceSummary(
                key, getPrefString(PreferKey.defaultCover)
            )
            PreferKey.language -> listView.postDelayed({
                LanguageUtils.setConfiguration(appCtx)
                val intent = Intent(appCtx, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                appCtx.startActivity(intent)
                Process.killProcess(Process.myPid())
            }, 1000)
            PreferKey.userAgent -> listView.post {
                upPreferenceSummary(PreferKey.userAgent, AppConfig.userAgent)
            }
        }
    }

    private fun upPreferenceSummary(preferenceKey: String, value: String?) {
        val preference = findPreference<Preference>(preferenceKey) ?: return
        when (preferenceKey) {
            PreferKey.preDownloadNum -> preference.summary =
                getString(R.string.pre_download_s, value)
            PreferKey.threadCount -> preference.summary = getString(R.string.threads_num, value)
            PreferKey.webPort -> preference.summary = getString(R.string.web_port_summary, value)
            else -> if (preference is ListPreference) {
                val index = preference.findIndexOfValue(value)
                // Set the summary to reflect the new value.
                preference.summary = if (index >= 0) preference.entries[index] else null
            } else {
                preference.summary = value
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showUserAgentDialog() {
        alert("UserAgent") {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater)
            alertBinding.editView.setText(AppConfig.userAgent)
            customView { alertBinding.root }
            okButton {
                val userAgent = alertBinding.editView.text?.toString()
                if (userAgent.isNullOrBlank()) {
                    removePref(PreferKey.userAgent)
                } else {
                    putPrefString(PreferKey.userAgent, userAgent)
                }
            }
            noButton()
        }.show()
    }

    private fun clearCache() {
        requireContext().alert(
            titleResource = R.string.clear_cache,
            messageResource = R.string.sure_del
        ) {
            okButton {
                BookHelp.clearCache()
                FileUtils.deleteFile(requireActivity().cacheDir.absolutePath)
                toastOnUi(R.string.clear_cache_success)
            }
            noButton()
        }.show()
    }

    private fun isProcessTextEnabled(): Boolean {
        return packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    private fun setProcessTextEnable(enable: Boolean) {
        if (enable) {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
            )
        } else {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun setCoverFromUri(uri: Uri) {
        if (uri.isContentScheme()) {
            val doc = DocumentFile.fromSingleUri(requireContext(), uri)
            doc?.name?.let {
                var file = requireContext().externalFilesDir
                file = FileUtils.createFileIfNotExist(file, "covers", it)
                kotlin.runCatching {
                    DocumentUtils.readBytes(requireContext(), doc.uri)
                }.getOrNull()?.let { byteArray ->
                    file.writeBytes(byteArray)
                    putPrefString(PreferKey.defaultCover, file.absolutePath)
                    CoverImageView.upDefaultCover()
                } ?: toastOnUi("获取文件出错")
            }
        } else {
            PermissionsCompat.Builder(this)
                .addPermissions(
                    Permissions.READ_EXTERNAL_STORAGE,
                    Permissions.WRITE_EXTERNAL_STORAGE
                )
                .rationale(R.string.bg_image_per)
                .onGranted {
                    RealPathUtil.getPath(requireContext(), uri)?.let { path ->
                        val imgFile = File(path)
                        if (imgFile.exists()) {
                            var file = requireContext().externalFilesDir
                            file = FileUtils.createFileIfNotExist(file, "covers", imgFile.name)
                            file.writeBytes(imgFile.readBytes())
                            putPrefString(PreferKey.defaultCover, file.absolutePath)
                            CoverImageView.upDefaultCover()
                        }
                    }
                }
                .request()
        }
    }

}