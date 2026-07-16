package moe.shizuku.manager.filemanager.domain

data class TrashItem(
    val id: String,
    val originalPath: String,
    val name: String,
    val deletedAt: Long,
    val isDirectory: Boolean
)
