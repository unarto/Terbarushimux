package moe.shizuku.manager.filemanager.texteditor.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePropertiesDialog(
    filePath: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onSyntaxChange: (String) -> Unit,
    currentSyntax: String,
    onEncodingChange: (String) -> Unit,
    currentEncoding: String,
    onReadOnlyToggle: (Boolean) -> Unit,
    currentReadOnly: Boolean
) {
    val file = File(filePath)
    var isReadOnly by remember { mutableStateOf(currentReadOnly) }
    var newFileName by remember { mutableStateOf(file.name) }
    var syntaxMode by remember { mutableStateOf(currentSyntax) }
    var syntaxDropdownExpanded by remember { mutableStateOf(false) }
    val availableSyntaxes = listOf("javascript", "typescript", "java", "cpp", "csharp", "css", "html", "json", "markdown", "php", "python", "rust", "xml", "text/plain")
    
    var encodingDropdownExpanded by remember { mutableStateOf(false) }
    var selectedEncoding by remember { mutableStateOf(currentEncoding) }
    val availableEncodings = listOf("UTF-8", "ISO-8859-1", "Windows-1252", "US-ASCII", "UTF-16")
    
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val lastModified = if (file.exists()) dateFormatter.format(Date(file.lastModified())) else "-"
    val fileSize = if (file.exists()) "${file.length()} bytes" else "-"
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Properti Berkas") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    label = { Text("Nama Berkas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(text = "Lokasi: ${file.parent}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Ukuran: $fileSize", style = MaterialTheme.typography.bodySmall)
                Text(text = "Dimodifikasi: $lastModified", style = MaterialTheme.typography.bodySmall)
                
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(text = "Hanya Baca", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isReadOnly,
                        onCheckedChange = { 
                            isReadOnly = it
                            onReadOnlyToggle(it)
                        }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Penyorotan Sintaks:")
                    Box {
                        OutlinedButton(onClick = { syntaxDropdownExpanded = true }) {
                            Text(syntaxMode)
                        }
                        DropdownMenu(
                            expanded = syntaxDropdownExpanded,
                            onDismissRequest = { syntaxDropdownExpanded = false }
                        ) {
                            availableSyntaxes.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode) },
                                    onClick = {
                                        syntaxMode = mode
                                        syntaxDropdownExpanded = false
                                        onSyntaxChange(mode)
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pengodean Teks:")
                    Box {
                        OutlinedButton(onClick = { encodingDropdownExpanded = true }) {
                            Text(selectedEncoding)
                        }
                        DropdownMenu(
                            expanded = encodingDropdownExpanded,
                            onDismissRequest = { encodingDropdownExpanded = false }
                        ) {
                            availableEncodings.forEach { enc ->
                                DropdownMenuItem(
                                    text = { Text(enc) },
                                    onClick = {
                                        selectedEncoding = enc
                                        onEncodingChange(enc)
                                        encodingDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                if (newFileName != file.name && newFileName.isNotBlank()) {
                    onRename(newFileName)
                } else {
                    onDismiss()
                }
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
