package io.legado.app.ui.book.import.remote

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.data.entities.rule.RowUi
import io.legado.app.databinding.DialogWebdavServerBinding
import io.legado.app.databinding.ItemSourceEditBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.ACache
import io.legado.app.utils.GSON
import io.legado.app.utils.applyTint
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding

class ServerConfigDialog : BaseDialogFragment(R.layout.dialog_webdav_server, true),
    Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogWebdavServerBinding::bind)

    private val serverUi = listOf(
        RowUi("url"),
        RowUi("user"),
        RowUi("password", RowUi.Type.password)
    )

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.inflateMenu(R.menu.server_config)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
        initConfigView()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> {
                val data = getConfigData()
                if (data.isEmpty()) {
                    ACache.get().remove("remoteServerConfig")
                } else {
                    ACache.get().put("remoteServerConfig", GSON.toJson(data))
                }
                dismissAllowingStateLoss()
            }
        }
        return true
    }

    private fun initConfigView() {
        val data = ACache.get().getAsJSONObject("remoteServerConfig")
        serverUi.forEachIndexed { index, rowUi ->
            when (rowUi.type) {
                RowUi.Type.text -> ItemSourceEditBinding.inflate(
                    layoutInflater,
                    binding.root,
                    false
                ).let {
                    binding.flexbox.addView(it.root)
                    it.root.id = index + 1000
                    it.textInputLayout.hint = rowUi.name
                    it.editText.setText(data?.getString(rowUi.name))
                }
                RowUi.Type.password -> ItemSourceEditBinding.inflate(
                    layoutInflater,
                    binding.root,
                    false
                ).let {
                    binding.flexbox.addView(it.root)
                    it.root.id = index + 1000
                    it.textInputLayout.hint = rowUi.name
                    it.editText.inputType =
                        InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
                    it.editText.setText(data?.getString(rowUi.name))
                }
            }
        }
    }

    private fun getConfigData(): Map<String, String> {
        val data = hashMapOf<String, String>()
        serverUi.forEachIndexed { index, rowUi ->
            val rowView = binding.root.findViewById<View>(index + 1000)
            ItemSourceEditBinding.bind(rowView).editText.text?.let {
                data[rowUi.name] = it.toString()
            }
        }
        return data
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        ((parentFragment as? Callback) ?: (activity as? Callback))
            ?.onDialogDismiss("serverConfig")
    }

    interface Callback {

        fun onDialogDismiss(tag: String)

    }
}