package moe.shizuku.manager.filemanager.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.filemanager.domain.repository.SettingsRepository
import moe.shizuku.manager.filemanager.presentation.settings.components.SettingsClickableItem
import moe.shizuku.manager.filemanager.presentation.settings.components.SettingsSwitchItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { SettingsViewModel(SettingsRepository(context.applicationContext)) }

    val isGridView by viewModel.isGridView.collectAsState(initial = false)
    val showHiddenFiles by viewModel.showHiddenFiles.collectAsState(initial = false)
    val showMediaThumbnails by viewModel.showMediaThumbnails.collectAsState(initial = true)
    
    val useRecycleBin by viewModel.useRecycleBin.collectAsState(initial = true)
    val confirmBeforeDelete by viewModel.confirmBeforeDelete.collectAsState(initial = true)
    
    val executeShInBackground by viewModel.executeShInBackground.collectAsState(initial = false)
    val useRootForTermux by viewModel.useRootForTermux.collectAsState(initial = false)

    var showClearPinsDialog by remember { mutableStateOf(false) }

    if (showClearPinsDialog) {
        AlertDialog(
            onDismissRequest = { showClearPinsDialog = false },
            title = { Text("Hapus Pin Folder") },
            text = { Text("Apakah Anda yakin ingin menghapus semua folder yang disematkan?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearPinnedFolders()
                    showClearPinsDialog = false
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showClearPinsDialog = false }) { Text("Batal") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan File Manager") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Kategori: Tampilan & Navigasi
                Text(
                    text = "Tampilan & Navigasi",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
                SettingsSwitchItem(
                    title = "Mode Grid Default",
                    subtitle = "Tampilkan file dalam format grid secara default",
                    checked = isGridView,
                    onCheckedChange = { viewModel.setGridView(it) }
                )
                SettingsSwitchItem(
                    title = "Tampilkan File Tersembunyi",
                    subtitle = "Tampilkan file dan folder yang diawali dengan titik",
                    checked = showHiddenFiles,
                    onCheckedChange = { viewModel.setShowHiddenFiles(it) }
                )
                SettingsSwitchItem(
                    title = "Thumbnail Media",
                    subtitle = "Tampilkan pratinjau untuk gambar dan video",
                    checked = showMediaThumbnails,
                    onCheckedChange = { viewModel.setShowMediaThumbnails(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Kategori: Manajemen & Operasi File
                Text(
                    text = "Operasi File",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
                SettingsSwitchItem(
                    title = "Gunakan Recycle Bin",
                    subtitle = "Pindahkan file ke tempat sampah sebelum dihapus permanen",
                    checked = useRecycleBin,
                    onCheckedChange = { viewModel.setUseRecycleBin(it) }
                )
                SettingsSwitchItem(
                    title = "Konfirmasi Hapus",
                    subtitle = "Tampilkan peringatan sebelum menghapus item",
                    checked = confirmBeforeDelete,
                    onCheckedChange = { viewModel.setConfirmBeforeDelete(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Kategori: Eksekusi Lanjut & Ekstensi
                Text(
                    text = "Eksekusi Lanjut (Termux)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
                SettingsSwitchItem(
                    title = "Jalankan .sh di Background",
                    subtitle = "Jangan tampilkan dialog log saat mengeksekusi script",
                    checked = executeShInBackground,
                    onCheckedChange = { viewModel.setExecuteShInBackground(it) }
                )
                SettingsSwitchItem(
                    title = "Akses Root (su)",
                    subtitle = "Coba jalankan perintah sebagai root jika tersedia",
                    checked = useRootForTermux,
                    onCheckedChange = { viewModel.setUseRootForTermux(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Kategori: Bookmark
                Text(
                    text = "Lainnya",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                )
                SettingsClickableItem(
                    title = "Bersihkan Folder Disematkan",
                    subtitle = "Hapus semua shortcut folder dari beranda",
                    onClick = { showClearPinsDialog = true }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Selesai")
            }
        }
    )
}
