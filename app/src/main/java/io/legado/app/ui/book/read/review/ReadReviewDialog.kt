package io.legado.app.ui.book.read.review

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.data.entities.BookReview
import io.legado.app.databinding.DialogReviewListBinding
import io.legado.app.databinding.ViewLoadMoreBinding
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.widget.dialog.PhotoDialog
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.viewbindingdelegate.viewBinding

class ReadReviewDialog() :
    BaseDialogFragment(R.layout.dialog_review_list, true),
    ReadReviewAdapter.CallBack {
    constructor(segmentIndex: String, reviewCount: String) : this() {
        arguments = Bundle().apply {
            putString("segmentIndex", segmentIndex)
            putString("reviewCount", reviewCount)
        }
    }

    private val binding by viewBinding(DialogReviewListBinding::bind)
    private val adapter by lazy { ReadReviewAdapter(requireContext(), this) }
    private val loadMoreView by lazy { LoadMoreView(requireContext()) }
    private val viewModel by viewModels<ReadReviewViewModel>()

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawableResource(R.color.transparent)
            decorView.setPadding(0, 0, 0, 0)
            val attr = attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            attributes = attr
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(), theme)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        (activity as ReadBookActivity).bottomDialog++
        initView()
        initData()
        initEvent()
    }

    private fun initView() {
        binding.dialogReview.setBackgroundColor(backgroundColor)
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.tvReviewCount.text =
            requireContext().getString(R.string.review_count, arguments?.getString("reviewCount"))
        binding.recyclerView.adapter = adapter
        adapter.addFooterView { ViewLoadMoreBinding.bind(loadMoreView) }
    }

    private fun initData() {
        loadMoreView.startLoad()
        viewModel.reviewsData.observe(this) { upData(it) }
        viewModel.errorLiveData.observe(this) { loadMoreView.error(it) }
        viewModel.initData(arguments)
    }

    private fun initEvent() {
        binding.ivReviewClose.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    scrollToBottom()
                }
            }
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    // 只在拖动和静止时加载，自动滑动时不加载。
                    RecyclerView.SCROLL_STATE_DRAGGING,
                    RecyclerView.SCROLL_STATE_SETTLING,
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        Glide.with(requireContext()).resumeRequests()
                    }
                }
            }
        })
        loadMoreView.setOnClickListener {
            if (!loadMoreView.isLoading) {
                loadMoreView.hasMore()
                loadMoreView.startLoad()
                viewModel.loadReviews(false)
            }
        }
    }

    /**
     * 图片长按
     */
    override fun onImageLongPress(src: String) {
        showDialogFragment(PhotoDialog(src))
    }

    private fun scrollToBottom() {
        adapter.let {
            if (loadMoreView.hasMore && !loadMoreView.isLoading) {
                loadMoreView.startLoad()
                viewModel.loadReviews(false)
            }
        }
    }

    private fun upData(reviews: List<BookReview>) {
        loadMoreView.stopLoad()
        if (reviews.isEmpty() && adapter.isEmpty()) {
            loadMoreView.noMore(getString(R.string.empty))
        } else if (reviews.isEmpty()) {
            loadMoreView.noMore()
        } else {
            adapter.addItems(reviews)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as ReadBookActivity).bottomDialog--
    }

}
