package io.legado.app.ui.agents

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivityAgentsBinding
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * Agents 全屏页面骨架。
 *
 * 本页面仅提供“可打开、可展示入口上下文提示、可本地回显发送”的 UI 基础能力，
 * 后续会话/持久化/模型调用等功能不在本任务范围内。
 *
 * Returns:
 *   无。
 */
class AgentsActivity : BaseActivity<ActivityAgentsBinding>() {

    override val binding by viewBinding(ActivityAgentsBinding::inflate)

    /**
     * Activity 创建完成后的初始化入口。
     *
     * 这里集中完成：顶部栏初始化、交互事件绑定、入口上下文弱提示展示，以及首屏占位消息注入。
     *
     * Args:
     *   savedInstanceState: 系统提供的状态保存对象；本页面骨架不依赖恢复逻辑。
     * Returns:
     *   无。
     */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initTopBar()
        bindUiEvent()
        applyEntryContextHint()
        ensureInitialPlaceholderMessage()
    }

    /**
     * 创建顶部栏右侧菜单（设置占位）。
     *
     * Args:
     *   menu: 需要被填充的菜单对象。
     * Returns:
     *   是否成功创建菜单。
     */
    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.agents, menu)
        return true
    }

    /**
     * 处理顶部栏菜单点击事件。
     *
     * Args:
     *   item: 被点击的菜单项。
     * Returns:
     *   是否已消费该事件。
     */
    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_settings -> {
                // 需求允许“设置”按钮先占位不实现跳转。
                toastOnUi(getString(R.string.not_available))
                return true
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    /**
     * 初始化顶部栏（左历史会话按钮 + 中标题 + 右设置按钮）。
     *
     * 说明：左侧按钮借用 Toolbar 的 navigationIcon 位置，以避免额外的自定义布局侵入。
     *
     * Returns:
     *   无。
     */
    private fun initTopBar() {
        binding.titleBar.title = getString(R.string.agents_title)

        // 左侧使用导航图标承载“历史会话”入口（占位）。
        binding.titleBar.toolbar.navigationIcon =
            AppCompatResources.getDrawable(this, R.drawable.ic_history)
        binding.titleBar.toolbar.navigationContentDescription =
            getString(R.string.agents_history)
        binding.titleBar.setNavigationOnClickListener {
            // 需求允许“历史会话”按钮先占位不实现抽屉。
            toastOnUi(getString(R.string.not_available))
        }
    }

    /**
     * 绑定页面内的交互事件：发送、本地回显、新建会话。
     *
     * Returns:
     *   无。
     */
    private fun bindUiEvent() {
        binding.btnNewSession.setOnClickListener {
            // “新建会话”占位：清空当前消息并提示。
            binding.llMessages.removeAllViews()
            addMessage(getString(R.string.agents_new_session_created))
        }

        binding.btnSend.setOnClickListener {
            val input = binding.etInput.text?.toString()?.trim().orEmpty()
            if (input.isBlank()) {
                toastOnUi(getString(R.string.agents_input_empty_tip))
                return@setOnClickListener
            }

            // 发送行为占位：本地回显，不调用模型。
            addMessage(getString(R.string.agents_message_me_format, input))
            addMessage(getString(R.string.agents_message_echo_format, input))

            binding.etInput.setText("")
            scrollToBottom()
        }
    }

    /**
     * 根据入口来源展示上下文弱提示。
     *
     * - 从阅读页进入：会携带书名与章节名，展示在标题下方。
     * - 从“我的”页进入：不携带上下文，弱提示隐藏。
     *
     * Returns:
     *   无。
     */
    private fun applyEntryContextHint() {
        val bookName = intent.getStringExtra(EXTRA_BOOK_NAME)
        val chapterName = intent.getStringExtra(EXTRA_CHAPTER_NAME)
        val shouldShow = !bookName.isNullOrBlank() && !chapterName.isNullOrBlank()

        if (shouldShow) {
            binding.tvContextHint.visibility = View.VISIBLE
            binding.tvContextHint.text = getString(
                R.string.agents_context_hint_format,
                bookName,
                chapterName
            )
        } else {
            binding.tvContextHint.visibility = View.GONE
        }
    }

    /**
     * 确保首次进入页面时内容区可滚动且有占位内容。
     *
     * Returns:
     *   无。
     */
    private fun ensureInitialPlaceholderMessage() {
        if (binding.llMessages.childCount > 0) return
        addMessage(getString(R.string.agents_placeholder_message))
        scrollToBottom()
    }

    /**
     * 向会话内容区追加一条文本消息。
     *
     * Args:
     *   text: 要追加的消息文本。
     * Returns:
     *   无。
     */
    private fun addMessage(text: String) {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 14f
            setLineSpacing(0f, 1.2f)
            // 说明：保持默认主题文字色，避免与主题/夜间模式强耦合。
        }
        binding.llMessages.addView(tv)
    }

    /**
     * 将滚动容器滚动到底部，以展示最新消息。
     *
     * Returns:
     *   无。
     */
    private fun scrollToBottom() {
        binding.scrollMessages.post {
            binding.scrollMessages.fullScroll(View.FOCUS_DOWN)
        }
    }

    companion object {
        /** 从阅读页入口传入的书名（用于弱提示展示）。 */
        const val EXTRA_BOOK_NAME = "extra_agents_book_name"

        /** 从阅读页入口传入的章节名（用于弱提示展示）。 */
        const val EXTRA_CHAPTER_NAME = "extra_agents_chapter_name"
    }
}
