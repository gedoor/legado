package io.legado.app.ui.widget.dialog

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogTextViewBinding
import io.legado.app.help.IntentData
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.applyTint
import io.legado.app.utils.setHtml
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TextDialog() : BaseDialogFragment(R.layout.dialog_text_view) {

    enum class Mode {
        MD, HTML, TEXT
    }

    constructor(
        title: String,
        content: String?,
        mode: Mode = Mode.TEXT,
        time: Long = 0,
        autoClose: Boolean = false
    ) : this() {
        arguments = Bundle().apply {
            putString("title", title)
            putString("content", IntentData.put(content))
            putString("mode", mode.name)
            putLong("time", time)
        }
        isCancelable = false
        this.autoClose = autoClose
    }

    private val binding by viewBinding(DialogTextViewBinding::bind)
    private var time = 0L
    private var autoClose: Boolean = false

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.inflateMenu(R.menu.dialog_text)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_close -> dismissAllowingStateLoss()
            }
            true
        }
        arguments?.let {
            binding.toolBar.title = it.getString("title")
            val content = IntentData.get(it.getString("content")) ?: ""
            when (it.getString("mode")) {
                Mode.MD.name -> viewLifecycleOwner.lifecycleScope.launch {
                    val markwon: Markwon
                    val markdown = withContext(IO) {
                        markwon = Markwon.builder(requireContext())
                            .usePlugin(GlideImagesPlugin.create(requireContext()))
                            .usePlugin(HtmlPlugin.create())
                            .usePlugin(TablePlugin.create(requireContext()))
                            .build()
                        markwon.toMarkdown(content)
                    }
                    markwon.setParsedMarkdown(binding.textView, markdown)
                }

                Mode.HTML.name -> binding.textView.setHtml(content)
                else -> {
                    if (content.length >= 32 * 1024) {
                        val truncatedContent =
                            content.substring(0, 32 * 1024) + "\n\n数据太大，无法全部显示…"
                        binding.textView.text = truncatedContent
                    } else {
                        binding.textView.text = content
                    }
                }
            }
            time = it.getLong("time", 0L)
        }
        if (time > 0) {
            binding.badgeView.setBadgeCount((time / 1000).toInt())
            lifecycleScope.launch {
                while (time > 0) {
                    delay(1000)
                    time -= 1000
                    binding.badgeView.setBadgeCount((time / 1000).toInt())
                    if (time <= 0) {
                        view.post {
                            dialog?.setCancelable(true)
                            if (autoClose) dialog?.cancel()
                        }
                    }
                }
            }
        } else {
            view.post {
                dialog?.setCancelable(true)
            }
        }
    }

}
