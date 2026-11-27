package fr.rafu.ooty

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/** Full view of the plan: the same page as the web dashboard, bundled in the app. */
class MainActivity : Activity() {
    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        // Links to other sites (the editable page on claude.ai) open in the browser
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url
                if (u.host == "ooty.app") return false
                startActivity(Intent(Intent.ACTION_VIEW, u))
                return true
            }
        }
        setContentView(web)
        load()
    }

    override fun onResume() {
        super.onResume()
        Thread {
            if (Plan.refresh(this)) runOnUiThread {
                load()
                TodayWidget.updateAll(this)
            }
        }.start()
    }

    private fun load() {
        web.loadDataWithBaseURL("https://ooty.app/", Plan.html(this), "text/html", "utf-8", null)
    }
}
