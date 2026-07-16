package moe.shizuku.manager.filemanager.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import moe.shizuku.manager.filemanager.presentation.settings.FileManagerSettingsDialog
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.CheckCircle
import moe.shizuku.manager.filemanager.presentation.components.BreadcrumbNavigation
import moe.shizuku.manager.filemanager.presentation.components.FileItemRow
import moe.shizuku.manager.filemanager.presentation.components.FileItemGridCell
import moe.shizuku.manager.filemanager.presentation.components.CreateFileDialog
import moe.shizuku.manager.filemanager.presentation.components.GitCloneDialog
import moe.shizuku.manager.filemanager.presentation.components.RenameFileDialog
import moe.shizuku.manager.filemanager.presentation.components.ErrorDialog
import moe.shizuku.manager.filemanager.presentation.components.ScriptExecutionDialog
import moe.shizuku.manager.filemanager.presentation.components.SortDialog
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import moe.shizuku.manager.filemanager.domain.FileItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel,
    onBack: () -> Unit,
    onNavigateToRecycleBin: () -> Unit,
    onOpenFile: (moe.shizuku.manager.filemanager.domain.FileItem) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var createType by remember { mutableStateOf("file") } // "file" or "dir"

    var renameDialogItem by remember { mutableStateOf<FileItem?>(null) }
    var showGitCloneDialog by remember { mutableStateOf(false) }
    var showPackageInstaller by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showTermuxSettings by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showFindAndReplaceItem by remember { mutableStateOf<FileItem?>(null) }
    
    // Clipboard for copy/move operations
    var clipboardPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var clipboardIsMove by remember { mutableStateOf(false) }
    
    var selectedItemForMenu by remember { mutableStateOf<FileItem?>(null) }
    var chmodDialogItem by remember { mutableStateOf<FileItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var detailsDialogItem by remember { mutableStateOf<FileItem?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.addSafStorage(uri)
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        if (state.selectedItems.isNotEmpty()) {
                            Text("${state.selectedItems.size} Terpilih")
                        } else if (state.currentPath == "home") {
                            Text("File Manager")
                        } else {
                            BreadcrumbNavigation(state.currentPath) { viewModel.navigateTo(it) }
                        }
                    },
                    navigationIcon = {
                        if (state.selectedItems.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearSelection() }) {
                                Text("Batal")
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (state.currentPath == "home") {
                                        onBack()
                                    } else {
                                        viewModel.navigateUp()
                                    }
                                },
                                modifier = Modifier.testTag("back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.selectedItems.isNotEmpty()) {
                        TextButton(onClick = { viewModel.selectAll() }) {
                            Text("Semua")
                        }
                        IconButton(onClick = {
                            clipboardPaths = state.selectedItems.map { it.path }
                            clipboardIsMove = false
                            viewModel.clearSelection()
                        }) {
                            Text("C", color = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = {
                            clipboardPaths = state.selectedItems.map { it.path }
                            clipboardIsMove = true
                            viewModel.clearSelection()
                        }) {
                            Text("M", color = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = {
                            viewModel.requestDeleteFiles(state.selectedItems.map { it.path })
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    } else if (clipboardPaths.isNotEmpty()) {
                        TextButton(onClick = {
                            clipboardPaths.forEach { path ->
                                val dest = if (state.currentPath.endsWith("/")) "${state.currentPath}${path.split("/").last()}" else "${state.currentPath}/${path.split("/").last()}"
                                if (clipboardIsMove) {
                                    viewModel.moveFile(path, dest)
                                } else {
                                    viewModel.copyFile(path, dest)
                                }
                            }
                            clipboardPaths = emptyList()
                        }) {
                            Text("Paste")
                        }
                        TextButton(onClick = { clipboardPaths = emptyList() }) {
                            Text("Cancel")
                        }
                    } else {
                        TextButton(onClick = { viewModel.toggleGridView() }) {
                            Text(if (state.isGridView) "List" else "Grid")
                        }
                        TextButton(onClick = { showSortDialog = true }) {
                            Text("Urut")
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Packages") },
                                    onClick = { 
                                        showOverflowMenu = false
                                        showPackageInstaller = true 
                                    },
                                    leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (state.showHiddenFiles) "Sembunyikan File Tersembunyi" else "Tampilkan File Tersembunyi") },
                                    onClick = { 
                                        showOverflowMenu = false
                                        viewModel.toggleShowHiddenFiles() 
                                    },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.AutoMirrored.Filled.List, 
                                            contentDescription = null
                                        ) 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Termux Settings") },
                                    onClick = { 
                                        showOverflowMenu = false
                                        showTermuxSettings = true 
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Tempat Sampah") },
                                    onClick = { 
                                        showOverflowMenu = false
                                        onNavigateToRecycleBin() 
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = { 
                                        showOverflowMenu = false
                                        showSettings = true 
                                    },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Cari file...") },
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            if (state.currentPath != "home") {
                FloatingActionButton(onClick = {
                    createType = "file"
                    showCreateDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = { safPickerLauncher.launch(null) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Tambah Akses Aplikasi") },
                    text = { Text("Tambah Akses Aplikasi") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredItems) { file ->
                        FileItemGridCell(
                            item = file,
                            isSelected = state.selectedItems.contains(file),
                            showThumbnails = state.showMediaThumbnails,
                            onClick = {
                                if (state.selectedItems.isNotEmpty()) {
                                    if (file.lastModified != 0L || file.size != 0L) {
                                        viewModel.toggleSelection(file)
                                    }
                                } else if (file.isDirectory) {
                                    viewModel.navigateTo(file.path)
                                } else if (file.name.endsWith(".sh") || file.name.endsWith(".py") || file.name.endsWith(".js")) {
                                    val escapedName = "'" + file.name.replace("'", "'\\''") + "'"
                                    val interpreter = when {
                                        file.name.endsWith(".sh") -> "sh"
                                        file.name.endsWith(".py") -> "python"
                                        file.name.endsWith(".js") -> "node"
                                        else -> "sh"
                                    }
                                    viewModel.executeTermuxScript(context, "$interpreter $escapedName")
                                } else {
                                    val ext = file.name.substringAfterLast('.', "").lowercase()
                                    val textExtensions = setOf("txt", "json", "xml", "html", "css", "js", "ts", "kt", "java", "c", "cpp", "h", "md", "csv", "sh", "py", "prop", "properties")
                                    if (ext in textExtensions || ext.isEmpty()) {
                                        onOpenFile(file)
                                    }
                                }
                            },
                            onLongClick = {
                                if (file.lastModified != 0L || file.size != 0L) {
                                    if (state.selectedItems.isEmpty()) {
                                        viewModel.toggleSelection(file)
                                    } else {
                                        selectedItemForMenu = file
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.filteredItems) { file ->
                        FileItemRow(
                            item = file,
                            isSelected = state.selectedItems.contains(file),
                            showThumbnails = state.showMediaThumbnails,
                            onClick = {
                                if (state.selectedItems.isNotEmpty()) {
                                    if (file.lastModified != 0L || file.size != 0L) {
                                        viewModel.toggleSelection(file)
                                    }
                                } else if (file.isDirectory) {
                                    viewModel.navigateTo(file.path)
                                } else if (file.name.endsWith(".sh") || file.name.endsWith(".py") || file.name.endsWith(".js")) {
                                    val escapedName = "'" + file.name.replace("'", "'\\''") + "'"
                                    val interpreter = when {
                                        file.name.endsWith(".sh") -> "sh"
                                        file.name.endsWith(".py") -> "python"
                                        file.name.endsWith(".js") -> "node"
                                        else -> "sh"
                                    }
                                    viewModel.executeTermuxScript(context, "$interpreter $escapedName")
                                } else {
                                    val ext = file.name.substringAfterLast('.', "").lowercase()
                                    val textExtensions = setOf("txt", "json", "xml", "html", "css", "js", "ts", "kt", "java", "c", "cpp", "h", "md", "csv", "sh", "py", "prop", "properties")
                                    if (ext in textExtensions || ext.isEmpty()) {
                                        onOpenFile(file)
                                    }
                                }
                            },
                            onLongClick = {
                                if (file.lastModified != 0L || file.size != 0L) {
                                    if (state.selectedItems.isEmpty()) {
                                        viewModel.toggleSelection(file)
                                    } else {
                                        selectedItemForMenu = file
                                    }
                                }
                            },
                            onMenuClick = {
                                if (file.lastModified != 0L || file.size != 0L) {
                                    selectedItemForMenu = file
                                }
                            }
                        )
                    }
                }
            }
        }
        
        if (state.pendingDeletePaths.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDelete() },
                title = { Text("Konfirmasi Hapus") },
                text = { Text("Apakah Anda yakin ingin menghapus ${state.pendingDeletePaths.size} item?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDelete() }) {
                        Text("Hapus", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDelete() }) {
                        Text("Batal")
                    }
                }
            )
        }

        if (state.error != null) {
            ErrorDialog(
                error = state.error!!,
                onDismiss = { viewModel.clearError() }
            )
        }
        
        if (state.isExecutingScript || state.scriptOutput.isNotEmpty()) {
            ScriptExecutionDialog(
                isExecuting = state.isExecutingScript,
                output = state.scriptOutput,
                onDismiss = { viewModel.closeScriptDialog() }
            )
        }

        if (showCreateDialog) {
            CreateFileDialog(
                onDismissRequest = { showCreateDialog = false },
                onCreate = { name, isDir ->
                    if (isDir) {
                        viewModel.createDirectory(name)
                    } else {
                        viewModel.createFile(name)
                    }
                }
            )
        }
        
        if (showGitCloneDialog) {
            GitCloneDialog(
                onDismissRequest = { showGitCloneDialog = false },
                onClone = { repoUrl ->
                    viewModel.executeTermuxScript(context, "git clone '$repoUrl'")
                }
            )
        }
        
        if (showPackageInstaller) {
            TermuxDependencyManagerDialog(
                context = context,
                viewModel = viewModel,
                onDismiss = { showPackageInstaller = false }
            )
        }
        
        if (showSortDialog) {
            SortDialog(
                currentOption = state.sortOption,
                currentOrder = state.sortOrder,
                onOptionSelected = { viewModel.setSortOption(it) },
                onOrderToggled = { viewModel.toggleSortOrder() },
                onDismiss = { showSortDialog = false }
            )
        }

        if (showSettings) {
            FileManagerSettingsDialog(
                onDismiss = { showSettings = false }
            )
        }

        if (showTermuxSettings) {
            moe.shizuku.manager.filemanager.presentation.components.TermuxSettingsDialog(
                context = context,
                viewModel = viewModel,
                onDismiss = { showTermuxSettings = false }
            )
        }
        
        if (showFindAndReplaceItem != null) {
            FindAndReplaceManagerDialog(
                context = context,
                viewModel = viewModel,
                targetDirectory = showFindAndReplaceItem!!.path,
                onDismiss = { showFindAndReplaceItem = null }
            )
        }
        
        if (renameDialogItem != null) {
            RenameFileDialog(
                item = renameDialogItem!!,
                onDismissRequest = { renameDialogItem = null },
                onRename = { newName ->
                    val parent = renameDialogItem!!.path.substringBeforeLast("/")
                    val newPath = "$parent/$newName"
                    viewModel.renameFile(renameDialogItem!!.path, newPath)
                    renameDialogItem = null
                }
            )
        }
        
        if (detailsDialogItem != null) {
            moe.shizuku.manager.filemanager.presentation.components.FileDetailDialog(
                item = detailsDialogItem!!,
                onDismissRequest = { detailsDialogItem = null }
            )
        }
        
        if (selectedItemForMenu != null) {
            moe.shizuku.manager.filemanager.presentation.components.FileContextMenuBottomSheet(
                item = selectedItemForMenu!!,
                sheetState = sheetState,
                viewModel = viewModel,
                currentPath = state.currentPath,
                onDismissRequest = { selectedItemForMenu = null },
                onShowDetails = { detailsDialogItem = it },
                onRename = { renameDialogItem = it },
                onCopy = {
                    clipboardPaths = listOf(it.path)
                    clipboardIsMove = false
                },
                onMove = {
                    clipboardPaths = listOf(it.path)
                    clipboardIsMove = true
                },
                onFindAndReplace = { showFindAndReplaceItem = it },
                onGitClone = { showGitCloneDialog = true },
                onShowChmodDialog = { chmodDialogItem = it }
            )
        }
        
        if (chmodDialogItem != null) {
            moe.shizuku.manager.filemanager.presentation.components.ChmodDialog(
                item = chmodDialogItem!!,
                onDismiss = { chmodDialogItem = null },
                onConfirm = { octalMode ->
                    val escapedName = "'" + chmodDialogItem!!.name.replace("'", "'\\''") + "'"
                    viewModel.executeTermuxScript(context, "chmod $octalMode $escapedName")
                    chmodDialogItem = null
                }
            )
        }
    }
}
