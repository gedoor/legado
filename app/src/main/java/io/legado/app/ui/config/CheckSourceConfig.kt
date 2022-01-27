package io.legado.app.ui.config

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogCheckSourceConfigBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.model.CheckSource
import io.legado.app.utils.setLayout
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import splitties.views.onClick
import io.legado.app.constant.PreferKey
import io.legado.app.utils.putPrefString

class CheckSourceConfig : BaseDialogFragment(R.layout.dialog_check_source_config) {

    private val binding by viewBinding(DialogCheckSourceConfigBinding::bind)
    //允许的最小超时时间，秒
    private val minTimeout = 60L

    override fun onStart() {
        super.onStart()
        setLayout(
            0.9f,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        CheckSource.run {
            binding.checkSourceTimeout.setText((timeout / 1000).toString())
            binding.checkSearch.isChecked = checkSearch
            binding.checkDiscovery.isChecked = checkDiscovery
            binding.checkInfo.isChecked = checkInfo
            binding.checkCategory.isChecked = checkCategory
            binding.checkContent.isChecked = checkContent
            binding.tvCancel.onClick {
                dismiss()
            }
            binding.tvOk.onClick {
                val text = binding.checkSourceTimeout.text.toString()
                when {
                    text.isBlank() -> {
                        toastOnUi("${getString(R.string.timeout)}${getString(R.string.cannot_empty)}")
                        return@onClick
                    }
                    text.toLong() < minTimeout -> {
                        toastOnUi(
                            "${getString(R.string.timeout)}${getString(R.string.less_than)}${minTimeout}${
                                getString(
                                    R.string.seconds
                                )
                            }"
                        )
                        return@onClick
                    }
                    else -> timeout = text.toLong() * 1000
                }
                val mCheckSearch = binding.checkSearch.isChecked
                val mCheckDiscovery = binding.checkDiscovery.isChecked
                if (!mCheckSearch && !mCheckDiscovery) {
                    toastOnUi(getString(R.string.error_check_source_config))
                    return@onClick
                }
                checkSearch = mCheckSearch
                checkDiscovery = mCheckDiscovery
                checkInfo = binding.checkInfo.isChecked
                checkCategory = binding.checkCategory.isChecked
                checkContent = binding.checkContent.isChecked
                putConfig()
                putPrefString(PreferKey.checkSource, summary)
                dismiss()
            }
        }
    }
}