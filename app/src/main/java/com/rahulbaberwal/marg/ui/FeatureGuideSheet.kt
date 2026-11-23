package com.rahulbaberwal.marg.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rahulbaberwal.marg.ui.theme.MargSaffron
import com.rahulbaberwal.marg.ui.theme.MargTeal

data class FeatureGuideItem(
    val icon: ImageVector,
    val lightTint: Color,
    val darkTint: Color,
    val title: String,
    val description: String,
    val tip: String
)

private val guideItems = listOf(
    FeatureGuideItem(
        icon = Icons.Default.Search,
        lightTint = MargTeal,
        darkTint = Color(0xFF80DEEA),
        title = "Search & Coordinates",
        description = "Search places with live autocomplete or paste 'latitude, longitude' (e.g. 28.6139, 77.2090) to drop pins instantly.",
        tip = "Tip: Swap Start & Destination anytime using the swap button."
    ),
    FeatureGuideItem(
        icon = Icons.Default.LocationOn,
        lightTint = MargSaffron,
        darkTint = Color(0xFFFFB74D),
        title = "Hold & Drop Pin Snapping",
        description = "Long-press the map to drop a pin. Press and hold any pin to lift and drag it; release to snap automatically to the nearest road.",
        tip = "Tip: Tap a pin to inspect, remove, or swap it."
    ),
    FeatureGuideItem(
        icon = Icons.AutoMirrored.Filled.AltRoute,
        lightTint = Color(0xFF00897B),
        darkTint = Color(0xFF4DB6AC),
        title = "Collapsible Multi-Stop Planner",
        description = "Add intermediate stops between Start & End using '+ Add stop'. Tap 'Done ✓' to collapse into a slim 52dp bar so the full map stays visible.",
        tip = "Tip: Reorder or delete any stop with a single tap."
    ),
    FeatureGuideItem(
        icon = Icons.Default.Explore,
        lightTint = Color(0xFFE53935),
        darkTint = Color(0xFFEF5350),
        title = "Live Rotating Compass",
        description = "The floating compass needle turns in real time as the map rotates. Tap the compass anytime to smoothly re-align to True North (0°).",
        tip = "Tip: Pinch and rotate with two fingers to change bearing."
    ),
    FeatureGuideItem(
        icon = Icons.Default.DirectionsCar,
        lightTint = Color(0xFF1E88E5),
        darkTint = Color(0xFF64B5F6),
        title = "5 Travel Modes & Jitter",
        description = "Switch seamlessly between Drive, Walk, Cycle, Train, and Fixed location. Tune the speed slider or adjust the indoor wander radius for realistic GPS jitter.",
        tip = "Tip: Fixed mode simulates realistic indoor wandering."
    ),
    FeatureGuideItem(
        icon = Icons.Default.NotificationsActive,
        lightTint = Color(0xFFD81B60),
        darkTint = Color(0xFFF06292),
        title = "Real-Time Route Notification",
        description = "Follow live progress, remaining distance (km), and ETA directly in the notification shade. Pause, Resume, or Stop mock location without opening the app.",
        tip = "Tip: Background simulation keeps moving reliably with real-time updates."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureGuideSheet(
    onDismiss: (dontShowAgain: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var dontShowAgain by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = { onDismiss(dontShowAgain) },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Quick Guide & Hints",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Everything you need to know to use Marg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Scrollable List of Feature Cards
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(guideItems) { index, item ->
                    FeatureCard(item = item, index = index + 1)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Don't show again" Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { dontShowAgain = !dontShowAgain }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = dontShowAgain,
                    onCheckedChange = { dontShowAgain = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "Do not show this guide automatically on startup"
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Don't show automatically on start",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Button
            Button(
                onClick = { onDismiss(dontShowAgain) },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = "Close guide and continue to app" }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Got It, Let's Go!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(item: FeatureGuideItem, index: Int) {
    val isDark = isSystemInDarkTheme()
    val activeTint = if (isDark) item.darkTint else item.lightTint

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.55f else 0.45f),
        border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = activeTint.copy(alpha = if (isDark) 0.22f else 0.14f),
                    border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, activeTint.copy(alpha = 0.4f)) else null,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = activeTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "$index. ${item.title}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.tip,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
