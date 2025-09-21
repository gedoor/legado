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

class SettingsDialog(private val callBack: CallBack) : BaseDialogFragment(R.layout.dialog_edit_settings)  {
    private val binding by viewBinding(DialogEditSettingsBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initData()
        initView()
    }

    @SuppressLint("SetTextI18n")
    private fun initData() {
        val fontScale = AppConfig.editFontScale
        val autoWrap = AppConfig.editAutoWrap
        binding.run {
            tvFontSize.text = "字体大小：$fontScale"
            cbAutoWrap.isChecked = autoWrap
        }
    }

    private fun initView() {
        binding.run {
            tvFontSize.setOnClickListener {
                NumberPickerDialog(requireContext())
                    .setTitle(getString(R.string.font_scale))
                    .setMaxValue(36)
                    .setMinValue(9)
                    .setValue(18)
                    .setCustomButton((R.string.btn_default_s)) {
                        putPrefInt(PreferKey.editFontScale, 18)
                        callBack.upEdit()
                    }
                    .show {
                        putPrefInt(PreferKey.editFontScale, it)
                        callBack.upEdit()
                    }
            }
            cbAutoWrap.setOnCheckedChangeListener { _, isChecked ->
                putPrefBoolean(PreferKey.editAutoWrap, isChecked)
                callBack.upEdit()
            }
        }
    }

    interface CallBack {
        fun upEdit()
    }

}