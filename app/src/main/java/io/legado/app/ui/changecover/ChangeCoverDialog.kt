package io.legado.app.ui.changecover

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import io.legado.app.R
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.dialog_change_source.*


class ChangeCoverDialog : DialogFragment() {

    private lateinit var viewModel: ChangeCoverViewModel
    private lateinit var adapter: CoverAdapter

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
        return inflater.inflate(R.layout.dialog_change_source, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tool_bar.setTitle(R.string.change_cover_source)
        recycler_view.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = CoverAdapter(requireContext())
        recycler_view.adapter = adapter
    }

}