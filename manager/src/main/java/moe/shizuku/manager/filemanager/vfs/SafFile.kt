package moe.shizuku.manager.filemanager.vfs

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream
import java.io.OutputStream

class SafFile(private val context: Context, val docFile: DocumentFile) : VFile {
    override val name: String get() = docFile.name ?: ""
    override val uriString: String get() = docFile.uri.toString()
    override val isDirectory: Boolean get() = docFile.isDirectory
    override val isFile: Boolean get() = docFile.isFile
    override val length: Long get() = docFile.length()
    override val lastModified: Long get() = docFile.lastModified()
    override val canRead: Boolean get() = docFile.canRead()
    override val canWrite: Boolean get() = docFile.canWrite()

    override fun exists(): Boolean = docFile.exists()

    override fun listFiles(): List<VFile> {
        return docFile.listFiles().map { SafFile(context, it) }
    }

    override fun delete(): Boolean = docFile.delete()

    override fun renameTo(newName: String): VFile? {
        return if (docFile.renameTo(newName)) this else null
    }

    override fun createFile(mimeType: String, name: String): VFile? {
        val newDoc = docFile.createFile(mimeType, name)
        return newDoc?.let { SafFile(context, it) }
    }

    override fun createDirectory(name: String): VFile? {
        val newDoc = docFile.createDirectory(name)
        return newDoc?.let { SafFile(context, it) }
    }

    override fun openInputStream(): InputStream {
        return context.contentResolver.openInputStream(docFile.uri) 
            ?: throw java.io.FileNotFoundException("Unable to open input stream for ${docFile.uri}")
    }

    override fun openOutputStream(mode: String): OutputStream {
        // mode: "w" for write, "wa" for write append, "wt" for write truncate
        val androidMode = if (mode == "wt") "rwt" else mode
        return context.contentResolver.openOutputStream(docFile.uri, androidMode)
            ?: throw java.io.FileNotFoundException("Unable to open output stream for ${docFile.uri}")
    }
}
