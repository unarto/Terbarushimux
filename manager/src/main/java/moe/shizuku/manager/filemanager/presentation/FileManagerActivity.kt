package moe.shizuku.manager.filemanager.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import moe.shizuku.manager.app.ThemeHelper

class FileManagerActivity : ComponentActivity() {

    private val viewModel: FileManagerViewModel by viewModels { FileManagerViewModelFactory(applicationContext) }
    private val textEditorViewModel: moe.shizuku.manager.filemanager.texteditor.viewmodel.TextEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        var initialScreen = "home"
        var initialFileToEdit: String? = null
        
        if (intent?.action == "moe.shizuku.manager.action.OPEN_FILE") {
            val path = intent.getStringExtra("file_path")
            if (path != null) {
                initialScreen = "text_editor"
                initialFileToEdit = path
            }
        } else if (intent?.action == android.content.Intent.ACTION_VIEW && intent.data != null) {
            // Might be a content uri, but we handle file uri here if possible
            val uriPath = intent.data?.path
            if (uriPath != null) {
                initialScreen = "text_editor"
                initialFileToEdit = uriPath
            }
        }
        
        setContent {
            var currentScreen by remember { mutableStateOf(initialScreen) } // "home", "recycle_bin", or "text_editor"
            var fileToEdit by remember { mutableStateOf<String?>(initialFileToEdit) }
            
            when (currentScreen) {
                "home" -> {
                    FileManagerScreen(
                        viewModel = viewModel,
                        onBack = { finish() },
                        onNavigateToRecycleBin = { currentScreen = "recycle_bin" },
                        onOpenFile = { file ->
                            fileToEdit = file.path
                            currentScreen = "text_editor"
                        }
                    )
                }
                "recycle_bin" -> {
                    RecycleBinScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = "home" }
                    )
                }
                "text_editor" -> {
                    moe.shizuku.manager.filemanager.texteditor.presentation.TextEditorScreen(
                        initialFilePath = fileToEdit,
                        viewModel = textEditorViewModel,
                        onBack = { 
                            currentScreen = "home"
                            fileToEdit = null
                        }
                    )
                }
            }
        }
    }
}
