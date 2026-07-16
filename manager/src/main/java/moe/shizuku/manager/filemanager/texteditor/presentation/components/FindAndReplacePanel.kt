package moe.shizuku.manager.filemanager.texteditor.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun FindAndReplacePanel(
    onDismiss: () -> Unit,
    onFindNext: (String, Boolean) -> Unit,
    onFindPrev: (String, Boolean) -> Unit,
    onReplace: (String) -> Unit,
    onReplaceAll: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var matchCase by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cari dan Ganti", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    // Trigger a find next automatically so the user sees results
                    if (it.isNotEmpty()) {
                        onFindNext(it, matchCase)
                    }
                },
                label = { Text("Cari") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    Row {
                        IconButton(onClick = { onFindPrev(searchQuery, matchCase) }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Sebelumnya")
                        }
                        IconButton(onClick = { onFindNext(searchQuery, matchCase) }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Berikutnya")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = replaceQuery,
                onValueChange = { replaceQuery = it },
                label = { Text("Ganti dengan") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = matchCase,
                    onCheckedChange = { 
                        matchCase = it
                        if (searchQuery.isNotEmpty()) {
                            onFindNext(searchQuery, matchCase)
                        }
                    }
                )
                Text("Cocokkan Huruf (Case Sensitive)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onReplace(replaceQuery) }) {
                    Text("Ganti")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onReplaceAll(replaceQuery) }) {
                    Text("Ganti Semua")
                }
            }
        }
    }
}
