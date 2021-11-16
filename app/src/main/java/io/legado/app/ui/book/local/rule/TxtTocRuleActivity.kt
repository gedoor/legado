package io.legado.app.ui.book.local.rule

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.databinding.ActivityTxtTocRuleBinding
import io.legado.app.utils.viewbindingdelegate.viewBinding

class TxtTocRuleActivity : VMBaseActivity<ActivityTxtTocRuleBinding, TxtTocRuleViewModel>(),
    TxtTocRuleAdapter.Callback {

    override val viewModel: TxtTocRuleViewModel by viewModels()
    override val binding: ActivityTxtTocRuleBinding by viewBinding(ActivityTxtTocRuleBinding::inflate)
    private val adapter: TxtTocRuleAdapter by lazy {
        TxtTocRuleAdapter(this, this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
    }

    private fun initView() = binding.run {
        recyclerView.adapter = adapter

    }

    override fun del(source: TxtTocRule) {
        viewModel.del(source)
    }

    override fun edit(source: TxtTocRule) {

    }

    override fun update(vararg source: TxtTocRule) {

    }

    override fun toTop(source: TxtTocRule) {

    }

    override fun toBottom(source: TxtTocRule) {

    }

    override fun upOrder() {

    }

    override fun upCountView() {

    }

}