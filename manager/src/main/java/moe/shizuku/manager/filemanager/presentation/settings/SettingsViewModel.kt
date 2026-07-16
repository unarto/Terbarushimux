package moe.shizuku.manager.filemanager.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.Flow
import moe.shizuku.manager.filemanager.domain.repository.SettingsRepository

class SettingsViewModel(
    val repository: SettingsRepository
) : ViewModel() {
    val isGridView: Flow<Boolean> = repository.isGridView
    val showHiddenFiles: Flow<Boolean> = repository.showHiddenFiles
    val showMediaThumbnails: Flow<Boolean> = repository.showMediaThumbnails
    val useRecycleBin: Flow<Boolean> = repository.useRecycleBin
    val confirmBeforeDelete: Flow<Boolean> = repository.confirmBeforeDelete
    val conflictResolution: Flow<String> = repository.conflictResolution
    val executeShInBackground: Flow<Boolean> = repository.executeShInBackground
    val useRootForTermux: Flow<Boolean> = repository.useRootForTermux

    fun setGridView(isGrid: Boolean) = repository.setGridView(isGrid)
    fun setShowHiddenFiles(show: Boolean) = repository.setShowHiddenFiles(show)
    fun setShowMediaThumbnails(show: Boolean) = repository.setShowMediaThumbnails(show)
    fun setUseRecycleBin(use: Boolean) = repository.setUseRecycleBin(use)
    fun setConfirmBeforeDelete(confirm: Boolean) = repository.setConfirmBeforeDelete(confirm)
    fun setConflictResolution(resolution: String) = repository.setConflictResolution(resolution)
    fun setExecuteShInBackground(inBackground: Boolean) = repository.setExecuteShInBackground(inBackground)
    fun setUseRootForTermux(useRoot: Boolean) = repository.setUseRootForTermux(useRoot)
    fun clearPinnedFolders() = repository.clearPinnedFolders()
}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(SettingsRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
