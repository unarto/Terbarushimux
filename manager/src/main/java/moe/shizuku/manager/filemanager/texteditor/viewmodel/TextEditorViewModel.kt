package moe.shizuku.manager.filemanager.texteditor.viewmodel

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shizuku.manager.filemanager.vfs.NativeFile
import moe.shizuku.manager.filemanager.vfs.SafFile
import moe.shizuku.manager.filemanager.vfs.VFile
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

sealed class EditorState {
    object Idle : EditorState()
    object Loading : EditorState()
    data class Success(val content: String, val mimeType: String) : EditorState()
    data class Error(val message: String) : EditorState()
}

class TextEditorViewModel : ViewModel() {
    private val _editorState = MutableStateFlow<EditorState>(EditorState.Idle)
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private var currentFile: VFile? = null

    fun initFile(context: Context, path: String, encoding: String = "UTF-8") {
        currentFile = getVFile(context, path)
        loadFile(encoding)
    }

    private fun getVFile(context: Context, path: String): VFile? {
        return if (path.startsWith("content://")) {
            val uri = Uri.parse(path)
            val docFile = DocumentFile.fromTreeUri(context, uri) ?: DocumentFile.fromSingleUri(context, uri)
            docFile?.let { SafFile(context, it) }
        } else {
            NativeFile(File(path))
        }
    }

    fun loadFile(encoding: String = "UTF-8") {
        val file = currentFile ?: return
        _editorState.value = EditorState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val charset = try {
                    java.nio.charset.Charset.forName(encoding)
                } catch (e: Exception) {
                    StandardCharsets.UTF_8
                }
                val content = file.openInputStream().use { inputStream ->
                    InputStreamReader(inputStream, charset).use { reader ->
                        reader.readText()
                    }
                }
                
                val ext = file.name.substringAfterLast('.', "").lowercase()
                val mimeType = when (ext) {
                    "js" -> "javascript"
                    "ts" -> "typescript"
                    "json" -> "json"
                    "kt", "java" -> "java"
                    "cpp", "c", "h", "hpp" -> "cpp"
                    "cs" -> "csharp"
                    "html", "htm" -> "html"
                    "xml", "svg" -> "xml"
                    "css" -> "css"
                    "md", "markdown" -> "markdown"
                    "py" -> "python"
                    "php" -> "php"
                    "rs", "rust" -> "rust"
                    else -> "text/plain"
                }

                withContext(Dispatchers.Main) {
                    _editorState.value = EditorState.Success(content, mimeType)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _editorState.value = EditorState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun saveAs(context: Context, newPath: String, content: String, encoding: String = "UTF-8", onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val charset = try {
                    java.nio.charset.Charset.forName(encoding)
                } catch (e: Exception) {
                    StandardCharsets.UTF_8
                }
                
                val newFile = getVFile(context, newPath)
                if (newFile == null) {
                    withContext(Dispatchers.Main) { onComplete(false) }
                    return@launch
                }
                
                newFile.openOutputStream("wt").use { outputStream ->
                    OutputStreamWriter(outputStream, charset).use { writer ->
                        writer.write(content)
                    }
                }
                
                // Update current file
                withContext(Dispatchers.Main) {
                    currentFile = newFile
                    onComplete(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun saveFile(content: String, encoding: String = "UTF-8", onComplete: (Boolean) -> Unit) {
        val file = currentFile
        if (file == null) {
            onComplete(false)
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val charset = try {
                    java.nio.charset.Charset.forName(encoding)
                } catch (e: Exception) {
                    StandardCharsets.UTF_8
                }
                file.openOutputStream("wt").use { outputStream ->
                    OutputStreamWriter(outputStream, charset).use { writer ->
                        writer.write(content)
                    }
                }
                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun renameFile(context: Context, newName: String, onComplete: (Boolean, String?) -> Unit) {
        val file = currentFile
        if (file == null) {
            onComplete(false, null)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newFile = file.renameTo(newName)
                withContext(Dispatchers.Main) {
                    if (newFile != null) {
                        currentFile = newFile
                        onComplete(true, newFile.uriString)
                    } else {
                        onComplete(false, null)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, null)
                }
            }
        }
    }
}
