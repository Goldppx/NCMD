package com.gem.neteasecloudmd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.gem.neteasecloudmd.api.SessionManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val sessionManager = SessionManager(this)
        val languageTag = SessionManager.languageTagFromMode(sessionManager.getLanguageMode())
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NCMDApp()
        }
    }
}
