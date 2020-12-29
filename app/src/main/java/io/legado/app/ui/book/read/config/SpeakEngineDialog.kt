package io.legado.app.ui.book.read.config

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.HttpTTS
import io.legado.app.databinding.DialogHttpTtsEditBinding
import io.legado.app.databinding.DialogRecyclerViewBinding
import io.legado.app.databinding.ItemHttpTtsBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryColor
import io.legado.app.service.help.ReadAloud
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import org.jetbrains.anko.sdk27.listeners.onClick

class SpeakEngineDialog : BaseDialogFragment(), Toolbar.OnMenuItemClickListener {
    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    lateinit var adapter: Adapter
    lateinit var viewModel: SpeakEngineViewModel
    private var httpTTSData: LiveData<List<HttpTTS>>? = null
    var engineId = App.INSTANCE.getPrefLong(PreferKey.speakEngine)

    override fun onStart() {
        super.onStart()
        val dm = requireActivity().getSize()
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = getViewModel(SpeakEngineViewModel::class.java)
        return inflater.inflate(R.layout.dialog_recycler_view, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initMenu()
        initData()
    }

    private fun initView() = with(binding) {
        toolBar.setBackgroundColor(primaryColor)
        toolBar.setTitle(R.string.speak_engine)
        ATH.applyEdgeEffectColor(recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = Adapter(requireContext())
        recyclerView.adapter = adapter
        tvFooterLeft.setText(R.string.local_tts)
        tvFooterLeft.visible()
        tvFooterLeft.onClick {
            removePref(PreferKey.speakEngine)
            dismiss()
        }
        tvOk.visible()
        tvOk.onClick {
            putPrefLong(PreferKey.speakEngine, engineId)
            dismiss()
        }
        tvCancel.visible()
        tvCancel.onClick {
            dismiss()
        }
    }

    private fun initMenu() = with(binding) {
        toolBar.inflateMenu(R.menu.speak_engine)
        toolBar.menu.applyTint(requireContext())
        toolBar.setOnMenuItemClickListener(this@SpeakEngineDialog)
    }

    private fun initData() {
        httpTTSData?.removeObservers(this)
        httpTTSData = App.db.httpTTSDao.observeAll()
        httpTTSData?.observe(this, {
            adapter.setItems(it)
        })
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_add -> editHttpTTS()
            R.id.menu_default -> viewModel.importDefault()
        }
        return true
    }

    @SuppressLint("InflateParams")
    private fun editHttpTTS(v: HttpTTS? = null) {
        val httpTTS = v?.copy() ?: HttpTTS()
        requireContext().alert(titleResource = R.string.speak_engine) {
            val alertBinding = DialogHttpTtsEditBinding.inflate(layoutInflater)
            alertBinding.tvName.setText(httpTTS.name)
            alertBinding.tvUrl.setText(httpTTS.url)
            customView = alertBinding.root
            cancelButton()
            okButton {
                alertBinding.apply {
                    httpTTS.name = tvName.text.toString()
                    httpTTS.url = tvUrl.text.toString()
                    App.db.httpTTSDao.insert(httpTTS)
                    ReadAloud.upReadAloudClass()
                }
            }
            neutralButton(R.string.help) {
                val helpStr = String(
                    requireContext().assets.open("help/httpTTSHelp.md").readBytes()
                )
                TextDialog.show(childFragmentManager, helpStr, TextDialog.MD)
            }
        }.show()
    }

    inner class Adapter(context: Context) :
        RecyclerAdapter<HttpTTS, ItemHttpTtsBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemHttpTtsBinding {
            return ItemHttpTtsBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemHttpTtsBinding,
            item: HttpTTS,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                cbName.text = item.name
                cbName.isChecked = item.id == engineId
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemHttpTtsBinding) {
            binding.apply {
                cbName.onClick {
                    getItem(holder.layoutPosition)?.let { httpTTS ->
                        engineId = httpTTS.id
                        notifyItemRangeChanged(0, itemCount)
                    }
                }
                ivEdit.onClick {
                    editHttpTTS(getItem(holder.layoutPosition))
                }
                ivMenuDelete.onClick {
                    getItem(holder.layoutPosition)?.let { httpTTS ->
                        App.db.httpTTSDao.delete(httpTTS)
                    }
                }
            }
        }


    }

}