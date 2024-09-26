package io.legado.app.ui.rss.favorites

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.data.entities.RssArticle
import io.legado.app.databinding.DialogRssFavoriteConfigBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding

class RssFavoritesDialog() : BaseDialogFragment(R.layout.dialog_rss_favorite_config, true) {

    constructor(rssArticle: RssArticle) : this() {
        arguments = Bundle().apply {
            putString("title", rssArticle.title)
            putString("group", rssArticle.group)
        }
    }

    private val binding by viewBinding(DialogRssFavoriteConfigBinding::bind)

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

        var title = arguments.getString("title") ?: "默认名称"
        var group = arguments.getString("group") ?: "默认分组"
        binding.run {
            editTitle.setText(title)
            editGroup.setText(group)
            tvCancel.setOnClickListener {
                dismiss()
            }
            tvOk.setOnClickListener {
                val editTitle = editTitle.text.toString()
                if (editTitle.isNotBlank()) {
                    title = editTitle
                }
                val editGroup = editGroup.text.toString()
                if (editGroup.isNotBlank()) {
                    group = editGroup
                }
                callback?.updateFavorite(title, group)
                dismiss()
            }
            tvFooterLeft.setOnClickListener {
                callback?.deleteFavorite()
                dismiss()
            }
        }
    }

    val callback get() = (parentFragment as? Callback) ?: (activity as? Callback)

    interface Callback {

        fun updateFavorite(title: String, group: String)

        fun deleteFavorite()

    }

}
