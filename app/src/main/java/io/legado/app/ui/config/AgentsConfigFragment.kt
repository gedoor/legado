package io.legado.app.ui.config

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.config.AgentsConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.prefs.fragment.PreferenceFragment
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.toastOnUi
import splitties.init.appCtx

/**
 * Agents 设置页。
 *
 * 本页面仅负责：
 * - 展示 Agents 相关配置项
 * - 在写入时做必要的输入校验，避免非法配置落库导致后续请求崩溃
 *
 * 配置读写的统一入口为 [AgentsConfig]。
 */
class AgentsConfigFragment : PreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * 初始化 Preference UI 并完成控件绑定。
     *
     * Args:
     *     savedInstanceState (Bundle?): Fragment 的状态快照。
     *     rootKey (String?): Preference 根 key（本项目通常为 null）。
     *
     * Returns:
     *     Unit: 无返回值。
     */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_agents)

        // 连接配置：为输入框设置合适的输入类型与初始摘要。
        bindEditTextPreference(
            key = PreferKey.agentsBaseUrl,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI,
        )
        bindEditTextPreference(
            key = PreferKey.agentsApiKey,
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
        )
        bindEditTextPreference(
            key = PreferKey.agentsModel,
            inputType = InputType.TYPE_CLASS_TEXT,
        )

        // 自定义提示词：多行输入。
        findPreference<EditTextPreference>(PreferKey.agentsCustomInstruction)?.let { pref ->
            pref.setOnBindEditTextListener { editText ->
                // 允许多行输入，并把光标移动到末尾，便于继续编辑。
                editText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                editText.isSingleLine = false
                editText.minLines = 4
                editText.gravity = Gravity.TOP or Gravity.START
                editText.setSelection(editText.text.length)
            }
        }

        // 初始摘要刷新（数字类项由 onPreferenceTreeClick 驱动更新）。
        refreshAllSummaries()
    }

    /**
     * Fragment 视图创建完成后设置标题与监听。
     *
     * Args:
     *     view (View): Fragment 根视图。
     *     savedInstanceState (Bundle?): Fragment 的状态快照。
     *
     * Returns:
     *     Unit: 无返回值。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.agents_setting)
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        listView.setEdgeEffectColor(primaryColor)
    }

    /**
     * 释放资源并注销偏好变更监听。
     *
     * Returns:
     *     Unit: 无返回值。
     */
    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    /**
     * 点击 Preference 项时的分发入口。
     *
     * Args:
     *     preference (Preference): 被点击的项。
     *
     * Returns:
     *     Boolean: 是否由父类继续处理。
     */
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            PreferKey.agentsRequestTimeout -> showRequestTimeoutDialog()
            PreferKey.agentsTemperature -> showTemperatureDialog()
            PreferKey.agentsMaxOutputTokens -> showMaxOutputTokensDialog()
            PreferKey.agentsHistoryLimit -> showHistoryLimitDialog()
        }
        return super.onPreferenceTreeClick(preference)
    }

    /**
     * 监听 SharedPreferences 变化后刷新 UI。
     *
     * Args:
     *     sharedPreferences (SharedPreferences?): 触发变更的 SharedPreferences。
     *     key (String?): 发生变化的 key。
     *
     * Returns:
     *     Unit: 无返回值。
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferKey.agentsBaseUrl,
            PreferKey.agentsApiKey,
            PreferKey.agentsModel,
            PreferKey.agentsCustomInstruction,
            PreferKey.agentsEnableBookshelfTools,
            PreferKey.agentsEnableLoadedChapterTools,
            PreferKey.agentsEnableOperationRuleTools,
            PreferKey.agentsAutoCreateTitle -> {
                // 这类项可能通过 Preference 自身持久化（开关）或通过 onPreferenceChangeListener 写入。
                listView.post { refreshAllSummaries() }
            }
        }
    }

    /**
     * 绑定 EditTextPreference：设置输入类型、拦截写入并做必要校验。
     *
     * Args:
     *     key (String): PreferKey 中定义的偏好 key。
     *     inputType (Int): Android EditText 输入类型。
     *
     * Returns:
     *     Unit: 无返回值。
     */
    private fun bindEditTextPreference(key: String, inputType: Int) {
        findPreference<EditTextPreference>(key)?.let { pref ->
            pref.setOnBindEditTextListener { editText ->
                // 统一设置输入类型，减少用户输入无效内容的概率。
                editText.inputType = inputType
                editText.setSelection(editText.text.length)
            }

            pref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val raw = newValue?.toString().orEmpty()
                val normalized = raw.trim()

                // 仅对 Base URL 做强校验：允许空；非空时必须是合法 http/https URL。
                if (key == PreferKey.agentsBaseUrl) {
                    if (normalized.isNotEmpty() && !AgentsConfig.isValidHttpUrl(normalized)) {
                        appCtx.toastOnUi(R.string.agents_invalid_base_url)
                        return@OnPreferenceChangeListener false
                    }
                }

                // 统一去掉首尾空白，避免后续读取/校验出现“看不见的空格”问题。
                if (normalized != raw) {
                    // WARNING: 这里通过二次 setText 触发持久化，避免把带空白的值写入偏好。
                    pref.text = normalized
                    return@OnPreferenceChangeListener false
                }

                true
            }
        }
    }

    /**
     * 刷新页面上所有需要动态展示的 summary。
     *
     * Returns:
     *     Unit: 无返回值。
     */
    private fun refreshAllSummaries() {
        // Base URL。
        findPreference<Preference>(PreferKey.agentsBaseUrl)?.summary =
            AgentsConfig.getBaseUrl().ifBlank { getString(R.string.agents_base_url_summary) }

        // API Key：脱敏显示。
        findPreference<Preference>(PreferKey.agentsApiKey)?.summary =
            if (AgentsConfig.getApiKey().isBlank()) {
                getString(R.string.agents_api_key_summary)
            } else {
                getString(R.string.agents_api_key_set_summary)
            }

        // Model。
        findPreference<Preference>(PreferKey.agentsModel)?.summary =
            AgentsConfig.getModel().ifBlank { getString(R.string.agents_model_summary) }

        // 数字项（点击弹窗修改）：展示当前值。
        findPreference<Preference>(PreferKey.agentsRequestTimeout)?.summary =
            getString(R.string.agents_request_timeout_summary, AgentsConfig.getRequestTimeoutSec())
        findPreference<Preference>(PreferKey.agentsTemperature)?.summary =
            getString(R.string.agents_temperature_summary, AgentsConfig.getTemperature())
        findPreference<Preference>(PreferKey.agentsMaxOutputTokens)?.summary =
            getString(R.string.agents_max_output_tokens_summary, AgentsConfig.getMaxOutputTokens())
        findPreference<Preference>(PreferKey.agentsHistoryLimit)?.summary =
            getString(R.string.agents_history_limit_summary, AgentsConfig.getHistoryLimit())

        // 自定义提示词：避免在 summary 中铺开长文本。
        findPreference<Preference>(PreferKey.agentsCustomInstruction)?.summary =
            if (AgentsConfig.getCustomInstruction().isBlank()) {
                getString(R.string.agents_custom_instruction_summary)
            } else {
                getString(R.string.agents_custom_instruction_set_summary)
            }
    }

    /**
     * 请求超时设置（秒）：5-600。
     *
     * Returns:
     *     Unit: 无返回值。
     */
    private fun showRequestTimeoutDialog() {
        NumberPickerDialog(requireContext())
            .setTitle(getString(R.string.agents_request_timeout_title))
            .setMaxValue(600)
            .setMinValue(5)
            .setValue(AgentsConfig.getRequestTimeoutSec())
            .show {
                if (!AgentsConfig.trySetRequestTimeoutSec(it)) {
                    appCtx.toastOnUi(R.string.agents_invalid_request_timeout)
                    return@show
                }
                refreshAllSummaries()
            }
    }

    /**
     * 最大输出 token：256-32768。
     *
     * Returns:
     *     Unit: 无返回值。
     */
    private fun showMaxOutputTokensDialog() {
        NumberPickerDialog(requireContext())
            .setTitle(getString(R.string.agents_max_output_tokens_title))
            .setMaxValue(32768)
            .setMinValue(256)
            .setValue(AgentsConfig.getMaxOutputTokens())
            .show {
                if (!AgentsConfig.trySetMaxOutputTokens(it)) {
                    appCtx.toastOnUi(R.string.agents_invalid_max_output_tokens)
                    return@show
                }
                refreshAllSummaries()
            }
    }

    /**
     * 历史上限：20-1000。
     *
     * Returns:
     *     Unit: 无返回值。
     */
    private fun showHistoryLimitDialog() {
        NumberPickerDialog(requireContext())
            .setTitle(getString(R.string.agents_history_limit_title))
            .setMaxValue(1000)
            .setMinValue(20)
            .setValue(AgentsConfig.getHistoryLimit())
            .show {
                if (!AgentsConfig.trySetHistoryLimit(it)) {
                    appCtx.toastOnUi(R.string.agents_invalid_history_limit)
                    return@show
                }
                refreshAllSummaries()
            }
    }

    /**
     * temperature：0-2。
     *
     * Returns:
     *     Unit: 无返回值。
     */
    private fun showTemperatureDialog() {
        val current = AgentsConfig.getTemperature()
        alert(getString(R.string.agents_temperature_title)) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                // 使用小数输入，便于配置 0~2 的范围。
                editView.inputType =
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                editView.hint = "0.0 ~ 2.0"
                editView.setText(current.toString())
                editView.setSelection(editView.text?.length ?: 0)
            }
            customView { alertBinding.root }
            okButton {
                val value = alertBinding.editView.text?.toString()?.trim()?.toDoubleOrNull()
                if (value == null || !AgentsConfig.trySetTemperature(value)) {
                    appCtx.toastOnUi(R.string.agents_invalid_temperature)
                    return@okButton
                }
                refreshAllSummaries()
            }
            cancelButton()
        }
    }
}
