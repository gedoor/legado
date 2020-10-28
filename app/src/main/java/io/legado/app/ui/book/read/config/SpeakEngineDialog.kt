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
import io.legado.app.base.adapter.SimpleRecyclerAdapter
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.HttpTTS
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.cancelButton
import io.legado.app.lib.dialogs.customView
import io.legado.app.lib.dialogs.okButton
import io.legado.app.lib.theme.primaryColor
import io.legado.app.service.help.ReadAloud
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.dialog_http_tts_edit.view.*
import kotlinx.android.synthetic.main.dialog_recycler_view.*
import kotlinx.android.synthetic.main.item_http_tts.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import java.io.File

class SpeakEngineDialog : BaseDialogFragment(), Toolbar.OnMenuItemClickListener {

    override fun onStart() {
        super.onStart()
        val dm = requireActivity().getSize()
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }

    lateinit var adapter: Adapter
    lateinit var viewModel: SpeakEngineViewModel
    private var httpTTSData: LiveData<List<HttpTTS>>? = null
    var engineId = App.INSTANCE.getPrefLong(PreferKey.speakEngine)

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

    private fun initView() {
        tool_bar.setBackgroundColor(primaryColor)
        tool_bar.setTitle(R.string.speak_engine)
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        adapter = Adapter(requireContext())
        recycler_view.adapter = adapter
        tv_footer_left.setText(R.string.local_tts)
        tv_footer_left.visible()
        tv_footer_left.onClick {
            removePref(PreferKey.speakEngine)
            dismiss()
        }
        tv_ok.visible()
        tv_ok.onClick {
            putPrefLong(PreferKey.speakEngine, engineId)
            dismiss()
        }
        tv_cancel.visible()
        tv_cancel.onClick {
            dismiss()
        }
    }

    private fun initMenu() {
        tool_bar.inflateMenu(R.menu.speak_engine)
        tool_bar.menu.applyTint(requireContext())
        tool_bar.setOnMenuItemClickListener(this)
    }

    private fun initData() {
        httpTTSData?.removeObservers(this)
        httpTTSData = App.db.httpTTSDao().observeAll()
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
            var rootView: View? = null
            customView {
                LayoutInflater.from(requireContext())
                    .inflate(R.layout.dialog_http_tts_edit, null).apply {
                        rootView = this
                        tv_name.setText(httpTTS.name)
                        tv_url.setText(httpTTS.url)
                    }
            }
            cancelButton()
            okButton {
                rootView?.apply {
                    httpTTS.name = tv_name.text.toString()
                    httpTTS.url = tv_url.text.toString()
                    App.db.httpTTSDao().insert(httpTTS)
                    ReadAloud.upReadAloudClass()
                }
            }
            neutralButton(R.string.help) {
                val helpStr = String(
                    requireContext().assets.open("help${File.separator}httpTts.md").readBytes()
                )
                TextDialog.show(childFragmentManager, helpStr, TextDialog.MD)
            }
        }.show().applyTint()
    }

    inner class Adapter(context: Context) :
        SimpleRecyclerAdapter<HttpTTS>(context, R.layout.item_http_tts) {

        override fun convert(holder: ItemViewHolder, item: HttpTTS, payloads: MutableList<Any>) {
            holder.itemView.apply {
                cb_name.text = item.name
                cb_name.isChecked = item.id == engineId
            }
        }

        override fun registerListener(holder: ItemViewHolder) {
            holder.itemView.apply {
                cb_name.onClick {
                    getItem(holder.layoutPosition)?.let { httpTTS ->
                        engineId = httpTTS.id
                        notifyItemRangeChanged(0, getActualItemCount())
                    }
                }
                iv_edit.onClick {
                    editHttpTTS(getItem(holder.layoutPosition))
                }
                iv_menu_delete.onClick {
                    getItem(holder.layoutPosition)?.let { httpTTS ->
                        App.db.httpTTSDao().delete(httpTTS)
                    }
                }
            }
        }


    }

}