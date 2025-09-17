package io.legado.app.ui.code.changetheme

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.DialogEditChangeThemeBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.checkByIndex
import io.legado.app.utils.getIndexById
import io.legado.app.utils.putPrefInt
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.ui.code.CodeEditViewModel


class ChangeThemeDialog() : BaseDialogFragment(R.layout.dialog_edit_change_theme) {
    private val binding by viewBinding(DialogEditChangeThemeBinding::bind)
    private val viewModel by activityViewModels<CodeEditViewModel>()
    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initData()
        initView()
    }

    private fun initData() {
        binding.run {
            chTheme.checkByIndex(AppConfig.editTheme)
        }
    }

    private fun initView() {
        binding.run {
            chTheme.setOnCheckedChangeListener { _, checkedId ->
                val int = chTheme.getIndexById(checkedId)
                putPrefInt(PreferKey.editTheme, int)
                viewModel.loadTextMateThemes()
            }
        }
    }

}