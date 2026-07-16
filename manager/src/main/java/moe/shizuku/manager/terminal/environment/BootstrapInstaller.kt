package moe.shizuku.manager.terminal.environment

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

class BootstrapInstaller(private val context: Context) {
    
    companion object {
        private const val TAG = "BootstrapInstaller"
        // TODO: Update with actual Shizuku-specific or Termux mirror bootstrap URLs
        private const val BASE_URL = "https://github.com/termux/termux-packages/releases/download/bootstrap-2024.01.01"
    }

    suspend fun isInstalled(): Boolean {
        return withContext(Dispatchers.IO) {
            val bash = File(TermuxEnvironmentManager.getTermuxPrefix(context), "bin/bash")
            bash.exists() && bash.canExecute()
        }
    }

    suspend fun install(onProgress: (String) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            val prefixDir = TermuxEnvironmentManager.getTermuxPrefix(context)
            val homeDir = TermuxEnvironmentManager.getTermuxHome(context)

            if (!prefixDir.exists()) prefixDir.mkdirs()
            if (!homeDir.exists()) homeDir.mkdirs()

            // 1. Download Bootstrap Zip
            val arch = getArch()
            val url = "$BASE_URL/bootstrap-$arch.zip"
            val zipFile = File(context.cacheDir, "bootstrap-$arch.zip")
            
            onProgress("Downloading bootstrap from $url")
            Log.d(TAG, "Downloading bootstrap from $url")
            downloadFile(url, zipFile)

            // 2. Extract Zip via Kotlin
            onProgress("Extracting bootstrap...")
            Log.d(TAG, "Extracting bootstrap...")
            extractZip(zipFile, prefixDir)
            
            // 3. Execute Native Bootstrap via Shizuku
            onProgress("Executing native bootstrap via Shizuku...")
            Log.d(TAG, "Executing native bootstrap via Shizuku...")
            val output = executeNativeBootstrap(prefixDir.absolutePath)
            onProgress(output)
            
            // Cleanup
            zipFile.delete()
            onProgress("Bootstrap installation completed.")
            Log.d(TAG, "Bootstrap installation completed.")
        }
    }

    private fun executeNativeBootstrap(prefixPath: String): String {
        val uid = android.os.Process.myUid()
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val bootstrapExecutable = File(nativeLibDir, "libtermux-bootstrap.so").absolutePath

        val command = arrayOf(
            "sh", "-c",
            "'$bootstrapExecutable' '$prefixPath' $uid $uid"
        )
        
        return try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            Log.d(TAG, "Native bootstrap exited with code $exitCode. Output:\n$output")
            output
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute native bootstrap", e)
            e.message ?: "Unknown error"
        }
    }

    private fun getArch(): String {
        return when (Build.SUPPORTED_ABIS[0]) {
            "arm64-v8a" -> "aarch64"
            "armeabi-v7a" -> "arm"
            "x86_64" -> "x86_64"
            "x86" -> "i686"
            else -> "aarch64"
        }
    }

    private fun downloadFile(urlString: String, dest: File) {
        // Implement robust download logic here
        // For now, it's a stub
        URL(urlString).openStream().use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun extractZip(zipFile: File, destDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(destDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { output ->
                        zis.copyTo(output)
                    }
                }
                entry = zis.nextEntry
            }
        }
    }
}
