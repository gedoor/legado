package io.legado.app.ui.code.config

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.DialogEditSettingsBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefInt
import io.legado.app.utils.viewbindingdelegate.viewBinding

class SettingsDialog(private val callBack: CallBack) :
    BaseDialogFragment(R.layout.dialog_edit_settings) {
    private val binding by viewBinding(DialogEditSettingsBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initData()
        initView()
    }

    val fontSizeStr by lazy { getString(R.string.font_size) + " " }

    @SuppressLint("SetTextI18n")
    private fun initData() {
        binding.run {
            tvFontSize.text = fontSizeStr + AppConfig.editFontScale
            cbAutoComplete.isChecked = AppConfig.editAutoComplete
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        binding.run {
            tvFontSize.setOnClickListener {
                NumberPickerDialog(requireContext())
                    .setTitle(getString(R.string.font_scale))
                    .setMaxValue(36)
                    .setMinValue(9)
                    .setValue(AppConfig.editFontScale)
                    .setCustomButton((R.string.btn_default_s)) {
                        putPrefInt(PreferKey.editFontScale, 16)
                        callBack.upEdit(16, null, null)
                        tvFontSize.text = fontSizeStr + "16"
                    }
                    .show {
                        putPrefInt(PreferKey.editFontScale, it)
                        callBack.upEdit(it, null, null)
                        tvFontSize.text = fontSizeStr + it
                    }
            }
            cbAutoComplete.setOnCheckedChangeListener { _, isChecked ->
                putPrefBoolean(PreferKey.editAutoComplete, isChecked)
                callBack.upEdit(null, isChecked, null)
            }
        }
    }

    interface CallBack {
        fun upEdit(fontSize: Int?, autoComplete: Boolean?, autoWarp: Boolean?)
    }

}