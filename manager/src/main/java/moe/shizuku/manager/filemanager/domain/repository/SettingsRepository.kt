package moe.shizuku.manager.filemanager.domain.repository

import android.content.Context
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val mmkv: MMKV = MMKV.mmkvWithID("file_manager_settings", MMKV.MULTI_PROCESS_MODE)
    private val safMmkv: MMKV = MMKV.mmkvWithID("saf_storages", MMKV.MULTI_PROCESS_MODE)
    private val pinnedMmkv: MMKV = MMKV.mmkvWithID("pinned_folders", MMKV.MULTI_PROCESS_MODE)

    // Tampilan & Navigasi
    private val _isGridView = MutableStateFlow(mmkv.decodeBool("is_grid_view", false))
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(mmkv.decodeBool("show_hidden_files", false))
    val showHiddenFiles: StateFlow<Boolean> = _showHiddenFiles.asStateFlow()
    
    private val _showMediaThumbnails = MutableStateFlow(mmkv.decodeBool("show_media_thumbnails", true))
    val showMediaThumbnails: StateFlow<Boolean> = _showMediaThumbnails.asStateFlow()

    // Manajemen & Operasi File
    private val _useRecycleBin = MutableStateFlow(mmkv.decodeBool("use_recycle_bin", true))
    val useRecycleBin: StateFlow<Boolean> = _useRecycleBin.asStateFlow()
    
    private val _confirmBeforeDelete = MutableStateFlow(mmkv.decodeBool("confirm_before_delete", true))
    val confirmBeforeDelete: StateFlow<Boolean> = _confirmBeforeDelete.asStateFlow()

    private val _conflictResolution = MutableStateFlow(mmkv.decodeString("conflict_resolution", "RENAME_AUTO") ?: "RENAME_AUTO")
    val conflictResolution: StateFlow<String> = _conflictResolution.asStateFlow()

    // Eksekusi Lanjut & Ekstensi
    private val _executeShInBackground = MutableStateFlow(mmkv.decodeBool("execute_sh_in_background", false))
    val executeShInBackground: StateFlow<Boolean> = _executeShInBackground.asStateFlow()
    
    private val _useRootForTermux = MutableStateFlow(mmkv.decodeBool("use_root_for_termux", false))
    val useRootForTermux: StateFlow<Boolean> = _useRootForTermux.asStateFlow()

    private val _useShizukuForTermux = MutableStateFlow(mmkv.decodeBool("use_shizuku_for_termux", false))
    val useShizukuForTermux: StateFlow<Boolean> = _useShizukuForTermux.asStateFlow()

    private val _termuxPrefixPath = MutableStateFlow(mmkv.decodeString("termux_prefix_path", "/data/data/com.termux/files/usr") ?: "/data/data/com.termux/files/usr")
    val termuxPrefixPath: StateFlow<String> = _termuxPrefixPath.asStateFlow()

    private val _termuxHomePath = MutableStateFlow(mmkv.decodeString("termux_home_path", "/data/data/com.termux/files/home") ?: "/data/data/com.termux/files/home")
    val termuxHomePath: StateFlow<String> = _termuxHomePath.asStateFlow()

    private val _backupDirectoryPath = MutableStateFlow(mmkv.decodeString("backup_directory_path", "/sdcard/") ?: "/sdcard/")
    val backupDirectoryPath: StateFlow<String> = _backupDirectoryPath.asStateFlow()

    // Text Editor
    private val editorMmkv: MMKV = MMKV.mmkvWithID("text_editor_settings", MMKV.MULTI_PROCESS_MODE)

    private val _editorWordWrap = MutableStateFlow(editorMmkv.decodeBool("editor_word_wrap", false))
    val editorWordWrap: StateFlow<Boolean> = _editorWordWrap.asStateFlow()

    private val _editorFontSize = MutableStateFlow(editorMmkv.decodeInt("editor_font_size", 14))
    val editorFontSize: StateFlow<Int> = _editorFontSize.asStateFlow()
    
    private val _editorTabSize = MutableStateFlow(editorMmkv.decodeInt("editor_tab_size", 4))
    val editorTabSize: StateFlow<Int> = _editorTabSize.asStateFlow()

    private val _editorTheme = MutableStateFlow(editorMmkv.decodeString("editor_theme", "darcula") ?: "darcula")
    val editorTheme: StateFlow<String> = _editorTheme.asStateFlow()

    private val _editorEncoding = MutableStateFlow(editorMmkv.decodeString("editor_encoding", "UTF-8") ?: "UTF-8")
    val editorEncoding: StateFlow<String> = _editorEncoding.asStateFlow()

    private val _editorAutoSave = MutableStateFlow(editorMmkv.decodeBool("editor_auto_save", true))
    val editorAutoSave: StateFlow<Boolean> = _editorAutoSave.asStateFlow()

    private val _editorOpenedFiles = MutableStateFlow(
        editorMmkv.decodeString("editor_opened_files_list", "")?.split("|:|")?.filter { it.isNotEmpty() } ?: emptyList()
    )
    val editorOpenedFiles: StateFlow<List<String>> = _editorOpenedFiles.asStateFlow()

    private val _editorActiveFile = MutableStateFlow(editorMmkv.decodeString("editor_active_file", null))
    val editorActiveFile: StateFlow<String?> = _editorActiveFile.asStateFlow()

    // SAF Uris
    private val _safUris = MutableStateFlow(safMmkv.decodeStringSet("saf_uris", emptySet()) ?: emptySet())
    val safUris: StateFlow<Set<String>> = _safUris.asStateFlow()

    // Pinned Folders
    private val _pinnedFolders = MutableStateFlow(pinnedMmkv.decodeStringSet("pinned", emptySet()) ?: emptySet())
    val pinnedFolders: StateFlow<Set<String>> = _pinnedFolders.asStateFlow()

    // Custom Packages
    private val defaultPackages = setOf("git", "zip unzip", "p7zip", "unrar")
    private val _customPackages = MutableStateFlow(
        mmkv.decodeStringSet("custom_packages", defaultPackages) ?: defaultPackages
    )
    val customPackages: StateFlow<Set<String>> = _customPackages.asStateFlow()

    init {
        // Migration from SharedPreferences to MMKV if needed
        val oldPrefs = context.getSharedPreferences("file_manager_settings", Context.MODE_PRIVATE)
        if (oldPrefs.contains("is_grid_view")) {
            mmkv.importFromSharedPreferences(oldPrefs)
            oldPrefs.edit().clear().apply()
            _isGridView.value = mmkv.decodeBool("is_grid_view", false)
            _showHiddenFiles.value = mmkv.decodeBool("show_hidden_files", false)
            _showMediaThumbnails.value = mmkv.decodeBool("show_media_thumbnails", true)
            _useRecycleBin.value = mmkv.decodeBool("use_recycle_bin", true)
            _confirmBeforeDelete.value = mmkv.decodeBool("confirm_before_delete", true)
            _conflictResolution.value = mmkv.decodeString("conflict_resolution", "RENAME_AUTO") ?: "RENAME_AUTO"
            _executeShInBackground.value = mmkv.decodeBool("execute_sh_in_background", false)
            _useRootForTermux.value = mmkv.decodeBool("use_root_for_termux", false)
        }
        
        val oldSafPrefs = context.getSharedPreferences("saf_storages", Context.MODE_PRIVATE)
        if (oldSafPrefs.contains("saf_uris")) {
            safMmkv.importFromSharedPreferences(oldSafPrefs)
            oldSafPrefs.edit().clear().apply()
            _safUris.value = safMmkv.decodeStringSet("saf_uris", emptySet()) ?: emptySet()
        }
        
        val oldPinnedPrefs = context.getSharedPreferences("pinned_folders", Context.MODE_PRIVATE)
        if (oldPinnedPrefs.contains("pinned")) {
            pinnedMmkv.importFromSharedPreferences(oldPinnedPrefs)
            oldPinnedPrefs.edit().clear().apply()
            _pinnedFolders.value = pinnedMmkv.decodeStringSet("pinned", emptySet()) ?: emptySet()
        }
    }

    fun setGridView(isGrid: Boolean) {
        mmkv.encode("is_grid_view", isGrid)
        _isGridView.value = isGrid
    }

    fun setShowHiddenFiles(show: Boolean) {
        mmkv.encode("show_hidden_files", show)
        _showHiddenFiles.value = show
    }
    
    fun setShowMediaThumbnails(show: Boolean) {
        mmkv.encode("show_media_thumbnails", show)
        _showMediaThumbnails.value = show
    }

    fun setUseRecycleBin(use: Boolean) {
        mmkv.encode("use_recycle_bin", use)
        _useRecycleBin.value = use
    }
    
    fun setConfirmBeforeDelete(confirm: Boolean) {
        mmkv.encode("confirm_before_delete", confirm)
        _confirmBeforeDelete.value = confirm
    }

    fun setConflictResolution(resolution: String) {
        mmkv.encode("conflict_resolution", resolution)
        _conflictResolution.value = resolution
    }

    fun setExecuteShInBackground(inBackground: Boolean) {
        mmkv.encode("execute_sh_in_background", inBackground)
        _executeShInBackground.value = inBackground
    }

    fun setUseRootForTermux(useRoot: Boolean) {
        mmkv.encode("use_root_for_termux", useRoot)
        _useRootForTermux.value = useRoot
    }

    fun setUseShizukuForTermux(useShizuku: Boolean) {
        mmkv.encode("use_shizuku_for_termux", useShizuku)
        _useShizukuForTermux.value = useShizuku
    }

    fun setTermuxPrefixPath(path: String) {
        mmkv.encode("termux_prefix_path", path)
        _termuxPrefixPath.value = path
    }

    fun setTermuxHomePath(path: String) {
        mmkv.encode("termux_home_path", path)
        _termuxHomePath.value = path
    }

    fun setBackupDirectoryPath(path: String) {
        mmkv.encode("backup_directory_path", path)
        _backupDirectoryPath.value = path
    }

    fun setEditorWordWrap(wrap: Boolean) {
        editorMmkv.encode("editor_word_wrap", wrap)
        _editorWordWrap.value = wrap
    }

    fun setEditorFontSize(size: Int) {
        editorMmkv.encode("editor_font_size", size)
        _editorFontSize.value = size
    }
    
    fun setEditorTabSize(size: Int) {
        editorMmkv.encode("editor_tab_size", size)
        _editorTabSize.value = size
    }

    fun setEditorTheme(theme: String) {
        editorMmkv.encode("editor_theme", theme)
        _editorTheme.value = theme
    }

    fun setEditorEncoding(encoding: String) {
        editorMmkv.encode("editor_encoding", encoding)
        _editorEncoding.value = encoding
    }

    fun setEditorAutoSave(autoSave: Boolean) {
        editorMmkv.encode("editor_auto_save", autoSave)
        _editorAutoSave.value = autoSave
    }

    fun addSafUri(uri: String) {
        val current = _safUris.value.toMutableSet()
        current.add(uri)
        safMmkv.encode("saf_uris", current)
        _safUris.value = current
    }

    fun removeSafUri(uri: String) {
        val current = _safUris.value.toMutableSet()
        current.remove(uri)
        safMmkv.encode("saf_uris", current)
        _safUris.value = current
    }

    fun togglePinnedFolder(path: String) {
        val current = _pinnedFolders.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        pinnedMmkv.encode("pinned", current)
        _pinnedFolders.value = current
    }

    fun clearPinnedFolders() {
        pinnedMmkv.removeValueForKey("pinned")
        _pinnedFolders.value = emptySet()
    }

    fun addCustomPackage(pkgName: String) {
        val current = _customPackages.value.toMutableSet()
        current.add(pkgName)
        mmkv.encode("custom_packages", current)
        _customPackages.value = current
    }

    fun removeCustomPackage(pkgName: String) {
        val current = _customPackages.value.toMutableSet()
        current.remove(pkgName)
        mmkv.encode("custom_packages", current)
        _customPackages.value = current
    }

    fun clearCustomPackages() {
        mmkv.encode("custom_packages", defaultPackages)
        _customPackages.value = defaultPackages
    }

    fun addEditorOpenedFile(path: String) {
        val current = _editorOpenedFiles.value.toMutableList()
        if (!current.contains(path)) {
            current.add(path)
            editorMmkv.encode("editor_opened_files_list", current.joinToString("|:|"))
            _editorOpenedFiles.value = current
        }
        setEditorActiveFile(path)
    }

    fun removeEditorOpenedFile(path: String) {
        val current = _editorOpenedFiles.value.toMutableList()
        current.remove(path)
        editorMmkv.encode("editor_opened_files_list", current.joinToString("|:|"))
        _editorOpenedFiles.value = current
        
        if (_editorActiveFile.value == path) {
            setEditorActiveFile(current.lastOrNull())
        }
    }

    fun setEditorActiveFile(path: String?) {
        if (path == null) {
            editorMmkv.removeValueForKey("editor_active_file")
        } else {
            editorMmkv.encode("editor_active_file", path)
        }
        _editorActiveFile.value = path
    }
}
