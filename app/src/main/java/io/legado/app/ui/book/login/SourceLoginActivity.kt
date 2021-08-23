package io.legado.app.ui.book.login

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.ActivitySourceLoginBinding
import io.legado.app.utils.viewbindingdelegate.viewBinding


class SourceLoginActivity : BaseActivity<ActivitySourceLoginBinding>() {

    override val binding by viewBinding(ActivitySourceLoginBinding::inflate)
    private val viewModel by viewModels<SourceLoginViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getStringExtra("sourceUrl")?.let {
            viewModel.initData(it) { bookSource ->
                initView(bookSource)
            }
        }
    }

    private fun initView(bookSource: BookSource) {
        if (bookSource.loginUi.isNullOrEmpty()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment, WebViewLoginFragment())
                .commit()
        } else {
            RuleUiLoginDialog().show(supportFragmentManager, "ruleUiLogin")
        }
    }

}