package moe.shizuku.manager.filemanager.presentation

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TermuxDependencyManagerDialog(
    context: Context,
    viewModel: FileManagerViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showPkgListDialog by remember { mutableStateOf(false) }

    if (showPkgListDialog) {
        PkgListAllDialog(
            viewModel = viewModel,
            onDismiss = { showPkgListDialog = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Instalasi Paket (Manual)") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Instal paket yang dibutuhkan secara manual jika perintah lanjutan (Advanced) gagal dijalankan.", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Button(
                            onClick = {
                                viewModel.executeTermuxScript(context, "pkg update -y && pkg upgrade -y")
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Update & Upgrade Semua Paket")
                        }
                    }

                    items(state.selectedCustomPackages.toList()) { pkg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    viewModel.executeTermuxScript(context, "pkg install -y $pkg")
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Instal $pkg")
                            }
                            IconButton(onClick = { viewModel.removeCustomPackage(pkg) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus $pkg", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showPkgListDialog = true }) {
                Text("Tambah Paket")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun PkgListAllDialog(
    viewModel: FileManagerViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadMasterPkgList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Master List Paket") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cari paket...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (state.isPkgListLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val filteredPackages = remember(searchQuery, state.masterPkgList) {
                        if (searchQuery.isBlank()) {
                            state.masterPkgList
                        } else {
                            state.masterPkgList.filter { it.contains(searchQuery, ignoreCase = true) }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(filteredPackages) { pkg ->
                            val isSelected = state.selectedCustomPackages.contains(pkg)
                            ListItem(
                                headlineContent = { Text(pkg) },
                                trailingContent = {
                                    if (isSelected) {
                                        Text("Ditambahkan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    } else {
                                        IconButton(onClick = { viewModel.addCustomPackage(pkg) }) {
                                            Icon(Icons.Default.Add, contentDescription = "Tambah")
                                        }
                                    }
                                }
                            )
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
