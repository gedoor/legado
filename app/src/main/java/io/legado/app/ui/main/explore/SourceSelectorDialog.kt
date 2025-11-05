package io.legado.app.ui.main.explore

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.constant.BookSourceType
import io.legado.app.databinding.DialogSourceSelectorBinding
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.utils.viewbindingdelegate.viewBinding
import splitties.init.appCtx
import io.legado.app.utils.getPrefStringSet
import io.legado.app.utils.putPrefStringSet
import io.legado.app.constant.AppLog

class SourceSelectorDialog : BaseDialogFragment(R.layout.dialog_source_selector) {

    interface CallBack {
        fun onSourceSelected(source: BookSource)
    }

    private val binding by viewBinding(DialogSourceSelectorBinding::bind)
    private val adapter by lazy { SourceSelectorAdapter(
        onClick = { onSelect(it) },
        onToggleFav = { toggleFavorite(it) },
        isFav = { isFavorite(it) }
    ) }
    private var selectedType: Int = BookSourceType.default
    private var currentSortMode: SortMode = SortMode.NAME

    // 简单偏好键
    private val favKey get() = "source_selector_favorites"
    private val recentKey get() = "source_selector_recent"
    
    private enum class SortMode {
        NAME, RESPOND
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawableResource(R.color.background)
            decorView.setPadding(0, 0, 0, 0)
            val attr = attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            attributes = attr
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        selectedType = arguments?.getInt("type") ?: BookSourceType.default
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        initSearch()
        initChips()
        loadSources(null)
    }

