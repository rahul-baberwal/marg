package com.rahulbaberwal.marg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.rahulbaberwal.marg.ui.MargApp
import com.rahulbaberwal.marg.ui.theme.MargTheme

class MainActivity : ComponentActivity() {

    private val vm: MargViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MargTheme {
                MargApp(vm = vm)
            }
        }
    }
}
