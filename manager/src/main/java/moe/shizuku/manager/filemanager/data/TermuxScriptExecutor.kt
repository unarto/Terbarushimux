package moe.shizuku.manager.filemanager.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import moe.shizuku.manager.terminal.environment.TermuxEnvironmentManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object TermuxScriptExecutor {
    fun execute(context: Context, command: String, workingDir: String = "/", useShizuku: Boolean = true): Flow<String> = flow {
        
        val envMap = TermuxEnvironmentManager.getEnvironmentVariables(context)
        val envArray = envMap.map { "${it.key}=${it.value}" }.toTypedArray()
        
        val escapedWorkingDir = "'" + workingDir.replace("'", "'\\''") + "'"
        val script = """
            cd $escapedWorkingDir || exit 1
            $command
        """.trimIndent()

        try {
            val process = if (useShizuku && Shizuku.pingBinder()) {
                val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                    "newProcess", 
                    Array<String>::class.java, 
                    Array<String>::class.java, 
                    String::class.java
                )
                newProcessMethod.isAccessible = true
                newProcessMethod.invoke(null, arrayOf("sh", "-c", script), envArray, null) as Process
            } else {
                Runtime.getRuntime().exec(arrayOf("sh", "-c", script), envArray)
            }
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            var line: String? = reader.readLine()
            while (line != null) {
                emit(line)
                line = reader.readLine()
            }
            
            var errorLine: String? = errorReader.readLine()
            while (errorLine != null) {
                emit("ERROR: $errorLine")
                errorLine = errorReader.readLine()
            }
            
            val exitCode = process.waitFor()
            emit("--- Process finished with exit code $exitCode ---")
            
        } catch (e: Exception) {
            emit("Execution Error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}
