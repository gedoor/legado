package io.legado.app.ui.config

import android.os.Bundle
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivityAiSummaryConfigBinding

class AiSummaryConfigActivity : BaseActivity<ActivityAiSummaryConfigBinding>() {

    override val binding: ActivityAiSummaryConfigBinding
        get() = ActivityAiSummaryConfigBinding.inflate(layoutInflater)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(io.legado.app.R.id.container, AiSummaryConfigFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}