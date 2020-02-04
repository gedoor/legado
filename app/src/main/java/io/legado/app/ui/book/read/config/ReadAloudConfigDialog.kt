package io.legado.app.ui.book.read.config

import android.content.SharedPreferences
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.R
import io.legado.app.constant.Bus
import io.legado.app.constant.PreferKey
import io.legado.app.lib.theme.ATH
import io.legado.app.service.BaseReadAloudService
import io.legado.app.service.help.ReadAloud
import io.legado.app.ui.book.read.Help
import io.legado.app.utils.getPrefString
import io.legado.app.utils.postEvent

class ReadAloudConfigDialog : DialogFragment() {
    private val readAloudPreferTag = "readAloudPreferTag"

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.let {
            Help.upSystemUiVisibility(it)
            it.windowManager?.defaultDisplay?.getMetrics(dm)
        }
        dialog?.window?.let {
            it.setBackgroundDrawableResource(R.color.transparent)
            it.setLayout((dm.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = LinearLayout(context)
        view.setBackgroundResource(R.color.background)
        view.id = R.id.tag1
        container?.addView(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var preferenceFragment = childFragmentManager.findFragmentByTag(readAloudPreferTag)
        if (preferenceFragment == null) preferenceFragment = ReadAloudPreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(view.id, preferenceFragment, readAloudPreferTag)
            .commit()
    }

    class ReadAloudPreferenceFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_config_aloud)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            ATH.applyEdgeEffectColor(listView)
            bindPreferenceSummaryToValue(findPreference(PreferKey.ttsSpeechPer))
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            when (key) {
                PreferKey.readAloudByPage -> {
                    if (BaseReadAloudService.isRun) {
                        postEvent(Bus.MEDIA_BUTTON, false)
                    }
                }
                PreferKey.readAloudOnLine -> {
                    ReadAloud.stop(requireContext())
                    ReadAloud.aloudClass = ReadAloud.getReadAloudClass()
                }
                PreferKey.ttsSpeechPer -> ReadAloud.upTtsSpeechRate(requireContext())
            }
        }

        override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
            val stringValue = newValue.toString()

            if (preference is ListPreference) {
                val index = preference.findIndexOfValue(stringValue)
                // Set the summary to reflect the new value.
                preference.setSummary(if (index >= 0) preference.entries[index] else null)
            } else {
                // For all other preferences, set the summary to the value's
                preference?.summary = stringValue
            }
            return false
        }

        private fun bindPreferenceSummaryToValue(preference: Preference?) {
            preference?.apply {
                onPreferenceChangeListener = this@ReadAloudPreferenceFragment
                onPreferenceChange(
                    this,
                    context.getPrefString(key)
                )
            }
        }

    }
}