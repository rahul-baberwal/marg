package com.rahulbaberwal.marg

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rahulbaberwal.marg.ui.MargApp
import com.rahulbaberwal.marg.ui.theme.MargTheme

class MainActivity : ComponentActivity() {

    private val vm: MargViewModel by viewModels()

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            val themeMode by vm.themeMode.collectAsState()
            val systemInDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemInDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            MargTheme(darkTheme = useDarkTheme) {
                MargApp(vm = vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent != null) {
            vm.handleIncomingIntent(intent)
        }
    }
}
