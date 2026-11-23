package com.rahulbaberwal.marg.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rahulbaberwal.marg.MargUiState
import com.rahulbaberwal.marg.Mode

@Composable
fun SearchCard(
    uiState: MargUiState,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
    onSwapClick: () -> Unit,
    onClearStart: () -> Unit,
    onClearEnd: () -> Unit,
    onRemoveWaypoint: (Int) -> Unit = {},
    onMoveWaypoint: (Int, Int) -> Unit = { _, _ -> },
    onAddStopClick: (Int) -> Unit = {},
    onEditStopClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // When stops exist, allows collapsing to minimum space; expands on click or when adding stops
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var reorderMenuIdx by rememberSaveable { mutableStateOf<Int?>(null) }

    val hasRouteOrStops = uiState.waypoints.isNotEmpty() || (uiState.startPoint != null && uiState.endPoint != null)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        if (!isExpanded && hasRouteOrStops && uiState.mode != Mode.FIXED) {
            // ==========================================
            // 1. COLLAPSED VIEW (Minimum Screen Space)
            // Single slim row ~52dp: Start -> Destination (N stops)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable { isExpanded = true }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Green bullseye for Start
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(2.dp, Color(0xFF16A34A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(0xFF16A34A), CircleShape)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Text(
                    text = uiState.startLabel.ifEmpty { "Start" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = " → ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                // Red pin for Destination
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(16.dp)
                )

                Spacer(Modifier.width(4.dp))

                Text(
                    text = uiState.endLabel.ifEmpty { "Destination" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Stops badge if any intermediate stops exist
                if (uiState.waypoints.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF16A34A).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${uiState.waypoints.size} ${if (uiState.waypoints.size == 1) "stop" else "stops"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Swap Button
                IconButton(
                    onClick = onSwapClick,
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Swap start and destination" }
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Expand Chevron Button
                IconButton(
                    onClick = { isExpanded = true },
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Expand route planner" }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            // ==========================================
            // 2. EXPANDED VIEW / ROUTE PLANNER
            // Exact layout with inline add stops, reordering & collapse
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Header (shown when in non-fixed mode and either expanded or has stops)
                if (uiState.mode != Mode.FIXED && (isExpanded || uiState.waypoints.isNotEmpty())) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Route planner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.waypoints.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF16A34A).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${uiState.waypoints.size} ${if (uiState.waypoints.size == 1) "stop" else "stops"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF16A34A),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Collapse Chevron
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier
                                .size(32.dp)
                                .semantics { contentDescription = "Collapse route planner" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // 1. START Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onStartClick)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bullseye Icon: Outer ring with centered dot
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(2.dp, Color(0xFF16A34A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF16A34A), CircleShape)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (uiState.mode == Mode.FIXED) "FIXED LOCATION" else "START",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.startLabel.ifEmpty { "Search, lat/lon or map pin" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (uiState.startLabel.isEmpty()) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (uiState.startPoint != null) {
                        IconButton(
                            onClick = onClearStart,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear start",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Drag handle visual indicator
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Reorder handle",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(end = 4.dp)
                    )
                }

                // Non-Fixed Mode: Intermediate Stops & Destination
                if (uiState.mode != Mode.FIXED) {
                    // Inline "+ Add stop" (between Start and Stop 1 or Destination)
                    InlineAddStopButton(
                        onClick = {
                            isExpanded = true
                            onAddStopClick(0)
                        }
                    )

                    // Waypoints list
                    uiState.waypoints.forEachIndexed { idx, wp ->
                        val stopLabel = uiState.waypointLabels.getOrNull(idx)?.takeIf { it.isNotBlank() }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onEditStopClick(idx) }
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Teal Number Badge (1, 2, 3...)
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F766E).copy(alpha = 0.2f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F766E)
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stopLabel ?: "%.4f, %.4f".format(wp.lat, wp.lon),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (stopLabel != null) {
                                    Text(
                                        text = "STOP ${idx + 1} · %.4f, %.4f".format(wp.lat, wp.lon),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Reorder Drag Handle with Dropdown Menu for Move Up / Down
                            Box {
                                IconButton(
                                    onClick = { reorderMenuIdx = idx },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Reorder stop ${idx + 1}",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = (reorderMenuIdx == idx),
                                    onDismissRequest = { reorderMenuIdx = null }
                                ) {
                                    if (idx > 0) {
                                        DropdownMenuItem(
                                            text = { Text("Move up") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.ArrowUpward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            onClick = {
                                                onMoveWaypoint(idx, idx - 1)
                                                reorderMenuIdx = null
                                            }
                                        )
                                    }
                                    if (idx < uiState.waypoints.size - 1) {
                                        DropdownMenuItem(
                                            text = { Text("Move down") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.ArrowDownward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            onClick = {
                                                onMoveWaypoint(idx, idx + 1)
                                                reorderMenuIdx = null
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Remove stop", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        onClick = {
                                            onRemoveWaypoint(idx)
                                            reorderMenuIdx = null
                                        }
                                    )
                                }
                            }

                            // Delete button (x)
                            IconButton(
                                onClick = { onRemoveWaypoint(idx) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove stop ${idx + 1}",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Inline "+ Add stop" after this stop
                        InlineAddStopButton(
                            onClick = {
                                isExpanded = true
                                onAddStopClick(idx + 1)
                            }
                        )
                    }

                    // 2. DESTINATION Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onEndClick)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDC2626).copy(alpha = 0.15f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "DESTINATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.endLabel.ifEmpty { "Search, lat/lon or map pin" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.endLabel.isEmpty()) {
                                    MaterialTheme.colorScheme.outline
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (uiState.endPoint != null) {
                            IconButton(
                                onClick = onClearEnd,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear destination",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Footer with Route Summary & Done Button
                    if (isExpanded || uiState.waypoints.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val distText = uiState.route?.let { "%.1f km • ".format(it.distanceM / 1000.0) } ?: ""
                            val stopsText = "${uiState.waypoints.size} ${if (uiState.waypoints.size == 1) "stop" else "stops"}"
                            Text(
                                text = "$distText$stopsText",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )

                            TextButton(
                                onClick = { isExpanded = false },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = "Done ✓",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineAddStopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 17.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical connector line
        Box(
            modifier = Modifier
                .width(1.5.dp)
                .height(14.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )

        Spacer(Modifier.width(15.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Add stop",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF16A34A),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
