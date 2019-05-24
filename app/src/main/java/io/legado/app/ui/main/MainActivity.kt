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

        // Replace rules
        val rFile = File(yuedu, "myBookReplaceRule.json")
        val replaceRules = mutableListOf<ReplaceRule>()
        if (rFile.exists()) try {
            val items: List<Map<String, Any>> = jsonPath.parse(rFile.readText()).read("$.*")
            for (item in items) {
                val jsonItem = jsonPath.parse(item)
                val rRule = ReplaceRule()
                rRule.name = jsonItem.readString("$.replaceSummary")
                rRule.pattern = jsonItem.readString("$.regex")
                rRule.replacement = jsonItem.readString("$.replacement")
                rRule.isRegex = jsonItem.readBool("$.isRegex")
                rRule.scope = jsonItem.readString("$.useTo")
                rRule.isEnabled = jsonItem.readBool("$.enable")
                rRule.order = jsonItem.readInt("$.serialNumber")
                replaceRules.add(rRule)
            }

            doAsync {
                App.db.replaceRuleDao().insert(*replaceRules.toTypedArray())
                val count = items.size
                val maxId = App.db.replaceRuleDao().maxOrder
                uiThread {
                    toast("成功地导入 $count 条净化替换规则")
                }
            }

        } catch (e: Exception) {
            Log.e(APP_TAG, e.localizedMessage)
        }
    }


}
