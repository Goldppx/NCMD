package com.gem.neteasecloudmd

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gem.neteasecloudmd.api.SessionManager
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val sessionManager = SessionManager(newBase)
        val languageTag = SessionManager.languageTagFromMode(sessionManager.getLanguageMode())
        if (languageTag.isBlank()) {
            super.attachBaseContext(newBase)
            return
        }

        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NCMDApp()
        }
    }
}
