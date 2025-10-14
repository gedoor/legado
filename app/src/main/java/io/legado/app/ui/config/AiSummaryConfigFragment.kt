package io.legado.app.ui.config

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.gson.Gson
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.prefs.PathPreference
import io.legado.app.ui.book.read.content.AiSummaryProvider
import io.legado.app.ui.browser.WebViewActivity
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiSummaryConfigFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val TAG = "AiSummaryConfig"
    private lateinit var openDirectoryLauncher: ActivityResultLauncher<Uri?>
    private lateinit var modelsUrlPreference: EditTextPreference
    private lateinit var customModelPreference: EditTextPreference
    private lateinit var modelIdPreference: ListPreference

    private val gson = Gson()
    private val originalSummaries = mutableMapOf<String, CharSequence?>()

    private data class ModelsResponse(val data: List<ModelData>?)
    private data class ModelData(val id: String?)

    private val summaryKeys = arrayOf(
        PreferKey.aiSummaryApiKey,
        PreferKey.aiSummaryApiUrl,
        PreferKey.aiSummaryModelsUrl,
        PreferKey.aiSummaryCustomModel,
        PreferKey.aiSummarySystemPrompt,
        PreferKey.aiSummaryChapterCount
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_ai_summary_config, rootKey)

        openDirectoryLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                val pathPreference = findPreference<PathPreference>(PreferKey.aiSummaryCachePath)
                pathPreference?.onPathSelected(uri)
            }

        val pathPreference = findPreference<PathPreference>(PreferKey.aiSummaryCachePath)
        pathPreference?.registerResultLauncher(openDirectoryLauncher)

        modelsUrlPreference = findPreference(PreferKey.aiSummaryModelsUrl)!!
        customModelPreference = findPreference(PreferKey.aiSummaryCustomModel)!!
        modelIdPreference = findPreference(PreferKey.aiSummaryModelId)!!

        setupModelPreferences()
        initSummary()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (AppConfig.aiSummaryModelList.isNullOrEmpty()) {
            fetchModelsFromServer()
        }
    }

    private fun setupModelPreferences() {
        initModelsUrl()
        loadModelsFromPrefs()

        modelsUrlPreference.setOnPreferenceChangeListener { _, newValue ->
            val url = newValue as String
            if (url.isNotEmpty()) {
                fetchModelsFromServer(url)
            }
            true
        }

        customModelPreference.setOnPreferenceChangeListener { _, _ ->
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) { Thread.sleep(200) } // Wait for pref to save
                withContext(Dispatchers.Main) { 
                    loadModelsFromPrefs()
                    toastOnUi("自定义模型已添加，请在模型列表中选择后使用")
                }
            }
            true
        }

        modelIdPreference.setOnPreferenceChangeListener { _, _ ->
            // Summary is handled by SimpleSummaryProvider
            true
        }
    }

    private fun initModelsUrl() {
        val apiUrl = AppConfig.aiSummaryApiUrl
        var modelsUrl = AppConfig.aiSummaryModelsUrl
        if (modelsUrl.isNullOrEmpty() && !apiUrl.isNullOrEmpty()) {
            modelsUrl = apiUrl.replace(Regex("(/v1)?/chat/completions$"), "") + "/v1/models"
            AppConfig.aiSummaryModelsUrl = modelsUrl
            modelsUrlPreference.text = modelsUrl
        }
    }

    private fun loadModelsFromPrefs() {
        val onlineModels = AppConfig.aiSummaryModelList
        val customModel = AppConfig.aiSummaryCustomModel

        val combinedModels = mutableListOf<String>()
        if (!customModel.isNullOrBlank()) {
            combinedModels.add(customModel)
        }
        if (!onlineModels.isNullOrEmpty()) {
            combinedModels.addAll(onlineModels)
        }

        if (combinedModels.isNotEmpty()) {
            val entries = combinedModels.distinct().toTypedArray()
            modelIdPreference.entries = entries
            modelIdPreference.entryValues = entries
            modelIdPreference.isEnabled = true
        } else {
            modelIdPreference.isEnabled = false
        }
    }

    private fun fetchModelsFromServer(url: String? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            modelIdPreference.isEnabled = false

            val modelsUrl = url ?: AppConfig.aiSummaryModelsUrl
            if (modelsUrl.isNullOrEmpty()) {
                toastOnUi("Models URL is not set.")
                modelIdPreference.isEnabled = true
                return@launch
            }
            val apiKey = AppConfig.aiSummaryApiKey
            if (apiKey.isNullOrEmpty()) {
                toastOnUi("API Key is not set.")
                modelIdPreference.isEnabled = true
                return@launch
            }

            try {
                withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()

                    val request = Request.Builder()
                        .url(modelsUrl)
                        .get()
                        .addHeader("Authorization", "Bearer $apiKey")
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw IOException("Failed to fetch models: ${response.code} ${response.message}")
                    }

                    val body = response.body?.string()
                    val modelsResponse = gson.fromJson(body, ModelsResponse::class.java)
                    val models = modelsResponse.data?.mapNotNull { it.id }?.toSet()

                    withContext(Dispatchers.Main) {
                        if (models != null) {
                            AppConfig.aiSummaryModelList = models
                            loadModelsFromPrefs()
                            toastOnUi("模型列表更新成功")
                        } else {
                            toastOnUi("Failed to parse models from response.")
                            modelIdPreference.isEnabled = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch models", e)
                withContext(Dispatchers.Main) {
                    toastOnUi("Error: ${e.message}")
                    modelIdPreference.isEnabled = true
                }
            }
        }
    }


    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "aiSummaryClearCache" -> {
                alert(
                    title = getString(R.string.ai_summary_clear_cache),
                    message = getString(R.string.ai_summary_clear_cache_confirm)
                ) {
                    okButton {
                        Coroutine.async {
                            AiSummaryProvider.clearAllAiSummaryCache()
                            toastOnUi(R.string.ai_summary_clear_cache_success)
                        }
                    }
                    cancelButton()
                }.show()
                return true
            }
            "aiSummaryHelp" -> {
                activity?.startActivity<WebViewActivity> {
                    putExtra("url", "file:///android_asset/web/help/md/AISummaryGuide.md")
                    putExtra("title", getString(R.string.ai_summary_help))
                }
                return true
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key in summaryKeys) {
            updatePreferenceSummary(key ?: return)
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun initSummary() {
        for (key in summaryKeys) {
            findPreference<EditTextPreference>(key)?.let {
                originalSummaries[key] = it.summary
                updatePreferenceSummary(key)
            }
        }
    }

    private fun updatePreferenceSummary(key: String) {
        findPreference<EditTextPreference>(key)?.let { preference ->
            if (preference.summaryProvider == null) {
                val value = preferenceScreen.sharedPreferences?.getString(key, "")
                if (!value.isNullOrEmpty()) {
                    preference.summary = if (key == PreferKey.aiSummaryApiKey) {
                        maskApiKey(value)
                    } else {
                        value
                    }
                } else {
                    preference.summary = originalSummaries[key]
                }
            }
        }
    }

    private fun maskApiKey(apiKey: String): String {
        if (apiKey.length > 12) {
            return "${apiKey.take(5)}...${apiKey.takeLast(4)}"
        }
        return "***"
    }

}