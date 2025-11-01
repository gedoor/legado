package io.legado.app.ui.code.config

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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


class ChangeThemeDialog(private val callBack: CallBack) : BaseDialogFragment(R.layout.dialog_edit_change_theme) {
    private val binding by viewBinding(DialogEditChangeThemeBinding::bind)
    private var isClick = false
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
            val themeIndex = AppConfig.editTheme
            if (themeIndex % 2 == 0) {
                chThemeL.checkByIndex(themeIndex / 2)
            } else {
                chThemeR.checkByIndex(themeIndex / 2)
            }
        }
    }

    private fun initView() {
        binding.run {
            chThemeL.setOnCheckedChangeListener { _, checkedId ->
                if (!isClick) {
                    isClick = true
                    chThemeR.clearCheck()
                    val int = chThemeL.getIndexById(checkedId)
                    putPrefInt(PreferKey.editTheme, int * 2)
                    callBack.upTheme(int * 2)
                    isClick = false
                }
            }
            chThemeR.setOnCheckedChangeListener { _, checkedId ->
                if (!isClick) {
                    isClick = true
                    chThemeL.clearCheck()
                    val int = chThemeR.getIndexById(checkedId)
                    putPrefInt(PreferKey.editTheme, int * 2 + 1)
                    callBack.upTheme(int * 2 + 1)
                    isClick = false
                }
            }
        }
    }
    interface CallBack {
        fun upTheme(index: Int)
    }

}