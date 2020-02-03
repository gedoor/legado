package io.legado.app.ui.changecover

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import io.legado.app.R
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.dialog_change_source.*


class ChangeCoverDialog : DialogFragment(), ChangeCoverViewModel.CallBack {

    companion object {
        const val tag = "changeCoverDialog"

        fun show(manager: FragmentManager, name: String, author: String) {
            val fragment = (manager.findFragmentByTag(tag) as? ChangeCoverDialog)
                ?: ChangeCoverDialog().apply {
                    val bundle = Bundle()
                    bundle.putString("name", name)
                    bundle.putString("author", author)
                    arguments = bundle
                }
            fragment.show(manager, tag)
        }
    }

    private var callBack: CallBack? = null
    private lateinit var viewModel: ChangeCoverViewModel
    override lateinit var adapter: CoverAdapter

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = getViewModel(ChangeCoverViewModel::class.java)
        viewModel.callBack = this
        return inflater.inflate(R.layout.dialog_change_source, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        callBack = activity as? CallBack
        tool_bar.setTitle(R.string.change_cover_source)
        arguments?.let { bundle ->
            bundle.getString("name")?.let {
                viewModel.name = it
            }
            bundle.getString("author")?.let {
                viewModel.author = it
            }
        }
        recycler_view.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = CoverAdapter(requireContext())
        recycler_view.adapter = adapter
    }

    interface CallBack {
        fun coverChangeTo(coverUrl: String)
    }
}