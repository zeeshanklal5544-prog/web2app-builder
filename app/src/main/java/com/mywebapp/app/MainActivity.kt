package com.mywebapp.app
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
  private lateinit var web: WebView
  private lateinit var spinner: ProgressBar
  private var fileCb: ValueCallback<Array<Uri>>? = null

  override fun onCreate(s: Bundle?) {
    super.onCreate(s)
    val cfg = Config.load(this)
    val color = try { Color.parseColor(cfg.optString("color", "#2563eb")) } catch (e: Exception) { Color.BLUE }
    window.statusBarColor = color

    val root = FrameLayout(this)
    val swipe = SwipeRefreshLayout(this)
    web = WebView(this)
    swipe.addView(web)
    swipe.setColorSchemeColors(color)
    root.addView(swipe)
    spinner = ProgressBar(this).apply {
      isIndeterminate = true
      indeterminateTintList = android.content.res.ColorStateList.valueOf(color)
    }
    root.addView(spinner, FrameLayout.LayoutParams(140, 140, Gravity.CENTER))
    setContentView(root)

    web.settings.apply {
      javaScriptEnabled = true; domStorageEnabled = true; databaseEnabled = true
      loadWithOverviewMode = true; useWideViewPort = true
      mediaPlaybackRequiresUserGesture = false; setSupportMultipleWindows(false)
    }
    CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
    web.webViewClient = object : WebViewClient() {
      override fun onPageStarted(v: WebView, u: String, f: Bitmap?) { spinner.visibility = View.VISIBLE }
      override fun onPageFinished(v: WebView, u: String) { spinner.visibility = View.GONE; swipe.isRefreshing = false }
      override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
        val u = r.url.toString()
        if (u.startsWith("http")) return false
        try { startActivity(Intent(Intent.ACTION_VIEW, r.url)) } catch (_: Exception) {}
        return true
      }
      override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
        if (r.isForMainFrame) v.loadData("<h3 style='font-family:sans-serif;text-align:center;margin-top:40%'>No internet. Pull down to retry.</h3>", "text/html", "utf-8")
      }
    }
    web.webChromeClient = object : WebChromeClient() {
      override fun onShowFileChooser(v: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams): Boolean {
        fileCb?.onReceiveValue(null); fileCb = cb
        return try { startActivityForResult(p.createIntent(), 7); true } catch (e: Exception) { fileCb = null; false }
      }
    }
    swipe.setOnRefreshListener { web.reload() }
    swipe.setOnChildScrollUpCallback { _, _ -> web.scrollY > 0 }

    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() { if (web.canGoBack()) web.goBack() else finish() }
    })

    web.loadUrl(intent?.dataString ?: cfg.optString("url"))

    lifecycleScope.launch { NotifyWorker.checkNow(applicationContext) }

    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
      ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
  }

  @Deprecated("") override fun onActivityResult(req: Int, res: Int, data: Intent?) {
    super.onActivityResult(req, res, data)
    if (req == 7) { fileCb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data)); fileCb = null }
  }
}
