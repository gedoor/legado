package io.legado.app.ui.book.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.activityViewModels
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.FragmentWebViewLoginBinding
import io.legado.app.help.http.CookieStore
import io.legado.app.utils.snackbar
import io.legado.app.utils.viewbindingdelegate.viewBinding

class WebViewLoginFragment : BaseFragment(R.layout.fragment_web_view_login) {

    private val binding by viewBinding(FragmentWebViewLoginBinding::bind)
    private val viewModel by activityViewModels<SourceLoginViewModel>()

    private var checking = false

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.bookSource?.let {
            binding.titleBar.title = getString(R.string.login_source, it.bookSourceName)
            initWebView(it)
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.source_login, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_ok -> {
                if (!checking) {
                    checking = true
                    binding.titleBar.snackbar(R.string.check_host_cookie)
                    viewModel.bookSource?.loginUrl?.let {
                        binding.webView.loadUrl(it)
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(bookSource: BookSource) {
        val settings = binding.webView.settings
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.javaScriptEnabled = true
        bookSource.getHeaderMap()[AppConst.UA_NAME]?.let {
            settings.userAgentString = it
        }
        val cookieManager = CookieManager.getInstance()
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                val cookie = cookieManager.getCookie(url)
                CookieStore.setCookie(bookSource.bookSourceUrl, cookie)
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val cookie = cookieManager.getCookie(url)
                CookieStore.setCookie(bookSource.bookSourceUrl, cookie)
                if (checking) {
                    activity?.finish()
                }
                super.onPageFinished(view, url)
            }
        }
        bookSource.loginUrl?.let {
            binding.webView.loadUrl(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }

}