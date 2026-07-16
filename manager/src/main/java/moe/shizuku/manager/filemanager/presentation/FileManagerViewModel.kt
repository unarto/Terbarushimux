package moe.shizuku.manager.filemanager.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.shizuku.manager.filemanager.data.FileManagerRepositoryImpl
import moe.shizuku.manager.filemanager.domain.FileItem
import moe.shizuku.manager.filemanager.domain.FileManagerRepository
import java.io.File

enum class SortOption { NAME, SIZE, DATE, EXTENSION }
enum class SortOrder { ASCENDING, DESCENDING }

data class FileManagerState(
    val currentPath: String = "home",
    val items: List<FileItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isExecutingScript: Boolean = false,
    val scriptOutput: List<String> = emptyList(),
    val sortOption: SortOption = SortOption.NAME,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val selectedItems: Set<FileItem> = emptySet(),
    val isGridView: Boolean = false,
    val pinnedFolders: Set<String> = emptySet(),
    val showHiddenFiles: Boolean = false,
    val showMediaThumbnails: Boolean = true,
    val trashItems: List<moe.shizuku.manager.filemanager.domain.TrashItem> = emptyList(),
    val isTrashLoading: Boolean = false,
    val trashConflicts: List<moe.shizuku.manager.filemanager.domain.TrashItem> = emptyList(),
    val pendingRestoreIds: List<String> = emptyList(),
    val confirmBeforeDelete: Boolean = true,
    val pendingDeletePaths: List<String> = emptyList(),
    val isPkgListLoading: Boolean = false,
    val masterPkgList: List<String> = emptyList(),
    val selectedCustomPackages: Set<String> = emptySet(),
    val executeShInBackground: Boolean = false,
    val useRootForTermux: Boolean = false,
    val useShizukuForTermux: Boolean = false,
    val termuxPrefixPath: String = "/data/data/com.termux/files/usr",
    val termuxHomePath: String = "/data/data/com.termux/files/home",
    val backupDirectoryPath: String = "/sdcard/"
) {
    val filteredItems: List<FileItem>
        get() {
            var filtered = if (searchQuery.isBlank()) items else items.filter { it.name.contains(searchQuery, ignoreCase = true) }
            if (!showHiddenFiles) {
                filtered = filtered.filter { !it.name.startsWith(".") }
            }
            val sorted = when (sortOption) {
                SortOption.NAME -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                SortOption.SIZE -> filtered.sortedBy { it.size }
                SortOption.DATE -> filtered.sortedBy { it.lastModified }
                SortOption.EXTENSION -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.substringAfterLast('.', "") })
            }
            val ordered = if (sortOrder == SortOrder.ASCENDING) sorted else sorted.reversed()
            return ordered.sortedByDescending { it.isDirectory }
        }
}

