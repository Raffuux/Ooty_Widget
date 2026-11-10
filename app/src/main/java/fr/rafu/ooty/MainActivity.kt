package fr.rafu.ooty

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.WebView

/** Full view of the plan: the same page as the web dashboard, bundled in the app. */
class MainActivity : Activity() {
    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
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
