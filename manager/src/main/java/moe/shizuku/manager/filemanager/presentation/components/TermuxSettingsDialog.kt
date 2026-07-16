package moe.shizuku.manager.filemanager.presentation.components

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.filemanager.presentation.FileManagerViewModel
import java.net.URLDecoder

@Composable
fun TermuxSettingsDialog(
    context: Context,
    viewModel: FileManagerViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val uriString = uri.toString()
            var path = "/sdcard/"
            if (uriString.contains("primary%3A")) {
                val subPath = uriString.substringAfter("primary%3A")
                path = "/storage/emulated/0/" + URLDecoder.decode(subPath, "UTF-8")
            } else if (uriString.contains("primary:")) {
                val subPath = uriString.substringAfter("primary:")
                path = "/storage/emulated/0/" + URLDecoder.decode(subPath, "UTF-8")
            }
            viewModel.setBackupDirectoryPath(path)
        }
    }

    var showEditPrefix by remember { mutableStateOf(false) }
    var tempPrefix by remember { mutableStateOf(state.termuxPrefixPath) }
    
    var showEditHome by remember { mutableStateOf(false) }
    var tempHome by remember { mutableStateOf(state.termuxHomePath) }

    if (showEditPrefix) {
        AlertDialog(
            onDismissRequest = { showEditPrefix = false },
            title = { Text("Edit Prefix Path") },
            text = {
                OutlinedTextField(
                    value = tempPrefix,
                    onValueChange = { tempPrefix = it },
                    label = { Text("PREFIX") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.setTermuxPrefixPath(tempPrefix)
                    showEditPrefix = false
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showEditPrefix = false }) { Text("Batal") }
            }
        )
    }

    if (showEditHome) {
        AlertDialog(
            onDismissRequest = { showEditHome = false },
            title = { Text("Edit Home Path") },
            text = {
                OutlinedTextField(
                    value = tempHome,
                    onValueChange = { tempHome = it },
                    label = { Text("HOME") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.setTermuxHomePath(tempHome)
                    showEditHome = false
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showEditHome = false }) { Text("Batal") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan Termux") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Eksekusi Latar Belakang") },
                    supportingContent = { Text("Jalankan script tanpa memblokir UI") },
                    trailingContent = {
                        Switch(
                            checked = state.executeShInBackground,
                            onCheckedChange = { viewModel.toggleExecuteShInBackground() }
                        )
                    }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("Gunakan Akses Root") },
                    supportingContent = { Text("Eksekusi script dengan su -c") },
                    trailingContent = {
                        Switch(
                            checked = state.useRootForTermux,
                            onCheckedChange = { viewModel.toggleUseRootForTermux() }
                        )
                    }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("Gunakan Akses Shizuku") },
                    supportingContent = { Text("Eksekusi script melalui layer Shizuku (Mode Non-Root)") },
                    trailingContent = {
                        Switch(
                            checked = state.useShizukuForTermux,
                            onCheckedChange = { viewModel.toggleUseShizukuForTermux() }
                        )
                    }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("Prefix Path") },
                    supportingContent = { Text(state.termuxPrefixPath) },
                    modifier = Modifier.clickable { 
                        tempPrefix = state.termuxPrefixPath
                        showEditPrefix = true 
                    }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("Home Path") },
                    supportingContent = { Text(state.termuxHomePath) },
                    modifier = Modifier.clickable { 
                        tempHome = state.termuxHomePath
                        showEditHome = true 
                    }
                )
                Divider()
                ListItem(
                    headlineContent = { Text("Folder Penyimpanan Backup") },
                    supportingContent = { Text(state.backupDirectoryPath) },
                    modifier = Modifier.clickable { 
                        folderPickerLauncher.launch(null)
                    }
                )
                Divider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            viewModel.backupTermux(context)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Backup Termux")
                    }
                    Button(
                        onClick = { 
                            viewModel.restoreTermux(context)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Restore Termux")
                    }
                }
                Divider()
                Button(
                    onClick = { viewModel.clearPackageData() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset / Bersihkan Data Paket")
                }
                Button(
                    onClick = { 
                        viewModel.executeTermuxScript(context, "termux-change-repo")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Ganti Mirror Repository")
                }
                Button(
                    onClick = { 
                        viewModel.executeTermuxScript(context, "termux-setup-storage")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Termux Setup Storage")
                }
                Button(
                    onClick = { 
                        viewModel.executeTermuxScript(context, "pkg install nodejs -y && npm install -g esbuild && echo 'esbuild berhasil diinstal. Anda sekarang dapat menggunakan esbuild untuk mem-build bundle.js.'")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Instalasi Cadangan esbuild/bundle.js")
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
