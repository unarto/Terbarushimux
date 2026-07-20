package moe.shizuku.manager.terminal.domain

import kotlinx.coroutines.flow.StateFlow
import com.termux.terminal.TerminalSession

interface TerminalRepository {
    val terminalOutput: StateFlow<String>
    val terminalSession: StateFlow<TerminalSession?>
    fun startSession(arguments: Array<String>, environment: Map<String, String>)
    fun writeInput(data: String)
    fun resizeTerminal(rows: Int, cols: Int)
    fun stopSession()
}
