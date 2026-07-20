package moe.shizuku.manager.terminal.data

import android.content.Context
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase
import com.termux.terminal.TerminalSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import moe.shizuku.manager.terminal.domain.TerminalRepository
import java.io.File

class TerminalRepositoryImpl(
    private val context: Context,
    private val envManager: RishEnvironmentManager
) : TerminalRepository {
    private val _terminalOutput = MutableStateFlow("")
    override val terminalOutput: StateFlow<String> = _terminalOutput

    private val _terminalSession = MutableStateFlow<TerminalSession?>(null)
    override val terminalSession: StateFlow<TerminalSession?> = _terminalSession

    override fun startSession(arguments: Array<String>, environment: Map<String, String>) {
        if (_terminalSession.value != null) return

        val fullEnv = envManager.prepareRishEnvironment() + environment + mapOf(
            "RISH_APPLICATION_ID" to context.packageName
        )
        val envArray = fullEnv.map { "${it.key}=${it.value}" }.toTypedArray()
        
        val rishFile = File(context.filesDir, "bin/rish")
        
        val sessionClient = object : TermuxTerminalSessionClientBase() {
            override fun onTextChanged(changedSession: TerminalSession) {
                val text = changedSession.emulator?.screen?.transcriptText ?: ""
                _terminalOutput.value = text
            }

            override fun onSessionFinished(finishedSession: TerminalSession) {
                _terminalOutput.value += "\n[Session Finished]"
                _terminalSession.value = null
            }
        }

        val session = TerminalSession(
            "/system/bin/sh",
            context.filesDir.absolutePath,
            arrayOf(rishFile.absolutePath) + arguments,
            envArray,
            1000,
            sessionClient
        )

        _terminalSession.value = session
    }

    override fun writeInput(data: String) {
        val session = _terminalSession.value ?: return
        val bytes = data.toByteArray()
        session.write(bytes, 0, bytes.size)
    }

    override fun resizeTerminal(rows: Int, cols: Int) {
        val session = _terminalSession.value ?: return
        session.updateSize(cols, rows, 0, 0)
    }

    override fun stopSession() {
        val session = _terminalSession.value
        if (session != null) {
            session.finishIfRunning()
            _terminalSession.value = null
        }
    }
}
