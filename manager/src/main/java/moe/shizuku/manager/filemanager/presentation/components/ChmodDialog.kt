package moe.shizuku.manager.filemanager.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.filemanager.domain.FileItem

@Composable
fun ChmodDialog(
    item: FileItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // In a real app we would parse item.permissions
    var read by remember { mutableStateOf(true) }
    var write by remember { mutableStateOf(true) }
    var execute by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Izin - ${item.name}") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = read, onCheckedChange = { read = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Read (r)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = write, onCheckedChange = { write = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Write (w)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = execute, onCheckedChange = { execute = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Execute (x)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                var permStr = ""
                if (read) permStr += "r" else permStr += "-"
                if (write) permStr += "w" else permStr += "-"
                if (execute) permStr += "x" else permStr += "-"
                
                // For simplicity, apply to a,u,g,o or use octal. 
                // E.g. chmod +rwx
                var octal = 0
                if (read) octal += 4
                if (write) octal += 2
                if (execute) octal += 1
                val octalStr = "${octal}${octal}${octal}" // e.g. 777
                
                onConfirm(octalStr)
            }) {
                Text("Terapkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
