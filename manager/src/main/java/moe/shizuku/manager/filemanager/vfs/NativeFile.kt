package moe.shizuku.manager.filemanager.vfs

import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class NativeFile(val file: File) : VFile {
    override val name: String get() = file.name
    override val uriString: String get() = Uri.fromFile(file).toString()
    override val isDirectory: Boolean get() = file.isDirectory
    override val isFile: Boolean get() = file.isFile
    override val length: Long get() = file.length()
    override val lastModified: Long get() = file.lastModified()
    override val canRead: Boolean get() = file.canRead()
    override val canWrite: Boolean get() = file.canWrite()

    override fun exists(): Boolean = file.exists()

    override fun listFiles(): List<VFile> {
        val files = file.listFiles() ?: return emptyList()
        return files.map { NativeFile(it) }
    }

    override fun delete(): Boolean {
        // Hapus rekursif jika directory
        return if (file.isDirectory) file.deleteRecursively() else file.delete()
    }

    override fun renameTo(newName: String): VFile? {
        val newFile = File(file.parentFile, newName)
        return if (file.renameTo(newFile)) NativeFile(newFile) else null
    }

    override fun createFile(mimeType: String, name: String): VFile? {
        val newFile = File(file, name)
        return try {
            if (newFile.createNewFile()) NativeFile(newFile) else null
        } catch (e: Exception) {
            null
        }
    }

    override fun createDirectory(name: String): VFile? {
        val newDir = File(file, name)
        return if (newDir.mkdirs()) NativeFile(newDir) else null
    }

    override fun openInputStream(): InputStream = FileInputStream(file)
    
    override fun openOutputStream(mode: String): OutputStream {
        // Native output stream logic
        val append = mode == "wa" || mode == "a"
        return FileOutputStream(file, append)
    }
}
