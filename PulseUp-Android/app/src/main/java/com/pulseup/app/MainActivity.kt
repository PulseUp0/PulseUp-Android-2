package com.pulseup.app

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var pendingWebPermission: PermissionRequest? = null
    private var pendingWebResources: Array<String> = emptyArray()

    companion object {
        private const val FILE_CHOOSER_REQUEST = 4101
        private const val WEB_PERMISSION_REQUEST = 4102
        private const val HOME_URL = "https://pulseup0.github.io/PulseUp/"
        private const val PULSEUP_HOST = "pulseup0.github.io"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 5, 27)
        window.navigationBarColor = android.graphics.Color.rgb(9, 5, 27)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val root = FrameLayout(this)
        webView = WebView(this)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = View.GONE
        }
        root.addView(webView, FrameLayout.LayoutParams(-1, -1))
        root.addView(progress, FrameLayout.LayoutParams(-1, dp(3)))
        setContentView(root)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            // Lets the website hide its "download Android app" banner inside the native app.
            userAgentString = "$userAgentString PulseUpAndroidApp/1.0"
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = true
            builtInZoomControls = false
            displayZoomControls = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "https" && uri.host == PULSEUP_HOST) return false
                if (uri.scheme == "about") return false
                return openExternal(uri)
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val uri = Uri.parse(url)
                if (uri.scheme == "https" && uri.host == PULSEUP_HOST) return false
                if (uri.scheme == "about") return false
                return openExternal(uri)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progress.visibility = View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.visibility = if (newProgress in 0..99) View.VISIBLE else View.GONE
                progress.progress = newProgress
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback
                return try {
                    val intent = fileChooserParams.createIntent().apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        if (type.isNullOrBlank()) type = "*/*"
                    }
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST)
                    true
                } catch (_: ActivityNotFoundException) {
                    fileChooserCallback = null
                    Toast.makeText(this@MainActivity, "Не удалось открыть выбор файла", Toast.LENGTH_SHORT).show()
                    false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    val resources = request.resources ?: emptyArray()
                    val needsCamera = resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                    val needsMic = resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                    val missing = mutableListOf<String>()
                    if (needsCamera && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        missing.add(Manifest.permission.CAMERA)
                    }
                    if (needsMic && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        missing.add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (missing.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        pendingWebPermission?.deny()
                        pendingWebPermission = request
                        pendingWebResources = resources
                        requestPermissions(missing.toTypedArray(), WEB_PERMISSION_REQUEST)
                    } else {
                        grantAllowedWebResources(request, resources)
                    }
                }
            }
        }

        if (savedInstanceState == null) {
            webView.loadUrl(HOME_URL)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    private fun grantAllowedWebResources(request: PermissionRequest, resources: Array<String>) {
        val allowed = resources.filter { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                else -> false
            }
        }.toTypedArray()
        if (allowed.isNotEmpty()) request.grant(allowed) else request.deny()
    }

    private fun openExternal(uri: Uri): Boolean {
        if (uri.scheme != "https" && uri.scheme != "http" && uri.scheme != "mailto" && uri.scheme != "tel") return true
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != FILE_CHOOSER_REQUEST) return
        val callback = fileChooserCallback ?: return
        fileChooserCallback = null
        val results = if (resultCode == RESULT_OK && data != null) {
            val clip = data.clipData
            when {
                clip != null -> Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
                data.data != null -> arrayOf(data.data!!)
                else -> null
            }
        } else null
        callback.onReceiveValue(results)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WEB_PERMISSION_REQUEST) {
            val request = pendingWebPermission
            pendingWebPermission = null
            if (request != null) grantAllowedWebResources(request, pendingWebResources)
            pendingWebResources = emptyArray()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Android API; retained for WebView back navigation")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        fileChooserCallback?.onReceiveValue(null)
        fileChooserCallback = null
        pendingWebPermission?.deny()
        pendingWebPermission = null
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            webView.destroy()
        }
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
