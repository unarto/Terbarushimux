package moe.shizuku.manager.terminal.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.view.TerminalView
import com.termux.shared.termux.terminal.TermuxTerminalViewClientBase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    onBack: () -> Unit
) {
    val sessionState by viewModel.session.collectAsState()

    // Memicu inisialisasi terminal sekali saja dengan 'LaunchedEffect'
    LaunchedEffect(Unit) {
        viewModel.initTerminal()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal Shizuku (Termux Powered)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Membungkus TerminalView Termux menggunakan AndroidView untuk performa maksimal
            AndroidView(
                factory = { context ->
                    TerminalView(context, null).apply {
                        val viewClient = object : TermuxTerminalViewClientBase() {
                            // Menggunakan implementasi default Termux
                        }
                        setTerminalViewClient(viewClient)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                update = { view ->
                    val session = sessionState
                    if (session != null) {
                        view.attachSession(session)
                        view.requestFocus()
                    }
                }
            )
        }
    }
}
