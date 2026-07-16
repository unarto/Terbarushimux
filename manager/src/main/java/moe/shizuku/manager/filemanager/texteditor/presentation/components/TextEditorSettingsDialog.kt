package moe.shizuku.manager.filemanager.texteditor.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.shizuku.manager.filemanager.domain.repository.SettingsRepository

@Composable
fun TextEditorSettingsDialog(
    settingsRepository: SettingsRepository,
    onDismiss: () -> Unit
) {
    val wordWrap by settingsRepository.editorWordWrap.collectAsState()
    val fontSize by settingsRepository.editorFontSize.collectAsState()
    val tabSize by settingsRepository.editorTabSize.collectAsState()
    val autoSave by settingsRepository.editorAutoSave.collectAsState()
    val editorTheme by settingsRepository.editorTheme.collectAsState()
    val availableThemes = listOf("darcula", "monokai", "material", "dracula", "eclipse")
    var themeDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editor Settings") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto Save")
                    Switch(
                        checked = autoSave,
                        onCheckedChange = { settingsRepository.setEditorAutoSave(it) }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Word Wrap")
                    Switch(
                        checked = wordWrap,
                        onCheckedChange = { settingsRepository.setEditorWordWrap(it) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tab Size: $tabSize")
                    Slider(
                        value = tabSize.toFloat(),
                        onValueChange = { settingsRepository.setEditorTabSize(it.toInt()) },
                        valueRange = 2f..8f,
                        steps = 5,
                        modifier = Modifier.weight(1f).padding(start = 16.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Font Size: $fontSize")
                    Slider(
                        value = fontSize.toFloat(),
                        onValueChange = { settingsRepository.setEditorFontSize(it.toInt()) },
                        valueRange = 10f..30f,
                        steps = 20,
                        modifier = Modifier.weight(1f).padding(start = 16.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Theme:")
                    Box {
                        OutlinedButton(onClick = { themeDropdownExpanded = true }) {
                            Text(editorTheme)
                        }
                        DropdownMenu(
                            expanded = themeDropdownExpanded,
                            onDismissRequest = { themeDropdownExpanded = false }
                        ) {
                            availableThemes.forEach { themeName ->
                                DropdownMenuItem(
                                    text = { Text(themeName) },
                                    onClick = {
                                        settingsRepository.setEditorTheme(themeName)
                                        themeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