    private fun initSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                loadSources(query)
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                loadSources(newText)
                return true
            }
        })
    }

    private fun initChips() {
        // 设置默认排序状态
        binding.chipSortName.isChecked = true
        
        // 监听筛选与排序变化
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, _ ->
            AppLog.put("SourceSelector: === 筛选状态变化 ===")
            AppLog.put("SourceSelector: 收藏: ${binding.chipFav.isChecked}, 最近: ${binding.chipRecent.isChecked}, 需登录: ${binding.chipNeedLogin.isChecked}")
            loadSources(binding.searchView.query?.toString())
        }
        binding.chipGroupSort.setOnCheckedStateChangeListener { _, _ ->
            AppLog.put("SourceSelector: === 排序状态变化 ===")
            AppLog.put("SourceSelector: 按名称: ${binding.chipSortName.isChecked}, 按响应: ${binding.chipSortRespond.isChecked}")
            loadSources(binding.searchView.query?.toString())
        }
        
        // 为每个排序 Chip 单独设置点击监听器
        binding.chipSortName.setOnClickListener {
            AppLog.put("SourceSelector: 点击按名称 Chip")
            currentSortMode = SortMode.NAME
            binding.chipSortName.isChecked = true
            binding.chipSortRespond.isChecked = false
            AppLog.put("SourceSelector: 设置状态 - 按名称: true, 按响应: false")
            loadSources(binding.searchView.query?.toString())
        }
        binding.chipSortRespond.setOnClickListener {
            AppLog.put("SourceSelector: 点击按响应 Chip")
            currentSortMode = SortMode.RESPOND
            binding.chipSortRespond.isChecked = true
            binding.chipSortName.isChecked = false
            AppLog.put("SourceSelector: 设置状态 - 按名称: false, 按响应: true")
            loadSources(binding.searchView.query?.toString())
        }
        // 长按：编辑/禁用/测试
        adapter.setOnLongAction { src, action ->
            when (action) {
                LongAction.EDIT -> {
                    val intent = android.content.Intent(requireContext(), io.legado.app.ui.book.source.edit.BookSourceEditActivity::class.java)
                    intent.putExtra("sourceUrl", src.bookSourceUrl)
                    startActivity(intent)
                }
                LongAction.ENABLE_TOGGLE -> {
                    // 简单切换启用状态
                    src.enabled = !src.enabled
                    appDb.bookSourceDao.update(src)
                    loadSources(binding.searchView.query?.toString())
                }
                LongAction.CHECK -> {
                    // 仅触发一次网络检查的入口占位（具体实现可接入现有校验流程）
                    // 这里先提示用户，后续可对接 CheckSource
                }
            }
        }
    }

    private fun loadSources(keyword: String?) {
        val all = appDb.bookSourceDao.getEnabledByType(selectedType)
            .filter { it.enabledExplore && !it.exploreUrl.isNullOrBlank() }

        val favSet = appCtx.getPrefStringSet(favKey) ?: mutableSetOf()
        val recentSet = appCtx.getPrefStringSet(recentKey) ?: mutableSetOf()
        
        // 调试信息
        AppLog.put("SourceSelector: 总书源数=${all.size}, 筛选状态: 收藏=${binding.chipFav.isChecked}, 最近=${binding.chipRecent.isChecked}, 需登录=${binding.chipNeedLogin.isChecked}, 排序=${if (binding.chipSortRespond.isChecked) "按响应" else "按名称"}")

        // 关键字过滤
        var list = if (keyword.isNullOrBlank()) all else all.filter { src ->
            src.bookSourceName.contains(keyword, true) || (src.bookSourceGroup?.contains(keyword, true) == true)
        }

        // 筛选
        if (binding.chipNeedLogin.isChecked) list = list.filter { !it.loginUrl.isNullOrBlank() }
        if (binding.chipRecent.isChecked) list = list.filter { recentSet.contains(it.bookSourceUrl) }
        if (binding.chipFav.isChecked) list = list.filter { favSet.contains(it.bookSourceUrl) }

        // 先进行收藏置顶分组
        val favList = list.filter { favSet.contains(it.bookSourceUrl) }
        val nonFavList = list.filter { !favSet.contains(it.bookSourceUrl) }
        
        // 排序
        val sortByRespond = currentSortMode == SortMode.RESPOND
        val sortByName = currentSortMode == SortMode.NAME
        
        // 详细的状态检查
        AppLog.put("SourceSelector: === 排序状态检查 ===")
        AppLog.put("SourceSelector: currentSortMode = $currentSortMode")
        AppLog.put("SourceSelector: chipSortRespond.isChecked = ${binding.chipSortRespond.isChecked}")
        AppLog.put("SourceSelector: chipSortName.isChecked = ${binding.chipSortName.isChecked}")
        AppLog.put("SourceSelector: 计算得出 - 按响应: $sortByRespond, 按名称: $sortByName")
        AppLog.put("SourceSelector: 收藏书源: ${favList.size}个, 非收藏书源: ${nonFavList.size}个")
        
        // 显示前几个书源的详细信息
        if (favList.isNotEmpty()) {
            AppLog.put("SourceSelector: 收藏书源前3个: ${favList.take(3).joinToString { "${it.bookSourceName}(${it.respondTime}ms)" }}")
        }
        if (nonFavList.isNotEmpty()) {
            AppLog.put("SourceSelector: 非收藏书源前3个: ${nonFavList.take(3).joinToString { "${it.bookSourceName}(${it.respondTime}ms)" }}")
        }
        
        // 分别对收藏和非收藏的书源进行排序
        val sortedFavList = if (sortByRespond) {
            // 按响应时间排序，默认值排在最后
            favList.sortedWith(compareBy<BookSource> { 
                if (it.respondTime <= 0) Long.MAX_VALUE else it.respondTime
            }.thenBy { it.bookSourceName })
        } else {
            // 按名称排序
            favList.sortedBy { it.bookSourceName }
        }
        
        val sortedNonFavList = if (sortByRespond) {
            // 按响应时间排序，默认值排在最后
            nonFavList.sortedWith(compareBy<BookSource> { 
                if (it.respondTime <= 0) Long.MAX_VALUE else it.respondTime
            }.thenBy { it.bookSourceName })
        } else {
            // 按名称排序
            nonFavList.sortedBy { it.bookSourceName }
        }
        
        // 合并列表：收藏的在前，非收藏的在后
        list = sortedFavList + sortedNonFavList
        
        AppLog.put("SourceSelector: === 排序结果 ===")
        AppLog.put("SourceSelector: 排序完成，总数: ${list.size}")
        AppLog.put("SourceSelector: 前10个书源: ${list.take(10).joinToString { "${it.bookSourceName}(${if (it.respondTime > 0) "${it.respondTime}ms" else "未测试"})" }}")
        
        // 验证排序是否正确
        if (sortByRespond && list.size > 1) {
            val firstWithTime = list.firstOrNull { it.respondTime > 0 }
            val lastWithTime = list.lastOrNull { it.respondTime > 0 }
            if (firstWithTime != null && lastWithTime != null) {
                AppLog.put("SourceSelector: 响应时间验证 - 第一个: ${firstWithTime.bookSourceName}(${firstWithTime.respondTime}ms), 最后一个: ${lastWithTime.bookSourceName}(${lastWithTime.respondTime}ms)")
                
                // 检查前几个有响应时间的书源是否按时间排序
                val sourcesWithTime = list.filter { it.respondTime > 0 }.take(5)
                if (sourcesWithTime.size > 1) {
                    val isSorted = sourcesWithTime.zipWithNext().all { (a, b) -> a.respondTime <= b.respondTime }
                    AppLog.put("SourceSelector: 前5个有响应时间的书源排序验证: $isSorted")
                    AppLog.put("SourceSelector: 排序详情: ${sourcesWithTime.joinToString { "${it.bookSourceName}(${it.respondTime}ms)" }}")
                }
            }
        }


        adapter.submitList(list)
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onSelect(source: BookSource) {
        // 记录最近使用
        val set = appCtx.getPrefStringSet(recentKey) ?: mutableSetOf()
        set.add(source.bookSourceUrl)
        appCtx.putPrefStringSet(recentKey, set)
        // 同时让 Explore 记住该选择（ViewModel会再次持久化，双保险）
        (parentFragment as? CallBack)?.onSourceSelected(source)
        (activity as? CallBack)?.onSourceSelected(source)
        dismissAllowingStateLoss()
    }

    private fun isFavorite(url: String): Boolean {
        val favSet = appCtx.getPrefStringSet(favKey) ?: mutableSetOf()
        return favSet.contains(url)
    }

    private fun toggleFavorite(url: String) {
        val favSet = appCtx.getPrefStringSet(favKey) ?: mutableSetOf()
        if (favSet.contains(url)) favSet.remove(url) else favSet.add(url)
        appCtx.putPrefStringSet(favKey, favSet)
        // 立即刷新以反映置顶与星标图标
        loadSources(binding.searchView.query?.toString())
    }
}

