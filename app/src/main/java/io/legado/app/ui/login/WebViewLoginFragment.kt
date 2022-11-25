package io.legado.app.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import androidx.fragment.app.activityViewModels
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.BaseSource
import io.legado.app.databinding.FragmentWebViewLoginBinding
import io.legado.app.help.http.CookieStore
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.gone
import io.legado.app.utils.snackbar
import io.legado.app.utils.viewbindingdelegate.viewBinding

class WebViewLoginFragment : BaseFragment(R.layout.fragment_web_view_login) {

    private val binding by viewBinding(FragmentWebViewLoginBinding::bind)
    private val viewModel by activityViewModels<SourceLoginViewModel>()

    private var checking = false

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        viewModel.source?.let {
            binding.titleBar.title = getString(R.string.login_source, it.getTag())
            initWebView(it)
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.source_webview_login, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_ok -> {
                if (!checking) {
                    checking = true
                    binding.titleBar.snackbar(R.string.check_host_cookie)
                    viewModel.source?.let { source ->
                        source.loginUrl?.let {
                            binding.webView.loadUrl(it, source.getHeaderMap(true))
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(source: BaseSource) {
        binding.progressBar.fontColor = accentColor
        binding.webView.settings.apply {
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            javaScriptEnabled = true
            displayZoomControls = false
            source.getHeaderMap()[AppConst.UA_NAME]?.let {
                userAgentString = it
            }
        }
        val cookieManager = CookieManager.getInstance()
        source.loginUrl?.let {
            cookieManager.setCookie(it, CookieStore.getCookie(it))
        }
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                val cookie = cookieManager.getCookie(url)
                CookieStore.setCookie(source.getKey(), cookie)
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val cookie = cookieManager.getCookie(url)
                CookieStore.setCookie(source.getKey(), cookie)
                if (checking) {
                    activity?.finish()
                }
                super.onPageFinished(view, url)
            }
        }
        binding.webView.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                binding.progressBar.setDurProgress(newProgress)
                binding.progressBar.gone(newProgress == 100)
            }

        }
        source.loginUrl?.let {
            binding.webView.loadUrl(it, source.getHeaderMap(true))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }

}