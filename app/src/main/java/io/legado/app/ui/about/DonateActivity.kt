package io.legado.app.ui.about


import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivityDonateBinding

/**
 * Created by GKF on 2018/1/13.
 * 捐赠页面
 */

class DonateActivity : BaseActivity<ActivityDonateBinding>() {

    override fun getViewBinding(): ActivityDonateBinding {
        return ActivityDonateBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val fTag = "donateFragment"
        var donateFragment = supportFragmentManager.findFragmentByTag(fTag)
        if (donateFragment == null) donateFragment = DonateFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_fragment, donateFragment, fTag)
            .commit()
    }

}
