package com.rahulbaberwal.marg.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Satellite
import com.rahulbaberwal.marg.MapLayer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rahulbaberwal.marg.AppThemeMode
import com.rahulbaberwal.marg.BuildConfig
import com.rahulbaberwal.marg.MargViewModel
import com.rahulbaberwal.marg.R

private fun launchUrl(context: Context, url: String) {
    try {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    } catch (_: Exception) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        } catch (_: Exception) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: MargViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val currentTheme by vm.themeMode.collectAsState()
    val uiState by vm.uiState.collectAsState()
    var showFeatureGuide by rememberSaveable { mutableStateOf(false) }

    val systemDark = isSystemInDarkTheme()
    val isAppDark = when (currentTheme) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Back to map" }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Appearance & Theme
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Appearance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Choose how Marg looks on your device",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 3-way Segmented Theme Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionChip(
                            mode = AppThemeMode.SYSTEM,
                            icon = Icons.Default.BrightnessAuto,
                            label = "System",
                            isSelected = (currentTheme == AppThemeMode.SYSTEM),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                vm.setThemeMode(AppThemeMode.SYSTEM)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ThemeOptionChip(
                            mode = AppThemeMode.LIGHT,
                            icon = Icons.Default.LightMode,
                            label = "Light",
                            isSelected = (currentTheme == AppThemeMode.LIGHT),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                vm.setThemeMode(AppThemeMode.LIGHT)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        ThemeOptionChip(
                            mode = AppThemeMode.DARK,
                            icon = Icons.Default.DarkMode,
                            label = "Dark",
                            isSelected = (currentTheme == AppThemeMode.DARK),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                vm.setThemeMode(AppThemeMode.DARK)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section 2: Default Map Style Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Default Map Style",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Choose your preferred cartography layer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MapLayerOptionChip(
                                layer = MapLayer.STANDARD,
                                icon = Icons.Default.Map,
                                label = "Standard",
                                sub = "OpenStreetMap",
                                isSelected = (uiState.selectedLayer == MapLayer.STANDARD),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    vm.setLayer(MapLayer.STANDARD)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            MapLayerOptionChip(
                                layer = MapLayer.SATELLITE,
                                icon = Icons.Default.Satellite,
                                label = "Satellite",
                                sub = "High-Res Aerial",
                                isSelected = (uiState.selectedLayer == MapLayer.SATELLITE),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    vm.setLayer(MapLayer.SATELLITE)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MapLayerOptionChip(
                                layer = MapLayer.TERRAIN,
                                icon = Icons.Default.Landscape,
                                label = "Terrain",
                                sub = "Topographic",
                                isSelected = (uiState.selectedLayer == MapLayer.TERRAIN),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    vm.setLayer(MapLayer.TERRAIN)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            MapLayerOptionChip(
                                layer = MapLayer.DARK,
                                icon = Icons.Default.DarkMode,
                                label = "Dark",
                                sub = "OpenFreeMap",
                                isSelected = (uiState.selectedLayer == MapLayer.DARK),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    vm.setLayer(MapLayer.DARK)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Section 3: Quick Guide & Feature Hints Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Feature Guide & How to Use",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Learn tips, controls, and shortcut gestures",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Master pin dragging with road snapping, multi-stop route planner, compass True North reset, travel modes, and real-time background tracking notifications.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showFeatureGuide = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .semantics { contentDescription = "Open interactive feature guide and hints" },
                        shape = RoundedCornerShape(23.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Open Quick Guide & Hints",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            // Section 3: App Info & Links
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_icon),
                            contentDescription = "Marg App Icon",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Marg: Route & GPS Mocker",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "A calm, native map-first tool for high-precision mock GPS simulation and realistic multi-modal route movement on Android.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { launchUrl(context, "https://rahul-baberwal.github.io/marg/privacy.html") },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .semantics { contentDescription = "Open privacy policy" },
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Privacy Policy", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = { launchUrl(context, "https://github.com/rahul-baberwal/marg") },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .semantics { contentDescription = "Open source code on GitHub" },
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Repository", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Section 4: Developer Profile Card (High-Contrast Icons in Dark Mode)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Developer",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Verified Creator",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.developer_avatar),
                            contentDescription = "Rahul Baberwal",
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Rahul Baberwal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Software Developer",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "MSc CS (MGSU) · AI Major (IIT Ropar)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Specializing in React, Python, Django, FastAPI, Machine Learning, and Android native utilities.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    // Primary Website Button
                    Button(
                        onClick = { launchUrl(context, "https://rahulbaberwal.com") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .semantics { contentDescription = "Visit Rahul Baberwal portfolio website" },
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Visit rahulbaberwal.com", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                    }

                    Spacer(Modifier.height(12.dp))

                    // Social & Contact links with High-Contrast Dark-Mode Adaptive Colors
                    val githubColor = if (isAppDark) Color.White else Color(0xFF24292E)
                    val linkedinColor = if (isAppDark) Color(0xFF3897F0) else Color(0xFF0A66C2)
                    val instagramColor = if (isAppDark) Color(0xFFFF5277) else Color(0xFFE4405F)
                    val emailColor = MaterialTheme.colorScheme.primary

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SocialLinkChip(
                            drawableRes = R.drawable.ic_github,
                            label = "GitHub",
                            onClick = { launchUrl(context, "https://github.com/RahulBaberwal") },
                            iconTint = githubColor,
                            modifier = Modifier.weight(1f)
                        )
                        SocialLinkChip(
                            drawableRes = R.drawable.ic_linkedin,
                            label = "LinkedIn",
                            onClick = { launchUrl(context, "https://www.linkedin.com/in/rahul-baberwal/") },
                            iconTint = linkedinColor,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SocialLinkChip(
                            drawableRes = R.drawable.ic_instagram,
                            label = "Instagram",
                            onClick = { launchUrl(context, "https://www.instagram.com/rahulbaberwal.in/") },
                            iconTint = instagramColor,
                            modifier = Modifier.weight(1f)
                        )
                        SocialLinkChip(
                            icon = Icons.Default.Email,
                            label = "Email",
                            onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:im@rahulbaberwal.com")
                                }
                                try { context.startActivity(emailIntent) } catch (_: Exception) {}
                            },
                            iconTint = emailColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section 5: Open Source Acknowledgments
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Open Source Acknowledgments",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))

                    CreditRow(
                        title = "OpenStreetMap",
                        subtitle = "Global map data under Open Database License (ODbL)",
                        url = "https://www.openstreetmap.org",
                        context = context
                    )
                    CreditRow(
                        title = "MapLibre GL Native",
                        subtitle = "High-performance vector and raster map rendering (BSD 2-Clause)",
                        url = "https://maplibre.org",
                        context = context
                    )
                    CreditRow(
                        title = "Project OSRM",
                        subtitle = "High-performance routing engine for road networks (BSD 2-Clause)",
                        url = "http://project-osrm.org",
                        context = context
                    )
                    CreditRow(
                        title = "OpenFreeMap",
                        subtitle = "Vector map tiles without limits or keys",
                        url = "https://openfreemap.org",
                        context = context
                    )
                    CreditRow(
                        title = "OpenTopoMap",
                        subtitle = "Topographic rendering by Stefan Erhardt (CC-BY-SA)",
                        url = "https://opentopomap.org",
                        context = context
                    )
                    CreditRow(
                        title = "Photon by Komoot",
                        subtitle = "Fast OpenStreetMap geocoding engine",
                        url = "https://photon.komoot.io",
                        context = context
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        if (showFeatureGuide) {
            FeatureGuideSheet(
                onDismiss = { showFeatureGuide = false }
            )
        }
    }
}

@Composable
private fun ThemeOptionChip(
    mode: AppThemeMode,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(250),
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "chipFg"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else null,
        modifier = modifier
            .height(48.dp)
            .semantics {
                role = Role.RadioButton
                contentDescription = "$label theme" + if (isSelected) ", selected" else ""
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.Check else icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                color = contentColor
            )
        }
    }
}

@Composable
private fun MapLayerOptionChip(
    layer: MapLayer,
    icon: ImageVector,
    label: String,
    sub: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(250),
        label = "layerChipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(250),
        label = "layerChipFg"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else null,
        modifier = modifier
            .height(58.dp)
            .semantics {
                role = Role.RadioButton
                contentDescription = "$label map style" + if (isSelected) ", selected" else ""
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.Check else icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = contentColor
                )
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SocialLinkChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    drawableRes: Int? = null,
    iconTint: Color? = null
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .height(44.dp)
            .semantics { contentDescription = "Open $label" },
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        if (drawableRes != null) {
            Icon(
                painter = painterResource(id = drawableRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconTint ?: Color.Unspecified
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconTint ?: MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CreditRow(
    title: String,
    subtitle: String,
    url: String,
    context: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                onClick = { launchUrl(context, url) },
                onClickLabel = "Visit $title"
            )
            .semantics {
                role = Role.Button
                contentDescription = "$title: $subtitle"
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        thickness = 0.5.dp
    )
}
