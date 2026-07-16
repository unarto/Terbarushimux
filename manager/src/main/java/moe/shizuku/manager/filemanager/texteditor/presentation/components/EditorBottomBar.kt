package moe.shizuku.manager.filemanager.texteditor.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorBottomBar(
    onActionClick: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            val symbols = listOf("{", "}", "[", "]", "(", ")", ";", "=", "/", "<", ">", ":", "'", "\"", "+", "-", "*", "\\", "|", "&", "!", "?")
            
            // Symbol Row (Scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                symbols.forEach { symbol ->
                    ExtraKeyButton(
                        text = symbol,
                        onClick = { onActionClick("INSERT_SYMBOL:$symbol") },
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            // Command Row (Scrollable)
            val commands = listOf(
                "TAB" to "TAB", 
                "←" to "LEFT", 
                "→" to "RIGHT", 
                "↑" to "UP", 
                "↓" to "DOWN",
                "UNDO" to "UNDO", 
                "REDO" to "REDO", 
                "SAVE" to "SAVE",
                "ENTER" to "ENTER",
                "ESC" to "ESC",
                "CTRL" to "CTRL",
                "ALT" to "ALT",
                "SHFT" to "SHFT",
                "PGUP" to "PGUP",
                "PGDN" to "PGDN"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                commands.forEach { (label, action) ->
                    ExtraKeyButton(
                        text = label,
                        onClick = { onActionClick(action) },
                        modifier = Modifier.padding(horizontal = 2.dp).defaultMinSize(minWidth = 48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtraKeyButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
