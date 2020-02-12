package io.legado.app.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.drakeet.multitype.MultiTypeAdapter
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.lib.theme.primaryColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.utils.shareText
import kotlinx.android.synthetic.main.activity_about_page.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.textColor
import org.jetbrains.anko.toast
import java.util.*

class AboutActivity : BaseActivity(R.layout.activity_about_page, fullScreen = false) {
    private val items: MutableList<Any> = ArrayList()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        icon.setImageResource(R.drawable.ic_launcher_foreground)
        slogan.setText(R.string.slogan)
        val versionString = getString(R.string.version) + " " + App.INSTANCE.versionName
        version.text = versionString

        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowHomeEnabled(true)
        }

        collapsingToolbar.setContentScrimColor(primaryColor)
        headerContentLayout.backgroundColor = primaryColor
        collapsingToolbar.setCollapsedTitleTextColor(primaryTextColor)
        slogan.textColor = primaryTextColor
        version.textColor = primaryTextColor
        version.setOnClickListener {
            openIntent(
                Intent.ACTION_VIEW,
                getString(R.string.latest_release_url)
            )
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val adapter = MultiTypeAdapter()
        adapter.register(ActionItem::class, ActionItemViewBinder(this))
        adapter.register(Category::class, CategoryViewBinder())
        onItemsCreated(items)

        adapter.items = items
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
        ATH.applyEdgeEffectColor(recyclerView)
    }

    private fun onItemsCreated(items: MutableList<Any>) {
        buildAboutCategory(items)
        buildContactUsCategory(items)
        buildDeveloperCategory(items)
        buildOthersCategory(items)
    }

    private fun buildAboutCategory(items: MutableList<Any>) {
        val iconColor = ThemeStore.textColorPrimary(this)
        items.add(Category(getString(R.string.about)))
        items.add(
            ActionItem.Builder()
                .icon(IconicsDrawable(this, CommunityMaterial.Icon2.cmd_post)
                    .apply {
                        colorInt(iconColor)
                        sizeDp(18)
                    })
                .text(R.string.update_log)
                .setOnClickAction(object : ActionListener {
                    override fun action() {
                        UpdateLog().show(supportFragmentManager, "update_log")
                    }
                })
                .build()
        )
        items.add(
            ActionItem.Builder()
                .icon(IconicsDrawable(this, CommunityMaterial.Icon2.cmd_thumb_up)
                    .apply {
                        colorInt(iconColor)
                        sizeDp(18)
                    })
                .text(R.string.scoring)
                .setOnClickAction(object : ActionListener {
                    override fun action() {
                        openIntent(Intent.ACTION_VIEW, "market://details?id=$packageName")
                    }
                })
                .build()
        )
        items.add(
            ActionItem.Builder()
                .icon(IconicsDrawable(this, CommunityMaterial.Icon.cmd_bug)
                    .apply {
                        colorInt(iconColor)
                        sizeDp(18)
                    })
                .text(R.string.send_mail)
                .subText(R.string.send_feedback_hint)
                .setOnClickAction(object : ActionListener {
                    override fun action() {
                        openIntent(Intent.ACTION_SENDTO, "mailto:kunfei.ge@gmail.com")
                    }
                })
                .build()
        )
        items.add(
            ActionItem.Builder()
                .icon(IconicsDrawable(this, CommunityMaterial.Icon2.cmd_share_variant)
                    .apply {
                        colorInt(iconColor)
                        sizeDp(18)
                    })
                .text(R.string.share_app)
                .setOnClickAction(object : ActionListener {
                    override fun action() {
                        shareText(
                            "App Share",
                            getString(R.string.app_share_description)
                        )
                    }
                })
                .build()
        )
    }

    private fun buildContactUsCategory(items: MutableList<Any>) {
        val iconColor = ThemeStore.textColorPrimary(this)
        items.add(Category(getString(R.string.contact_us)))
        items.add(
            ActionItem.Builder()
                .icon(IconicsDrawable(this, CommunityMaterial.Icon2.cmd_wechat)
                    .apply {
                        colorInt(iconColor)
                        sizeDp(18)
                    })
                .text(R.string.wechat_public_account)
                .subText(R.string.wechat_public_account_name)
                .setOnClickAction(object : ActionListener {
                    override fun action() {
                        // TODO 提供关注微信公众号的方法
                    }
                })
                .build()
        )
        items.add(
            ActionItem.Builder()
                .icon(IconicsDrawable(this, CommunityMaterial.Icon2.cmd_qqchat)
                    .apply {
                        colorInt(iconColor)
                        sizeDp(18)
                    })
                .text(R.string.join_qq_group)
                .setOnClickAction(object : ActionListener {
                    override fun action() {
                        // TODO 提供QQ加群方式
                    }
                })
                .build()
        )
        items.add(
            ActionItem.Builder()
                .icon(IconicsDrawable(this, CommunityMaterial.Icon.cmd_github_circle)
                    .apply {
                        colorInt(iconColor)
                        sizeDp(18)
                    })
                .text(R.string.git_hub)
                .subText(R.string.this_github_url)
                .setOnClickAction(object : ActionListener {
                    override fun action() {
                        openIntent(Intent.ACTION_VIEW, getString(R.string.this_github_url))
                    }
                })
                .build()
        )
        items.add(
            ActionItem.Builder()
                .icon(IconicsDrawable(this, CommunityMaterial.Icon2.cmd_web_box)
                    .apply {
                        colorInt(iconColor)
                        sizeDp(18)
                    })
                .text(R.string.home_page)
                .subText(R.string.home_page_url)
                .setOnClickAction(object : ActionListener {
                    override fun action() {
                        openIntent(Intent.ACTION_VIEW, getString(R.string.home_page_url))
                    }
                })
                .build()
        )
    }

    private fun buildDeveloperCategory(items: MutableList<Any>) {
        val iconColor = ThemeStore.textColorPrimary(this)
        items.add(Category(getString(R.string.developers)))
        items.add(
            ActionItem.Builder()
                .icon(IconicsDrawable(this, CommunityMaterial.Icon.cmd_developer_board)
                    .apply {
                        colorInt(iconColor)
                        sizeDp(18)
                    })
                .text(R.string.developer_description)
                .subText(R.string.developer_detail)
                .setOnClickAction(object : ActionListener {
                    override fun action() {
                        openIntent(Intent.ACTION_VIEW, getString(R.string.contributors_url))
                    }
                })
                .build()
        )
    }

    private fun buildOthersCategory(items: MutableList<Any>) {
        val iconColor = ThemeStore.textColorPrimary(this)
        items.add(Category("其他"))
        items.add(
            ActionItem.Builder()
                .icon(IconicsDrawable(this, CommunityMaterial.Icon2.cmd_license)
                    .apply {
                        colorInt(iconColor)
                        sizeDp(18)
                    })
                .text("开源许可")
                .setOnClickAction(object : ActionListener {
                    override fun action() {
                        // TODO 打开开源许可页面
                    }
                })
                .build()
        )
        items.add(
            ActionItem.Builder()
                .showIcon(true)
                .text(R.string.disclaimer)
                .setOnClickAction(object : ActionListener {
                    override fun action() {
                        // TODO 打开免责声明页面
                    }
                })
                .build()
        )
    }

    private fun openIntent(intentName: String, address: String) {
        try {
            val intent = Intent(intentName)
            intent.data = Uri.parse(address)
            startActivity(intent)
        } catch (e: Exception) {
            toast(R.string.can_not_open)
        }
    }
}
