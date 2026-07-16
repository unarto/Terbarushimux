package moe.shizuku.manager.terminal.data

import android.content.Context
import moe.shizuku.manager.terminal.environment.TermuxEnvironmentManager
import java.io.File

class RishEnvironmentManager(val context: Context) {

    fun prepareRishEnvironment(): Map<String, String> {
        val termuxPrefix = TermuxEnvironmentManager.getTermuxPrefix(context)
        val binDir = File(termuxPrefix, "bin")
        if (!binDir.exists()) binDir.mkdirs()

        val rishFile = File(binDir, "rish")
        val dexFile = File(binDir, "rish_shizuku.dex")

        // Ekspor rish & rish_shizuku.dex dari assets ke termux usr/bin
        context.assets.open("rish").use { input ->
            rishFile.outputStream().use { output -> input.copyTo(output) }
        }
        context.assets.open("rish_shizuku.dex").use { input ->
            dexFile.outputStream().use { output -> input.copyTo(output) }
        }

        rishFile.setExecutable(true)

        // Racik environment map untuk mengotomatisasi Shizuku Binder injection
        return mapOf(
            "SHIZUKU_DEX_PATH" to dexFile.absolutePath
        )
    }
}
