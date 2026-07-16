package moe.shizuku.manager.filemanager.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.filemanager.domain.FileItem

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontFamily

@Composable
fun ErrorDialog(
    error: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(error) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

@Composable
fun ScriptExecutionDialog(
    isExecuting: Boolean,
    output: List<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isExecuting) onDismiss() },
        title = { Text(if (isExecuting) "Executing Termux Script..." else "Execution Finished") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                output.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            if (isExecuting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
fun CreateFileDialog(
    onDismissRequest: () -> Unit,
    onCreate: (name: String, isDir: Boolean) -> Unit
) {
    var inputName by remember { mutableStateOf("") }
    var createType by remember { mutableStateOf("file") } // "file" or "dir"

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Buat Baru") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = createType == "file",
                        onClick = { createType = "file" }
                    )
                    Text("File")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(
                        selected = createType == "dir",
                        onClick = { createType = "dir" }
                    )
                    Text("Folder")
                }
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Nama") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (inputName.isNotBlank()) {
                    onCreate(inputName, createType == "dir")
                }
                onDismissRequest()
            }) {
                Text("Buat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Batal") }
        }
    )
}

@Composable
fun GitCloneDialog(
    onDismissRequest: () -> Unit,
    onClone: (repoUrl: String) -> Unit
) {
    var repoUrl by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Git Clone") },
        text = {
            OutlinedTextField(
                value = repoUrl,
                onValueChange = { repoUrl = it },
                label = { Text("URL Repository") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (repoUrl.isNotBlank()) {
                    onClone(repoUrl)
                }
                onDismissRequest()
            }) {
                Text("Clone")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Batal") }
        }
    )
}

@Composable
fun RenameFileDialog(
    item: FileItem,
    onDismissRequest: () -> Unit,
    onRename: (newName: String) -> Unit
) {
    var inputName by remember { mutableStateOf(item.name) }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Ganti Nama") },
        text = {
            OutlinedTextField(
                value = inputName,
                onValueChange = { inputName = it },
                label = { Text("Nama Baru") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (inputName.isNotBlank() && inputName != item.name) {
                    onRename(inputName)
                }
                onDismissRequest()
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Batal") }
        }
    )
}
