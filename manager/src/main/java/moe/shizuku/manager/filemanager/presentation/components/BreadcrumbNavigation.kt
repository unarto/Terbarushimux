package moe.shizuku.manager.filemanager.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun BreadcrumbNavigation(
    currentPath: String,
    onNavigate: (String) -> Unit
) {
    if (currentPath == "home") {
        Text("File Manager", style = MaterialTheme.typography.titleLarge)
        return
    }

    if (currentPath.startsWith("content://")) {
        Text(
            text = android.net.Uri.decode(currentPath).substringAfterLast("/").substringBeforeLast("%3A"),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        return
    }

    val parts = currentPath.split("/").filter { it.isNotEmpty() }
    
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            Text(
                text = "Root",
                modifier = Modifier
                    .clickable { onNavigate("/") }
                    .padding(4.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
            Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        items(parts.size) { index ->
            val part = parts[index]
            val path = "/" + parts.take(index + 1).joinToString("/")
            
            Text(
                text = part,
                modifier = Modifier
                    .clickable { onNavigate(path) }
                    .padding(4.dp),
                color = if (index == parts.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
            
            if (index < parts.size - 1) {
                Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
