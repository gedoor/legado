package io.legado.app.ui.book.import.remote

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.entities.Server
import io.legado.app.databinding.DialogRecyclerViewBinding
import io.legado.app.databinding.ItemServerSelectBinding
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding

class ServersDialog : BaseDialogFragment(R.layout.dialog_recycler_view) {

    val binding by viewBinding(DialogRecyclerViewBinding::bind)
    val viewModel by viewModels<ServersViewModel>()

    private val callback get() = (activity as? Callback)
    private val adapter by lazy { ServersAdapter(requireContext()) }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }


    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)

    }

    private fun initView() {

    }

    private fun initData() {

    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback?.onDialogDismiss("serversDialog")
    }

    class ServersAdapter(context: Context) :
        RecyclerAdapter<Server, ItemServerSelectBinding>(context) {

        private var selectServerId: Long? = null

        override fun getViewBinding(parent: ViewGroup): ItemServerSelectBinding {
            return ItemServerSelectBinding.inflate(inflater, parent, false)
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemServerSelectBinding) {

        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemServerSelectBinding,
            item: Server,
            payloads: MutableList<Any>
        ) {
            binding.run {
                root.setBackgroundColor(context.backgroundColor)
                rbServer.text = item.name
                rbServer.isChecked = item.id == selectServerId
            }
        }

    }

    interface Callback {

        fun onDialogDismiss(tag: String)

    }

}