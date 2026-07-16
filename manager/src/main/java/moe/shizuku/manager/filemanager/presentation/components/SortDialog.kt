package moe.shizuku.manager.filemanager.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.filemanager.presentation.SortOption
import moe.shizuku.manager.filemanager.presentation.SortOrder

@Composable
fun SortDialog(
    currentOption: SortOption,
    currentOrder: SortOrder,
    onOptionSelected: (SortOption) -> Unit,
    onOrderToggled: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Urutkan Berdasarkan") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Urutan:")
                    TextButton(onClick = onOrderToggled) {
                        Text(if (currentOrder == SortOrder.ASCENDING) "Menaik (A-Z)" else "Menurun (Z-A)")
                    }
                }
                HorizontalDivider()
                SortOption.values().forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(when (option) {
                            SortOption.NAME -> "Nama"
                            SortOption.SIZE -> "Ukuran"
                            SortOption.DATE -> "Tanggal"
                            SortOption.EXTENSION -> "Ekstensi"
                        }, modifier = Modifier.padding(top = 12.dp))
                        RadioButton(
                            selected = option == currentOption,
                            onClick = { onOptionSelected(option) }
                        )
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
