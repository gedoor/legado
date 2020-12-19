package io.legado.app.ui.book.changecover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogChangeCoverBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.applyTint
import io.legado.app.utils.getSize
import io.legado.app.utils.getViewModel
import io.legado.app.utils.viewbindingdelegate.viewBinding


class ChangeCoverDialog : BaseDialogFragment(),
    Toolbar.OnMenuItemClickListener,
    CoverAdapter.CallBack {

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

    private val binding by viewBinding(DialogChangeCoverBinding::bind)
    private var callBack: CallBack? = null
    private lateinit var viewModel: ChangeCoverViewModel
    lateinit var adapter: CoverAdapter

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
        callBack = activity as? CallBack
        viewModel = getViewModel(ChangeCoverViewModel::class.java)
        return inflater.inflate(R.layout.dialog_change_cover, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.setTitle(R.string.change_cover_source)
        viewModel.initData(arguments)
        initMenu()
        initView()
    }

    private fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.change_cover)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    private fun initView() {
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = CoverAdapter(requireContext(), this)
        binding.recyclerView.adapter = adapter
        viewModel.loadDbSearchBook()
    }

    override fun observeLiveBus() {
        super.observeLiveBus()
        viewModel.searchStateData.observe(viewLifecycleOwner, {
            binding.refreshProgressBar.isAutoLoading = it
            if (it) {
                stopMenuItem?.setIcon(R.drawable.ic_stop_black_24dp)
            } else {
                stopMenuItem?.setIcon(R.drawable.ic_refresh_black_24dp)
            }
            binding.toolBar.menu.applyTint(requireContext())
        })
        viewModel.searchBooksLiveData.observe(viewLifecycleOwner, {
            adapter.setItems(it)
        })
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_stop -> viewModel.stopSearch()
        }
        return false
    }

    private val stopMenuItem: MenuItem?
        get() = binding.toolBar.menu.findItem(R.id.menu_stop)

    override fun changeTo(coverUrl: String) {
        callBack?.coverChangeTo(coverUrl)
        dismiss()
    }

    interface CallBack {
        fun coverChangeTo(coverUrl: String)
    }
}