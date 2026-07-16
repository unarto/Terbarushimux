package moe.shizuku.manager.filemanager.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.manager.filemanager.domain.TrashItem
import org.json.JSONArray
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class TrashManager(private val context: Context) {

    private val trashDir = "/sdcard/.Trash"
    private val metadataFile = "$trashDir/metadata.json"

    private fun executeCommand(command: String): String {
        if (!Shizuku.pingBinder()) return ""
        try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                output.append(line).append("\n")
                line = reader.readLine()
            }
            process.waitFor()
            return output.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    suspend fun initTrash() = withContext(Dispatchers.IO) {
        val checkAndCreate = """
            if [ ! -d "$trashDir" ]; then
                mkdir -p "$trashDir"
                echo "[]" > "$metadataFile"
            elif [ ! -f "$metadataFile" ]; then
                echo "[]" > "$metadataFile"
            fi
        """.trimIndent()
        executeCommand(checkAndCreate)
    }

    suspend fun moveToTrash(paths: List<String>): Boolean = withContext(Dispatchers.IO) {
        initTrash()
        
        val metadataJson = executeCommand("cat '$metadataFile'")
        val array = if (metadataJson.isNotBlank()) {
            try { JSONArray(metadataJson) } catch (e: Exception) { JSONArray() }
        } else {
            JSONArray()
        }
        
        var success = true
        for (path in paths) {
            if (path.startsWith("content://")) {
                // SAF paths cannot be moved using shell easily, fallback to regular delete
                // You can add proper SAF logic here if needed, for now we skip or just return false
                success = false
                continue
            }
            
            val id = UUID.randomUUID().toString()
            val name = path.substringAfterLast("/")
            val isDir = executeCommand("if [ -d '$path' ]; then echo 'true'; else echo 'false'; fi").trim() == "true"
            
            val destPath = "$trashDir/$id"
            
            executeCommand("mv '$path' '$destPath' 2>&1")
            
            // Check if successful
            val moved = executeCommand("if [ -e '$destPath' ]; then echo 'true'; else echo 'false'; fi").trim() == "true"
            
            if (moved) {
                val item = JSONObject().apply {
                    put("id", id)
                    put("originalPath", path)
                    put("name", name)
                    put("deletedAt", System.currentTimeMillis())
                    put("isDirectory", isDir)
                }
                array.put(item)
            } else {
                success = false
            }
        }
        
        val newMetadata = array.toString()
        val escapedMetadata = newMetadata.replace("'", "'\\''")
        executeCommand("echo '$escapedMetadata' > '$metadataFile'")
        
        success
    }

    suspend fun getTrashItems(): List<TrashItem> = withContext(Dispatchers.IO) {
        initTrash()
        val metadataJson = executeCommand("cat '$metadataFile'")
        val items = mutableListOf<TrashItem>()
        if (metadataJson.isNotBlank()) {
            try {
                val array = JSONArray(metadataJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    items.add(
                        TrashItem(
                            id = obj.getString("id"),
                            originalPath = obj.getString("originalPath"),
                            name = obj.getString("name"),
                            deletedAt = obj.getLong("deletedAt"),
                            isDirectory = obj.getBoolean("isDirectory")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        items.sortedByDescending { it.deletedAt }
    }

    suspend fun checkConflicts(ids: List<String>): List<TrashItem> = withContext(Dispatchers.IO) {
        val currentItems = getTrashItems()
        val itemsToCheck = currentItems.filter { it.id in ids }
        val conflicts = mutableListOf<TrashItem>()
        for (item in itemsToCheck) {
            val destPath = item.originalPath
            val exists = executeCommand("if [ -e '$destPath' ]; then echo 'true'; else echo 'false'; fi").trim() == "true"
            if (exists) {
                conflicts.add(item)
            }
        }
        conflicts
    }

    suspend fun restore(ids: List<String>, conflictResolution: Map<String, String> = emptyMap()): Boolean = withContext(Dispatchers.IO) {
        val currentItems = getTrashItems().toMutableList()
        val itemsToRestore = currentItems.filter { it.id in ids }
        
        var allSuccess = true
        for (item in itemsToRestore) {
            val srcPath = "$trashDir/${item.id}"
            val destPath = item.originalPath
            
            val destDir = destPath.substringBeforeLast("/")
            executeCommand("mkdir -p '$destDir'")
            
            val exists = executeCommand("if [ -e '$destPath' ]; then echo 'true'; else echo 'false'; fi").trim() == "true"
            
            if (exists) {
                val resolution = conflictResolution[item.id] ?: "RENAME_AUTO"
                if (resolution == "SKIP") {
                    continue
                }
                
                val finalDestPath = if (resolution == "OVERWRITE") {
                    destPath
                } else if (resolution.startsWith("RENAME:")) {
                    val newName = resolution.substringAfter("RENAME:")
                    "$destDir/$newName"
                } else {
                    val nameWithoutExt = item.name.substringBeforeLast(".")
                    val ext = if (item.name.contains(".")) "." + item.name.substringAfterLast(".") else ""
                    val newName = "${nameWithoutExt}_restored_${System.currentTimeMillis()}$ext"
                    "$destDir/$newName"
                }
                
                executeCommand("mv -f '$srcPath' '$finalDestPath' 2>&1")
                val restored = executeCommand("if [ -e '$finalDestPath' ]; then echo 'true'; else echo 'false'; fi").trim() == "true"
                if (restored) {
                    currentItems.remove(item)
                } else {
                    allSuccess = false
                }
            } else {
                executeCommand("mv '$srcPath' '$destPath' 2>&1")
                val restored = executeCommand("if [ -e '$destPath' ]; then echo 'true'; else echo 'false'; fi").trim() == "true"
                if (restored) {
                    currentItems.remove(item)
                } else {
                    allSuccess = false
                }
            }
        }
        
        saveMetadata(currentItems)
        allSuccess
    }

    suspend fun deletePermanently(ids: List<String>): Boolean = withContext(Dispatchers.IO) {
        val currentItems = getTrashItems().toMutableList()
        val itemsToDelete = currentItems.filter { it.id in ids }
        
        for (item in itemsToDelete) {
            val srcPath = "$trashDir/${item.id}"
            executeCommand("rm -rf '$srcPath'")
            currentItems.remove(item)
        }
        
        saveMetadata(currentItems)
        true
    }

    suspend fun emptyTrash(): Boolean = withContext(Dispatchers.IO) {
        executeCommand("rm -rf $trashDir/*")
        saveMetadata(emptyList())
        true
    }

    suspend fun cleanupOldItems(daysOld: Int = 30): Boolean = withContext(Dispatchers.IO) {
        val currentItems = getTrashItems()
        val thresholdTime = System.currentTimeMillis() - (daysOld.toLong() * 24 * 60 * 60 * 1000)
        
        val itemsToDelete = currentItems.filter { it.deletedAt < thresholdTime }
        if (itemsToDelete.isEmpty()) return@withContext true
        
        deletePermanently(itemsToDelete.map { it.id })
    }

    private suspend fun saveMetadata(items: List<TrashItem>) {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("originalPath", item.originalPath)
                put("name", item.name)
                put("deletedAt", item.deletedAt)
                put("isDirectory", item.isDirectory)
            }
            array.put(obj)
        }
        val escapedMetadata = array.toString().replace("'", "'\\''")
        executeCommand("echo '$escapedMetadata' > '$metadataFile'")
    }
}
