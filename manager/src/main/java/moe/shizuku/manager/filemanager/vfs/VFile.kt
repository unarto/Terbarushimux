package moe.shizuku.manager.filemanager.vfs

import java.io.InputStream
import java.io.OutputStream

/**
 * Abstraksi Virtual File untuk menyeragamkan akses operasi file
 * baik melalui jalur Native (java.io.File) maupun jalur SAF (DocumentFile).
 */
interface VFile {
    val name: String
    val uriString: String
    val isDirectory: Boolean
    val isFile: Boolean
    val length: Long
    val lastModified: Long
    val canRead: Boolean
    val canWrite: Boolean

    fun exists(): Boolean
    fun listFiles(): List<VFile>
    fun delete(): Boolean
    fun renameTo(newName: String): VFile?
    
    // Pembuatan file/folder baru
    fun createFile(mimeType: String, name: String): VFile?
    fun createDirectory(name: String): VFile?
    
    // Operasi stream I/O
    @Throws(java.io.IOException::class)
    fun openInputStream(): InputStream
    @Throws(java.io.IOException::class)
    fun openOutputStream(mode: String = "w"): OutputStream
}
