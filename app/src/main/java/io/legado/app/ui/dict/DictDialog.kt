package io.legado.app.ui.dict

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogDictBinding
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 词典
 */
class DictDialog : BaseDialogFragment() {

    companion object {

        fun dict(manager: FragmentManager, word: String) {
            DictDialog().apply {
                val bundle = Bundle()
                bundle.putString("word", word)
                arguments = bundle
            }.show(manager, word)
        }

    }

    private val viewModel by viewModels<DictViewModel>()
    private val binding by viewBinding(DialogDictBinding::bind)

    override fun onStart() {
        super.onStart()
        dialog?.window
            ?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.dialog_dict, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val word = arguments?.getString("word")
        if (word.isNullOrEmpty()) {
            toastOnUi(R.string.cannot_empty)
            dismiss()
            return
        }
        viewModel.dictHtmlData.observe(viewLifecycleOwner) {
            binding.tvDict.text = Html.fromHtml(it)
        }
        viewModel.dict(word)

    }


}