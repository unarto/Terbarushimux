package moe.shizuku.manager.filemanager.texteditor.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindFileDialog(
    currentDir: String,
    onDismiss: () -> Unit,
    onFileSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var allFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isSearching by remember { mutableStateOf(true) }

    LaunchedEffect(currentDir) {
        isSearching = true
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val root = File(currentDir)
            if (root.exists() && root.isDirectory) {
                // Limit depth to avoid out of memory on large folders
                allFiles = root.walk().maxDepth(4).filter { it.isFile && !it.isHidden }.toList()
            }
            isSearching = false
        }
    }

    val filteredFiles = remember(searchQuery, allFiles) {
        if (searchQuery.isBlank()) allFiles.take(100)
        else allFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }.take(100)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Temukan Berkas") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp, max = 400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cari nama berkas") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus pencarian")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )

                if (isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (filteredFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada berkas yang ditemukan.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredFiles) { file ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFileSelected(file.absolutePath) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Text(text = file.name, style = MaterialTheme.typography.bodyLarge)
                                Text(text = file.parentFile?.absolutePath ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
