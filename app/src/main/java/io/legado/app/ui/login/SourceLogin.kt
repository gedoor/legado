package io.legado.app.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivitySourceLoginBinding
import io.legado.app.help.http.CookieStore
import io.legado.app.utils.snackbar


class SourceLogin : BaseActivity<ActivitySourceLoginBinding>() {

    var sourceUrl: String? = null
    var loginUrl: String? = null
    var checking = false

    override fun getViewBinding(): ActivitySourceLoginBinding {
        return ActivitySourceLoginBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        sourceUrl = intent.getStringExtra("sourceUrl")
        loginUrl = intent.getStringExtra("loginUrl")
        title = getString(R.string.login_source, sourceUrl)
        initWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        val settings = binding.webView.settings
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.javaScriptEnabled = true
        val cookieManager = CookieManager.getInstance()
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                val cookie = cookieManager.getCookie(url)
                sourceUrl?.let {
                    CookieStore.setCookie(it, cookie)
                }
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val cookie = cookieManager.getCookie(url)
                sourceUrl?.let {
                    CookieStore.setCookie(it, cookie)
                }
                if (checking) {
                    finish()
                }
                super.onPageFinished(view, url)
            }
        }
        loginUrl?.let {
            binding.webView.loadUrl(it)
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.source_login, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_success -> {
                if (!checking) {
                    checking = true
                    binding.titleBar.snackbar(R.string.check_host_cookie)
                    loginUrl?.let {
                        binding.webView.loadUrl(it)
                    }
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }
}