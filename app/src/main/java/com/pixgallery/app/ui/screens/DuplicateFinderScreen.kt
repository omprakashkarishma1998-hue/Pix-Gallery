package com.pixgallery.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixgallery.app.model.MediaItem
import com.pixgallery.app.util.DuplicateFinder
import com.pixgallery.app.util.DuplicateGroup
import kotlinx.coroutines.launch

private sealed class ScanState {
    data object Idle : ScanState()
    data class Scanning(val checked: Int, val total: Int) : ScanState()
    data class Done(val groups: List<DuplicateGroup>) : ScanState()
}

/**
 * Scans the on-device library for exact duplicate photos/videos (same file
 * content, e.g. a photo saved twice or re-downloaded) and lets the user bulk
 * move the extras to the trash bin, always keeping one copy of each group.
 */
@Composable
fun DuplicateFinderScreen(
    allMedia: List<MediaItem>,
    onBack: () -> Unit,
    onMoveToTrash: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ScanState>(ScanState.Idle) }
    // ids the user has checked for removal, defaults to "everything but the newest in each group"
    var selectedForRemoval by remember { mutableStateOf<Set<Long>>(emptySet()) }

    fun startScan() {
        state = ScanState.Scanning(0, 0)
        scope.launch {
            val groups = DuplicateFinder.findDuplicates(context, allMedia) { checked, total ->
                state = ScanState.Scanning(checked, total)
            }
            state = ScanState.Done(groups)
            // Default selection: keep the newest item in each group, pre-select the rest for removal.
            selectedForRemoval = groups.flatMap { it.items.drop(1) }.map { it.id }.toSet()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Duplicate Finder", style = MaterialTheme.typography.titleLarge)
        }

        when (val s = state) {
            is ScanState.Idle -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Find photos and videos that are exact duplicates and free up space.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { startScan() }) {
                        Text("Scan ${allMedia.size} items")
                    }
                }
            }

            is ScanState.Scanning -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    val label = if (s.total > 0) "Checked ${s.checked} / ${s.total}" else "Scanning your library..."
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            is ScanState.Done -> {
                if (s.groups.isEmpty()) {
                    EmptyState("No duplicates found - your library is clean!")
                } else {
                    Text(
                        "${s.groups.size} duplicate group(s) found. The newest copy in each group is kept by default.",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(s.groups.size) { index ->
                            val group = s.groups[index]
                            DuplicateGroupRow(
                                group = group,
                                selectedForRemoval = selectedForRemoval,
                                onToggle = { id ->
                                    selectedForRemoval = if (id in selectedForRemoval) {
                                        selectedForRemoval - id
                                    } else {
                                        selectedForRemoval + id
                                    }
                                }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            onMoveToTrash(selectedForRemoval)
                            state = ScanState.Done(s.groups.map { g ->
                                g.copy(items = g.items.filterNot { it.id in selectedForRemoval })
                            }.filter { it.items.size > 1 })
                            selectedForRemoval = emptySet()
                        },
                        enabled = selectedForRemoval.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Text("  Move ${selectedForRemoval.size} duplicate(s) to bin")
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupRow(
    group: DuplicateGroup,
    selectedForRemoval: Set<Long>,
    onToggle: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            "${group.items.size} copies",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().height((90 * ((group.items.size + 3) / 4)).dp),
            userScrollEnabled = false
        ) {
            items(group.items, key = { it.id }) { item ->
                val marked = item.id in selectedForRemoval
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggle(item.id) }
                ) {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (marked) Color.Black.copy(alpha = 0.45f) else Color.Transparent)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp)
                    ) {
                        Icon(
                            imageVector = if (marked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = if (marked) "Marked for removal" else "Kept",
                            tint = if (marked) MaterialTheme.colorScheme.error else Color.White,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}
