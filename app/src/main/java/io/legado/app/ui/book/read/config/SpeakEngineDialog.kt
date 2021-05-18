package io.legado.app.ui.book.read.config

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.HttpTTS
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.DialogHttpTtsEditBinding
import io.legado.app.databinding.DialogRecyclerViewBinding
import io.legado.app.databinding.ItemHttpTtsBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryColor
import io.legado.app.service.help.ReadAloud
import io.legado.app.ui.document.FilePicker
import io.legado.app.ui.document.FilePickerParam
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import splitties.init.appCtx


class SpeakEngineDialog : BaseDialogFragment(), Toolbar.OnMenuItemClickListener {
    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val ttsUrlKey = "ttsUrlKey"
    lateinit var adapter: Adapter
    private val viewModel: SpeakEngineViewModel by viewModels()
    private var httpTTSData: LiveData<List<HttpTTS>>? = null
    private var engineId = appCtx.getPrefLong(PreferKey.speakEngine)
    private val importDocResult = registerForActivityResult(FilePicker()) {
        it?.let {
            viewModel.importLocal(it)
        }
    }
    private val exportDirResult = registerForActivityResult(FilePicker()) {
        it?.let {
            viewModel.export(it)
        }
    }

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
        tvFooterLeft.setText(R.string.system_tts)
        tvFooterLeft.visible()
        tvFooterLeft.setOnClickListener {
            removePref(PreferKey.speakEngine)
            dismissAllowingStateLoss()
        }
        tvOk.visible()
        tvOk.setOnClickListener {
            putPrefLong(PreferKey.speakEngine, engineId)
            dismissAllowingStateLoss()
        }
        tvCancel.visible()
        tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun initMenu() = with(binding) {
        toolBar.inflateMenu(R.menu.speak_engine)
        toolBar.menu.applyTint(requireContext())
        toolBar.setOnMenuItemClickListener(this@SpeakEngineDialog)
    }

    private fun initData() {
        httpTTSData?.removeObservers(this)
        httpTTSData = appDb.httpTTSDao.observeAll()
        httpTTSData?.observe(this, {
            adapter.setItems(it)
        })
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_add -> editHttpTTS()
            R.id.menu_default -> viewModel.importDefault()
            R.id.menu_import_local -> importDocResult.launch(
                FilePickerParam(
                    mode = FilePicker.FILE,
                    allowExtensions = arrayOf("txt", "json")
                )
            )
            R.id.menu_import_onLine -> importAlert()
            R.id.menu_export -> exportDirResult.launch(null)
        }
        return true
    }

    private fun importAlert() {
        val aCache = ACache.get(requireContext(), cacheDir = false)
        val cacheUrls: MutableList<String> = aCache
            .getAsString(ttsUrlKey)
            ?.splitNotBlank(",")
            ?.toMutableList() ?: mutableListOf()
        alert(R.string.import_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.setFilterValues(cacheUrls)
                editView.delCallBack = {
                    cacheUrls.remove(it)
                    aCache.put(ttsUrlKey, cacheUrls.joinToString(","))
                }
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let { url ->
                    if (!cacheUrls.contains(url)) {
                        cacheUrls.add(0, url)
                        aCache.put(ttsUrlKey, cacheUrls.joinToString(","))
                    }
                    viewModel.importOnLine(url)
                }
            }
        }.show()
    }

    @SuppressLint("InflateParams")
    private fun editHttpTTS(v: HttpTTS? = null) {
        val httpTTS = v?.copy() ?: HttpTTS()
        requireContext().alert(titleResource = R.string.speak_engine) {
            val alertBinding = DialogHttpTtsEditBinding.inflate(layoutInflater)
            alertBinding.tvName.setText(httpTTS.name)
            alertBinding.tvUrl.setText(httpTTS.url)
            customView { alertBinding.root }
            cancelButton()
            okButton {
                alertBinding.apply {
                    httpTTS.name = tvName.text.toString()
                    httpTTS.url = tvUrl.text.toString()
                    appDb.httpTTSDao.insert(httpTTS)
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
                cbName.setOnClickListener {
                    getItem(holder.layoutPosition)?.let { httpTTS ->
                        engineId = httpTTS.id
                        notifyItemRangeChanged(0, itemCount)
                    }
                }
                ivEdit.setOnClickListener {
                    editHttpTTS(getItem(holder.layoutPosition))
                }
                ivMenuDelete.setOnClickListener {
                    getItem(holder.layoutPosition)?.let { httpTTS ->
                        appDb.httpTTSDao.delete(httpTTS)
                    }
                }
            }
        }

    }

}