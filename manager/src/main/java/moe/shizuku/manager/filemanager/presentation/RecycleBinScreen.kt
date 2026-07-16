package moe.shizuku.manager.filemanager.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.filemanager.domain.TrashItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    viewModel: FileManagerViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showEmptyConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadTrashItems()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (selectedIds.isNotEmpty()) {
                        Text("${selectedIds.size} Terpilih")
                    } else {
                        Text("Tempat Sampah") 
                    }
                },
                navigationIcon = {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Batal")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.restoreTrashItems(selectedIds.toList())
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Pulihkan")
                        }
                        IconButton(onClick = {
                            viewModel.deleteTrashItemsPermanently(selectedIds.toList())
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus Permanen")
                        }
                    } else if (state.trashItems.isNotEmpty()) {
                        IconButton(onClick = { showEmptyConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Kosongkan Sampah")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isTrashLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.trashItems.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Tempat Sampah Kosong",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.trashItems) { item ->
                        TrashItemRow(
                            item = item,
                            isSelected = selectedIds.contains(item.id),
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    val newIds = selectedIds.toMutableSet()
                                    if (newIds.contains(item.id)) {
                                        newIds.remove(item.id)
                                    } else {
                                        newIds.add(item.id)
                                    }
                                    selectedIds = newIds
                                }
                            },
                            onLongClick = {
                                val newIds = selectedIds.toMutableSet()
                                if (newIds.contains(item.id)) {
                                    newIds.remove(item.id)
                                } else {
                                    newIds.add(item.id)
                                }
                                selectedIds = newIds
                            }
                        )
                    }
                }
            }
        }
        
        if (showEmptyConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyConfirmDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = { Text("Kosongkan Tempat Sampah?") },
                text = { Text("Semua file di dalam tempat sampah akan dihapus secara permanen dan tidak dapat dipulihkan.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.emptyTrash()
                        showEmptyConfirmDialog = false
                    }) {
                        Text("Kosongkan", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyConfirmDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
        
        if (state.trashConflicts.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelRestore() },
                title = { Text("Bentrok Nama File") },
                text = { Text("Terdapat ${state.trashConflicts.size} file yang sudah ada di lokasi tujuan. Pilih aksi untuk semua file ini:") },
                confirmButton = {
                    TextButton(onClick = {
                        val resolution = state.trashConflicts.associate { it.id to "RENAME_AUTO" }
                        viewModel.resolveRestoreConflicts(resolution)
                    }) {
                        Text("Ganti Nama Otomatis")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            val resolution = state.trashConflicts.associate { it.id to "OVERWRITE" }
                            viewModel.resolveRestoreConflicts(resolution)
                        }) {
                            Text("Timpa", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = {
                            val resolution = state.trashConflicts.associate { it.id to "SKIP" }
                            viewModel.resolveRestoreConflicts(resolution)
                        }) {
                            Text("Lewati")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun TrashItemRow(
    item: TrashItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val df = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }
    val dateStr = remember(item.deletedAt) { df.format(Date(item.deletedAt)) }
    
    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = { 
            Column {
                Text(item.originalPath, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                Text("Dihapus: $dateStr", style = MaterialTheme.typography.bodySmall)
            }
        },
        leadingContent = {
            Icon(
                painter = painterResource(id = if (item.isDirectory) moe.shizuku.manager.R.drawable.ic_code_24dp else moe.shizuku.manager.R.drawable.ic_terminal_24),
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    )
}
