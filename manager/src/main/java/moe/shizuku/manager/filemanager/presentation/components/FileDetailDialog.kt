package moe.shizuku.manager.filemanager.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import moe.shizuku.manager.filemanager.domain.FileItem
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

@Composable
fun FileDetailDialog(
    item: FileItem,
    onDismissRequest: () -> Unit
) {
    var md5 by remember { mutableStateOf<String?>(null) }
    var sha1 by remember { mutableStateOf<String?>(null) }
    var sha256 by remember { mutableStateOf<String?>(null) }
    var dirSize by remember { mutableStateOf<Long?>(null) }
    var isCalculating by remember { mutableStateOf(false) }

    LaunchedEffect(item) {
        if (!item.isDirectory && !item.path.startsWith("content://")) {
            isCalculating = true
            withContext(Dispatchers.IO) {
                try {
                    val file = File(item.path)
                    if (file.exists() && file.length() < 100 * 1024 * 1024) { // Limit to 100MB to avoid long freezes
                        val buffer = ByteArray(8192)
                        val md5Digest = MessageDigest.getInstance("MD5")
                        val sha1Digest = MessageDigest.getInstance("SHA-1")
                        val sha256Digest = MessageDigest.getInstance("SHA-256")
                        
                        file.inputStream().use { input ->
                            var bytesRead = input.read(buffer)
                            while (bytesRead != -1) {
                                md5Digest.update(buffer, 0, bytesRead)
                                sha1Digest.update(buffer, 0, bytesRead)
                                sha256Digest.update(buffer, 0, bytesRead)
                                bytesRead = input.read(buffer)
                            }
                        }
                        
                        md5 = md5Digest.digest().joinToString("") { "%02x".format(it) }
                        sha1 = sha1Digest.digest().joinToString("") { "%02x".format(it) }
                        sha256 = sha256Digest.digest().joinToString("") { "%02x".format(it) }
                    } else {
                        md5 = "File too large (> 100MB) or not accessible"
                    }
                } catch (e: Exception) {
                    md5 = "Error: ${e.message}"
                }
            }
            isCalculating = false
        } else if (item.isDirectory && !item.path.startsWith("content://")) {
             isCalculating = true
             withContext(Dispatchers.IO) {
                 try {
                     fun calculateSize(f: File): Long {
                         var size = 0L
                         if (f.isDirectory) {
                             f.listFiles()?.forEach { child ->
                                 size += calculateSize(child)
                             }
                         } else {
                             size = f.length()
                         }
                         return size
                     }
                     dirSize = calculateSize(File(item.path))
                 } catch (e: Exception) {
                     dirSize = -1L
                 }
             }
             isCalculating = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Detail File") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Nama: ${item.name}")
                Spacer(modifier = Modifier.height(4.dp))
                Text("Lokasi: ${item.path}")
                Spacer(modifier = Modifier.height(4.dp))
                
                if (item.isDirectory) {
                    if (isCalculating) {
                        Text("Ukuran: Menghitung...")
                    } else if (dirSize != null && dirSize != -1L) {
                        Text("Ukuran: $dirSize bytes")
                    } else {
                        Text("Ukuran: ${item.size} bytes (Unknown contents)")
                    }
                } else {
                    Text("Ukuran: ${item.size} bytes")
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text("Izin: ${item.permissions}")
                Spacer(modifier = Modifier.height(4.dp))
                val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
                Text("Dimodifikasi: ${formatter.format(Date(item.lastModified))}")
                
                if (!item.isDirectory && !item.path.startsWith("content://")) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Hashes:", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                    if (isCalculating) {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                    } else {
                        Text("MD5: ${md5 ?: "N/A"}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("SHA-1: ${sha1 ?: "N/A"}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("SHA-256: ${sha256 ?: "N/A"}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text("Tutup") }
        }
    )
}
