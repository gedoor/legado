package io.legado.app.ui.rss.favorites

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.data.entities.RssArticle
import io.legado.app.databinding.DialogRssfavoritesBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch

class RssFavoritesDialog() : BaseDialogFragment(R.layout.dialog_rssfavorites, true) {

    constructor(rssArticle: RssArticle) : this() {
        arguments = Bundle().apply {
            putParcelable("rssArticle", rssArticle)
        }
    }

    private val binding by viewBinding(DialogRssfavoritesBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        val arguments = arguments ?: let {
            dismiss()
            return
        }

        @Suppress("DEPRECATION")
        val rssArticle = arguments.getParcelable<RssArticle>("rssArticle")
        rssArticle ?: let {
            dismiss()
            return
        }
        binding.run {
            editTitle.setText(rssArticle.title)
            editGroup.setText(rssArticle.group ?: "默认分组")
            tvCancel.setOnClickListener {
                dismiss()
            }
            tvOk.setOnClickListener {
                rssArticle.title = editTitle.text?.toString() ?: ""
                rssArticle.group = editGroup.text?.toString() ?: ""
                lifecycleScope.launch {
                    callback?.setRssArticle(rssArticle, true)
                    dismiss()
                }
            }
            tvFooterLeft.setOnClickListener {
                lifecycleScope.launch {
                    callback?.setRssArticle(rssArticle, false)
                    dismiss()
                }
            }
        }
    }

    val callback get() = (parentFragment as? Callback) ?: (activity as? Callback)

    interface Callback {

        fun setRssArticle(rssArticle: RssArticle, editPos: Boolean)

    }

}