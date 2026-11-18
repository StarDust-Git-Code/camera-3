package dev.camera3.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.camera3.model.LogEvent
import dev.camera3.model.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(events: List<LogEvent>, back: () -> Unit, export: () -> Unit, exporting: Boolean, clear: () -> Unit) {
    var level by rememberSaveable { mutableStateOf<LogLevel?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var live by rememberSaveable { mutableStateOf(true) }
    var confirmClear by remember { mutableStateOf(false) }
    val filtered = remember(events, level, query) {
        events.filter { (level == null || it.level == level) && "${it.tag} ${it.message}".contains(query, ignoreCase = true) }
    }
    val list = rememberLazyListState()
    LaunchedEffect(filtered.lastOrNull()?.sequence, live) {
        if (live && filtered.isNotEmpty()) list.scrollToItem(filtered.lastIndex)
    }
    LaunchedEffect(list) {
        list.interactionSource.interactions.collect { if (it is DragInteraction.Start) live = false }
    }
    val format = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    PageHeader("Logs", back) {
        IconButton(onClick = export, enabled = !exporting) { Icon(Icons.Default.Share, "Export logs and diagnostics") }
        IconButton(onClick = { confirmClear = true }) { Icon(Icons.Default.DeleteOutline, "Clear application logs") }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search messages or tags") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected = level == null, onClick = { level = null }, label = { Text("All") }) }
            items(LogLevel.entries) { severity ->
                FilterChip(selected = level == severity, onClick = { level = severity }, label = { Text(severity.name.lowercase().replaceFirstChar { it.uppercase() }) })
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${filtered.size} recent events", color = Muted, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
            Text("Live", style = MaterialTheme.typography.labelSmall)
            Switch(checked = live, onCheckedChange = { live = it })
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f), state = list) {
            if (filtered.isEmpty()) item { Text("No matching log events", color = Muted, modifier = Modifier.padding(vertical = 24.dp)) }
            items(filtered, key = { it.sequence }) { event ->
                val color = when (event.level) {
                    LogLevel.DEBUG -> Color(0xFF6CC4E4); LogLevel.INFO -> Accent
                    LogLevel.WARNING -> Color(0xFFE0C547); LogLevel.ERROR -> Color(0xFFFF6B61)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Box(Modifier.padding(top = 4.dp).size(6.dp).background(color, androidx.compose.foundation.shape.CircleShape))
                    Spacer(Modifier.width(10.dp))
                    SelectionContainer {
                        Column {
                            Text("${format.format(Date(event.timeMillis))}  ${event.level}  ${event.tag}",
                                fontFamily = FontFamily.Monospace, color = Muted, style = MaterialTheme.typography.labelSmall)
                            Text(event.message, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .35f))
            }
        }
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("Clear app logs?") },
        text = { Text("This deletes Camera3's saved logs, not your photos or system logs.") },
        confirmButton = { TextButton(onClick = { confirmClear = false; clear() }) { Text("Clear") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } })
}
