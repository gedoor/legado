package io.legado.app.ui.about


import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivityDonateBinding
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * Created by GKF on 2018/1/13.
 * 捐赠页面
 */

class DonateActivity : BaseActivity<ActivityDonateBinding>() {

    override val binding by viewBinding(ActivityDonateBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val fTag = "donateFragment"
        var donateFragment = supportFragmentManager.findFragmentByTag(fTag)
        if (donateFragment == null) donateFragment = DonateFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_fragment, donateFragment, fTag)
            .commit()
    }

}
