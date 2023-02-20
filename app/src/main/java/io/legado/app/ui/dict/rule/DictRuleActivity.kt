package io.legado.app.ui.dict.rule

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityDictRuleBinding
import io.legado.app.utils.viewbindingdelegate.viewBinding

class DictRuleActivity : VMBaseActivity<ActivityDictRuleBinding, DictRuleViewModel>() {

    override val viewModel by viewModels<DictRuleViewModel>()
    override val binding by viewBinding(ActivityDictRuleBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {

    }
}