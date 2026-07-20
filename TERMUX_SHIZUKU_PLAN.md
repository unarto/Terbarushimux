# RENCANA INTEGRASI TERMINAL TERMUX KE DALAM SHIZUKU (TERMUX-SHIZUKU INTEGRATION PLAN)
## File ID: `TERMUX_SHIZUKU_PLAN.md` (Untuk Agent AI / Developer)

Dokumen ini adalah cetak biru (blueprint) lengkap untuk mengintegrasikan emulator terminal Termux (`termux-app`) langsung ke dalam menu "Use Shizuku in terminal apps" di aplikasi Shizuku Manager. Rencana ini disusun menggunakan prinsip **Arsitektur Bersih (Clean Architecture)**, **Single Responsibility Principle (SRP)**, **Modularisasi**, dan **Jetpack Compose Best Practices**.

---

## 1. STRUKTUR ARSITEKTUR & MODULARISASI (ARCHITECTURAL STRUCTURE)

Untuk mencegah kelas yang membengkak (*Fat Classes*) dan menjaga kepatuhan terhadap SRP, fitur terminal terintegrasi ini dibagi menjadi beberapa modul dan komponen terpisah:

```
[UI Layer (Jetpack Compose)] 
      │ (Membaca UI State)
      ▼
[Presentation: TerminalViewModel]
      │ (Interaksi melalui Interface Abstraksi)
      ▼
[Domain: TerminalRepository (Interface)]
      │
      ├───────────────────────────────┐
      ▼                               ▼
[Data: TerminalRepositoryImpl]  [Data: RishEnvironmentManager]
      │ (Mengelola Proses Shell)      │ (Mengkonfigurasi rish & dex)
      ▼                               ▼
[Terminal Emulator Core (termux-shared / terminal-view)]
```

### Pemisahan Tanggung Jawab (Separation of Concerns):
1. **`RishEnvironmentManager`**: Bertanggung jawab penuh untuk mempersiapkan file `rish` dan `rish_shizuku.dex`, serta mengekspornya ke direktori internal yang aman agar bisa diakses oleh terminal.
2. **`TerminalSessionManager`**: Bertanggung jawab mengelola pembuatan, pemeliharaan, dan penghancuran sesi terminal (menggunakan `TerminalSession` dari Termux).
3. **`TerminalService`**: Sebuah `Foreground Service` Android untuk memastikan sesi shell tetap hidup di latar belakang dan tidak mati ketika pengguna berpindah aplikasi.
4. **`TerminalViewModel`**: Menggunakan `MutableStateFlow` untuk mengekspos state UI ke layar Compose dan menangani interaksi pengguna.
5. **`TerminalScreen` (Compose)**: UI modular berkinerja tinggi yang merender terminal menggunakan `AndroidView` untuk membungkus `TerminalView` Termux.

---

## 2. PENGATURAN MODUL GRADLE (GRADLE CONFIGURATION)

Untuk mengaktifkan pustaka Termux di dalam Shizuku, kita harus mendaftarkan proyek `:terminal-view` dan `:termux-shared` di Gradle Shizuku.

### Langkah 2.1: Edit `/settings.gradle`
Tambahkan modul-modul Termux ke dalam file pengaturan utama:
```groovy
include ':terminal-view'
project(':terminal-view').projectDir = file("termux-app${File.separator}terminal-view")

include ':termux-shared'
project(':termux-shared').projectDir = file("termux-app${File.separator}termux-shared")
```

### Langkah 2.2: Tambahkan Dependensi di `/manager/build.gradle`
```groovy
dependencies {
    // ... dependensi lainnya ...
    implementation project(':terminal-view')
    implementation project(':termux-shared')
}
```

---

## 3. IMPLEMENTASI LAYER DATA & DOMAIN (BACKEND LOGIC)

Sesuai dengan **Aturan Tambahan Pengguna (Additional Instructions)**, semua kode harus didefinisikan menggunakan interface untuk memisahkan logika dari UI.

### Langkah 3.1: Buat Interface Abstraksi `TerminalRepository`
File: `/manager/src/main/java/moe/shizuku/manager/terminal/domain/TerminalRepository.kt`
```kotlin
package moe.shizuku.manager.terminal.domain

import kotlinx.coroutines.flow.StateFlow

interface TerminalRepository {
    val terminalOutput: StateFlow<String>
    fun startSession(arguments: Array<String>, environment: Map<String, String>)
    fun writeInput(data: String)
    fun resizeTerminal(rows: Int, cols: Int)
    fun stopSession()
}
```

### Langkah 3.2: Buat `RishEnvironmentManager` (Logika Ekspor rish & Otomatisasi Shizuku)
Komponen ini secara otomatis menyiapkan environment variabel agar terminal langsung memiliki akses Shizuku tanpa penyiapan manual dari pengguna.
File: `/manager/src/main/java/moe/shizuku/manager/terminal/data/RishEnvironmentManager.kt`
```kotlin
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
```

