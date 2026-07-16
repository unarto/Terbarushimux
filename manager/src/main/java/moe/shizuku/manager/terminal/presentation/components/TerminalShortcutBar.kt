package moe.shizuku.manager.terminal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TerminalShortcutBar(
    isCtrlDown: Boolean,
    isAltDown: Boolean,
    onCtrlToggle: () -> Unit,
    onAltToggle: () -> Unit,
    onKey: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        val row1 = listOf("ESC", "TAB", "CTRL", "ALT", "-", "/", "_", "$")
        val row2 = listOf("HOME", "END", "LEFT", "UP", "DOWN", "RIGHT", "BKSP", "ENTER")

        @Composable
        fun KeyRow(keys: List<String>) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                keys.forEach { key ->
                    val isToggled = (key == "CTRL" && isCtrlDown) || (key == "ALT" && isAltDown)
                    val containerColor = if (isToggled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    val contentColor = if (isToggled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    
                    Button(
                        onClick = {
                            when (key) {
                                "CTRL" -> onCtrlToggle()
                                "ALT" -> onAltToggle()
                                else -> onKey(key)
                            }
                        },
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = containerColor,
                            contentColor = contentColor
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .height(36.dp)
                    ) {
                        Text(text = key, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        KeyRow(keys = row1)
        KeyRow(keys = row2)
    }
}
