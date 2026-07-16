package moe.shizuku.manager.filemanager.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import moe.shizuku.manager.filemanager.domain.FileItem
import moe.shizuku.manager.filemanager.domain.FileManagerRepository
import moe.shizuku.manager.filemanager.vfs.NativeFile
import moe.shizuku.manager.filemanager.vfs.SafFile
import moe.shizuku.manager.filemanager.vfs.VFile
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileManagerRepositoryImpl(private val context: Context) : FileManagerRepository {

    private fun getVFile(path: String): VFile? {
        return if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            val docFile = DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)
            docFile?.let { SafFile(context, it) }
        } else {
            NativeFile(File(path))
        }
    }

    override suspend fun listFiles(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileItem>()
        
        if (path.startsWith("content://")) {
            val vFile = getVFile(path)
            if (vFile != null && vFile.isDirectory) {
                vFile.listFiles().forEach { file ->
                    items.add(
                        FileItem(
                            name = file.name,
                            path = file.uriString,
                            isDirectory = file.isDirectory,
                            size = file.length,
                            lastModified = file.lastModified,
                            permissions = if (file.isDirectory) "d" else "-" + (if (file.canRead) "r" else "-") + (if (file.canWrite) "w" else "-"),
                            isSymlink = false
                        )
                    )
                }
            }
        } else {
            if (Shizuku.pingBinder()) {
                try {
                    val script = """
                        cd "$path" || exit 1
                        for f in * .[!.]* ..?*; do
                            if [ -e "${'$'}f" ] || [ -L "${'$'}f" ]; then
                                if [ "${'$'}f" != "*" ] && [ "${'$'}f" != ".[!.]*" ] && [ "${'$'}f" != "..?*" ]; then
                                    stat -c "%A|%s|%Y|%n" "${'$'}f"
                                fi
                            fi
                        done
                    """.trimIndent()
                    
                    val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                    newProcessMethod.isAccessible = true
                    val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", script), null, null) as Process

                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    
                    var line: String? = reader.readLine()
                    while (line != null) {
                        val parts = line.split("|", limit = 4)
                        if (parts.size == 4) {
                            val permissions = parts[0]
                            val size = parts[1].toLongOrNull() ?: 0L
                            val lastModified = (parts[2].toLongOrNull() ?: 0L) * 1000L
                            val name = parts[3]
                            
                            items.add(
                                FileItem(
                                    name = name,
                                    path = if (path.endsWith("/")) "$path$name" else "$path/$name",
                                    isDirectory = permissions.startsWith("d"),
                                    size = size,
                                    lastModified = lastModified,
                                    permissions = permissions,
                                    isSymlink = permissions.startsWith("l")
                                )
                            )
                        }
                        line = reader.readLine()
                    }
                    process.waitFor()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                val vFile = getVFile(path)
                if (vFile != null && vFile.isDirectory) {
                    vFile.listFiles().forEach { file ->
                        items.add(
                            FileItem(
                                name = file.name,
                                path = file.uriString, // ini sama dengan absolutePath kalau NativeFile
                                isDirectory = file.isDirectory,
                                size = file.length,
                                lastModified = file.lastModified,
                                permissions = if (file.isDirectory) "d" else "-" + (if (file.canRead) "r" else "-") + (if (file.canWrite) "w" else "-"),
                                isSymlink = false
                            )
                        )
                    }
                }
            }
        }
        
        return@withContext items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    override suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        if (path.startsWith("content://")) {
            val vFile = getVFile(path)
            return@withContext vFile?.delete() ?: false
        } else {
            if (Shizuku.pingBinder()) {
                try {
                    val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                    newProcessMethod.isAccessible = true
                    val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", "rm -rf '$path'"), null, null) as Process
                    return@withContext process.waitFor() == 0
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext false
                }
            } else {
                return@withContext getVFile(path)?.delete() ?: false
            }
        }
    }

    override suspend fun rename(oldPath: String, newPath: String): Boolean = withContext(Dispatchers.IO) {
        if (oldPath.startsWith("content://")) {
            val vFile = getVFile(oldPath)
            return@withContext (vFile?.renameTo(newPath) != null)
        } else {
            if (Shizuku.pingBinder()) {
                try {
                    val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                    newProcessMethod.isAccessible = true
                    val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", "mv '$oldPath' '$newPath'"), null, null) as Process
                    return@withContext process.waitFor() == 0
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext false
                }
            } else {
                return@withContext (getVFile(oldPath)?.renameTo(newPath) != null)
            }
        }
    }

    override suspend fun copy(sourcePath: String, destPath: String): Boolean = withContext(Dispatchers.IO) {
        if (sourcePath.startsWith("content://") || destPath.startsWith("content://")) {
            try {
                val srcFile = getVFile(sourcePath)
                val destFile = getVFile(destPath)
                
                if (srcFile != null && destFile != null && destFile.isDirectory) {
                    val newFile = destFile.createFile("application/octet-stream", srcFile.name)
                    if (newFile != null) {
                        srcFile.openInputStream().use { input ->
                            newFile.openOutputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        return@withContext true
                    }
                }
                return@withContext false
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        } else {
            if (Shizuku.pingBinder()) {
                try {
                    val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                    newProcessMethod.isAccessible = true
                    val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", "cp -r '$sourcePath' '$destPath'"), null, null) as Process
                    return@withContext process.waitFor() == 0
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext false
                }
            } else {
                try {
                    File(sourcePath).copyRecursively(File(destPath), overwrite = true)
                    return@withContext true
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext false
                }
            }
        }
    }

    override suspend fun move(sourcePath: String, destPath: String): Boolean = withContext(Dispatchers.IO) {
        if (sourcePath.startsWith("content://") || destPath.startsWith("content://")) {
            val success = copy(sourcePath, destPath)
            if (success) {
                return@withContext delete(sourcePath)
            }
            return@withContext false
        } else {
            if (Shizuku.pingBinder()) {
                try {
                    val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                    newProcessMethod.isAccessible = true
                    val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", "mv '$sourcePath' '$destPath'"), null, null) as Process
                    return@withContext process.waitFor() == 0
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext false
                }
            } else {
                try {
                    val dest = File(destPath)
                    if (dest.exists()) {
                        dest.deleteRecursively()
                    }
                    return@withContext File(sourcePath).renameTo(dest)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext false
                }
            }
        }
    }

    override suspend fun createFile(path: String): Boolean = withContext(Dispatchers.IO) {
        if (path.startsWith("content://")) {
            return@withContext false 
        } else {
            if (Shizuku.pingBinder()) {
                try {
                    val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                    newProcessMethod.isAccessible = true
                    val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", "touch '$path'"), null, null) as Process
                    return@withContext process.waitFor() == 0
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext false
                }
            } else {
                try {
                    return@withContext File(path).createNewFile()
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext false
                }
            }
        }
    }

    override suspend fun createDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        if (path.startsWith("content://")) {
             return@withContext false
        } else {
            if (Shizuku.pingBinder()) {
                try {
                    val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                    newProcessMethod.isAccessible = true
                    val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", "mkdir -p '$path'"), null, null) as Process
                    return@withContext process.waitFor() == 0
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext false
                }
            } else {
                try {
                    return@withContext File(path).mkdirs()
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@withContext false
                }
            }
        }
    }

    private val trashManager = TrashManager(context)

    override suspend fun moveToTrash(paths: List<String>): Boolean {
        return trashManager.moveToTrash(paths)
    }

    override suspend fun getTrashItems(): List<moe.shizuku.manager.filemanager.domain.TrashItem> {
        return trashManager.getTrashItems()
    }

    override suspend fun restoreFromTrash(ids: List<String>): Boolean {
        return trashManager.restore(ids)
    }

    override suspend fun deletePermanently(ids: List<String>): Boolean {
        return trashManager.deletePermanently(ids)
    }

    override suspend fun emptyTrash(): Boolean {
        return trashManager.emptyTrash()
    }
}
