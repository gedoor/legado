package io.legado.app.ui.book.audio.config

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogAudioSkipCreditsBinding
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.data.entities.Book
import java.lang.ref.WeakReference
import android.content.DialogInterface

class AudioSkipCredits : BaseDialogFragment(R.layout.dialog_audio_skip_credits) {
    private val binding by viewBinding(DialogAudioSkipCreditsBinding::bind)

    companion object {
        private var bookRef: WeakReference<Book>? = null

        fun newInstance(book: Book): AudioSkipCredits {
            return AudioSkipCredits().apply {
                bookRef = WeakReference(book)
            }
        }
    }

    private val book: Book by lazy {
        bookRef?.get() ?: throw IllegalStateException("Book reference lost")
    }
 
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initData()
        initView()
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun initData() {
        binding.run {
            // 初始设置片头片尾（单位：秒）
            openCredits.progress = book.getopencredits()
            closeCredits.progress = book.getclosecredits()
        }
    }

    private fun initView() {
        binding.run {
            // 设定值
            openCredits.onChanged = {
                book.setopencredits(it)
            }
            closeCredits.onChanged = {
                book.setclosecredits(it)
            }
        }
    }
    
    override fun onDismiss(dialog: DialogInterface) {
        //保存设定
        super.onDismiss(dialog)
        book.save()
    }
}