private class SourceSelectorAdapter(
    val onClick: (BookSource) -> Unit,
    val onToggleFav: (String) -> Unit,
    val isFav: (String) -> Boolean
) : RecyclerView.Adapter<SourceVH>() {
    private val items = mutableListOf<BookSource>()
    private var onLongAction: ((BookSource, LongAction) -> Unit)? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_source_selector, parent, false)
        return SourceVH(view)
    }
    override fun onBindViewHolder(holder: SourceVH, position: Int) {
        holder.bind(items[position], onClick, onToggleFav, isFav)
    }
    override fun getItemCount(): Int = items.size
    fun submitList(list: List<BookSource>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun setOnLongAction(block: (BookSource, LongAction) -> Unit) {
        onLongAction = block
    }

    fun triggerLongAction(item: BookSource, action: LongAction) {
        onLongAction?.invoke(item, action)
    }
}

private class SourceVH(view: View) : RecyclerView.ViewHolder(view) {
    private val tvTitle = itemView.findViewById<android.widget.TextView>(R.id.tv_title)
    private val tvSub = itemView.findViewById<android.widget.TextView>(R.id.tv_sub)
    private val ivStar = itemView.findViewById<android.widget.ImageView>(R.id.iv_star)

    fun bind(
        item: BookSource,
        onClick: (BookSource) -> Unit,
        onToggleFav: (String) -> Unit,
        isFav: (String) -> Boolean
    ) {
        tvTitle.text = item.bookSourceName
        val subText = buildString {
            if (!item.bookSourceGroup.isNullOrBlank()) {
                append(item.bookSourceGroup)
                append(" · ")
            }
            append("响应 ")
            if (item.respondTime > 0) {
                append(item.respondTime)
                append("ms")
            } else {
                append("未测试")
            }
        }
        tvSub.text = subText

        val fav = isFav(item.bookSourceUrl)
        ivStar.setImageResource(if (fav) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
        ivStar.contentDescription = if (fav) "unstar" else "star"

        itemView.setOnClickListener { onClick(item) }
        itemView.setOnLongClickListener {
            showContextMenu(it, item)
            true
        }
        ivStar.setOnClickListener {
            onToggleFav(item.bookSourceUrl)
        }
    }

    private fun showContextMenu(v: View, item: BookSource) {
        val popup = androidx.appcompat.widget.PopupMenu(v.context, v)
        popup.menu.add(0, 1, 0, "编辑")
        popup.menu.add(0, 2, 1, if (item.enabled) "禁用" else "启用")
        popup.menu.add(0, 3, 2, "测试连通性")
        popup.setOnMenuItemClickListener { mi ->
            when (mi.itemId) {
                1 -> ( (v.parent as? RecyclerView)?.adapter as? SourceSelectorAdapter )?.triggerLongAction(item, LongAction.EDIT)
                2 -> ( (v.parent as? RecyclerView)?.adapter as? SourceSelectorAdapter )?.triggerLongAction(item, LongAction.ENABLE_TOGGLE)
                3 -> ( (v.parent as? RecyclerView)?.adapter as? SourceSelectorAdapter )?.triggerLongAction(item, LongAction.CHECK)
            }
            true
        }
        popup.show()
    }
}

private enum class LongAction { EDIT, ENABLE_TOGGLE, CHECK }


