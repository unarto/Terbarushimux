package moe.shizuku.manager.terminal.data

import com.termux.terminal.TerminalSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import moe.shizuku.manager.terminal.domain.TerminalRepository
import moe.shizuku.manager.terminal.environment.TermuxEnvironmentManager
import java.io.File

class TerminalRepositoryImpl(
    private val envManager: RishEnvironmentManager
) : TerminalRepository {

    private val _currentSession = MutableStateFlow<TerminalSession?>(null)
    override val currentSession: StateFlow<TerminalSession?> = _currentSession

    override fun startSession(arguments: Array<String>, environment: Map<String, String>) {
        val rishEnv = envManager.prepareRishEnvironment()
        val termuxEnv = TermuxEnvironmentManager.getEnvironmentVariables(envManager.context)
        
        // Merge environments (Rish paths + Termux paths)
        val fullEnv = mutableMapOf<String, String>()
        fullEnv.putAll(termuxEnv)
        fullEnv.putAll(rishEnv)
        
        // Tambahkan PATH sistem Android agar fallback sh/toolbox tersedia
        val systemPath = System.getenv("PATH") ?: ""
        val combinedPath = "${termuxEnv["PATH"]}:$systemPath"
        fullEnv["PATH"] = combinedPath

        val envArray = fullEnv.map { "${it.key}=${it.value}" }.toTypedArray()
        
        val client = TerminalSessionClientImpl {
            // onScreenUpdated callback can be handled by TerminalView automatically in compose
        }
        
        val termuxPrefix = TermuxEnvironmentManager.getTermuxPrefix(envManager.context).absolutePath
        val bashPath = "$termuxPrefix/bin/bash"
        val rishPath = "$termuxPrefix/bin/rish"

        val session = TerminalSession(
            "/system/bin/sh",
            TermuxEnvironmentManager.getTermuxHome(envManager.context).absolutePath,
            arrayOf("-c", "$rishPath $bashPath -l"),
            envArray,
            1000,
            client
        )
        
        _currentSession.value = session
    }

    override fun writeInput(data: String) {
        _currentSession.value?.write(data.toByteArray(), 0, data.toByteArray().size)
    }

    override fun resizeTerminal(rows: Int, cols: Int) {
        _currentSession.value?.updateSize(cols, rows)
    }

    override fun stopSession() {
        _currentSession.value?.finishIfRunning()
        _currentSession.value = null
    }
}
