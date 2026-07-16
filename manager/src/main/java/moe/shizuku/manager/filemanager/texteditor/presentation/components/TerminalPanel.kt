package moe.shizuku.manager.filemanager.texteditor.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalBottomSheet(
    onDismiss: () -> Unit,
    workingDirectory: String = "/"
) {
    var outputLines by remember { mutableStateOf(listOf("Shimux Terminal", "----------------")) }
    var inputCommand by remember { mutableStateOf("") }
    var useShizuku by remember { mutableStateOf(Shizuku.pingBinder()) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1E1E), // Dark terminal background
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                                Text(
                    text = "Terminal",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    
                    Text("Root/ADB", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                    Switch(checked = useShizuku, onCheckedChange = { useShizuku = it }, modifier = Modifier.padding(end = 8.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))
            
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(outputLines) { line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ",
                    fontFamily = FontFamily.Monospace,
                    color = Color.Green,
                    fontSize = 14.sp
                )
                BasicTextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (inputCommand.isNotBlank()) {
                                val cmd = inputCommand
                                outputLines = outputLines + "$ $cmd"
                                inputCommand = ""
                                
                                coroutineScope.launch {
                                    try {
                                        val output = executeCommand(cmd, workingDirectory, useShizuku)
                                        outputLines = outputLines + output
                                    } catch (e: Exception) {
                                        outputLines = outputLines + listOf("Error: ${e.message}")
                                    }
                                    listState.animateScrollToItem(outputLines.size)
                                }
                            }
                        }
                    )
                )
            }
        }
    }
    
    LaunchedEffect(outputLines.size) {
        if (outputLines.isNotEmpty()) {
            listState.animateScrollToItem(outputLines.size - 1)
        }
    }
}

suspend fun executeCommand(command: String, workingDir: String, useShizuku: Boolean = false): List<String> = withContext(Dispatchers.IO) {
    val result = mutableListOf<String>()
    try {
        val process = if (useShizuku && Shizuku.pingBinder()) {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess", 
                Array<String>::class.java, 
                Array<String>::class.java, 
                String::class.java
            )
            newProcessMethod.isAccessible = true
            newProcessMethod.invoke(null, arrayOf("sh", "-c", "cd '$workingDir' && $command"), null, null) as Process
        } else {
            Runtime.getRuntime().exec(arrayOf("sh", "-c", "cd '$workingDir' && $command"))
        }
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val errorReader = BufferedReader(InputStreamReader(process.errorStream))
        
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            line?.let { result.add(it) }
        }
        while (errorReader.readLine().also { line = it } != null) {
            line?.let { result.add(it) }
        }
        process.waitFor()
    } catch (e: Exception) {
        result.add("Failed to execute: ${e.message}")
    }
    result
}