### Langkah 3.3: Implementasikan `TerminalRepositoryImpl`
Menangani eksekusi proses shell PTY sesungguhnya menggunakan emulator Termux.
File: `/manager/src/main/java/moe/shizuku/manager/terminal/data/TerminalRepositoryImpl.kt`
```kotlin
package moe.shizuku.manager.terminal.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import moe.shizuku.manager.terminal.domain.TerminalRepository
import java.io.InputStream
import java.io.OutputStream

class TerminalRepositoryImpl(
    private val envManager: RishEnvironmentManager
) : TerminalRepository {

    private val _terminalOutput = MutableStateFlow("")
    override val terminalOutput: StateFlow<String> = _terminalOutput

    private var process: Process? = null
    private var outputStream: OutputStream? = null

    override fun startSession(arguments: Array<String>, environment: Map<String, String>) {
        val fullEnv = envManager.prepareRishEnvironment() + environment
        val envArray = fullEnv.map { "${it.key}=${it.value}" }.toTypedArray()

        // Menjalankan shell default dengan rish langsung terinjeksi secara otomatis
        process = Runtime.getRuntime().exec(
            arrayOf("/system/bin/sh", "-c", "rish"),
            envArray,
            null
        )

        outputStream = process?.outputStream
        
        // Membaca output secara asinkron menggunakan Coroutine di background thread
        Thread {
            val buffer = ByteArray(1024)
            val inputStream: InputStream? = process?.inputStream
            try {
                var bytesRead: Int
                while (inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                    val text = String(buffer, 0, bytesRead)
                    _terminalOutput.value += text
                }
            } catch (e: Exception) {
                _terminalOutput.value += "\nSession Ended: ${e.message}\n"
            }
        }.start()
    }

    override fun writeInput(data: String) {
        outputStream?.write(data.toByteArray())
        outputStream?.flush()
    }

    override fun resizeTerminal(rows: Int, cols: Int) {
        // Logika resize PTY termux
    }

    override fun stopSession() {
        process?.destroy()
        process = null
    }
}
```

---

## 4. DESIGN UI JETPACK COMPOSE & OPTIMASI (PRESENTATION LAYER)

Sesuai dengan panduan optimasi, fungsi Composable dirancang sangat ringan, menghindari rekomposisi berlebih (*recomposition dampening*), dan menggunakan `derivedStateOf`.

### Langkah 4.1: Buat `TerminalViewModel`
File: `/manager/src/main/java/moe/shizuku/manager/terminal/presentation/TerminalViewModel.kt`
```kotlin
package moe.shizuku.manager.terminal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.shizuku.manager.terminal.domain.TerminalRepository

class TerminalViewModel(
    private val repository: TerminalRepository
) : ViewModel() {

    val output = repository.terminalOutput
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

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
```

### Langkah 4.2: Buat UI Composable `TerminalScreen` Berkinerja Tinggi
File: `/manager/src/main/java/moe/shizuku/manager/terminal/presentation/TerminalScreen.kt`
```kotlin
package moe.shizuku.manager.terminal.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.view.TerminalView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    onBack: () -> Unit
) {
    val outputState by viewModel.output.collectAsState()

    // Memicu inisialisasi terminal sekali saja dengan 'LaunchedEffect'
    LaunchedEffect(Unit) {
        viewModel.initTerminal()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal Shizuku (Termux Powered)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Membungkus TerminalView Termux menggunakan AndroidView untuk performa maksimal
            AndroidView(
                factory = { context ->
                    TerminalView(context, null).apply {
                        // Hubungkan dengan Sesi Terminal yang dikelola oleh Repository
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                update = { view ->
                    // Perbarui visual view ketika state berubah secara efisien
                }
            )
        }
    }
}
```

### Langkah 4.3: Penerapan **Baseline Profile** untuk Menghindari Lag JIT
Tambahkan aturan pengoptimalan kompilasi di file konfigurasi Baseline Profile untuk memastikan rendering terminal berada di angka 60-120 FPS tanpa jitter:
```txt
# Baseline Profile Rules for Embedded Terminal
HSmoe/shizuku/manager/terminal/presentation/TerminalScreenKt;->TerminalScreen
HScom/termux/view/TerminalView;->onDraw
HSmoe/shizuku/manager/terminal/data/TerminalRepositoryImpl;->writeInput
```

---

## 5. MENGINTEGRASIKAN LAYAR KE MENU "USE SHIZUKU IN TERMINAL APPS"

Untuk mengganti behavior lama (yang hanya menampilkan tutorial pasif) menjadi terminal interaktif aktif:

1. Di file `/manager/src/main/java/moe/shizuku/manager/home/TerminalViewHolder.kt`, ubah fungsi `onClick`:
   ```kotlin
   override fun onClick(v: View) {
       // Buka TerminalActivity baru yang memuat Compose TerminalScreen
       val intent = Intent(v.context, TerminalActivity::class.java)
       v.context.startActivity(intent)
   }
   ```
2. Buat `TerminalActivity` yang memuat `TerminalScreen` Jetpack Compose menggunakan `setContent`.

---

## Ringkasan Eksekusi Masa Depan (AI Agent Handover Note):
Ketika agen AI melanjutkan proyek ini, cukup lakukan langkah berikut:
1. Hubungkan proyek Gradle `:terminal-view` dan `:termux-shared` di `/settings.gradle`.
2. Buat package `moe.shizuku.manager.terminal` dan letakkan kelas-kelas dari bab 3 & 4 ke sana.
3. Deklarasikan `TerminalActivity` di `AndroidManifest.xml`.
4. Run `compile_applet` untuk memverifikasi.
