package moe.shizuku.manager.filemanager.domain

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val permissions: String,
    val isSymlink: Boolean = false,
    val extraInfo: String? = null
)
