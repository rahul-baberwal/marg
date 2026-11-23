package com.rahulbaberwal.marg.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rahulbaberwal.marg.MargViewModel

@Composable
fun MargApp(
    modifier: Modifier = Modifier,
    vm: MargViewModel = viewModel()
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        // MapScreen is preserved in composition so MapView, route, and pins never unload
        MapScreen(
            vm = vm,
            onOpenAbout = { showSettings = true }
        )

        // SettingsScreen overlays smoothly when opened and slides back on dismiss
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            SettingsScreen(
                vm = vm,
                onBack = { showSettings = false }
            )
        }
    }
}
