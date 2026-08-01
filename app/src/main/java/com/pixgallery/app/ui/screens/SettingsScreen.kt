package com.pixgallery.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    darkModeOverride: Boolean?,
    onDarkModeOverrideChange: (Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewHiddenAlbums by remember { mutableStateOf(true) }
    var loopAllSlides by remember { mutableStateOf(true) }
    var secureSharing by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        SectionLabel("APPEARANCE")
        ThemeSelector(
            selected = darkModeOverride,
            onSelected = onDarkModeOverrideChange
        )

        SectionLabel("GALLERY")
        SimpleRow("Sort by", trailingText = "Date taken") {}
        SwitchRow("View hidden albums", "View and manage hidden albums", viewHiddenAlbums) { viewHiddenAlbums = it }
        SwitchRow("Loop all slides", null, loopAllSlides) { loopAllSlides = it }

        SectionLabel("SEND")
        SwitchRow("Secure sharing", null, secureSharing) { secureSharing = it }

        SectionLabel("OTHER SETTINGS")
        SimpleRow("Trash bin", trailingText = null, subtitle = "View items in Trash bin") {}
        SimpleRow("Privacy Policy") {}
    }
}

/** Light / Dark / System theme picker - actually changes the app's theme live. */
@Composable
private fun ThemeSelector(
    selected: Boolean?,
    onSelected: (Boolean?) -> Unit
) {
    val options = listOf(
        Triple("System default", null, "Follows your phone's theme"),
        Triple("Light", false, null),
        Triple("Dark", true, null)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .selectableGroup()
    ) {
        options.forEach { (label, value, subtitle) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .selectable(
                        selected = selected == value,
                        onClick = { onSelected(value) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == value, onClick = { onSelected(value) })
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                    subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun SimpleRow(
    title: String,
    trailingText: String? = null,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
        trailingText?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 4.dp))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
