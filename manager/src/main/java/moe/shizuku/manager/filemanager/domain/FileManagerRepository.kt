package moe.shizuku.manager.filemanager.domain

interface FileManagerRepository {
    suspend fun listFiles(path: String): List<FileItem>
    suspend fun delete(path: String): Boolean // Still available, but maybe we can use moveToTrash instead
    suspend fun rename(oldPath: String, newPath: String): Boolean
    suspend fun copy(sourcePath: String, destPath: String): Boolean
    suspend fun move(sourcePath: String, destPath: String): Boolean
    suspend fun createFile(path: String): Boolean
    suspend fun createDirectory(path: String): Boolean
    
    // Trash / Recycle Bin Operations
    suspend fun moveToTrash(paths: List<String>): Boolean
    suspend fun getTrashItems(): List<TrashItem>
    suspend fun restoreFromTrash(ids: List<String>): Boolean
    suspend fun deletePermanently(ids: List<String>): Boolean
    suspend fun emptyTrash(): Boolean
}
