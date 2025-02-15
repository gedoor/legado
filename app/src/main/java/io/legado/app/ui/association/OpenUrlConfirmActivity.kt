package io.legado.app.ui.association

import android.os.Bundle
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivityTranslucenceBinding
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.viewbindingdelegate.viewBinding

class OpenUrlConfirmActivity :
    BaseActivity<ActivityTranslucenceBinding>() {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getStringExtra("uri")?.let {
            val mimeType = intent.getStringExtra("mimeType")
            val sourceOrigin = intent.getStringExtra("sourceOrigin")
            val sourceName = intent.getStringExtra("sourceName")
            showDialogFragment(OpenUrlConfirmDialog(it, mimeType, sourceOrigin, sourceName))
        } ?: finish()
    }

}
