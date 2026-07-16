package moe.shizuku.manager.filemanager.texteditor.presentation

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moe.shizuku.manager.filemanager.texteditor.presentation.components.*
import moe.shizuku.manager.filemanager.domain.repository.SettingsRepository
import moe.shizuku.manager.filemanager.texteditor.viewmodel.EditorState
import moe.shizuku.manager.filemanager.texteditor.viewmodel.TextEditorViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TextEditorScreen(
    initialFilePath: String?,
    onBack: () -> Unit,
    viewModel: TextEditorViewModel
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showOutlineDialog by remember { mutableStateOf(false) }
    var showTerminalPanel by remember { mutableStateOf(false) }
    var currentOutlineJson by remember { mutableStateOf("[]") }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var isReadOnlyMode by remember { mutableStateOf(false) }
    var showFindFileDialog by remember { mutableStateOf(false) }
    var showFindReplacePanel by remember { mutableStateOf(false) }
    var showRecentFilesDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    
    var showGoToLineDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var lineInput by remember { mutableStateOf("") }
    var pathInput by remember { mutableStateOf("") }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val settingsRepository = remember { SettingsRepository(context) }
    
    val openedFiles by settingsRepository.editorOpenedFiles.collectAsState()
    val activeFile by settingsRepository.editorActiveFile.collectAsState()
    val editorState by viewModel.editorState.collectAsState()
    val wordWrap by settingsRepository.editorWordWrap.collectAsState()
    val fontSize by settingsRepository.editorFontSize.collectAsState()
    val editorTheme by settingsRepository.editorTheme.collectAsState()
    val editorEncoding by settingsRepository.editorEncoding.collectAsState()
    val editorAutoSave by settingsRepository.editorAutoSave.collectAsState()
    
    LaunchedEffect(wordWrap) {
        webViewRef?.evaluateJavascript("window.EditorAPI.setWordWrap(${wordWrap})", null)
    }
    LaunchedEffect(fontSize) {
        webViewRef?.evaluateJavascript("window.EditorAPI.setFontSize(${fontSize})", null)
    }
    LaunchedEffect(editorTheme) {
        webViewRef?.evaluateJavascript("window.EditorAPI.setTheme('${editorTheme}')", null)
    }
    LaunchedEffect(editorAutoSave) {
        webViewRef?.evaluateJavascript("window.EditorAPI.setAutoSave(${editorAutoSave})", null)
    }
    LaunchedEffect(initialFilePath) {
        if (initialFilePath != null) {
            settingsRepository.addEditorOpenedFile(initialFilePath)
        }
    }
    
    val currentPath = activeFile ?: openedFiles.firstOrNull()
    LaunchedEffect(currentPath, editorEncoding) {
        if (currentPath != null) {
            viewModel.initFile(context, currentPath, editorEncoding)
        }
    }
    val fileName = currentPath?.substringAfterLast("/") ?: "No file opened"
    
    BackHandler {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            onBack()
        }
    }

    if (showFindFileDialog) {
        FindFileDialog(
            currentDir = currentPath?.substringBeforeLast("/") ?: "/",
            onDismiss = { showFindFileDialog = false },
            onFileSelected = { selectedPath ->
                showFindFileDialog = false
                settingsRepository.addEditorOpenedFile(selectedPath)
                settingsRepository.setEditorActiveFile(selectedPath)
            }
        )
    }

    if (showFindReplacePanel) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(onDismissRequest = { showFindReplacePanel = false }) {
            FindAndReplacePanel(
                onDismiss = { showFindReplacePanel = false },
                onFindNext = { query, matchCase ->
                    webViewRef?.evaluateJavascript("window.EditorAPI.setSearchQuery('${query.replace("'", "\\'")}', $matchCase, ''); window.EditorAPI.findNext()", null)
                },
                onFindPrev = { query, matchCase ->
                    webViewRef?.evaluateJavascript("window.EditorAPI.setSearchQuery('${query.replace("'", "\\'")}', $matchCase, ''); window.EditorAPI.findPrev()", null)
                },
                onReplace = { replaceWith ->
                    webViewRef?.evaluateJavascript("window.EditorAPI.setSearchQuery(null, false, '${replaceWith.replace("'", "\\'")}'); window.EditorAPI.replaceNext()", null)
                },
                onReplaceAll = { replaceWith ->
                    webViewRef?.evaluateJavascript("window.EditorAPI.setSearchQuery(null, false, '${replaceWith.replace("'", "\\'")}'); window.EditorAPI.replaceAll()", null)
                }
            )
        }
    }

    if (showPropertiesDialog) {
        val currentSyntax = (editorState as? EditorState.Success)?.mimeType ?: "javascript"
        FilePropertiesDialog(
            filePath = currentPath ?: "",
            onDismiss = { showPropertiesDialog = false },
            onRename = { newName ->
                viewModel.renameFile(context, newName) { success, newPath ->
                    if (success && newPath != null && currentPath != null) {
                        settingsRepository.removeEditorOpenedFile(currentPath)
                        settingsRepository.addEditorOpenedFile(newPath)
                        settingsRepository.setEditorActiveFile(newPath)
                        showPropertiesDialog = false
                        Toast.makeText(context, "Renamed successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to rename", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onSyntaxChange = { syntax ->
                showPropertiesDialog = false
                webViewRef?.evaluateJavascript("window.EditorAPI.setMode('${syntax}')", null)
                Toast.makeText(context, "Syntax set to $syntax", Toast.LENGTH_SHORT).show()
            },
            currentSyntax = currentSyntax,
            onEncodingChange = { enc ->
                settingsRepository.setEditorEncoding(enc)
                if (currentPath != null) {
                    viewModel.initFile(context, currentPath, enc)
                }
                Toast.makeText(context, "Encoding set to $enc", Toast.LENGTH_SHORT).show()
            },
            currentEncoding = editorEncoding,
            onReadOnlyToggle = { ro ->
                isReadOnlyMode = ro
                webViewRef?.evaluateJavascript("window.EditorAPI.setReadOnly($ro)", null)
            },
            currentReadOnly = isReadOnlyMode
        )
    }

    if (showTerminalPanel) {
        TerminalBottomSheet(
            onDismiss = { showTerminalPanel = false },
            workingDirectory = currentPath?.let { path ->
                if (path.startsWith("/")) path.substringBeforeLast("/") else "/"
            } ?: "/"
        )
    }

    if (showOutlineDialog) {
        OutlineDialog(
            outlineJson = currentOutlineJson,
            onDismiss = { showOutlineDialog = false },
            onLineSelected = { line ->
                showOutlineDialog = false
                webViewRef?.evaluateJavascript("window.EditorAPI.gotoLine($line)", null)
            }
        )
    }

    if (showSettingsDialog) {
        TextEditorSettingsDialog(
            settingsRepository = settingsRepository,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showGoToLineDialog) {
        AlertDialog(
            onDismissRequest = { showGoToLineDialog = false },
            title = { Text("Pergi ke Baris") },
            text = {
                OutlinedTextField(
                    value = lineInput,
                    onValueChange = { lineInput = it },
                    label = { Text("Nomor Baris") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val lineNum = lineInput.toIntOrNull()
                    if (lineNum != null && lineNum > 0) {
                        webViewRef?.evaluateJavascript("window.EditorAPI.gotoLine($lineNum)", null)
                    }
                    showGoToLineDialog = false
                }) { Text("Pergi") }
            },
            dismissButton = {
                TextButton(onClick = { showGoToLineDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Berkas Baru") },
            text = {
                OutlinedTextField(
                    value = pathInput,
                    onValueChange = { pathInput = it },
                    label = { Text("Path Lengkap") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pathInput.isNotBlank()) {
                        viewModel.saveAs(context, pathInput, "", settingsRepository.editorEncoding.value) { success ->
                            if (success) {
                                Toast.makeText(context, "File dibuat", Toast.LENGTH_SHORT).show()
                                settingsRepository.addEditorOpenedFile(pathInput)
                                settingsRepository.setEditorActiveFile(pathInput)
                            } else {
                                Toast.makeText(context, "Gagal membuat file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    showNewFileDialog = false
                }) { Text("Buat") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showSaveAsDialog) {
        AlertDialog(
            onDismissRequest = { showSaveAsDialog = false },
            title = { Text("Simpan Sebagai") },
            text = {
                OutlinedTextField(
                    value = pathInput,
                    onValueChange = { pathInput = it },
                    label = { Text("Path Lengkap") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pathInput.isNotBlank()) {
                        webViewRef?.evaluateJavascript("window.EditorAPI.getTextForSaveAs()", null)
                    }
                    showSaveAsDialog = false
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveAsDialog = false }) { Text("Batal") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Shimux Editor",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Berkas baru") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        pathInput = currentPath?.substringBeforeLast("/")?.plus("/newfile.txt") ?: "/sdcard/newfile.txt"
                        showNewFileDialog = true
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Simpan") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        webViewRef?.evaluateJavascript("window.EditorAPI.getText()", null)
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Simpan sebagai") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        pathInput = currentPath ?: ""
                        showSaveAsDialog = true 
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Berkas") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        onBack() 
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Tutup berkas") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        if (currentPath != null) {
                            settingsRepository.removeEditorOpenedFile(currentPath)
                            if (settingsRepository.editorOpenedFiles.value.isEmpty()) {
                                onBack()
                            } else {
                                settingsRepository.setEditorActiveFile(settingsRepository.editorOpenedFiles.value.last())
                            }
                        } else {
                            onBack()
                        }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Buka baru-baru ini") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        showRecentFilesDialog = true
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Temukan berkas") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; showFindFileDialog = true }
                )
                NavigationDrawerItem(
                    label = { Text("Konsol") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; showTerminalPanel = true }
                )
                NavigationDrawerItem(
                    label = { Text("Terminal") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; showTerminalPanel = true }
                )
                NavigationDrawerItem(
                    label = { Text("Pengaturan") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; showSettingsDialog = true }
                )
                NavigationDrawerItem(
                    label = { Text("Bantuan") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        showHelpDialog = true
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Keluar") },
                    selected = false,
                    onClick = { onBack() }
                )
            }
        },
        content = {
            Scaffold(
                topBar = {
                    Column {
                        EditorTopAppBar(
                            fileName = fileName,
                            filePath = currentPath ?: "No Path",
                            onNavIconClick = { scope.launch { drawerState.open() } },
                            onAction = { action ->
                                when (action) {
                                                                        "FORMAT" -> webViewRef?.evaluateJavascript("window.EditorAPI.formatCode()", null)
                                    "SHARE" -> {
                                        if (currentPath != null) {
                                            val file = java.io.File(currentPath)
                                            if (file.exists()) {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(android.content.Intent.EXTRA_TEXT, file.readText())
                                                    putExtra(android.content.Intent.EXTRA_TITLE, file.name)
                                                }
                                                context.startActivity(android.content.Intent.createChooser(intent, "Bagikan via"))
                                            }
                                        }
                                    }
                                    "OPEN_WITH" -> {
                                        if (currentPath != null) {
                                            try {
                                                val file = java.io.File(currentPath)
                                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "*/*")
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(android.content.Intent.createChooser(intent, "Buka dengan"))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Gagal membuka file: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    "EDIT_WITH" -> {
                                        if (currentPath != null) {
                                            try {
                                                val file = java.io.File(currentPath)
                                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val intent = android.content.Intent(android.content.Intent.ACTION_EDIT).apply {
                                                    setDataAndType(uri, "text/*")
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                                }
                                                context.startActivity(android.content.Intent.createChooser(intent, "Edit dengan"))
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Gagal mengedit file: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    "ADD_TO_HOMESCREEN" -> {
                                        if (currentPath != null) {
                                            val shortcut = androidx.core.content.pm.ShortcutInfoCompat.Builder(context, "file_${currentPath.hashCode()}")
                                                .setShortLabel(fileName)
                                                .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(context, moe.shizuku.manager.R.drawable.ic_code_24dp))
                                                .setIntent(android.content.Intent(context, moe.shizuku.manager.filemanager.presentation.FileManagerActivity::class.java).apply {
                                                    this.action = "moe.shizuku.manager.action.OPEN_FILE"
                                                    putExtra("file_path", currentPath)
                                                })
                                                .build()
                                            if (androidx.core.content.pm.ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
                                                androidx.core.content.pm.ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
                                            } else {
                                                Toast.makeText(context, "Tidak didukung oleh launcher", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    "PIN_TAB" -> {
                                        if (currentPath != null) {
                                            val shortcut = androidx.core.content.pm.ShortcutInfoCompat.Builder(context, "pin_${currentPath.hashCode()}")
                                                .setShortLabel(fileName)
                                                .setIcon(androidx.core.graphics.drawable.IconCompat.createWithResource(context, moe.shizuku.manager.R.drawable.ic_code_24dp))
                                                .setIntent(android.content.Intent(context, moe.shizuku.manager.filemanager.presentation.FileManagerActivity::class.java).apply {
                                                    this.action = "moe.shizuku.manager.action.OPEN_FILE"
                                                    putExtra("file_path", currentPath)
                                                })
                                                .build()
                                            androidx.core.content.pm.ShortcutManagerCompat.addDynamicShortcuts(context, listOf(shortcut))
                                            Toast.makeText(context, "Tab disematkan ke pintasan aplikasi", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    "GO_TO_LINE" -> showGoToLineDialog = true
                                    "CLOSE_RIGHT" -> {
                                        val currentIndex = openedFiles.indexOf(currentPath)
                                        if (currentIndex >= 0 && currentIndex < openedFiles.size - 1) {
                                            val filesToClose = openedFiles.subList(currentIndex + 1, openedFiles.size).toList()
                                            filesToClose.forEach { settingsRepository.removeEditorOpenedFile(it) }
                                        }
                                    }
                                    "CLOSE_LEFT" -> {
                                        val currentIndex = openedFiles.indexOf(currentPath)
                                        if (currentIndex > 0) {
                                            val filesToClose = openedFiles.subList(0, currentIndex).toList()
                                            filesToClose.forEach { settingsRepository.removeEditorOpenedFile(it) }
                                        }
                                    }
                                    "CLOSE_OTHERS" -> {
                                        openedFiles.forEach { path ->
                                            if (path != currentPath) {
                                                settingsRepository.removeEditorOpenedFile(path)
                                            }
                                        }
                                    }
                                    "FIND_REPLACE" -> showFindReplacePanel = true
                                    "COLOR_PAD" -> webViewRef?.evaluateJavascript("window.EditorAPI.insertText('#FF0000')", null)
                                    "DELETE_LINE" -> webViewRef?.evaluateJavascript("window.EditorAPI.execCommand('deleteLine')", null)
                                    "COPY_ALL" -> webViewRef?.evaluateJavascript("window.EditorAPI.copyAll()", null)
                                                                        "PASTE" -> {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        if (clipboard.hasPrimaryClip() && clipboard.primaryClip?.itemCount ?: 0 > 0) {
                                            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                            val escapedText = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
                                            webViewRef?.evaluateJavascript("window.EditorAPI.insertText(\"${escapedText}\")", null)
                                            Toast.makeText(context, "Ditempel dari papan klip", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Papan klip kosong", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    "SELECT_ALL" -> webViewRef?.evaluateJavascript("window.EditorAPI.execCommand('selectAll')", null)
                                }
                            }
                        )
                        
                        if (openedFiles.isNotEmpty()) {
                            ScrollableTabRow(
                                selectedTabIndex = openedFiles.indexOf(currentPath).coerceAtLeast(0),
                                edgePadding = 8.dp
                            ) {
                                openedFiles.forEach { path ->
                                    val name = path.substringAfterLast("/")
                                    Tab(
                                        selected = currentPath == path,
                                        onClick = { settingsRepository.setEditorActiveFile(path) },
                                        text = { 
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(name)
                                                Spacer(Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = {
                                                        settingsRepository.removeEditorOpenedFile(path)
                                                        if (currentPath == path) {
                                                            if (settingsRepository.editorOpenedFiles.value.isEmpty()) {
                                                                onBack()
                                                            } else {
                                                                settingsRepository.setEditorActiveFile(settingsRepository.editorOpenedFiles.value.last())
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(16.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Tutup Tab",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    EditorBottomBar(
                        onActionClick = { action ->
                            if (action.startsWith("SYMBOL_") || action.startsWith("INSERT_SYMBOL:")) {
                                val symbol = if (action.startsWith("SYMBOL_")) {
                                    action.removePrefix("SYMBOL_")
                                } else {
                                    action.removePrefix("INSERT_SYMBOL:")
                                }
                                val escapedSymbol = symbol
                                    .replace("\\\\", "\\\\\\\\")
                                    .replace("'", "\\\\'")
                                    .replace("\"", "\\\\\"")
                                webViewRef?.evaluateJavascript("window.EditorAPI.insertText('${escapedSymbol}')", null)
                                return@EditorBottomBar
                            }
                            
                            when (action) {
                                "SAVE" -> {
                                    webViewRef?.evaluateJavascript("window.EditorAPI.getText()", null)
                                }
                                "UNDO" -> webViewRef?.evaluateJavascript("window.EditorAPI.undo()", null)
                                "REDO" -> webViewRef?.evaluateJavascript("window.EditorAPI.redo()", null)
                                "UP" -> webViewRef?.evaluateJavascript("window.EditorAPI.execCommand('goLineUp')", null)
                                "DOWN" -> webViewRef?.evaluateJavascript("window.EditorAPI.execCommand('goLineDown')", null)
                                "LEFT" -> webViewRef?.evaluateJavascript("window.EditorAPI.execCommand('goCharLeft')", null)
                                "RIGHT" -> webViewRef?.evaluateJavascript("window.EditorAPI.execCommand('goCharRight')", null)
                                "PGUP" -> webViewRef?.evaluateJavascript("window.EditorAPI.execCommand('goPageUp')", null)
                                "PGDN" -> webViewRef?.evaluateJavascript("window.EditorAPI.execCommand('goPageDown')", null)
                                "TAB" -> webViewRef?.evaluateJavascript("window.EditorAPI.execCommand('defaultTab')", null)
                                "ENTER" -> webViewRef?.evaluateJavascript("window.EditorAPI.execCommand('insertNewlineAndIndent')", null)
                                "ESC" -> { /* Handle escape if needed */ }
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (val state = editorState) {
                        is EditorState.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        is EditorState.Error -> {
                            Text(
                                text = "Error: ${state.message}",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        is EditorState.Success -> {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                        webViewClient = WebViewClient()
                                        
                                        val jsInterface = AndroidJSInterface(
                                            onContentSaved = { content, isAutoSave ->
                                                val currentEncoding = settingsRepository.editorEncoding.value
                                                viewModel.saveFile(content, currentEncoding) { success ->
                                                    if (!isAutoSave) {
                                                        if (success) {
                                                            Toast.makeText(context, "File saved ($currentEncoding)", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            },
                                            onContentSavedAsCallback = { content ->
                                                val currentEncoding = settingsRepository.editorEncoding.value
                                                viewModel.saveAs(context, pathInput, content, currentEncoding) { success ->
                                                    if (success) {
                                                        Toast.makeText(context, "File saved as $pathInput", Toast.LENGTH_SHORT).show()
                                                        settingsRepository.addEditorOpenedFile(pathInput)
                                                        settingsRepository.setEditorActiveFile(pathInput)
                                                    } else {
                                                        Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            onEditorReadyCallback = {
                                                post {
                                                    val escapedContent = state.content
                                                        .replace("\\\\", "\\\\\\\\")
                                                        .replace("'", "\\\\'")
                                                        .replace("\\n", "\\\\n")
                                                        .replace("\\r", "")
                                                    
                                                    evaluateJavascript("window.EditorAPI.setText('${escapedContent}')", null)
                                                    evaluateJavascript("window.EditorAPI.setMode('${state.mimeType}')", null)
                                                    evaluateJavascript("window.EditorAPI.setWordWrap(${settingsRepository.editorWordWrap.value})", null)
                                                    evaluateJavascript("window.EditorAPI.setFontSize(${settingsRepository.editorFontSize.value})", null)
                                                    evaluateJavascript("window.EditorAPI.setTabSize(${settingsRepository.editorTabSize.value})", null)
                                                    evaluateJavascript("window.EditorAPI.setTheme('${settingsRepository.editorTheme.value}')", null)
                                                    evaluateJavascript("window.EditorAPI.setAutoSave(${settingsRepository.editorAutoSave.value})", null)
                                                }
                                            },
                                            onCopyAllCallback = { content ->
                                                val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clipData = android.content.ClipData.newPlainText("editor_content", content)
                                                clipboardManager.setPrimaryClip(clipData)
                                                Toast.makeText(context, "Disalin ke clipboard", Toast.LENGTH_SHORT).show()
                                            },
                                            onOutlineReadyCallback = { outlineJson ->
                                                post {
                                                    currentOutlineJson = outlineJson
                                                    showOutlineDialog = true
                                                }
                                            }
                                        )
                                        addJavascriptInterface(jsInterface, "AndroidBridge")
                                        loadUrl("file:///android_asset/editor/index.html")
                                    }
                                },
                                update = { view ->
                                    webViewRef = view
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    )
}
