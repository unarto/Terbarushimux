package moe.shizuku.manager.terminal.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import moe.shizuku.manager.terminal.data.RishEnvironmentManager
import moe.shizuku.manager.terminal.data.TerminalRepositoryImpl
import moe.shizuku.manager.terminal.environment.BootstrapInstaller

class TerminalActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val envManager = RishEnvironmentManager(this)
        val repository = TerminalRepositoryImpl(envManager)
        val bootstrapInstaller = BootstrapInstaller(this)
        
        // Buat manual ViewModel karena tidak pakai Dagger/Hilt
        val viewModel = TerminalViewModel(repository, bootstrapInstaller)

        setContent {
            val isBootstrapInstalled by viewModel.isBootstrapInstalled.collectAsState()
            val bootstrapLogs by viewModel.bootstrapLogs.collectAsState()

            if (isBootstrapInstalled) {
                TerminalScreen(viewModel = viewModel, onBack = { finish() })
            } else {
                BootstrapInstallScreen(
                    logOutput = bootstrapLogs,
                    onBack = { finish() }
                )
            }
        }
    }
}
