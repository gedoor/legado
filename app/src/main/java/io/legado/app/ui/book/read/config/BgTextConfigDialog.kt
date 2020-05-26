package io.legado.app.ui.book.read.config

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.adapter.ItemViewHolder
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.constant.EventBus
import io.legado.app.help.AppConfig
import io.legado.app.help.ImageLoader
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.ui.book.read.Help
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.dialog_read_bg_text.*
import kotlinx.android.synthetic.main.item_bg_image.view.*
import org.jetbrains.anko.sdk27.listeners.onCheckedChange
import org.jetbrains.anko.sdk27.listeners.onClick

class BgTextConfigDialog : BaseDialogFragment() {

    companion object {
        const val TEXT_COLOR = 121
        const val BG_COLOR = 122
    }

    private val resultSelectBg = 123
    private lateinit var adapter: BgAdapter

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.let {
            Help.upSystemUiVisibility(it)
            it.windowManager?.defaultDisplay?.getMetrics(dm)
        }
        dialog?.window?.let {
            it.setBackgroundDrawableResource(R.color.background)
            it.decorView.setPadding(0, 5, 0, 0)
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
        return inflater.inflate(R.layout.dialog_read_bg_text, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initData()
        initView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        ReadBookConfig.save()
    }

    @SuppressLint("InflateParams")
    private fun initData() = with(ReadBookConfig.durConfig) {
        sw_dark_status_icon.isChecked = statusIconDark()
        adapter = BgAdapter(requireContext())
        recycler_view.isEnableScroll = !AppConfig.isEInkMode
        recycler_view.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        recycler_view.adapter = adapter
        val headerView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_bg_image, recycler_view, false)
        adapter.addHeaderView(headerView)
        headerView.tv_name.text = getString(R.string.select_image)
        headerView.iv_bg.setImageResource(R.drawable.ic_image)
        headerView.iv_bg.setColorFilter(getCompatColor(R.color.tv_text_default))
        headerView.onClick { selectImage() }
        requireContext().assets.list("bg/")?.let {
            adapter.setItems(it.toList())
        }
    }

    private fun initView() = with(ReadBookConfig.durConfig) {
        sw_dark_status_icon.onCheckedChange { buttonView, isChecked ->
            if (buttonView?.isPressed == true) {
                setStatusIconDark(isChecked)
                activity?.let {
                    Help.upSystemUiVisibility(it)
                }
            }
        }
        tv_text_color.onClick {
            ColorPickerDialog.newBuilder()
                .setColor(textColor())
                .setShowAlphaSlider(false)
                .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                .setDialogId(TEXT_COLOR)
                .show(requireActivity())
        }
        tv_bg_color.onClick {
            val bgColor =
                if (bgType() == 0) Color.parseColor(bgStr())
                else Color.parseColor("#015A86")
            ColorPickerDialog.newBuilder()
                .setColor(bgColor)
                .setShowAlphaSlider(false)
                .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                .setDialogId(BG_COLOR)
                .show(requireActivity())
        }
        tv_default.onClick {
            ReadBookConfig.resetDur()
            postEvent(EventBus.UP_CONFIG, false)
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        startActivityForResult(intent, resultSelectBg)
    }

    class BgAdapter(context: Context) :
        SimpleRecyclerAdapter<String>(context, R.layout.item_bg_image) {

        override fun convert(holder: ItemViewHolder, item: String, payloads: MutableList<Any>) {
            with(holder.itemView) {
                ImageLoader.load(context, context.assets.open("bg/$item").readBytes())
                    .centerCrop()
                    .into(iv_bg)
                tv_name.text = item.substringBeforeLast(".")
            }
        }

        override fun registerListener(holder: ItemViewHolder) {
            holder.itemView.apply {
                this.onClick {
                    getItemByLayoutPosition(holder.layoutPosition)?.let {
                        ReadBookConfig.durConfig.setBg(1, it)
                        ReadBookConfig.upBg()
                        postEvent(EventBus.UP_CONFIG, false)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            resultSelectBg -> {
                if (resultCode == RESULT_OK) {
                    data?.data?.let { uri ->
                        setBgFromUri(uri)
                    }
                }
            }
        }
    }

    private fun setBgFromUri(uri: Uri) {
        if (uri.toString().isContentPath()) {
            val doc = DocumentFile.fromSingleUri(requireContext(), uri)
            doc?.name?.let {
                var file = requireContext().getExternalFilesDir(null)
                    ?: requireContext().filesDir
                file = FileUtils.createFileIfNotExist(file, it, "bg")
                kotlin.runCatching {
                    DocumentUtils.readBytes(requireContext(), doc.uri)
                }.getOrNull()?.let { byteArray ->
                    file.writeBytes(byteArray)
                    ReadBookConfig.durConfig.setBg(2, file.absolutePath)
                    ReadBookConfig.upBg()
                    postEvent(EventBus.UP_CONFIG, false)
                } ?: toast("获取文件出错")
            }
        } else {
            PermissionsCompat.Builder(this)
                .addPermissions(
                    Permissions.READ_EXTERNAL_STORAGE,
                    Permissions.WRITE_EXTERNAL_STORAGE
                )
                .rationale(R.string.bg_image_per)
                .onGranted {
                    RealPathUtil.getPath(requireContext(), uri)?.let { path ->
                        ReadBookConfig.durConfig.setBg(2, path)
                        ReadBookConfig.upBg()
                        postEvent(EventBus.UP_CONFIG, false)
                    }
                }
                .request()
        }
    }
}