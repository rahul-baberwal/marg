package com.rahulbaberwal.marg.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rahulbaberwal.marg.MargUiState
import com.rahulbaberwal.marg.Mode
import com.rahulbaberwal.marg.SimState
import kotlin.math.roundToInt

data class ModeTab(
    val mode: Mode,
    val label: String,
    val icon: ImageVector
)

private val MODES = listOf(
    ModeTab(Mode.DRIVE, "Drive", Icons.Default.DirectionsCar),
    ModeTab(Mode.WALK, "Walk", Icons.AutoMirrored.Filled.DirectionsWalk),
    ModeTab(Mode.BIKE, "Cycle", Icons.AutoMirrored.Filled.DirectionsBike),
    ModeTab(Mode.TRAIN, "Train", Icons.Default.Train),
    ModeTab(Mode.FIXED, "Fixed", Icons.Default.Home),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomControlSheet(
    uiState: MargUiState,
    onModeSelected: (Mode) -> Unit,
    onSpeedChanged: (Float) -> Unit,
    onIndoorRadiusChanged: (Float) -> Unit,
    onJitterToggled: (Boolean) -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onOpenMockLocationSettings: () -> Unit,
    onDismissMockLocationCard: () -> Unit,
    onClearAllStops: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isRunning = uiState.simState == SimState.RUNNING
    val isPaused = uiState.simState == SimState.PAUSED
    val isSimActive = isRunning || isPaused

    val canStart = if (uiState.mode == Mode.FIXED) {
        uiState.startPoint != null || isSimActive
    } else {
        uiState.route != null || isSimActive
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mock location warning card (Inline card, no popups)
        AnimatedVisibility(
            visible = uiState.isMockLocationNotSet,
            enter = fadeIn(spring(dampingRatio = 0.8f)) + scaleIn(spring(dampingRatio = 0.8f)),
            exit = fadeOut(spring(dampingRatio = 0.8f)) + scaleOut(spring(dampingRatio = 0.8f))
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Mock location setup needed" }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Set Marg as Mock Location App",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Developer options > Select mock location app > Choose Marg. Tap below to jump straight to the option.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onOpenMockLocationSettings,
                                modifier = Modifier
                                    .height(36.dp)
                                    .semantics { contentDescription = "Open Developer Settings" }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open Settings", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(
                                onClick = onDismissMockLocationCard,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Mode Segmented Control
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Transportation mode selector" }
        ) {
            MODES.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = uiState.mode == item.mode,
                    onClick = {
                        if (!isSimActive) onModeSelected(item.mode)
                    },
                    enabled = !isSimActive,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = MODES.size),
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "${item.label} mode${if (uiState.mode == item.mode) ", selected" else ""}"
                    }
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (uiState.mode == item.mode) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Speed / Radius Slider with Live ETA
        if (uiState.mode == Mode.FIXED) {
            // Indoor Wander Radius Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Indoor Wander Radius",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (uiState.indoorRadiusM.roundToInt() == 0) {
                            "Stationary spot (0m)"
                        } else {
                            "~${uiState.indoorRadiusM.roundToInt()}m radius"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = uiState.indoorRadiusM.coerceIn(0f, 30f),
                    onValueChange = onIndoorRadiusChanged,
                    valueRange = 0f..30f,
                    steps = 29,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Indoor wander radius: ${uiState.indoorRadiusM.roundToInt()} meters" }
                )
            }
        } else {
            // Speed Slider + ETA
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Simulation Speed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val route = uiState.route
                    val etaString = if (route != null && route.distanceM > 0) {
                        val mps = uiState.speedKmh / 3.6
                        val totalSeconds = (route.distanceM / mps).roundToInt()
                        val mins = totalSeconds / 60
                        val distKm = route.distanceM / 1000.0
                        "%.1f km · ~%d min".format(distKm, mins.coerceAtLeast(1))
                    } else {
                        ""
                    }

                    Text(
                        text = "${uiState.speedKmh.roundToInt()} km/h${if (etaString.isNotEmpty()) " ($etaString)" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = uiState.speedKmh.coerceIn(1f, 200f),
                    onValueChange = onSpeedChanged,
                    valueRange = 1f..200f,
                    steps = 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Speed slider: ${uiState.speedKmh.roundToInt()} kilometers per hour" }
                )
            }
        }

        // GPS Jitter Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (uiState.mode == Mode.FIXED) "Realistic GPS Jitter (indoor)" else "Realistic GPS Jitter",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (uiState.mode == Mode.FIXED) "Simulates natural walking drift inside home" else "Adds micro-drift and satellite antenna noise",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Switch(
                checked = uiState.jitter,
                onCheckedChange = onJitterToggled,
                modifier = Modifier.semantics {
                    contentDescription = "GPS jitter switch, currently ${if (uiState.jitter) "enabled" else "disabled"}"
                }
            )
        }

        // Progress bar for active route
        if (uiState.mode != Mode.FIXED && (isSimActive || uiState.progress > 0f)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isPaused) "Paused" else if (isRunning) "Simulating route" else "Completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${(uiState.progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { uiState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Waypoints / Stops summary if any exist
        if (uiState.mode != Mode.FIXED && uiState.waypoints.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${uiState.waypoints.size} stop${if (uiState.waypoints.size > 1) "s" else ""} added",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (!isSimActive) {
                    TextButton(
                        onClick = onClearAllStops,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Clear stops",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Simulation Control Buttons (Morphing Start / Pause / Resume / Stop / Reset)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Morphing Play / Pause Button
            val animatedButtonColor by animateColorAsState(
                targetValue = when (uiState.simState) {
                    SimState.RUNNING -> MaterialTheme.colorScheme.secondary
                    SimState.PAUSED -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primary
                },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "sim_button_color"
            )

            AnimatedContent(
                targetState = uiState.simState,
                transitionSpec = {
                    (fadeIn(spring(dampingRatio = 0.8f, stiffness = 400f)) +
                        scaleIn(spring(dampingRatio = 0.8f, stiffness = 400f))) togetherWith
                        (fadeOut(spring(dampingRatio = 0.8f, stiffness = 400f)) +
                            scaleOut(spring(dampingRatio = 0.8f, stiffness = 400f)))
                },
                label = "play_pause_button",
                modifier = Modifier.weight(1f)
            ) { state ->
                when (state) {
                    SimState.RUNNING -> {
                        Button(
                            onClick = onPlayPause,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .semantics { contentDescription = "Pause simulation" },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = animatedButtonColor)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pause", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    SimState.PAUSED -> {
                        Button(
                            onClick = onPlayPause,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .semantics { contentDescription = "Resume simulation" },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = animatedButtonColor)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Resume", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Button(
                            onClick = onPlayPause,
                            enabled = canStart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .semantics { contentDescription = "Start simulation" },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = animatedButtonColor)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Start", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Stop / Reset Button
            OutlinedButton(
                onClick = onStop,
                enabled = isSimActive,
                modifier = Modifier
                    .height(56.dp)
                    .semantics {
                        contentDescription = if (isPaused) "Reset simulation to start" else "Stop simulation"
                    },
                shape = RoundedCornerShape(28.dp)
            ) {
                AnimatedContent(
                    targetState = isPaused,
                    label = "stop_reset_icon"
                ) { paused ->
                    if (paused) {
                        Text("Reset", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Stop", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(2.dp))
    }
}
