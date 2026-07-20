package moe.shizuku.manager.terminal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.termux.terminal.TerminalSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.shizuku.manager.terminal.domain.TerminalRepository

class TerminalViewModel(
    private val repository: TerminalRepository
) : ViewModel() {
    val output = repository.terminalOutput
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val session = repository.terminalSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