class FileManagerViewModel(
    private val repository: FileManagerRepository,
    private val context: android.content.Context
) : ViewModel() {

    private val _state = MutableStateFlow(FileManagerState())
    val state: StateFlow<FileManagerState> = _state.asStateFlow()
    
    private val settingsRepository = moe.shizuku.manager.filemanager.domain.repository.SettingsRepository(context)
    private val trashManager = moe.shizuku.manager.filemanager.data.TrashManager(context)

    init {
        viewModelScope.launch {
            settingsRepository.pinnedFolders.collect { pinned ->
                _state.value = _state.value.copy(pinnedFolders = pinned)
            }
        }
        viewModelScope.launch {
            settingsRepository.showHiddenFiles.collect { showHidden ->
                val prev = _state.value.showHiddenFiles
                _state.value = _state.value.copy(showHiddenFiles = showHidden)
                if (prev != showHidden && _state.value.currentPath != "home") {
                    loadDirectory(_state.value.currentPath)
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.isGridView.collect { isGrid ->
                _state.value = _state.value.copy(isGridView = isGrid)
            }
        }
        viewModelScope.launch {
            settingsRepository.confirmBeforeDelete.collect { confirm ->
                _state.value = _state.value.copy(confirmBeforeDelete = confirm)
            }
        }
        viewModelScope.launch {
            settingsRepository.showMediaThumbnails.collect { showThumbnails ->
                _state.value = _state.value.copy(showMediaThumbnails = showThumbnails)
            }
        }
        viewModelScope.launch {
            settingsRepository.customPackages.collect { packages ->
                _state.value = _state.value.copy(selectedCustomPackages = packages)
            }
        }
        viewModelScope.launch {
            settingsRepository.executeShInBackground.collect { executeShInBackground ->
                _state.value = _state.value.copy(executeShInBackground = executeShInBackground)
            }
        }
        viewModelScope.launch {
            settingsRepository.useRootForTermux.collect { useRootForTermux ->
                _state.value = _state.value.copy(useRootForTermux = useRootForTermux)
            }
        }
        viewModelScope.launch {
            settingsRepository.useShizukuForTermux.collect { useShizukuForTermux ->
                _state.value = _state.value.copy(useShizukuForTermux = useShizukuForTermux)
            }
        }
        viewModelScope.launch {
            settingsRepository.termuxPrefixPath.collect { termuxPrefixPath ->
                _state.value = _state.value.copy(termuxPrefixPath = termuxPrefixPath)
            }
        }
        viewModelScope.launch {
            settingsRepository.termuxHomePath.collect { termuxHomePath ->
                _state.value = _state.value.copy(termuxHomePath = termuxHomePath)
            }
        }
        viewModelScope.launch {
            settingsRepository.backupDirectoryPath.collect { backupDirectoryPath ->
                _state.value = _state.value.copy(backupDirectoryPath = backupDirectoryPath)
            }
        }
        
        loadDirectory(_state.value.currentPath)
    }

    fun toggleExecuteShInBackground() {
        settingsRepository.setExecuteShInBackground(!_state.value.executeShInBackground)
    }

    fun toggleUseRootForTermux() {
        settingsRepository.setUseRootForTermux(!_state.value.useRootForTermux)
    }

    fun toggleUseShizukuForTermux() {
        settingsRepository.setUseShizukuForTermux(!_state.value.useShizukuForTermux)
    }

    fun setTermuxPrefixPath(path: String) {
        settingsRepository.setTermuxPrefixPath(path)
    }

    fun setTermuxHomePath(path: String) {
        settingsRepository.setTermuxHomePath(path)
    }

    fun setBackupDirectoryPath(path: String) {
        settingsRepository.setBackupDirectoryPath(path)
    }

    fun backupTermux(context: android.content.Context) {
        val backupDir = _state.value.backupDirectoryPath.removeSuffix("/")
        val cmd = "tar -zcf '$backupDir/termux-backup.tar.gz' -C /data/data/com.termux/files ./home ./usr"
        executeTermuxScript(context, cmd)
    }

    fun restoreTermux(context: android.content.Context) {
        val backupDir = _state.value.backupDirectoryPath.removeSuffix("/")
        val cmd = "tar -zxf '$backupDir/termux-backup.tar.gz' -C /data/data/com.termux/files --recursive-unlink --preserve-permissions"
        executeTermuxScript(context, cmd)
    }

    fun clearPackageData() {
        _state.value = _state.value.copy(masterPkgList = emptyList())
        settingsRepository.clearCustomPackages()
    }

    fun loadMasterPkgList() {
        if (_state.value.masterPkgList.isNotEmpty()) return
        _state.value = _state.value.copy(isPkgListLoading = true)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "/data/data/com.termux/files/usr/bin/pkg list-all"))
                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                val packages = mutableListOf<String>()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val pkgName = line?.substringBefore("/")
                    if (pkgName != null && pkgName.isNotBlank() && !pkgName.startsWith("Listing")) {
                        packages.add(pkgName.trim())
                    }
                }
                process.waitFor()
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _state.value = _state.value.copy(isPkgListLoading = false, masterPkgList = packages.distinct())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _state.value = _state.value.copy(isPkgListLoading = false)
                }
            }
        }
    }

    fun addCustomPackage(pkgName: String) {
        settingsRepository.addCustomPackage(pkgName)
    }

    fun removeCustomPackage(pkgName: String) {
        settingsRepository.removeCustomPackage(pkgName)
    }

    fun toggleShowHiddenFiles() {
        settingsRepository.setShowHiddenFiles(!_state.value.showHiddenFiles)
    }

    fun navigateTo(path: String) {
        loadDirectory(path)
    }

    fun navigateUp() {
        val currentPath = _state.value.currentPath
        if (currentPath == "home") return
        
        if (currentPath == "/storage/emulated/0" || currentPath == "/" || currentPath == "/storage") {
             loadDirectory("home")
             return
        }
        
        val parent = File(currentPath).parent
        if (parent != null) {
            loadDirectory(parent)
        } else {
            loadDirectory("home")
        }
    }

    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun setSortOption(option: SortOption) {
        _state.value = _state.value.copy(sortOption = option)
    }

    fun toggleSortOrder() {
        val newOrder = if (_state.value.sortOrder == SortOrder.ASCENDING) SortOrder.DESCENDING else SortOrder.ASCENDING
        _state.value = _state.value.copy(sortOrder = newOrder)
    }

    fun toggleSelection(item: FileItem) {
        val currentSelection = _state.value.selectedItems.toMutableSet()
        if (currentSelection.contains(item)) {
            currentSelection.remove(item)
        } else {
            currentSelection.add(item)
        }
        _state.value = _state.value.copy(selectedItems = currentSelection)
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedItems = emptySet())
    }

    fun selectAll() {
        _state.value = _state.value.copy(selectedItems = _state.value.filteredItems.toSet())
    }

    fun toggleGridView() {
        settingsRepository.setGridView(!_state.value.isGridView)
    }

    fun togglePinFolder(path: String) {
        settingsRepository.togglePinnedFolder(path)
        if (_state.value.currentPath == "home") {
            loadDirectory("home")
        }
    }

    private fun getStorageStats(path: String): String? {
        try {
            val stat = android.os.StatFs(path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val total = totalBlocks * blockSize
            val available = availableBlocks * blockSize
            
            fun format(bytes: Long): String {
                if (bytes < 1024) return "$bytes B"
                val kb = bytes / 1024.0
                if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
                val mb = kb / 1024.0
                if (mb < 1024) return String.format(java.util.Locale.US, "%.1f MB", mb)
                val gb = mb / 1024.0
                return String.format(java.util.Locale.US, "%.1f GB", gb)
            }
            return "${format(available)} free of ${format(total)}"
        } catch (e: Exception) {
            return null
        }
    }

    private fun loadDirectory(path: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, currentPath = path, searchQuery = "", selectedItems = emptySet())
            
            if (path == "home") {
                val homeItems = mutableListOf(
                    FileItem("Penyimpanan Internal", "/storage/emulated/0", true, 0, 0, "d", extraInfo = getStorageStats("/storage/emulated/0")),
                    FileItem("Kartu SD", "/storage", true, 0, 0, "d"),
                    FileItem("Termux", "/data/data/com.termux/files/home", true, 0, 0, "d", extraInfo = getStorageStats("/data/data/com.termux/files/home")),
                    FileItem("Root", "/", true, 0, 0, "d", extraInfo = getStorageStats("/"))
                )
                
                // Add SAF storages
                val safUris = settingsRepository.safUris.value
                safUris.forEach { uriStr ->
                    try {
                        val uri = android.net.Uri.parse(uriStr)
                        val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
                        if (docFile != null && docFile.isDirectory) {
                            val appName = moe.shizuku.manager.filemanager.utils.SafHelper.getAppNameFromSafUri(context, uri)
                            val cleanPath = moe.shizuku.manager.filemanager.utils.SafHelper.getCleanPathFromSafUri(uri)
                            
                            val folderName = docFile.name ?: "Aplikasi Eksternal"
                            val displayName = if (appName != null) "$appName ($folderName)" else folderName
                            
                            homeItems.add(
                                FileItem(displayName, uriStr, true, 0, 0, "d", extraInfo = cleanPath)
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Add pinned folders
                _state.value.pinnedFolders.forEach { pinnedPath ->
                    val name = if (pinnedPath.startsWith("content://")) {
                        android.net.Uri.decode(pinnedPath).substringAfterLast("/").substringBeforeLast("%3A")
                    } else {
                        pinnedPath.substringAfterLast("/")
                    }
                    homeItems.add(FileItem("📌 $name", pinnedPath, true, 0, 0, "d"))
                }
                
                _state.value = _state.value.copy(isLoading = false, items = homeItems)
                return@launch
            }
            
            try {
                val files = repository.listFiles(path)
                _state.value = _state.value.copy(isLoading = false, items = files)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Unknown error")
            }
        }
    }

    fun addSafStorage(uri: android.net.Uri) {
        // Take persistable URI permission
        val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                             android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        settingsRepository.addSafUri(uri.toString())
        
        if (_state.value.currentPath == "home") {
            loadDirectory("home")
        }
    }

    fun requestDeleteFiles(paths: List<String>) {
        if (_state.value.confirmBeforeDelete) {
            _state.value = _state.value.copy(pendingDeletePaths = paths)
        } else {
            paths.forEach { deleteFile(it) }
            clearSelection()
        }
    }

    fun confirmDelete() {
        val paths = _state.value.pendingDeletePaths
        paths.forEach { deleteFile(it) }
        _state.value = _state.value.copy(pendingDeletePaths = emptyList())
        clearSelection()
    }

    fun cancelDelete() {
        _state.value = _state.value.copy(pendingDeletePaths = emptyList())
    }

    fun deleteFile(path: String) {
        viewModelScope.launch {
            val useRecycleBin = settingsRepository.useRecycleBin.value
            val success = if (useRecycleBin && !path.startsWith("content://")) {
                trashManager.moveToTrash(listOf(path))
            } else {
                repository.delete(path)
            }
            
            if (success) {
                loadDirectory(_state.value.currentPath)
            } else {
                _state.value = _state.value.copy(error = "Failed to delete file")
            }
        }
    }

    fun renameFile(oldPath: String, newPath: String) {
        viewModelScope.launch {
            val success = repository.rename(oldPath, newPath)
            if (success) {
                loadDirectory(_state.value.currentPath)
            } else {
                _state.value = _state.value.copy(error = "Failed to rename file")
            }
        }
    }

    fun copyFile(sourcePath: String, destPath: String) {
        viewModelScope.launch {
            val success = repository.copy(sourcePath, destPath)
            if (success) {
                loadDirectory(_state.value.currentPath)
            } else {
                _state.value = _state.value.copy(error = "Failed to copy file")
            }
        }
    }

    fun moveFile(sourcePath: String, destPath: String) {
        viewModelScope.launch {
            val success = repository.move(sourcePath, destPath)
            if (success) {
                loadDirectory(_state.value.currentPath)
            } else {
                _state.value = _state.value.copy(error = "Failed to move file")
            }
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            val path = if (_state.value.currentPath.endsWith("/")) "${_state.value.currentPath}$name" else "${_state.value.currentPath}/$name"
            val success = repository.createFile(path)
            if (success) {
                loadDirectory(_state.value.currentPath)
            } else {
                _state.value = _state.value.copy(error = "Failed to create file")
            }
        }
    }

    fun createDirectory(name: String) {
        viewModelScope.launch {
            val path = if (_state.value.currentPath.endsWith("/")) "${_state.value.currentPath}$name" else "${_state.value.currentPath}/$name"
            val success = repository.createDirectory(path)
            if (success) {
                loadDirectory(_state.value.currentPath)
            } else {
                _state.value = _state.value.copy(error = "Failed to create directory")
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun executeTermuxScript(context: android.content.Context, command: String) {
        val inBackground = settingsRepository.executeShInBackground.value
        val useRoot = settingsRepository.useRootForTermux.value
        
        // Escape quotes to be safe when passing to su -c
        val finalCommand = if (useRoot) "su -c \"$command\"" else command
        
        if (!inBackground) {
            _state.value = _state.value.copy(isExecutingScript = true, scriptOutput = emptyList())
        }
        
        viewModelScope.launch {
            moe.shizuku.manager.filemanager.data.TermuxScriptExecutor.execute(context, finalCommand, _state.value.currentPath, settingsRepository.useShizukuForTermux.value).collect { line ->
                if (!inBackground) {
                    _state.value = _state.value.copy(scriptOutput = _state.value.scriptOutput + line)
                }
            }
            if (!inBackground) {
                _state.value = _state.value.copy(isExecutingScript = false)
            }
            loadDirectory(_state.value.currentPath)
        }
    }

    fun closeScriptDialog() {
        _state.value = _state.value.copy(scriptOutput = emptyList())
    }

    fun loadTrashItems() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isTrashLoading = true)
            val items = trashManager.getTrashItems()
            _state.value = _state.value.copy(isTrashLoading = false, trashItems = items)
        }
    }

    fun restoreTrashItems(ids: List<String>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isTrashLoading = true)
            val conflicts = trashManager.checkConflicts(ids)
            if (conflicts.isNotEmpty()) {
                _state.value = _state.value.copy(
                    isTrashLoading = false,
                    trashConflicts = conflicts,
                    pendingRestoreIds = ids
                )
                return@launch
            }
            
            val success = trashManager.restore(ids)
            if (!success) {
                _state.value = _state.value.copy(error = "Gagal memulihkan beberapa file.")
            }
            loadTrashItems()
            if (_state.value.currentPath != "home") {
                loadDirectory(_state.value.currentPath)
            }
        }
    }

    fun resolveRestoreConflicts(resolutionMap: Map<String, String>) {
        viewModelScope.launch {
            val ids = _state.value.pendingRestoreIds
            _state.value = _state.value.copy(isTrashLoading = true, trashConflicts = emptyList(), pendingRestoreIds = emptyList())
            val success = trashManager.restore(ids, resolutionMap)
            if (!success) {
                _state.value = _state.value.copy(error = "Gagal memulihkan beberapa file.")
            }
            loadTrashItems()
            if (_state.value.currentPath != "home") {
                loadDirectory(_state.value.currentPath)
            }
        }
    }
    
    fun cancelRestore() {
        _state.value = _state.value.copy(trashConflicts = emptyList(), pendingRestoreIds = emptyList())
    }

    fun deleteTrashItemsPermanently(ids: List<String>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isTrashLoading = true)
            val success = trashManager.deletePermanently(ids)
            if (!success) {
                _state.value = _state.value.copy(error = "Gagal menghapus beberapa file secara permanen.")
            }
            loadTrashItems()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isTrashLoading = true)
            val success = trashManager.emptyTrash()
            if (!success) {
                _state.value = _state.value.copy(error = "Gagal mengosongkan tempat sampah.")
            }
            loadTrashItems()
        }
    }
}

class FileManagerViewModelFactory(private val context: android.content.Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileManagerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FileManagerViewModel(FileManagerRepositoryImpl(context), context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
