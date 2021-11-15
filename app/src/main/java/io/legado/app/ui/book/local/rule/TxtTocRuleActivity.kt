package io.legado.app.ui.book.local.rule

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityTxtTocRuleBinding
import io.legado.app.utils.viewbindingdelegate.viewBinding

class TxtTocRuleActivity : VMBaseActivity<ActivityTxtTocRuleBinding, TxtTocRuleViewModel>() {

    override val viewModel: TxtTocRuleViewModel by viewModels()
    override val binding: ActivityTxtTocRuleBinding by viewBinding(ActivityTxtTocRuleBinding::inflate)
    private val adapter: TxtTocRuleAdapter by lazy {
        TxtTocRuleAdapter(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        TODO("Not yet implemented")
    }

    private fun initView() {

    }

}