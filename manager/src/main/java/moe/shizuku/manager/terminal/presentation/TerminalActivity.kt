package moe.shizuku.manager.terminal.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import moe.shizuku.manager.terminal.data.RishEnvironmentManager
import moe.shizuku.manager.terminal.data.TerminalRepositoryImpl
import moe.shizuku.manager.terminal.domain.TerminalRepository

class TerminalActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository: TerminalRepository = TerminalRepositoryImpl(this, RishEnvironmentManager(this))
        val viewModel = TerminalViewModel(repository)
        
        setContent {
            TerminalScreen(
                viewModel = viewModel,
                onBack = { finish() }
            )
        }
    }
}
