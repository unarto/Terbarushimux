package moe.shizuku.manager.terminal.data

import android.content.Context
import java.io.File

class RishEnvironmentManager(private val context: Context) {
    private val binDir = File(context.filesDir, "bin")

    fun prepareRishEnvironment(): Map<String, String> {
        if (!binDir.exists()) binDir.mkdirs()
        val rishFile = File(binDir, "rish")
        val dexFile = File(binDir, "rish_shizuku.dex")

        // Ekspor rish & rish_shizuku.dex dari assets ke files/bin
        context.assets.open("rish").use { input ->
            rishFile.outputStream().use { output -> input.copyTo(output) }
        }
        context.assets.open("rish_shizuku.dex").use { input ->
            dexFile.outputStream().use { output -> input.copyTo(output) }
        }
        
        rishFile.setExecutable(true)

        // Racik environment map untuk mengotomatisasi Shizuku Binder injection
        return mapOf(
            "PATH" to "${binDir.absolutePath}:${System.getenv("PATH")}",
            "SHIZUKU_DEX_PATH" to dexFile.absolutePath,
            "HOME" to context.filesDir.absolutePath
        )
    }
}
