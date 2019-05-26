package io.legado.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.constant.AppConst.APP_TAG
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.ui.replacerule.ReplaceRuleActivity
import io.legado.app.ui.search.SearchActivity
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.File

class MainActivity : BaseActivity<MainDataBinding, MainViewModel>(), NavigationView.OnNavigationItemSelectedListener {
    override val viewModel: MainViewModel
        get() = getViewModel(MainViewModel::class.java)

    override val layoutID: Int
        get() = R.layout.activity_main

    override fun onViewModelCreated(viewModel: MainViewModel, savedInstanceState: Bundle?) {
        fab.setOnClickListener { startActivity(Intent(this, SearchActivity::class.java)) }

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, title_bar.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_backup -> {
                // Handle the camera action
            }
            R.id.nav_import -> {

            }
            R.id.nav_import_old -> PermissionsCompat.Builder(this)
                .addPermissions(*Permissions.Group.STORAGE)
                .rationale(R.string.tip_perm_request_storage)
                .onGranted { importYueDu() }.request()
            R.id.nav_import_github -> {

            }
            R.id.nav_replace_rule -> startActivity<ReplaceRuleActivity>()
            R.id.nav_send -> {

            }
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    /*
    * import from YueDu backup data
    * */
    fun importYueDu() {
        val yuedu = File(getSdPath(), "YueDu")
        val jsonPath = JsonPath.using(
            Configuration.builder()
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build()
        )

        // 导入书架
        val shelfFile = File(yuedu, "myBookShelf.json")
        val books = mutableListOf<Book>()
        if (shelfFile.exists()) try {
            doAsync {
                val items: List<Map<String, Any>> = jsonPath.parse(shelfFile.readText()).read("$")
                val existingBooks = App.db.bookDao().allBookUrls.toSet()
                for (item in items) {
                    val jsonItem = jsonPath.parse(item)
                    val book = Book()
                    book.descUrl = jsonItem.readString("$.noteUrl") ?: ""
                    if (book.descUrl.isBlank()) continue
                    book.name = jsonItem.readString("$.bookInfoBean.name")
                    if (book.descUrl in existingBooks) {
                        Log.d(APP_TAG, "Found existing book: ${book.name}")
                        continue
                    }
                    book.author = jsonItem.readString("$.bookInfoBean.author")
                    book.type = if (jsonItem.readString("$.bookInfoBean.bookSourceType") == "AUDIO") 1 else 0
                    book.tocUrl = jsonItem.readString("$.bookInfoBean.chapterUrl") ?: book.descUrl
                    book.coverUrl = jsonItem.readString("$.bookInfoBean.coverUrl")
                    book.customCoverUrl = jsonItem.readString("$.customCoverPath")
                    book.lastCheckTime = jsonItem.readLong("$.bookInfoBean.finalRefreshData") ?: 0
                    book.canUpdate = jsonItem.readBool("$.allowUpdate") == true
                    book.totalChapterNum = jsonItem.readInt("$.chapterListSize") ?: 0
                    book.durChapterIndex = jsonItem.readInt("$.durChapter") ?: 0
                    book.durChapterTitle = jsonItem.readString("$.durChapterName")
                    book.durChapterPos = jsonItem.readInt("$.durChapterPage") ?: 0
                    book.durChapterTime = jsonItem.readLong("$.finalDate") ?: 0
                    book.group = jsonItem.readInt("$.group") ?: 0
                    // book. = jsonItem.readString("$.hasUpdate")
                    // book. = jsonItem.readString("$.isLoading")
                    book.latestChapterTitle = jsonItem.readString("$.lastChapterName")
                    book.lastCheckCount = jsonItem.readInt("$.newChapters") ?: 0
                    book.order = jsonItem.readInt("$.serialNumber") ?: 0
                    book.useReplaceRule = jsonItem.readBool("$.useReplaceRule") == true
                    book.variable = jsonItem.readString("$.variable")
                    books.add(book)
                    Log.d(APP_TAG, "Added ${book.name}")
                }
                App.db.bookDao().insert(*books.toTypedArray())
                val count = books.size

                uiThread {
                    toast(if (count > 0) "成功地导入 $count 本新书和音频" else "没有发现新书或音频")
                }

            }

        } catch (e: Exception) {
            Log.e(APP_TAG, "Failed to import book shelf.", e)
            toast("Unable to import books:\n${e.localizedMessage}")
        }

        // Replace rules
        val ruleFile = File(yuedu, "myBookReplaceRule.json")
        val replaceRules = mutableListOf<ReplaceRule>()
        if (ruleFile.exists()) try {
            doAsync {
                val items: List<Map<String, Any>> = jsonPath.parse(ruleFile.readText()).read("$")
                val existingRules = App.db.replaceRuleDao().all.map { it.pattern }.toSet()
                for (item in items) {
                    val jsonItem = jsonPath.parse(item)
                    val rRule = ReplaceRule()
                    rRule.pattern = jsonItem.readString("$.regex")
                    if (rRule.pattern.isNullOrEmpty() || rRule.pattern in existingRules) continue
                    rRule.name = jsonItem.readString("$.replaceSummary")
                    rRule.replacement = jsonItem.readString("$.replacement")
                    rRule.isRegex = jsonItem.readBool("$.isRegex") == true
                    rRule.scope = jsonItem.readString("$.useTo")
                    rRule.isEnabled = jsonItem.readBool("$.enable") == true
                    rRule.order = jsonItem.readInt("$.serialNumber") ?: 0
                    replaceRules.add(rRule)
                }
                App.db.replaceRuleDao().insert(*replaceRules.toTypedArray())
                val count = replaceRules.size
                val maxId = App.db.replaceRuleDao().maxOrder
                uiThread {
                    toast(if (count > 0) "成功地导入 $count 条净化替换规则" else "没有发现新的净化替换规则")
                }
            }

        } catch (e: Exception) {
            Log.e(APP_TAG, e.localizedMessage)
        }
    }


}
