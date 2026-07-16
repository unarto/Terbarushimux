package moe.shizuku.manager.terminal.presentation

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.view.TerminalView
import moe.shizuku.manager.terminal.data.TerminalSessionClientImpl
import moe.shizuku.manager.terminal.data.TerminalViewClientImpl
import moe.shizuku.manager.terminal.presentation.components.QuickPathBar
import moe.shizuku.manager.terminal.presentation.components.TerminalKeyMapper
import moe.shizuku.manager.terminal.presentation.components.TerminalShortcutBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    onBack: () -> Unit
) {
    val sessionState by viewModel.session.collectAsState()
    
    var isCtrlDown by remember { mutableStateOf(false) }
    var isAltDown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initTerminal()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal Shizuku") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black) // Terminal mostly expects black background
        ) {
            AndroidView(
                factory = { context ->
                    TerminalView(context, null).apply {
                        val density = context.resources.displayMetrics.density
                        setTextSize((14 * density).toInt()) // Scaled text size
                        
                        isFocusable = true
                        isFocusableInTouchMode = true
                        
                        setTerminalViewClient(TerminalViewClientImpl(
                            onSingleTap = {
                                requestFocus()
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                            },
                            readCtrl = { isCtrlDown },
                            readAlt = { isAltDown },
                            onKeyConsumed = {
                                isCtrlDown = false
                                isAltDown = false
                            }
                        ))

                        sessionState?.let { 
                            attachSession(it)
                            it.updateTerminalSessionClient(TerminalSessionClientImpl {
                                onScreenUpdated()
                            })
                            requestFocus()
                            post {
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                update = { view ->
                    if (sessionState != null && view.currentSession != sessionState) {
                        view.attachSession(sessionState)
                        sessionState?.updateTerminalSessionClient(TerminalSessionClientImpl {
                            view.onScreenUpdated()
                        })
                        view.requestFocus()
                        view.post {
                            val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                        }
                    }
                }
            )
            
            QuickPathBar(
                onPathSelected = { pathCommand ->
                    sessionState?.write(pathCommand.toByteArray(), 0, pathCommand.toByteArray().size)
                }
            )
            
            TerminalShortcutBar(
                isCtrlDown = isCtrlDown,
                isAltDown = isAltDown,
                onCtrlToggle = { isCtrlDown = !isCtrlDown },
                onAltToggle = { isAltDown = !isAltDown },
                onKey = { key ->
                    TerminalKeyMapper.handleKey(key, sessionState, { isCtrlDown = !isCtrlDown }, { isAltDown = !isAltDown })
                }
            )
        }
    }
}

