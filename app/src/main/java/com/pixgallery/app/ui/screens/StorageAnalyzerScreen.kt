package com.pixgallery.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixgallery.app.model.MediaItem
import com.pixgallery.app.util.StorageAnalyzer
import com.pixgallery.app.util.StorageCategory
import com.pixgallery.app.util.StorageReport

/**
 * Answers "where did my storage go?" - a different question from Duplicate
 * Finder (which finds copies). Breaks total usage down by folder category
 * (Camera, WhatsApp, Screenshots, Downloads, Other) with a simple bar chart,
 * and surfaces the single largest files (typically long screen recordings or
 * videos) so the user can quickly clear space.
 */
@Composable
fun StorageAnalyzerScreen(
    allMedia: List<MediaItem>,
    onBack: () -> Unit,
    onMoveToTrash: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    val report = remember(allMedia) { StorageAnalyzer.analyze(allMedia) }
    var selected by remember(report) { mutableStateOf<Set<Long>>(emptySet()) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Icon(Icons.Filled.PieChart, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Storage Analyzer", style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            item { TotalUsageCard(report) }

            item {
                Text(
                    "By category",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            items(report.categories) { category ->
                CategoryRow(category, report.totalBytes)
            }

            if (report.largestFiles.isNotEmpty()) {
                item {
                    Text(
                        "Largest files (tap to mark for cleanup)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, top = 20.dp, bottom = 8.dp)
                    )
                }
                items(report.largestFiles, key = { it.id }) { item ->
                    LargeFileRow(
                        item = item,
                        marked = item.id in selected,
                        onToggle = {
                            selected = if (item.id in selected) selected - item.id else selected + item.id
                        }
                    )
                }
            } else {
                item {
                    Text(
                        "No unusually large files found - nice and tidy!",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        if (selected.isNotEmpty()) {
            Button(
                onClick = {
                    onMoveToTrash(selected)
                    selected = emptySet()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Text("  Move ${selected.size} file(s) to bin")
            }
        }
    }
}

@Composable
private fun TotalUsageCard(report: StorageReport) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Total used by Photos & Videos", style = MaterialTheme.typography.labelMedium)
            Text(
                StorageAnalyzer.formatBytes(report.totalBytes),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${StorageAnalyzer.formatBytes(report.photoBytes)} in photos · " +
                    "${StorageAnalyzer.formatBytes(report.videoBytes)} in videos",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun CategoryRow(category: StorageCategory, totalBytes: Long) {
    val fraction = if (totalBytes > 0) (category.totalBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${category.label} · ${category.itemCount} items", style = MaterialTheme.typography.bodyMedium)
            Text(StorageAnalyzer.formatBytes(category.totalBytes), style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun LargeFileRow(item: MediaItem, marked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = item.uri,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                "${StorageAnalyzer.formatBytes(item.sizeBytes)} · ${item.bucketName}",
                style = MaterialTheme.typography.labelSmall
            )
        }
        Surface(
            shape = CircleShape,
            color = Color.Transparent
        ) {
            Icon(
                imageVector = if (marked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (marked) "Marked for removal" else "Not marked",
                tint = if (marked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
