# Rencana Implementasi Termux ke Shimux (Shizuku)

Berdasarkan `pandu_gradle_build.txt`, berikut adalah cetak biru teknis untuk mengintegrasikan modul lokal Termux ke dalam arsitektur build aplikasi Shimux. Seluruh konfigurasi difokuskan pada modul `:manager`.

## 1. Konfigurasi Variant dan Manifest Placeholders
Implementasi variabel lingkungan `TERMUX_PACKAGE_VARIANT` dan injeksi nilai placeholders ke dalam `AndroidManifest.xml` untuk mendukung penamaan varian aplikasi Shimux.
- **Variabel**: `TERMUX_PACKAGE_VARIANT` (default: `apt-android-7`).
- **Placeholders**: `TERMUX_APP_NAME` menjadi "Shimux", `TERMUX_API_APP_NAME` menjadi "Shimux:API", dll.
- **Tujuan**: Memastikan konsistensi penamaan dan identitas paket di seluruh ekosistem aplikasi tanpa mengubah namespace asli Shizuku.

## 2. Pembangunan Native (C/C++ CMake)
Eksekusi pembangunan file JNI C/C++ milik arsitektur dasar.
- **Path**: `src/main/jni/CMakeLists.txt`
- **Konfigurasi Tambahan**: `-DANDROID_STL=none` disisipkan ke dalam argumen CMake, dengan versi CMake menunjuk `3.31.0+`.
- **Packaging JNI**: Menggunakan `jniLibs.srcDirs = ['src/main/jniLibs']` dan `useLegacyPackaging true`.
- **Perhatian**: Seperti diatur di `AGENTS.md` dan `GEMINI.md`, dilarang memodifikasi versi SDK NDK/CMake sembarangan demi menghindari resolusi compiler gagal (`[CXX1300]`).

## 3. Integrasi Modul Dependensi Termux
Penggabungan (linking) modul-modul Termux asli yang telah dikloning secara lokal.
- `implementation project(':terminal-view')`
- `implementation project(':termux-shared')`
- `implementation project(':terminal-emulator')`
Langkah ini menempatkan fungsionalitas CLI dasar dari proyek repositori `termux-app` langsung ke dalam modul `manager` Shimux.

## 4. Injeksi Bundle JavaScript Khusus
Menambahkan siklus Gradle khusus (`buildTextEditorJs`) untuk memproses (bundling) aset teks editor berbasis JavaScript.
- **Lokasi Eksekusi**: `src/main/assets/editor`
- **Tugas**: Eksekusi perintah shell `npx esbuild src/EditorManager.js --bundle --outfile=bundle.js`.
- **Integrasi**: Dieksekusi otomatis sebelum task `mergeAssets` selesai.

## 5. Sinkronisasi Otomatis Bootstraps Native Compiler
Siklus download package bootstrap Termux (`downloadBootstraps`) untuk berbagai arsitektur (AArch64, ARM, i686, x86_64).
- Bergantung pada nilai `TERMUX_PACKAGE_VARIANT` (mendukung `apt-android-7` dan `apt-android-5`).
- Verifikasi keamanan checksum berbasis `SHA-256` wajib lolos sebelum build native (CMake) berjalan.
- **Tujuan**: Menyiapkan file `.zip` compiler statis untuk environment shell (su/root/rish).

---
*Peringatan: Seluruh proses di atas harus dikerjakan secara modular dan sekuensial (RSP Rule 1).*
