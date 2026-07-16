package moe.shizuku.manager.filemanager.saf

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.shizuku.manager.filemanager.vfs.SafFile
import org.json.JSONArray
import org.json.JSONObject

class SdcardBridge(
    private val context: Context,
    private val webView: WebView,
    private val coroutineScope: CoroutineScope,
    private val requestStorageAccess: (String, String, String) -> Unit
) {

    private fun invokeCallback(callbackId: String, data: String) {
        val js = "javascript:if(window.executeCallback) { window.executeCallback('$callbackId', $data); }"
        webView.post {
            webView.evaluateJavascript(js, null)
        }
    }

    private fun invokeError(callbackId: String, message: String) {
        val errorJson = JSONObject().apply { put("error", message) }.toString()
        invokeCallback(callbackId, errorJson)
    }

    private fun getVFile(uriString: String): SafFile? {
        val uri = Uri.parse(uriString)
        val docFile = DocumentFile.fromSingleUri(context, uri) ?: DocumentFile.fromTreeUri(context, uri)
        return docFile?.let { SafFile(context, it) }
    }

    private fun getVFileTree(uriString: String): SafFile? {
        val uri = Uri.parse(uriString)
        val docFile = DocumentFile.fromTreeUri(context, uri)
        return docFile?.let { SafFile(context, it) }
    }

    @JavascriptInterface
    fun read(uriString: String, successCallbackId: String, errorCallbackId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val file = getVFile(uriString)
                if (file != null && file.exists()) {
                    file.openInputStream().use { inputStream ->
                        val bytes = inputStream.readBytes()
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        val result = JSONObject().apply {
                            put("data", base64)
                            put("encoding", "base64")
                        }.toString()
                        invokeCallback(successCallbackId, result)
                    }
                } else {
                    invokeError(errorCallbackId, "File not found")
                }
            } catch (e: Exception) {
                invokeError(errorCallbackId, e.message ?: "Unknown error")
            }
        }
    }

    @JavascriptInterface
    fun listDir(uriString: String, successCallbackId: String, errorCallbackId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val dir = getVFileTree(uriString)
                if (dir != null && dir.isDirectory) {
                    val children = dir.listFiles()
                    val array = JSONArray()
                    for (child in children) {
                        val obj = JSONObject().apply {
                            put("name", child.name)
                            put("url", child.uriString)
                            put("isDirectory", child.isDirectory)
                            put("isFile", child.isFile)
                            put("size", child.length)
                            put("canRead", child.canRead)
                            put("canWrite", child.canWrite)
                            put("modifiedDate", child.lastModified)
                        }
                        array.put(obj)
                    }
                    invokeCallback(successCallbackId, array.toString())
                } else {
                    invokeError(errorCallbackId, "Directory not found")
                }
            } catch (e: Exception) {
                invokeError(errorCallbackId, e.message ?: "Unknown error")
            }
        }
    }

    @JavascriptInterface
    fun delete(uriString: String, successCallbackId: String, errorCallbackId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val file = getVFile(uriString)
                if (file != null && file.exists()) {
                    if (file.delete()) {
                        invokeCallback(successCallbackId, "{}")
                    } else {
                        invokeError(errorCallbackId, "Failed to delete file")
                    }
                } else {
                    invokeError(errorCallbackId, "File not found")
                }
            } catch (e: Exception) {
                invokeError(errorCallbackId, e.message ?: "Unknown error")
            }
        }
    }

    @JavascriptInterface
    fun renameFile(uriString: String, newName: String, successCallbackId: String, errorCallbackId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val file = getVFile(uriString)
                if (file != null && file.exists()) {
                    val newFile = file.renameTo(newName)
                    if (newFile != null) {
                        invokeCallback(successCallbackId, "{}")
                    } else {
                        invokeError(errorCallbackId, "Failed to rename file")
                    }
                } else {
                    invokeError(errorCallbackId, "File not found")
                }
            } catch (e: Exception) {
                invokeError(errorCallbackId, e.message ?: "Unknown error")
            }
        }
    }

    @JavascriptInterface
    fun stat(uriString: String, successCallbackId: String, errorCallbackId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val file = getVFile(uriString)
                if (file != null && file.exists()) {
                    val obj = JSONObject().apply {
                        put("name", file.name)
                        put("url", file.uriString)
                        put("isDirectory", file.isDirectory)
                        put("isFile", file.isFile)
                        put("size", file.length)
                        put("canRead", file.canRead)
                        put("canWrite", file.canWrite)
                        put("modifiedDate", file.lastModified)
                        put("exists", true)
                    }
                    invokeCallback(successCallbackId, obj.toString())
                } else {
                    val obj = JSONObject().apply {
                        put("exists", false)
                    }
                    invokeCallback(successCallbackId, obj.toString())
                }
            } catch (e: Exception) {
                invokeError(errorCallbackId, e.message ?: "Unknown error")
            }
        }
    }

    @JavascriptInterface
    fun formatUri(uriString: String, successCallbackId: String, errorCallbackId: String) {
        invokeCallback(successCallbackId, "\"$uriString\"")
    }

    @JavascriptInterface
    fun createFile(parentUriString: String, filename: String, successCallbackId: String, errorCallbackId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val parentDir = getVFileTree(parentUriString)
                if (parentDir != null && parentDir.isDirectory) {
                    val mimeType = "text/plain"
                    val newFile = parentDir.createFile(mimeType, filename)
                    if (newFile != null) {
                        invokeCallback(successCallbackId, "\"${newFile.uriString}\"")
                    } else {
                        invokeError(errorCallbackId, "Failed to create file")
                    }
                } else {
                    invokeError(errorCallbackId, "Parent directory not found")
                }
            } catch (e: Exception) {
                invokeError(errorCallbackId, e.message ?: "Unknown error")
            }
        }
    }

    @JavascriptInterface
    fun createDir(parentUriString: String, dirname: String, successCallbackId: String, errorCallbackId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val parentDir = getVFileTree(parentUriString)
                if (parentDir != null && parentDir.isDirectory) {
                    val newDir = parentDir.createDirectory(dirname)
                    if (newDir != null) {
                        invokeCallback(successCallbackId, "\"${newDir.uriString}\"")
                    } else {
                        invokeError(errorCallbackId, "Failed to create directory")
                    }
                } else {
                    invokeError(errorCallbackId, "Parent directory not found")
                }
            } catch (e: Exception) {
                invokeError(errorCallbackId, e.message ?: "Unknown error")
            }
        }
    }

    @JavascriptInterface
    fun listStorages(successCallbackId: String, errorCallbackId: String) {
        invokeCallback(successCallbackId, "[]")
    }

    @JavascriptInterface
    fun getPath(uriString: String, filename: String, successCallbackId: String, errorCallbackId: String) {
        invokeCallback(successCallbackId, "\"$uriString%2F$filename\"")
    }

    @JavascriptInterface
    fun write(uriString: String, dataBase64: String, successCallbackId: String, errorCallbackId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val file = getVFile(uriString)
                if (file != null) {
                    file.openOutputStream("wt").use { outputStream ->
                        val bytes = Base64.decode(dataBase64, Base64.DEFAULT)
                        outputStream.write(bytes)
                        invokeCallback(successCallbackId, "{}")
                    }
                } else {
                    invokeError(errorCallbackId, "File not found")
                }
            } catch (e: Exception) {
                invokeError(errorCallbackId, e.message ?: "Unknown error")
            }
        }
    }

    @JavascriptInterface
    fun copy(srcUriString: String, destUriString: String, successCallbackId: String, errorCallbackId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val srcFile = getVFile(srcUriString)
                val destDir = getVFileTree(destUriString)

                if (srcFile != null && destDir != null && destDir.isDirectory) {
                    // Extract mime type if possible, though SafFile doesn't expose it directly yet.
                    // We can use a default for now.
                    val mimeType = "application/octet-stream"
                    val newFile = destDir.createFile(mimeType, srcFile.name.ifEmpty { "copied_file" })
                    if (newFile != null) {
                        srcFile.openInputStream().use { inputStream ->
                            newFile.openOutputStream("wt").use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        invokeCallback(successCallbackId, "\"${newFile.uriString}\"")
                    } else {
                        invokeError(errorCallbackId, "Failed to create destination file")
                    }
                } else {
                    invokeError(errorCallbackId, "Source or destination invalid")
                }
            } catch (e: Exception) {
                invokeError(errorCallbackId, e.message ?: "Unknown error")
            }
        }
    }

    @JavascriptInterface
    fun move(srcUriString: String, destUriString: String, successCallbackId: String, errorCallbackId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val srcFile = getVFile(srcUriString)
                val destDir = getVFileTree(destUriString)

                if (srcFile != null && destDir != null && destDir.isDirectory) {
                    val mimeType = "application/octet-stream"
                    val newFile = destDir.createFile(mimeType, srcFile.name.ifEmpty { "moved_file" })
                    if (newFile != null) {
                        srcFile.openInputStream().use { inputStream ->
                            newFile.openOutputStream("wt").use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        srcFile.delete()
                        invokeCallback(successCallbackId, "\"${newFile.uriString}\"")
                    } else {
                        invokeError(errorCallbackId, "Failed to create destination file")
                    }
                } else {
                    invokeError(errorCallbackId, "Source or destination invalid")
                }
            } catch (e: Exception) {
                invokeError(errorCallbackId, e.message ?: "Unknown error")
            }
        }
    }

    @JavascriptInterface
    fun getStorageAccessPermission(uuid: String, successCallbackId: String, errorCallbackId: String) {
        requestStorageAccess(uuid, successCallbackId, errorCallbackId)
    }
}
