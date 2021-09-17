package io.legado.app.ui.book.read.config

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.documentfile.provider.DocumentFile
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.DialogReadBgTextBinding
import io.legado.app.databinding.ItemBgImageBinding
import io.legado.app.help.DefaultData
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.http.newCall
import io.legado.app.help.http.okHttpClient
import io.legado.app.lib.dialogs.SelectItem
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.lib.theme.getSecondaryTextColor
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.document.HandleFileContract
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import java.io.File

class BgTextConfigDialog : BaseDialogFragment() {

    companion object {
        const val TEXT_COLOR = 121
        const val BG_COLOR = 122
    }

    private val binding by viewBinding(DialogReadBgTextBinding::bind)
    private val configFileName = "readConfig.zip"
    private val adapter by lazy { BgAdapter(requireContext(), secondaryTextColor) }
    private var primaryTextColor = 0
    private var secondaryTextColor = 0
    private val importFormNet = "网络导入"
    private val selectBgImage = registerForActivityResult(SelectImageContract()) {
        it?.second?.let { uri ->
            setBgFromUri(uri)
        }
    }
    private val selectExportDir = registerForActivityResult(HandleFileContract()) {
        it ?: return@registerForActivityResult
        exportConfig(it)
    }
    private val selectImportDoc = registerForActivityResult(HandleFileContract()) {
        it ?: return@registerForActivityResult
        if (it.toString() == importFormNet) {
            importNetConfigAlert()
        } else {
            importConfig(it)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            it.setBackgroundDrawableResource(R.color.background)
            it.decorView.setPadding(0, 0, 0, 0)
            val attr = it.attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            it.attributes = attr
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as ReadBookActivity).bottomDialog++
        return inflater.inflate(R.layout.dialog_read_bg_text, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initData()
        initEvent()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        ReadBookConfig.save()
        (activity as ReadBookActivity).bottomDialog--
    }

    private fun initView() = binding.run {
        val bg = requireContext().bottomBackground
        val isLight = ColorUtils.isColorLight(bg)
        primaryTextColor = requireContext().getPrimaryTextColor(isLight)
        secondaryTextColor = requireContext().getSecondaryTextColor(isLight)
        rootView.setBackgroundColor(bg)
        tvNameTitle.setTextColor(primaryTextColor)
        tvName.setTextColor(secondaryTextColor)
        ivEdit.setColorFilter(secondaryTextColor)
        tvRestore.setTextColor(primaryTextColor)
        swDarkStatusIcon.setTextColor(primaryTextColor)
        ivImport.setColorFilter(primaryTextColor)
        ivExport.setColorFilter(primaryTextColor)
        ivDelete.setColorFilter(primaryTextColor)
        tvBgImage.setTextColor(primaryTextColor)
        recyclerView.adapter = adapter
        adapter.addHeaderView {
            ItemBgImageBinding.inflate(layoutInflater, it, false).apply {
                tvName.setTextColor(secondaryTextColor)
                tvName.text = getString(R.string.select_image)
                ivBg.setImageResource(R.drawable.ic_image)
                ivBg.setColorFilter(primaryTextColor)
                root.setOnClickListener {
                    selectBgImage.launch(null)
                }
            }
        }
        requireContext().assets.list("bg")?.let {
            adapter.setItems(it.toList())
        }
    }

    @SuppressLint("InflateParams")
    private fun initData() = with(ReadBookConfig.durConfig) {
        binding.tvName.text = name.ifBlank { "文字" }
        binding.swDarkStatusIcon.isChecked = curStatusIconDark()
    }

    @SuppressLint("InflateParams")
    private fun initEvent() = with(ReadBookConfig.durConfig) {
        binding.ivEdit.setOnClickListener {
            alert(R.string.style_name) {
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = "name"
                    editView.setText(ReadBookConfig.durConfig.name)
                }
                customView { alertBinding.root }
                okButton {
                    alertBinding.editView.text?.toString()?.let {
                        binding.tvName.text = it
                        ReadBookConfig.durConfig.name = it
                    }
                }
                cancelButton()
            }.show()
        }
        binding.tvRestore.setOnClickListener {
            val defaultConfigs = DefaultData.readConfigs
            val layoutNames = defaultConfigs.map { it.name }
            context?.selector("选择预设布局", layoutNames) { _, i ->
                if (i >= 0) {
                    ReadBookConfig.durConfig = defaultConfigs[i]
                    initData()
                    postEvent(EventBus.UP_CONFIG, true)
                }
            }
        }
        binding.swDarkStatusIcon.setOnCheckedChangeListener { _, isChecked ->
            setCurStatusIconDark(isChecked)
            (activity as? ReadBookActivity)?.upSystemUiVisibility()
        }
        binding.tvTextColor.setOnClickListener {
            ColorPickerDialog.newBuilder()
                .setColor(curTextColor())
                .setShowAlphaSlider(false)
                .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                .setDialogId(TEXT_COLOR)
                .show(requireActivity())
        }
        binding.tvBgColor.setOnClickListener {
            val bgColor =
                if (curBgType() == 0) Color.parseColor(curBgStr())
                else Color.parseColor("#015A86")
            ColorPickerDialog.newBuilder()
                .setColor(bgColor)
                .setShowAlphaSlider(false)
                .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                .setDialogId(BG_COLOR)
                .show(requireActivity())
        }
        binding.ivImport.setOnClickListener {
            selectImportDoc.launch {
                mode = HandleFileContract.FILE
                title = getString(R.string.import_str)
                allowExtensions = arrayOf("zip")
                otherActions = arrayListOf(SelectItem(importFormNet, -1))
            }
        }
        binding.ivExport.setOnClickListener {
            selectExportDir.launch {
                title = getString(R.string.export_str)
            }
        }
        binding.ivDelete.setOnClickListener {
            if (ReadBookConfig.deleteDur()) {
                postEvent(EventBus.UP_CONFIG, true)
                dismissAllowingStateLoss()
            } else {
                toastOnUi("数量已是最少,不能删除.")
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun exportConfig(uri: Uri) {
        val exportFileName = if (ReadBookConfig.config.name.isBlank()) {
            configFileName
        } else {
            "${ReadBookConfig.config.name}.zip"
        }
        execute {
            val exportFiles = arrayListOf<File>()
            val configDirPath = FileUtils.getPath(requireContext().externalCache, "readConfig")
            FileUtils.deleteFile(configDirPath)
            val configDir = FileUtils.createFolderIfNotExist(configDirPath)
            val configExportPath = FileUtils.getPath(configDir, "readConfig.json")
            FileUtils.deleteFile(configExportPath)
            val configExportFile = FileUtils.createFileIfNotExist(configExportPath)
            configExportFile.writeText(GSON.toJson(ReadBookConfig.getExportConfig()))
            exportFiles.add(configExportFile)
            val fontPath = ReadBookConfig.textFont
            if (fontPath.isNotEmpty()) {
                val fontName = FileUtils.getName(fontPath)
                val fontBytes = fontPath.parseToUri().readBytes(requireContext())
                fontBytes?.let {
                    val fontExportFile = FileUtils.createFileIfNotExist(configDir, fontName)
                    fontExportFile.writeBytes(it)
                    exportFiles.add(fontExportFile)
                }
            }
            if (ReadBookConfig.durConfig.bgType == 2) {
                val bgName = FileUtils.getName(ReadBookConfig.durConfig.bgStr)
                val bgFile = File(ReadBookConfig.durConfig.bgStr)
                if (bgFile.exists()) {
                    val bgExportFile = File(FileUtils.getPath(configDir, bgName))
                    bgFile.copyTo(bgExportFile)
                    exportFiles.add(bgExportFile)
                }
            }
            if (ReadBookConfig.durConfig.bgTypeNight == 2) {
                val bgName = FileUtils.getName(ReadBookConfig.durConfig.bgStrNight)
                val bgFile = File(ReadBookConfig.durConfig.bgStrNight)
                if (bgFile.exists()) {
                    val bgExportFile = File(FileUtils.getPath(configDir, bgName))
                    bgFile.copyTo(bgExportFile)
                    exportFiles.add(bgExportFile)
                }
            }
            if (ReadBookConfig.durConfig.bgTypeEInk == 2) {
                val bgName = FileUtils.getName(ReadBookConfig.durConfig.bgStrEInk)
                val bgFile = File(ReadBookConfig.durConfig.bgStrEInk)
                if (bgFile.exists()) {
                    val bgExportFile = File(FileUtils.getPath(configDir, bgName))
                    bgFile.copyTo(bgExportFile)
                    exportFiles.add(bgExportFile)
                }
            }
            val configZipPath = FileUtils.getPath(requireContext().externalCache, configFileName)
            if (ZipUtils.zipFiles(exportFiles, File(configZipPath))) {
                if (uri.isContentScheme()) {
                    DocumentFile.fromTreeUri(requireContext(), uri)?.let { treeDoc ->
                        treeDoc.findFile(exportFileName)?.delete()
                        treeDoc.createFile("", exportFileName)
                            ?.writeBytes(requireContext(), File(configZipPath).readBytes())
                    }
                } else {
                    val exportPath = FileUtils.getPath(File(uri.path!!), exportFileName)
                    FileUtils.deleteFile(exportPath)
                    FileUtils.createFileIfNotExist(exportPath)
                        .writeBytes(File(configZipPath).readBytes())
                }
            }
        }.onSuccess {
            toastOnUi("导出成功, 文件名为 $exportFileName")
        }.onError {
            it.printOnDebug()
            longToast("导出失败:${it.localizedMessage}")
        }
    }

    @SuppressLint("InflateParams")
    private fun importNetConfigAlert() {
        alert("输入地址") {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater)
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let { url ->
                    importNetConfig(url)
                }
            }
            noButton()
        }.show()
    }

    private fun importNetConfig(url: String) {
        execute {
            @Suppress("BlockingMethodInNonBlockingContext")
            okHttpClient.newCall {
                url(url)
            }.bytes().let {
                importConfig(it)
            }
        }.onError {
            longToast(it.msg)
        }
    }

    private fun importConfig(uri: Uri) {
        execute {
            @Suppress("BlockingMethodInNonBlockingContext")
            importConfig(uri.readBytes(requireContext())!!)
        }.onError {
            it.printOnDebug()
            longToast("导入失败:${it.localizedMessage}")
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext", "BlockingMethodInNonBlockingContext")
    private fun importConfig(byteArray: ByteArray) {
        execute {
            ReadBookConfig.import(byteArray)
        }.onSuccess {
            ReadBookConfig.durConfig = it
            postEvent(EventBus.UP_CONFIG, true)
            toastOnUi("导入成功")
        }.onError {
            it.printOnDebug()
            longToast("导入失败:${it.localizedMessage}")
        }
    }

    private fun setBgFromUri(uri: Uri) {
        readUri(uri) { name, bytes ->
            var file = requireContext().externalFiles
            file = FileUtils.createFileIfNotExist(file, "bg", name)
            file.writeBytes(bytes)
            ReadBookConfig.durConfig.setCurBg(2, file.absolutePath)
            ReadBookConfig.upBg()
            postEvent(EventBus.UP_CONFIG, false)
        }
    }
}