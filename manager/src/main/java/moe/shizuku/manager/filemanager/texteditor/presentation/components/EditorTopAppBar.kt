package moe.shizuku.manager.filemanager.texteditor.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopAppBar(
    fileName: String,
    filePath: String,
    onNavIconClick: () -> Unit,
    onAction: (String) -> Unit
) {
    var isOverflowMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { 
            androidx.compose.foundation.layout.Column {
                Text(text = fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Text(text = filePath, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavIconClick) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = { isOverflowMenuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options"
                )
                DropdownMenu(
                    expanded = isOverflowMenuExpanded,
                    onDismissRequest = { isOverflowMenuExpanded = false }
                ) {
                    DropdownMenuItem(text = { Text("Format") }, onClick = { isOverflowMenuExpanded = false; onAction("FORMAT") })
                    DropdownMenuItem(text = { Text("Bagikan") }, onClick = { isOverflowMenuExpanded = false; onAction("SHARE") })
                    DropdownMenuItem(text = { Text("Buka dengan") }, onClick = { isOverflowMenuExpanded = false; onAction("OPEN_WITH") })
                    DropdownMenuItem(text = { Text("Edit dengan") }, onClick = { isOverflowMenuExpanded = false; onAction("EDIT_WITH") })
                    DropdownMenuItem(text = { Text("Tambah ke layar beranda") }, onClick = { isOverflowMenuExpanded = false; onAction("ADD_TO_HOMESCREEN") })
                    DropdownMenuItem(text = { Text("Sematkan tab") }, onClick = { isOverflowMenuExpanded = false; onAction("PIN_TAB") })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Tutup Kanan") }, onClick = { isOverflowMenuExpanded = false; onAction("CLOSE_RIGHT") })
                    DropdownMenuItem(text = { Text("Tutup Kiri") }, onClick = { isOverflowMenuExpanded = false; onAction("CLOSE_LEFT") })
                    DropdownMenuItem(text = { Text("Tutup Lainnya") }, onClick = { isOverflowMenuExpanded = false; onAction("CLOSE_OTHERS") })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Cari dan Ganti") }, onClick = { 
                        isOverflowMenuExpanded = false
                        onAction("FIND_REPLACE")
                    })
                    DropdownMenuItem(text = { Text("Pergi ke baris") }, onClick = { isOverflowMenuExpanded = false; onAction("GO_TO_LINE") })
                    DropdownMenuItem(text = { Text("Sisipkan warna (Color Pad)") }, onClick = { 
                        isOverflowMenuExpanded = false
                        onAction("COLOR_PAD")
                    })
                    DropdownMenuItem(text = { Text("Hapus baris") }, onClick = { 
                        isOverflowMenuExpanded = false
                        onAction("DELETE_LINE")
                    })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Salin semua") }, onClick = { 
                        isOverflowMenuExpanded = false
                        onAction("COPY_ALL")
                    })
                    DropdownMenuItem(text = { Text("Tempel") }, onClick = { isOverflowMenuExpanded = false; onAction("PASTE") })
                    DropdownMenuItem(text = { Text("Pilih semua") }, onClick = { 
                        isOverflowMenuExpanded = false
                        onAction("SELECT_ALL")
                    })
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
