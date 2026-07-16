package moe.shizuku.manager.terminal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termux.terminal.TerminalSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.shizuku.manager.terminal.domain.TerminalRepository
import moe.shizuku.manager.terminal.environment.BootstrapInstaller

class TerminalViewModel(
    private val repository: TerminalRepository,
    private val bootstrapInstaller: BootstrapInstaller
) : ViewModel() {

    val session: StateFlow<TerminalSession?> = repository.currentSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isBootstrapInstalled = MutableStateFlow(true)
    val isBootstrapInstalled: StateFlow<Boolean> = _isBootstrapInstalled.asStateFlow()

    private val _bootstrapLogs = MutableStateFlow("Initializing...")
    val bootstrapLogs: StateFlow<String> = _bootstrapLogs.asStateFlow()

    init {
        checkBootstrap()
    }

    private fun checkBootstrap() {
        viewModelScope.launch {
            if (!bootstrapInstaller.isInstalled()) {
                _isBootstrapInstalled.value = false
                installBootstrap()
            } else {
                _isBootstrapInstalled.value = true
                initTerminal()
            }
        }
    }

    private suspend fun installBootstrap() {
        bootstrapInstaller.install { log ->
            _bootstrapLogs.value = _bootstrapLogs.value + "\n" + log
        }
        _isBootstrapInstalled.value = true
        initTerminal()
    }

    fun initTerminal() {
        viewModelScope.launch {
            repository.startSession(emptyArray(), emptyMap())
        }
    }

    fun sendCommand(cmd: String) {
        viewModelScope.launch {
            repository.writeInput(cmd)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopSession()
    }
}
