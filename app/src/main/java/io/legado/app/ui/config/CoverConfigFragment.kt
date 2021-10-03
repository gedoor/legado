package io.legado.app.ui.config

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import io.legado.app.R
import io.legado.app.base.BasePreferenceFragment
import io.legado.app.constant.PreferKey
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.primaryColor
import io.legado.app.model.BookCover
import io.legado.app.ui.widget.prefs.SwitchPreference
import io.legado.app.utils.*

class CoverConfigFragment : BasePreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val requestCodeCover = 111
    private val requestCodeCoverDark = 112
    private val selectImage = registerForActivityResult(SelectImageContract()) {
        val uri = it?.second ?: return@registerForActivityResult
        when (it.first) {
            requestCodeCover -> setCoverFromUri(PreferKey.defaultCover, uri)
            requestCodeCoverDark -> setCoverFromUri(PreferKey.defaultCoverDark, uri)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_cover)
        upPreferenceSummary(PreferKey.defaultCover, getPrefString(PreferKey.defaultCover))
        upPreferenceSummary(PreferKey.defaultCoverDark, getPrefString(PreferKey.defaultCoverDark))
        findPreference<SwitchPreference>(PreferKey.coverShowAuthor)
            ?.isEnabled = getPrefBoolean(PreferKey.coverShowName)
        findPreference<SwitchPreference>(PreferKey.coverShowAuthorN)
            ?.isEnabled = getPrefBoolean(PreferKey.coverShowNameN)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.cover_config)
        listView.setEdgeEffectColor(primaryColor)
        setHasOptionsMenu(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            PreferKey.defaultCover,
            PreferKey.defaultCoverDark -> {
                upPreferenceSummary(key, getPrefString(key))
            }
            PreferKey.coverShowName -> {
                findPreference<SwitchPreference>(PreferKey.coverShowAuthor)
                    ?.isEnabled = getPrefBoolean(key)
                BookCover.upDefaultCover()
            }
            PreferKey.coverShowNameN -> {
                findPreference<SwitchPreference>(PreferKey.coverShowAuthorN)
                    ?.isEnabled = getPrefBoolean(key)
                BookCover.upDefaultCover()
            }
            PreferKey.coverShowAuthor,
            PreferKey.coverShowAuthorN -> {
                BookCover.upDefaultCover()
            }
        }
    }

    @SuppressLint("PrivateResource")
    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (val key = preference?.key) {
            PreferKey.defaultCover ->
                if (getPrefString(PreferKey.defaultCover).isNullOrEmpty()) {
                    selectImage.launch(requestCodeCover)
                } else {
                    context?.selector(items = arrayListOf("删除图片", "选择图片")) { _, i ->
                        if (i == 0) {
                            removePref(PreferKey.defaultCover)
                            BookCover.upDefaultCover()
                        } else {
                            selectImage.launch(requestCodeCover)
                        }
                    }
                }
            PreferKey.defaultCoverDark ->
                if (getPrefString(PreferKey.defaultCoverDark).isNullOrEmpty()) {
                    selectImage.launch(requestCodeCoverDark)
                } else {
                    context?.selector(items = arrayListOf("删除图片", "选择图片")) { _, i ->
                        if (i == 0) {
                            removePref(PreferKey.defaultCoverDark)
                            BookCover.upDefaultCover()
                        } else {
                            selectImage.launch(requestCodeCoverDark)
                        }
                    }
                }
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun upPreferenceSummary(preferenceKey: String, value: String?) {
        val preference = findPreference<Preference>(preferenceKey) ?: return
        when (preferenceKey) {
            PreferKey.defaultCover,
            PreferKey.defaultCoverDark -> preference.summary = if (value.isNullOrBlank()) {
                getString(R.string.select_image)
            } else {
                value
            }
            else -> preference.summary = value
        }
    }

    private fun setCoverFromUri(preferenceKey: String, uri: Uri) {
        readUri(uri) { name, bytes ->
            var file = requireContext().externalFiles
            file = FileUtils.createFileIfNotExist(file, "covers", name)
            file.writeBytes(bytes)
            putPrefString(preferenceKey, file.absolutePath)
            BookCover.upDefaultCover()
        }
    }

}