package moe.shizuku.manager.terminal.environment

import android.content.Context
import java.io.File

object TermuxEnvironmentManager {

    fun getTermuxFilesDir(context: Context): File {
        return File(context.filesDir, "termux")
    }

    fun getTermuxPrefix(context: Context): File {
        return File(getTermuxFilesDir(context), "usr")
    }

    fun getTermuxHome(context: Context): File {
        return File(getTermuxFilesDir(context), "home")
    }

    fun getTermuxTmp(context: Context): File {
        return File(getTermuxPrefix(context), "tmp")
    }

    fun getEnvironmentVariables(context: Context): Map<String, String> {
        val prefix = getTermuxPrefix(context).absolutePath
        val home = getTermuxHome(context).absolutePath
        
        return mapOf(
            "PREFIX" to prefix,
            "HOME" to home,
            "PATH" to "$prefix/bin",
            "LD_LIBRARY_PATH" to "$prefix/lib",
            "TMPDIR" to getTermuxTmp(context).absolutePath,
            "ANDROID_DATA" to System.getenv("ANDROID_DATA")!!,
            "ANDROID_ROOT" to System.getenv("ANDROID_ROOT")!!,
            "TERM" to "xterm-256color"
        )
    }
}
