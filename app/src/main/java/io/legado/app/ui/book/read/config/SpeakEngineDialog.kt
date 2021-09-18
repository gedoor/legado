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
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.RecyclerAdapter
import io.legado.app.data.appDb
import io.legado.app.data.entities.HttpTTS
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.DialogHttpTtsEditBinding
import io.legado.app.databinding.DialogRecyclerViewBinding
import io.legado.app.databinding.ItemHttpTtsBinding
import io.legado.app.help.AppConfig
import io.legado.app.help.DirectLinkUpload
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryColor
import io.legado.app.model.ReadAloud
import io.legado.app.ui.document.HandleFileContract
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class SpeakEngineDialog : BaseDialogFragment(), Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel: SpeakEngineViewModel by viewModels()
    private val ttsUrlKey = "ttsUrlKey"
    private val adapter by lazy { Adapter(requireContext()) }
    private var ttsEngine: String? = AppConfig.ttsEngine
    private val importDocResult = registerForActivityResult(HandleFileContract()) {
        it?.let {
            viewModel.importLocal(it)
        }
    }
    private val exportDirResult = registerForActivityResult(HandleFileContract()) { uri ->
        uri ?: return@registerForActivityResult
        alert(R.string.export_success) {
            if (uri.toString().isAbsUrl()) {
                DirectLinkUpload.getSummary()?.let { summary ->
                    setMessage(summary)
                }
            }
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = getString(R.string.path)
                editView.setText(uri.toString())
            }
            customView { alertBinding.root }
            okButton {
                requireContext().sendToClip(uri.toString())
            }
        }.show()
    }

    override fun onStart() {
        super.onStart()
        val dm = requireActivity().windowSize
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

    private fun initView() = binding.run {
        toolBar.setBackgroundColor(primaryColor)
        toolBar.setTitle(R.string.speak_engine)
        ATH.applyEdgeEffectColor(recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        tvFooterLeft.setText(R.string.system_tts)
        tvFooterLeft.visible()
        tvFooterLeft.setOnClickListener {
            selectSysTts()
        }
        tvOk.visible()
        tvOk.setOnClickListener {
            AppConfig.ttsEngine = ttsEngine
            dismissAllowingStateLoss()
        }
        tvCancel.visible()
        tvCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun initMenu() = binding.run {
        toolBar.inflateMenu(R.menu.speak_engine)
        toolBar.menu.applyTint(requireContext())
        toolBar.setOnMenuItemClickListener(this@SpeakEngineDialog)
    }

    private fun initData() {
        launch {
            appDb.httpTTSDao.flowAll().collect {
                adapter.setItems(it)
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_add -> editHttpTTS()
            R.id.menu_default -> viewModel.importDefault()
            R.id.menu_import_local -> importDocResult.launch {
                mode = HandleFileContract.FILE
                allowExtensions = arrayOf("txt", "json")
            }
            R.id.menu_import_onLine -> importAlert()
            R.id.menu_export -> exportDirResult.launch {
                mode = HandleFileContract.EXPORT
                fileData = Triple(
                    "httpTts.json",
                    GSON.toJson(adapter.getItems()).toByteArray(),
                    "application/json"
                )
            }
        }
        return true
    }

    private fun selectSysTts() {
        val ttsItems = viewModel.tts.engines.map {
            SelectItem(it.label, it.name)
        }
        context?.selector(R.string.system_tts, ttsItems) { _, item, _ ->
            AppConfig.ttsEngine = GSON.toJson(item)
            ttsEngine = null
            adapter.notifyItemRangeChanged(0, adapter.itemCount)
            dismissAllowingStateLoss()
        }
    }

    private fun importAlert() {
        val aCache = ACache.get(requireContext(), cacheDir = false)
        val cacheUrls: MutableList<String> = aCache
            .getAsString(ttsUrlKey)
            ?.splitNotBlank(",")
            ?.toMutableList() ?: mutableListOf()
        alert(R.string.import_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
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
                cbName.isChecked = item.id.toString() == ttsEngine
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemHttpTtsBinding) {
            binding.run {
                cbName.setOnClickListener {
                    getItemByLayoutPosition(holder.layoutPosition)?.let { httpTTS ->
                        ttsEngine = httpTTS.id.toString()
                        notifyItemRangeChanged(getHeaderCount(), itemCount)
                    }
                }
                ivEdit.setOnClickListener {
                    editHttpTTS(getItemByLayoutPosition(holder.layoutPosition))
                }
                ivMenuDelete.setOnClickListener {
                    getItemByLayoutPosition(holder.layoutPosition)?.let { httpTTS ->
                        appDb.httpTTSDao.delete(httpTTS)
                    }
                }
            }
        }

    }

}