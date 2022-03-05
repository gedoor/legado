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
import io.legado.app.utils.*

class WelcomeConfigFragment : BasePreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val requestWelcomeImage = 221
    private val requestWelcomeImageDark = 222
    private val selectImage = registerForActivityResult(SelectImageContract()) {
        it.uri?.let { uri ->
            when (it.requestCode) {
                requestWelcomeImage -> setCoverFromUri(PreferKey.defaultCover, uri)
                requestWelcomeImageDark -> setCoverFromUri(PreferKey.defaultCoverDark, uri)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_welcome)
        upPreferenceSummary(PreferKey.welcomeImage, getPrefString(PreferKey.welcomeImage))
        upPreferenceSummary(PreferKey.welcomeImageDark, getPrefString(PreferKey.welcomeImageDark))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.welcome_style)
        listView.setEdgeEffectColor(primaryColor)
        setHasOptionsMenu(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            PreferKey.welcomeImage,
            PreferKey.welcomeImageDark -> {
                upPreferenceSummary(key, getPrefString(key))
            }
        }
    }

    @SuppressLint("PrivateResource")
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            PreferKey.welcomeImage ->
                if (getPrefString(preference.key).isNullOrEmpty()) {
                    selectImage.launch(requestWelcomeImage)
                } else {
                    context?.selector(
                        items = arrayListOf(
                            getString(R.string.delete),
                            getString(R.string.select_image)
                        )
                    ) { _, i ->
                        if (i == 0) {
                            removePref(preference.key)
                            BookCover.upDefaultCover()
                        } else {
                            selectImage.launch(requestWelcomeImage)
                        }
                    }
                }
            PreferKey.welcomeImageDark ->
                if (getPrefString(preference.key).isNullOrEmpty()) {
                    selectImage.launch(requestWelcomeImageDark)
                } else {
                    context?.selector(
                        items = arrayListOf(
                            getString(R.string.delete),
                            getString(R.string.select_image)
                        )
                    ) { _, i ->
                        if (i == 0) {
                            removePref(preference.key)
                            BookCover.upDefaultCover()
                        } else {
                            selectImage.launch(requestWelcomeImageDark)
                        }
                    }
                }
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun upPreferenceSummary(preferenceKey: String, value: String?) {
        val preference = findPreference<Preference>(preferenceKey) ?: return
        when (preferenceKey) {
            PreferKey.welcomeImage,
            PreferKey.welcomeImageDark -> preference.summary = if (value.isNullOrBlank()) {
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
        }
    }

}