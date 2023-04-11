package com.abast.homebot

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.abast.homebot.databinding.ActivityLicensesBinding

class LicensesActivity : AppCompatActivity() {

    companion object {
        const val LICENSES_HTML_PATH = "file:///android_asset/licenses.html"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLicensesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.webview.settings.loadWithOverviewMode = true
        binding.webview.settings.useWideViewPort = true
        binding.webview.loadUrl(LICENSES_HTML_PATH)
    }

}
