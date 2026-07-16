package moe.shizuku.manager.terminal.domain

import com.termux.terminal.TerminalSession
import kotlinx.coroutines.flow.StateFlow

interface TerminalRepository {
    val currentSession: StateFlow<TerminalSession?>
    
    fun startSession(arguments: Array<String>, environment: Map<String, String>)
    fun writeInput(data: String)
    fun resizeTerminal(rows: Int, cols: Int)
    fun stopSession()
}
