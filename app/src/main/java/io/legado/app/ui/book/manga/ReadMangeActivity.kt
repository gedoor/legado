package io.legado.app.ui.book.manga

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityMangeBinding
import io.legado.app.utils.viewbindingdelegate.viewBinding

class ReadMangeActivity : VMBaseActivity<ActivityMangeBinding, MangaViewModel>() {
    override val binding by viewBinding(ActivityMangeBinding::inflate)
    override val viewModel by viewModels<MangaViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {

    }

}